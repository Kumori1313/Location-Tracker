package com.example.locationtracker.importer

import org.json.JSONArray

object JSONParser {

    fun parse(content: String): List<ParsedPoint> {
        return try {
            val array = JSONArray(content)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                runCatching {
                    ParsedPoint(
                        latitude  = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        timestamp = obj.getLong("timestamp"),
                        accuracy  = obj.optDouble("accuracy", 0.0).toFloat()
                    )
                }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
