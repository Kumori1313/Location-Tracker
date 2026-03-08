package com.example.locationtracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class SessionWithPointCount(
    @Embedded val session: Session,
    @ColumnInfo(name = "pointCount") val pointCount: Int
)
