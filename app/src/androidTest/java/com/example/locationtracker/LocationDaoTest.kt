package com.example.locationtracker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.dao.LocationDao
import com.example.locationtracker.database.entities.LocationPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: LocationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.locationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieve_singlePoint() = runTest {
        val point = LocationPoint(
            latitude = 37.422,
            longitude = -122.084,
            timestamp = 1_700_000_000_000L,
            accuracy = 5.0f
        )
        dao.insert(point)

        val points = dao.getAllPoints().first()
        assertEquals(1, points.size)
        assertEquals(37.422, points[0].latitude, 0.000001)
        assertEquals(-122.084, points[0].longitude, 0.000001)
        assertEquals(1_700_000_000_000L, points[0].timestamp)
        assertEquals(5.0f, points[0].accuracy)
    }

    @Test
    fun getAllPoints_orderedByTimestampAscending() = runTest {
        dao.insert(LocationPoint(latitude = 1.0, longitude = 1.0, timestamp = 2000L, accuracy = 1f))
        dao.insert(LocationPoint(latitude = 2.0, longitude = 2.0, timestamp = 1000L, accuracy = 1f))
        dao.insert(LocationPoint(latitude = 3.0, longitude = 3.0, timestamp = 3000L, accuracy = 1f))

        val points = dao.getAllPoints().first()
        assertEquals(3, points.size)
        assertEquals(1000L, points[0].timestamp)
        assertEquals(2000L, points[1].timestamp)
        assertEquals(3000L, points[2].timestamp)
    }

    @Test
    fun deleteAll_clearsTable() = runTest {
        dao.insert(LocationPoint(latitude = 1.0, longitude = 1.0, timestamp = 1000L, accuracy = 1f))
        dao.insert(LocationPoint(latitude = 2.0, longitude = 2.0, timestamp = 2000L, accuracy = 1f))

        dao.deleteAll()

        val points = dao.getAllPoints().first()
        assertTrue(points.isEmpty())
    }

    @Test
    fun insert_autoGeneratesUniqueIds() = runTest {
        dao.insert(LocationPoint(latitude = 1.0, longitude = 1.0, timestamp = 1000L, accuracy = 1f))
        dao.insert(LocationPoint(latitude = 2.0, longitude = 2.0, timestamp = 2000L, accuracy = 1f))

        val points = dao.getAllPoints().first()
        assertEquals(2, points.size)
        assertTrue(points[0].id != points[1].id)
    }
}
