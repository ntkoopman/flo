package dev.cat6.aemo

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class InvalidFieldFormat(msg: String, exception: RuntimeException? = null) : RuntimeException(msg, exception)

object Format {

  private val DATE8 = DateTimeFormatter.ofPattern("yyyyMMdd")
  private val DATE_TIME_12 = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
  private val DATE_TIME_14 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  /**
   * Convert a DateTime(12) field
   *
   * See [3.3.2] of the specification
   */
  fun dateTime12(value: String): LocalDateTime {
    var dateTime = LocalDateTime.parse(value, DATE_TIME_12)
    return when {
      // "The time standard for the end of the day is 00:00 of the following day."
      dateTime.toLocalTime().equals(LocalTime.MIDNIGHT) -> dateTime.plusDays(1)
      else -> dateTime
    }
  }

  /**
   * Parse a Date(8) field
   *
   * See [3.3.2] of the specification
   */
  fun date8(value: String): LocalDate = LocalDate.parse(value, DATE8)

  /**
   * Parse a VarChar(n) field
   *
   * This format is not documented in the specification
   */
  fun varChar(maxLength: Int, value: String): String {
    if (value.length > maxLength) {
      throw InvalidFieldFormat("'$value' exceeds the maximum length of $maxLength characters")
    }
    return value
  }

  /**
   * Parse a Char(n) field
   *
   * This format is not documented in the specification
   */
  fun char(length: Int, value: String): String {
    if (value.length != length) {
      throw InvalidFieldFormat("'$value' does not match expected length $length")
    }
    return value
  }

  /**
   * Parse a Numeric(n) field
   *
   * This format is not documented in the specification
   */
  fun numeric(maxLength: Int, value: String): Int {
    if (value.length > maxLength) {
      throw InvalidFieldFormat("'$value' exceeds the maximum length of $maxLength characters")
    }
    return value.toInt()
  }
}
