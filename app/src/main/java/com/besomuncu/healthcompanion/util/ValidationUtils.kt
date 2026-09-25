package com.besomuncu.healthcompanion.util

object ValidationUtils {
    fun isTimeValid(time: String): Boolean {
        val regex = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
        return regex.matches(time)
    }

    fun isNumberValid(number: String): Boolean {
        return number.toIntOrNull() != null && number.toInt() > 0
    }
}
