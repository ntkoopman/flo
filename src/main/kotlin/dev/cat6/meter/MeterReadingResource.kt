package dev.cat6.meter

import dev.cat6.aemo.Nem12Parser
import dev.cat6.aemo.Record
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import java.io.Reader
import java.sql.Connection
import java.sql.Timestamp
import javax.sql.DataSource
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.postgresql.copy.CopyIn
import org.postgresql.jdbc.PgConnection

@Path("/meter_reading")
class MeterReadingResource {

  @Inject lateinit var dataSource: DataSource

  data class MeterUsageUploadResponse(var numberOfRecords: Long)

  @Operation(summary = "Read NEM12 formatted data and store consumption data in the database")
  @RequestBody(required = true, description = "Meter data in NEM12 format")
  @POST
  @Consumes("text/csv")
  @Produces(MediaType.APPLICATION_JSON)
  @Transactional
  fun upload(body: Reader): MeterUsageUploadResponse {
    var numberOfIntervalRecords = 0L

    dataSource.connection.use { connection ->
      connection.prepareCopyIn("COPY meter_readings (nmi, timestamp, consumption) FROM STDIN") { copyIn ->
        // Last seen data details record
        var dataDetails: Record.DataDetails? = null

        for (record in Nem12Parser.createSequence(body.buffered())) {
          when (record) {
            is Record.DataDetails -> dataDetails = record
            is Record.IntervalData -> {
              numberOfIntervalRecords += 1
              for (value in record.intervalValues) {
                val kiloWattHour = dataDetails!!.unitOfMeasure.toKWH(value.value)
                // TODO: The start time here does not have a timezone, since there is no mention
                //   of timezones in the specification. The data is written to a postgres
                //   `timestamp` column which is also a local timezone, but it might not be the
                //   _same_ local timezone. I strongly suggest determining what the correct
                //   incoming timezone is and storing that data as `timestamp with time zone`
                //   (or any other way that implicitly or explicitly stores a timezone in the
                //   database)
                val timestamp = Timestamp.valueOf(value.startTime)
                // Write data to the DB in TEXT format.
                copyIn.writeToCopy("${dataDetails.nmi}\t$timestamp\t$kiloWattHour\n")
              }
            }
            else -> {
              /* ignored for now */
            }
          }
        }
      }
    }

    return MeterUsageUploadResponse(numberOfIntervalRecords)
  }

  /**
   * Execute a COPY FROM statement.
   *
   * COPY FROM is a PostgreSQL specific method for efficiently inserting bulk data. See
   * [the PostgreSQL documentation](https://www.postgresql.org/docs/current/sql-copy.html) for details.
   */
  private fun Connection.prepareCopyIn(stmt: String, block: (CopyIn) -> Unit) {
    val copyIn = this.unwrap(PgConnection::class.java).copyAPI.copyIn(stmt)
    try {
      block(copyIn)
      copyIn.endCopy()
    } catch (e: Exception) {
      if (copyIn.isActive) copyIn.cancelCopy()
      throw e
    }
  }

  /** Write a string to the copy operation */
  private fun CopyIn.writeToCopy(value: String) {
    var array = value.toByteArray()
    this.writeToCopy(array, 0, array.size)
  }
}
