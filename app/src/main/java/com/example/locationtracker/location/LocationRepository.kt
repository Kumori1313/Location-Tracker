package com.example.locationtracker.location

import com.example.locationtracker.database.dao.LocationDao
import com.example.locationtracker.database.entities.LocationPoint
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val dao: LocationDao) {

    val allPoints: Flow<List<LocationPoint>> = dao.getAllPoints()

    suspend fun save(point: LocationPoint) = dao.insert(point)

    suspend fun clearAll() = dao.deleteAll()
}
