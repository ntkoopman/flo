package dev.cat6.meter

import dev.cat6.aemo.Nem12Parser
import dev.cat6.aemo.Record
import dev.cat6.aemo.UnitOfMeasure.KWH
import dev.cat6.aemo.UnitOfMeasure.MWH
import dev.cat6.aemo.UnitOfMeasure.WH
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.postgresql.jdbc.PgConnection
import java.io.Reader
import java.math.BigDecimal
import java.sql.Timestamp
import javax.sql.DataSource
import kotlin.use

@Path("/meter_usage")
class MeterUsageResource {

    @Inject
    lateinit var dataSource: DataSource

    data class MeterUsageUploadResponse(var numberOfRecords: Long)

    @POST
    @Consumes("text/csv")
    @Produces(MediaType.APPLICATION_JSON)
    fun upload(body: Reader): MeterUsageUploadResponse {
        var numberOfRecords = 0L
        dataSource.connection.use { connection ->
            val copyIn = connection.unwrap(PgConnection::class.java).copyAPI
                .copyIn("COPY meter_readings (nmi, timestamp, consumption) FROM STDIN")
            var data: Record.DataDetails? = null
            var conversionFactor = BigDecimal.ONE
            for (record in Nem12Parser.createSequence(body.buffered())) {
                when (record) {
                    is Record.DataDetails -> {
                        data = record
                        conversionFactor = when (record.unitOfMeasure) {
                            MWH -> BigDecimal("1000")
                            KWH -> BigDecimal.ONE
                            WH -> BigDecimal("0.001")
                            else -> throw RuntimeException("Unsupported unit of measure: ${record.unitOfMeasure}")
                        }
                    }

                    is Record.IntervalData -> {
                        numberOfRecords += 1
                        for (value in record.intervalValues) {
                            val kiloWattHour = value.value * conversionFactor
                            val row = "${data?.nmi}\t${Timestamp.valueOf(value.startTime)}\t$kiloWattHour\n"
                            var array = row.toByteArray()
                            copyIn.writeToCopy(array, 0, array.size)
                        }
                    }

                    else -> {
                        /* ignored */
                    }
                }
            }
            copyIn.endCopy()
        }

        return MeterUsageUploadResponse(numberOfRecords)
    }
}