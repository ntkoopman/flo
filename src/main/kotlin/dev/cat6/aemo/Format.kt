package dev.cat6.aemo

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Format {
    private val DATE8 = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val DATE_TIME_12 = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    private val DATE_TIME_14 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun dateTime12(value: String): LocalDateTime = LocalDateTime.parse(value, DATE_TIME_12)
    fun date8(value: String): LocalDate = LocalDate.parse(value, DATE8)

    fun varChar(maxLength: Int, value: String): String {
        if (value.length > maxLength) throw InvalidFieldFormat("'$value' exceeds the maximum length of $maxLength characters")
        return value
    }
    fun char(length: Int, value: String): String {
        if (value.length !=length) throw InvalidFieldFormat("'$value' does not match expected length $length")
        return value
    }
    fun numeric(maxLength: Int, value: String): Int {
        return value.toInt()
    }

}