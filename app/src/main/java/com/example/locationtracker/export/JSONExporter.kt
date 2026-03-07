package com.example.locationtracker.export

import com.example.locationtracker.database.entities.LocationPoint

object JSONExporter {

    fun export(points: List<LocationPoint>): String {
        val entries = points.joinToString(",\n  ") { p ->
            """{"id":${p.id},"latitude":${p.latitude},"longitude":${p.longitude},"timestamp":${p.timestamp},"accuracy":${p.accuracy}}"""
        }
        return "[\n  $entries\n]"
    }
}
