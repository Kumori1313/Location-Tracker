package com.example.locationtracker.importer

object CSVParser {

    fun parse(content: String): List<ParsedPoint> {
        val lines = content.trim().lines()
        if (lines.size < 2) return emptyList()

        val header = lines[0].split(",").map { it.trim().lowercase() }
        val latIdx = header.indexOf("latitude")
        val lngIdx = header.indexOf("longitude")
        val tsIdx  = header.indexOf("timestamp")
        val accIdx = header.indexOf("accuracy")

        if (latIdx < 0 || lngIdx < 0 || tsIdx < 0) return emptyList()

        return lines.drop(1).mapNotNull { line ->
            val cols = line.split(",")
            try {
                ParsedPoint(
                    latitude  = cols[latIdx].trim().toDouble(),
                    longitude = cols[lngIdx].trim().toDouble(),
                    timestamp = cols[tsIdx].trim().toLong(),
                    accuracy  = if (accIdx >= 0) cols[accIdx].trim().toFloat() else 0f
                )
            } catch (_: Exception) { null }
        }
    }
}
