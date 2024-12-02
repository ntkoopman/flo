package dev.cat6.aemo

import dev.cat6.aemo.Record.*
import java.io.BufferedReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class NemParseException(lineNumber: Long, exception: RuntimeException) :
    RuntimeException("Parse exception on line $lineNumber: ${exception.message}", exception)

private class InvalidFileFormat(msg: String) : RuntimeException(msg)

object Nem12Parser {

  private val ALLOWED_TRANSITIONS =
      mapOf(
          null to listOf("100"),
          "100" to listOf("200"),
          "200" to listOf("300"),
          "300" to listOf("200", "300", "400", "500", "900"),
          "400" to listOf("200", "300", "400", "500", "900"),
          "500" to listOf("200", "300", "500", "900"),
          "900" to listOf())

  /**
   * Lazily iterate over NEM12 formatted date
   *
   * This will iterate over individual records contained in the data. If an issue with the data is detected, iteration
   * will terminate with an exception.
   */
  fun createSequence(data: BufferedReader): Sequence<Record> = sequence {
    // Previously seen record (previous line/row)
    var previousRecord: String? = null
    // Last seen data details (200) record
    var currentDetails: DataDetails? = null
    // Last seen interval date from an interval data (300) record
    var previousIntervalDate: LocalDate? = null
    // Current line number for error reporting
    var lineNumber = 1L
    try {
      while (true) {
        val line = data.readLine()
        if (line == null) break // end of file

        // [3.3.1.d] "Commas are not permitted in any data field", so it's safe to split the entire
        // line here.
        val fields = line.split(',')

        val recordIndicator = fields[0]

        // [4.1] Check for allowed records order according to the blocking cycle.
        if (ALLOWED_TRANSITIONS[previousRecord]?.contains(recordIndicator) != true) {
          throw InvalidFileFormat("Unexpected record indicator: $recordIndicator after $previousRecord")
        }

        when (recordIndicator) {
          "100" -> yield(Header.deserialize(fields))
          "200" -> {
            val record = DataDetails.deserialize(fields)
            yield(record)
            currentDetails = record

            // [4.4] Used to check 300 records are in sequential order. The specification is unclear
            // here, it never mentions that the ordering is sequential only within 200 records.
            previousIntervalDate = null
          }
          "300" -> {
            val record = IntervalData.deserialize(fields, details = currentDetails!!)

            // [4.4] 300 records must be presented in date sequential order.
            if (previousIntervalDate != null && record.intervalDate <= previousIntervalDate) {
              throw InvalidFileFormat("Dates must be presented in date sequential order")
            }
            previousIntervalDate = record.intervalDate

            yield(record)
          }
          "400" -> yield(IntervalEvent.deserialize(fields))
          "500" -> yield(B2BDetails.deserialize(fields))
          "900" -> {
            if (fields.size != 1) throw InvalidFileFormat("Incorrect field count for end of data record")
            // Do nothing. Iteration doesn't stop to make sure an exception is thrown on garbage
            // trailing data.
          }
          else -> throw InvalidFileFormat("Unexpected record indicator: $recordIndicator")
        }

        previousRecord = recordIndicator
        lineNumber += 1
      }

      if (previousRecord != "900") throw InvalidFileFormat("Missing end of data record")
    } catch (e: RuntimeException) {
      throw NemParseException(lineNumber, e)
    }
  }
}

data class IntervalValue(val value: BigDecimal, val startTime: LocalDateTime)

sealed interface Record {
  /**
   * Header record (100)
   *
   * See section [4.2] in the specification
   */
  data class Header(val recordIndicator: Int = 100, val dateTime: LocalDateTime) : Record {
    companion object {
      fun deserialize(fields: List<String>): Header {
        if (fields.size != 5) {
          throw InvalidFileFormat("Incorrect field count for header record: ${fields.size}")
        }
        if (fields[1] != "NEM12") {
          throw InvalidFileFormat("Unsupported header version: ${fields[1]}")
        }
        return Header(
            // Version
            dateTime = Format.dateTime12(fields[2])
            // FromParticipant
            // ToParticipant
            )
      }
    }
  }

