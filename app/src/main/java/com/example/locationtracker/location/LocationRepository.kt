package com.example.locationtracker.location

import com.example.locationtracker.database.dao.LocationDao
import com.example.locationtracker.database.dao.SessionDao
import com.example.locationtracker.database.entities.LocationPoint
import com.example.locationtracker.database.entities.Session
import kotlinx.coroutines.flow.Flow

class LocationRepository(
    private val locationDao: LocationDao,
    private val sessionDao: SessionDao
) {

    val allPoints: Flow<List<LocationPoint>> = locationDao.getAllPoints()

    suspend fun save(point: LocationPoint) = locationDao.insert(point)

    suspend fun clearAll() = locationDao.deleteAll()

    suspend fun startSession(startTime: Long): Long =
        sessionDao.insert(Session(startTime = startTime))

    suspend fun closeSession(id: Long, startTime: Long, endTime: Long) {
        sessionDao.update(
            Session(
                id = id,
                startTime = startTime,
                endTime = endTime,
                durationMs = endTime - startTime
            )
        )
    }
}
