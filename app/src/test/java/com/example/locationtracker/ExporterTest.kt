package com.example.locationtracker

import com.example.locationtracker.database.entities.LocationPoint
import com.example.locationtracker.export.CSVExporter
import com.example.locationtracker.export.GPXExporter
import com.example.locationtracker.export.JSONExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExporterTest {

    private val samplePoint = LocationPoint(
        id = 1L,
        latitude = 37.422,
        longitude = -122.084,
        timestamp = 1_700_000_000_000L,
        accuracy = 5.0f
    )

    private val twoPoints = listOf(
        samplePoint,
        LocationPoint(id = 2L, latitude = 37.423, longitude = -122.085,
            timestamp = 1_700_000_010_000L, accuracy = 3.5f)
    )

    // ── CSV ──────────────────────────────────────────────────────────────────

    @Test
    fun csv_emptyList_returnsOnlyHeader() {
        val result = CSVExporter.export(emptyList())
        assertEquals("id,latitude,longitude,timestamp,accuracy", result)
    }

    @Test
    fun csv_singlePoint_hasCorrectColumns() {
        val result = CSVExporter.export(listOf(samplePoint))
        val lines = result.lines()
        assertEquals(2, lines.size)
        assertEquals("id,latitude,longitude,timestamp,accuracy", lines[0])
        assertEquals("1,37.422,-122.084,1700000000000,5.0", lines[1])
    }

    @Test
    fun csv_multiplePoints_correctRowCount() {
        val result = CSVExporter.export(twoPoints)
        val lines = result.lines()
        // 1 header + 2 data rows
        assertEquals(3, lines.size)
    }

    // ── GPX ──────────────────────────────────────────────────────────────────

    @Test
    fun gpx_emptyList_containsNoTrkpt() {
        val result = GPXExporter.export(emptyList())
        assertTrue(result.contains("<gpx"))
        assertTrue(!result.contains("<trkpt"))
    }

    @Test
    fun gpx_singlePoint_containsCorrectLatLon() {
        val result = GPXExporter.export(listOf(samplePoint))
        assertTrue(result.contains("lat=\"37.422\""))
        assertTrue(result.contains("lon=\"-122.084\""))
        assertTrue(result.contains("<time>"))
    }

    @Test
    fun gpx_multiplePoints_allTrkptsPresent() {
        val result = GPXExporter.export(twoPoints)
        val count = result.split("<trkpt").size - 1
        assertEquals(2, count)
    }

    // ── JSON ─────────────────────────────────────────────────────────────────

    @Test
    fun json_singlePoint_containsAllFields() {
        val result = JSONExporter.export(listOf(samplePoint))
        assertTrue(result.contains("\"id\":1"))
        assertTrue(result.contains("\"latitude\":37.422"))
        assertTrue(result.contains("\"longitude\":-122.084"))
        assertTrue(result.contains("\"timestamp\":1700000000000"))
        assertTrue(result.contains("\"accuracy\":5.0"))
    }

    @Test
    fun json_multiplePoints_commaSeparated() {
        val result = JSONExporter.export(twoPoints)
        // Two objects means one comma separator between them
        val objectCount = result.split("\"id\"").size - 1
        assertEquals(2, objectCount)
    }

    @Test
    fun json_outputStartsAndEndsWithBrackets() {
        val result = JSONExporter.export(listOf(samplePoint))
        assertTrue(result.trimStart().startsWith("["))
        assertTrue(result.trimEnd().endsWith("]"))
    }
}
