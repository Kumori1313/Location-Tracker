package com.example.locationtracker.export

import com.example.locationtracker.database.entities.LocationPoint

// Exports location history to CSV format.
// Implemented in Phase 9.
object CSVExporter {

    fun export(points: List<LocationPoint>): String {
        val header = "id,latitude,longitude,timestamp,accuracy"
        val rows = points.joinToString("\n") { p ->
            "${p.id},${p.latitude},${p.longitude},${p.timestamp},${p.accuracy}"
        }
        return if (rows.isEmpty()) header else "$header\n$rows"
    }
}
