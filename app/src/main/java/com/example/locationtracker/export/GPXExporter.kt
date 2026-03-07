package com.example.locationtracker.export

import com.example.locationtracker.database.entities.LocationPoint
import java.time.Instant

// Exports location history to GPX format.
// Implemented in Phase 9.
object GPXExporter {

    fun export(points: List<LocationPoint>): String {
        val trackPoints = points.joinToString("\n") { p ->
            val time = Instant.ofEpochMilli(p.timestamp)
            """      <trkpt lat="${p.latitude}" lon="${p.longitude}">
        <time>$time</time>
      </trkpt>"""
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="LocationTracker">
  <trk>
    <trkseg>
$trackPoints
    </trkseg>
  </trk>
</gpx>"""
    }
}
