package com.example.locationtracker.database.entities

import androidx.room.ColumnInfo

/** Aggregated travel-time statistics for a single route, produced by RouteDao.getStatsPerRoute(). */
data class RouteStats(
    @ColumnInfo(name = "routeId")   val routeId: Long,
    @ColumnInfo(name = "sessionCount") val sessionCount: Int,
    @ColumnInfo(name = "fastestMs") val fastestMs: Long,
    @ColumnInfo(name = "averageMs") val averageMs: Long
)
