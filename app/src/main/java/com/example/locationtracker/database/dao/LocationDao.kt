package com.example.locationtracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.locationtracker.database.entities.LocationPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insert(point: LocationPoint)

    @Query("SELECT * FROM location_points ORDER BY timestamp ASC")
    fun getAllPoints(): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getPointsForSession(sessionId: Long): Flow<List<LocationPoint>>

    @Query("DELETE FROM location_points WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)

    @Query("DELETE FROM location_points")
    suspend fun deleteAll()
}
