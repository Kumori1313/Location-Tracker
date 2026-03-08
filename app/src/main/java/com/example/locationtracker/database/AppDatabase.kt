package com.example.locationtracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.locationtracker.database.dao.LocationDao
import com.example.locationtracker.database.dao.RouteDao
import com.example.locationtracker.database.dao.SessionDao
import com.example.locationtracker.database.entities.LocationPoint
import com.example.locationtracker.database.entities.Route
import com.example.locationtracker.database.entities.Session

@Database(
    entities = [LocationPoint::class, Session::class, Route::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun sessionDao(): SessionDao
    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create sessions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL DEFAULT 0,
                        `durationMs` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // SQLite's ALTER TABLE cannot add FK constraints, so recreate
                // location_points with the sessionId FK baked into the schema.
                db.execSQL("""
                    CREATE TABLE `location_points_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER DEFAULT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `accuracy` REAL NOT NULL,
                        FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `location_points_new`
                        (`id`, `sessionId`, `latitude`, `longitude`, `timestamp`, `accuracy`)
                    SELECT `id`, NULL, `latitude`, `longitude`, `timestamp`, `accuracy`
                    FROM `location_points`
                """.trimIndent())
                db.execSQL("DROP TABLE `location_points`")
                db.execSQL("ALTER TABLE `location_points_new` RENAME TO `location_points`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_location_points_sessionId` ON `location_points`(`sessionId`)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `name` TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `routes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `startLat` REAL DEFAULT NULL,
                        `startLng` REAL DEFAULT NULL,
                        `endLat` REAL DEFAULT NULL,
                        `endLng` REAL DEFAULT NULL,
                        `arrivalRadiusMeters` REAL NOT NULL DEFAULT 50
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `routeId` INTEGER DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "location_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }
    }
}
