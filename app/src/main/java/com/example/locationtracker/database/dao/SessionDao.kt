package com.example.locationtracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.locationtracker.database.entities.Session
import com.example.locationtracker.database.entities.SessionWithPointCount
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sessions SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String?)

    // Route stats — fastest first
    @Query("SELECT * FROM sessions WHERE endTime > 0 ORDER BY durationMs ASC")
    fun getAllCompleted(): Flow<List<Session>>

    // Session selector (map) — most recent first
    @Query("SELECT * FROM sessions WHERE endTime > 0 ORDER BY startTime DESC")
    fun getAllCompletedByDate(): Flow<List<Session>>

    // History session cards — most recent first with point counts
    @Query("""
        SELECT s.*, COUNT(lp.id) AS pointCount
        FROM sessions s
        LEFT JOIN location_points lp ON lp.sessionId = s.id
        WHERE s.endTime > 0
        GROUP BY s.id
        ORDER BY s.startTime DESC
    """)
    fun getSessionsWithPointCount(): Flow<List<SessionWithPointCount>>
}