  /**
   * NMI data details record (200)
   *
   * See section [4.3] in the specification
   */
  data class DataDetails(
      val recordIndicator: Int = 200,
      val nmi: String,
      val intervalLength: Int,
      val unitOfMeasure: UnitOfMeasure
  ) : Record {
    companion object {
      fun deserialize(fields: List<String>): DataDetails {
        if (fields.size != 10) {
          throw InvalidFileFormat("Incorrect field count for data details record: ${fields.size}")
        }
        val intervalLength = Format.numeric(2, fields[8])
        if (intervalLength != 5 && intervalLength != 14 && intervalLength != 30) {
          throw InvalidFieldFormat("IntervalLength is $intervalLength but must be either 5, 15, or 30")
        }
        return DataDetails(
            nmi = fields[1],
            // NMIConfiguration
            // RegisterID
            // NMISuffix
            // MDMDataStreamIdentifier
            // MeterSerialNumber
            unitOfMeasure =
                UnitOfMeasure.valueOf(fields[7].uppercase()), // The allowed values for UOM are not case sensitive
            intervalLength = intervalLength,
            // NextScheduledReadDate
        )
      }
    }
  }

  /**
   * Interval data record (300)
   *
   * See section [4.4] in the specification
   */
  data class IntervalData(
      val recordIndicator: Int = 300,
      val intervalDate: LocalDate,
      val intervalValues: List<IntervalValue>
  ) : Record {
    companion object {
      fun deserialize(fields: List<String>, details: DataDetails): IntervalData {
        val intervalDate = Format.date8(fields[1])

        // [4.4] The number of [IntervalValues] provided must equal 1440 divided by the
        // IntervalLength.
        val intervalLength = details.intervalLength.toLong()
        val intervalCount = 1440 / details.intervalLength
        if (intervalCount != fields.size - 7) {
          throw InvalidFileFormat("Incorrect field count for interval data record")
        }

        // [3.3.3] Interval metering data is presented in time sequence order, with the first
        // Interval for a day
        // being the first Interval after midnight for the interval length that is programmed into
        // the meter.
        val intervalValues =
            (0 ..< intervalCount)
                .map { i ->
                  IntervalValue(
                      value =
                          try {
                            // TODO: Appendix B specifies a maximum precision for some units of
                            // measure
                            BigDecimal(fields[2 + i])
                          } catch (e: RuntimeException) {
                            throw InvalidFieldFormat("Not a valid numeric value", e)
                          },
                      startTime = intervalDate.atStartOfDay().plusMinutes(intervalLength * i))
                }
                .toList()

        return IntervalData(
            intervalDate = intervalDate,
            intervalValues = intervalValues,
            // QualityMethod
            // ReasonCode
            // ReasonDescription
            // UpdateDateTime
            // MSATALoadDateTime
        )
      }
    }
  }

  /**
   * Interval data record (400)
   *
   * See section [4.5] in the specification
   */
  data class IntervalEvent(val recordIndicator: Int = 400) : Record {
    companion object {
      fun deserialize(fields: List<String>): IntervalEvent {
        if (fields.size != 6) {
          throw InvalidFileFormat("Incorrect field count for internal event record: ${fields.size}")
        }
        return IntervalEvent() // TODO
      }
    }
  }

  /**
   * B2B details record (500)
   *
   * See section [4.6] in the specification
   */
  data class B2BDetails(val recordIndicator: Int = 500) : Record {
    companion object {
      fun deserialize(fields: List<String>): B2BDetails {
        if (fields.size != 5) {
          throw InvalidFileFormat("Incorrect field count for b2b record: ${fields.size}")
        }
        return B2BDetails() // TODO
      }
    }
  }
}
