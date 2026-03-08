package com.example.locationtracker.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_points",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("sessionId")]
)
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long? = null,   // null for points recorded before Phase 13
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float
)
