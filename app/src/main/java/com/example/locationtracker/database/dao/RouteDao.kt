package com.example.locationtracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.locationtracker.database.entities.Route
import com.example.locationtracker.database.entities.RouteStats
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Insert
    suspend fun insert(route: Route): Long

    @Update
    suspend fun update(route: Route)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM routes ORDER BY name ASC")
    fun getAllRoutes(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getById(id: Long): Route?

    @Query("""
        SELECT routeId,
               COUNT(*)        AS sessionCount,
               MIN(durationMs) AS fastestMs,
               CAST(AVG(durationMs) AS INTEGER) AS averageMs
        FROM sessions
        WHERE routeId IS NOT NULL AND endTime > 0
        GROUP BY routeId
    """)
    fun getStatsPerRoute(): Flow<List<RouteStats>>
}
