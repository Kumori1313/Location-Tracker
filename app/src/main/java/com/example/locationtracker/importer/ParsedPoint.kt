package com.example.locationtracker.importer

data class ParsedPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float
)
