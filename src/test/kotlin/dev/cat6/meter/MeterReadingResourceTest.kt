package dev.cat6.meter

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import jakarta.inject.Inject
import javax.sql.DataSource
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class MeterReadingResourceTest {

  @Inject lateinit var dataSource: DataSource

  @BeforeEach
  fun beforeEach() {
    dataSource.connection.use { connection ->
      connection.prepareStatement("truncate meter_readings").use { it.execute() }
    }
  }

  @Test
  fun `uploading a valid file store the meter readings`() {
    RestAssured.given()
        .body(
            """
            100,NEM12,200506081149,UNITEDDP,NEMMCO
            200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610
            300,20050301,0,0,0,0,0,0,0,0,0,0,0,0,0.461,0.810,0.568,1.234,1.353,1.507,1.344,1.773,0.848,1.271,0.895,1.327,1.013,1.793,0.988,0.985,0.876,0.555,0.760,0.938,0.566,0.512,0.970,0.760,0.731,0.615,0.886,0.531,0.774,0.712,0.598,0.670,0.587,0.657,0.345,0.231,A,,,20050310121004,20050310182204
            300,20050302,0,0,0,0,0,0,0,0,0,0,0,0,0.235,0.567,0.890,1.123,1.345,1.567,1.543,1.234,0.987,1.123,0.876,1.345,1.145,1.173,1.265,0.987,0.678,0.998,0.768,0.954,0.876,0.845,0.932,0.786,0.999,0.879,0.777,0.578,0.709,0.772,0.625,0.653,0.543,0.599,0.432,0.432,A,,,20050310121004,20050310182204
            300,20050303,0,0,0,0,0,0,0,0,0,0,0,0,0.261,0.310,0.678,0.934,1.211,1.134,1.423,1.370,0.988,1.207,0.890,1.320,1.130,1.913,1.180,0.950,0.746,0.635,0.956,0.887,0.560,0.700,0.788,0.668,0.543,0.738,0.802,0.490,0.598,0.809,0.520,0.670,0.570,0.600,0.289,0.321,A,,,20050310121004,20050310182204
            300,20050304,0,0,0,0,0,0,0,0,0,0,0,0,0.335,0.667,0.790,1.023,1.145,1.777,1.563,1.344,1.087,1.453,0.996,1.125,1.435,1.263,1.085,1.487,1.278,0.768,0.878,0.754,0.476,1.045,1.132,0.896,0.879,0.679,0.887,0.784,0.954,0.712,0.599,0.593,0.674,0.799,0.232,0.612,A,,,20050310121004,20050310182204
            500,O,S01009,20050310121004,
            200,NEM1201010,E1E2,2,E2,,01009,kWh,30,20050610
            300,20050301,0,0,0,0,0,0,0,0,0,0,0,0,0.154,0.460,0.770,1.003,1.059,1.750,1.423,1.200,0.980,1.111,0.800,1.403,1.145,1.173,1.065,1.187,0.900,0.998,0.768,1.432,0.899,1.211,0.873,0.786,1.504,0.719,0.817,0.780,0.709,0.700,0.565,0.655,0.543,0.786,0.430,0.432,A,,,20050310121004,
            300,20050302,0,0,0,0,0,0,0,0,0,0,0,0,0.461,0.810,0.776,1.004,1.034,1.200,1.310,1.342,0.998,1.311,1.095,1.320,1.115,1.436,0.890,1.255,0.916,0.955,0.711,0.780,0.606,0.510,0.905,0.660,0.835,0.798,0.965,1.122,1.004,0.772,0.508,0.670,0.670,0.432,0.415,0.220,A,,,20050310121004,
            300,20050303,0,0,0,0,0,0,0,0,0,0,0,0,0.335,0.667,0.790,1.023,1.145,1.777,1.563,1.344,1.087,1.453,0.996,1.125,1.435,1.263,1.085,1.487,1.278,0.768,0.878,0.754,0.476,1.045,1.132,0.896,0.879,0.679,0.887,0.784,0.954,0.712,0.599,0.593,0.674,0.799,0.232,0.610,A,,,20050310121004,
            300,20050304,0,0,0,0,0,0,0,0,0,0,0,0,0.461,0.415,0.778,0.940,1.191,1.345,1.390,1.222,1.134,1.207,0.877,1.655,1.099,1.625,1.010,0.950,1.255,0.635,0.956,0.880,0.660,0.810,0.878,0.778,0.643,0.838,0.812,0.490,0.598,0.811,0.572,0.417,0.707,0.670,0.290,0.355,A,,,20050310121004,
            500,O,S01009,20050310121004,
            900
            """
                .trimIndent())
        .contentType("text/csv")
        .post("/meter_reading")
        .then()
        .statusCode(200)
        .body("numberOfRecords", CoreMatchers.equalTo(8))

    dataSource.connection.use { connection ->
      connection.prepareStatement("select count(distinct nmi), count(*) from meter_readings").use { statement ->
        statement.executeQuery().use { resultSet ->
          resultSet.next()
          Assertions.assertEquals(2L, resultSet.getLong(1)) // 2 separate nmi
          Assertions.assertEquals(8L * 48L, resultSet.getLong(2)) // 8 records with 48 entries each
        }
      }
    }
  }

  @Test
  fun `uploading an invalid file does not commit any data`() {
    RestAssured.given()
        .body(
            """
            100,NEM12,200506081149,UNITEDDP,NEMMCO
            200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610
            300,20050301,0,0,0,0,0,0,0,0,0,0,0,0,0.461,0.810,0.568,1.234,1.353,1.507,1.344,1.773,0.848,1.271,0.895,1.327,1.013,1.793,0.988,0.985,0.876,0.555,0.760,0.938,0.566,0.512,0.970,0.760,0.731,0.615,0.886,0.531,0.774,0.712,0.598,0.670,0.587,0.657,0.345,0.231,A,,,20050310121004,20050310182204
            """
                .trimIndent())
        .contentType("text/csv")
        .post("/meter_reading")
        .then()
        .statusCode(400)

    dataSource.connection.use { connection ->
      connection.prepareStatement("select count(*) from meter_readings").use { statement ->
        statement.executeQuery().use { resultSet ->
          resultSet.next()
          Assertions.assertEquals(0L, resultSet.getLong(1))
        }
      }
    }
  }

  @Test
  fun `uploading duplicate data fails`() {
    val readings =
        """
        100,NEM12,200506081149,UNITEDDP,NEMMCO
        200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610
        300,20050301,0,0,0,0,0,0,0,0,0,0,0,0,0.461,0.810,0.568,1.234,1.353,1.507,1.344,1.773,0.848,1.271,0.895,1.327,1.013,1.793,0.988,0.985,0.876,0.555,0.760,0.938,0.566,0.512,0.970,0.760,0.731,0.615,0.886,0.531,0.774,0.712,0.598,0.670,0.587,0.657,0.345,0.231,A,,,20050310121004,20050310182204
        900
        """
            .trimIndent()

    RestAssured.given().body(readings).contentType("text/csv").post("/meter_reading").then().statusCode(200)

    RestAssured.given().body(readings).contentType("text/csv").post("/meter_reading").then().statusCode(500)
  }
}