package com.example.locationtracker.importer

import org.w3c.dom.Element
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

object GPXParser {

    fun parse(content: String): List<ParsedPoint> {
        return try {
            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(content.byteInputStream())

            val trkpts = doc.getElementsByTagName("trkpt")
            (0 until trkpts.length).mapNotNull { i ->
                val node = trkpts.item(i) as? Element ?: return@mapNotNull null
                val lat = node.getAttribute("lat").toDoubleOrNull() ?: return@mapNotNull null
                val lon = node.getAttribute("lon").toDoubleOrNull() ?: return@mapNotNull null
                val timeStr = node.getElementsByTagName("time").item(0)?.textContent
                val timestamp = timeStr?.let {
                    runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
                } ?: System.currentTimeMillis()
                ParsedPoint(lat, lon, timestamp, 0f)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
