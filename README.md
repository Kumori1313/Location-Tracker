# Android Location Tracker -- Development Roadmap

## Project Overview

A privacy‑focused Android application that: - Tracks user location -
Stores coordinates locally - Displays routes on a map - Allows exporting
location history (CSV / GPX / JSON)

Tech Stack: - Language: Kotlin - Build System: Kotlin DSL (Gradle) - UI:
Jetpack Compose - Maps: Google Maps SDK - Location: Google Play Services
Location - Background Tasks: WorkManager - Database: Room - IDE: Android
Studio

------------------------------------------------------------------------

# Phase 1 -- Project Setup - Complete

## 1. Install Required Tools

-   Android Studio
-   Android SDK
-   Kotlin
-   Gradle (bundled with Android Studio)

## 2. Create Project - Complete

Android Studio → New Project

Configuration: - Template: Empty Activity - Language: Kotlin - Build
configuration: Kotlin DSL - Minimum SDK: 26+

Project structure:

    LocationTracker/
     ├── build.gradle.kts
     ├── settings.gradle.kts
     └── app/
          ├── build.gradle.kts
          └── src/

------------------------------------------------------------------------

# Phase 2 -- Dependency Setup - Complete

Add dependencies to:

    app/build.gradle.kts

Example dependencies:

``` kotlin
dependencies {

    // Location services
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

}
```

Then sync Gradle.

------------------------------------------------------------------------

# Phase 3 -- Permissions Setup - Complete

Edit:

    AndroidManifest.xml

Add permissions:

``` xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
<uses-permission android:name="android.permission.INTERNET"/>
```

Implement runtime permission requests.

------------------------------------------------------------------------

# Phase 4 -- Core App Architecture - Complete

Recommended architecture:

    app/
     ├── ui/
     │    ├── screens/
     │    └── components/
     │
     ├── location/
     │    ├── LocationService.kt
     │    └── LocationRepository.kt
     │
     ├── database/
     │    ├── entities/
     │    ├── dao/
     │    └── AppDatabase.kt
     │
     ├── workers/
     │    └── LocationWorker.kt
     │
     ├── export/
     │    ├── CSVExporter.kt
     │    └── GPXExporter.kt
     │
     └── MainActivity.kt

------------------------------------------------------------------------

# Phase 5 -- Location Tracking - Complete

Use Google Play Services:

-   FusedLocationProviderClient
-   LocationRequest
-   LocationCallback

Steps:

1.  Initialize location client
2.  Request location updates
3.  Receive GPS coordinates
4.  Store results in database

Fields to store:

-   latitude
-   longitude
-   timestamp
-   accuracy

------------------------------------------------------------------------

# Phase 6 -- Database (Room) - Complete

Create entity:

    LocationPoint

Fields:

-   id
-   latitude
-   longitude
-   timestamp
-   accuracy

Components needed:

-   Entity
-   DAO
-   Database

------------------------------------------------------------------------

# Phase 7 -- Background Tracking - Complete

Use WorkManager for periodic tasks.

Example uses:

-   periodic location logging
-   exporting data
-   cleanup tasks

Note: Continuous GPS tracking requires a Foreground Service.

------------------------------------------------------------------------

# Phase 8 -- Map Visualization - Complete

Use Google Maps SDK.

Features to implement:

-   show current location
-   draw polyline path
-   zoom camera to user
-   display recorded tracks

------------------------------------------------------------------------

# Phase 9 -- Data Export - Complete

Allow exporting location history.

Recommended formats:

### CSV

Easy for spreadsheets.

### GPX

Standard GPS format.

### JSON

Useful for integrations.

Export options:

-   share file
-   save locally
-   upload (optional)

------------------------------------------------------------------------

# Phase 10 -- UI Development (Jetpack Compose) - Complete

Key screens:

## Home Screen

-   Start tracking
-   Stop tracking
-   Current coordinates

## Map Screen

-   Map view
-   Display route

## History Screen

-   List of recorded sessions

## Export Screen

-   Export data
-   Share file

------------------------------------------------------------------------

# Phase 11 -- Testing - Complete

Testing types:

### Unit Tests

-   database logic
-   exporters

### Instrumentation Tests

-   location permission handling
-   map rendering

### Real Device Tests

-   GPS accuracy
-   battery usage
-   background execution

------------------------------------------------------------------------

# Phase 12 -- Optimization - Complete

Improve:

-   battery usage
-   database size
-   location polling frequency

Possible improvements:

-   adaptive location interval
-   track compression
-   background service reliability

## Settings Screen

A 5th navigation tab added alongside Home, Map, History, and Export.
Preferences persisted via Jetpack DataStore.

### Tracking Interval

-   User-configurable recording frequency (e.g. 5s, 10s, 30s, 60s)
-   `LocationService` reads interval from DataStore instead of hardcoded constants
-   Requires: DataStore dependency, `SettingsRepository`

### Dark Mode Toggle

-   Overrides system theme
-   `LocationTrackerTheme` updated to accept a `darkTheme` parameter driven by stored preference

------------------------------------------------------------------------

# Phase 13 -- Route Statistics - Complete

Track and compare travel times across repeated routes.

-   Record the total duration of each tracking session
-   Compare each new run's time against your personal average for that route
-   Display sessions sorted fastest to slowest
-   Requires a `Session` entity (start time, end time, duration) linked to `LocationPoint` records

------------------------------------------------------------------------

# Phase 14 -- Multi-Session Tracking - Complete

Support distinct, named tracking sessions rather than a single continuous log.

-   Each start/stop cycle creates a discrete session with its own metadata (name, date, point count)
-   History screen groups points by session instead of showing a flat list
-   Map screen can display a selected session's route in isolation
-   Likely requires a `Session` table and a foreign key on `LocationPoint`

------------------------------------------------------------------------

# Phase 15 -- Import - Complete

Allow previously exported files to be loaded back into the app.

-   File picker via `ActivityResultContracts.OpenDocument`
-   Per-format parsers for CSV, GPX, and JSON
-   Choice of merging imported points into the existing database or displaying them as a separate read-only session
-   Requires deciding on schema handling (same `LocationPoint` table vs. a separate imported-sessions table)

------------------------------------------------------------------------

# Phase 16 -- Session Management - Complete

Allow users to edit session details directly from the History screen.

-   Rename sessions (requires adding a `name: String?` column to the `Session` entity + Room migration)
-   Delete individual sessions (cascades to remove all associated `LocationPoint` records via FK)
-   Edit UI: long-press or trailing icon on a session card opens a bottom sheet or dialog with rename and delete options

------------------------------------------------------------------------

# Phase 17 -- Route Definition - Complete

Allow users to define named routes with explicit start and end points.

-   New `Route` entity: `id`, `name`, `startLat`, `startLng`, `endLat`, `endLng`, `arrivalRadiusMeters`
-   Routes screen (new nav tab or sub-section) for creating, naming, and managing saved routes
-   When starting tracking, optionally associate a session with a saved route
-   Map screen can display the defined start/end markers for the active route alongside the live polyline

------------------------------------------------------------------------

# Phase 18 -- Auto-Stop on Arrival - Complete

Automatically stop tracking when the device enters a configurable radius around the route destination.

-   `LocationService` compares each incoming fix against the active route's end point using the Haversine formula
-   If distance ≤ arrival radius, broadcasts `ACTION_STOP` to itself and updates the session
-   Arrival radius is user-configurable per route (set during route creation in Phase 17) with a global default in Settings
-   Settings screen gains a **Default Arrival Radius** field (e.g. 25 m, 50 m, 100 m) persisted via DataStore

------------------------------------------------------------------------

# Phase 19 -- Play Store Preparation

Requirements:

-   Privacy policy
-   Location data disclosure
-   App icon
-   Screenshots
-   Store description

Checklist:

-   remove debug logs
-   enable ProGuard
-   release build signing
-   fix lint errors (45 errors deferred from pre-release APK build):
    -   remove `lint { checkReleaseBuilds = false }` block from `app/build.gradle.kts`
    -   `InvalidFragmentVersionForActivityResult` in `MainActivity.kt` is a known false positive for `registerForActivityResult` used in an Activity — suppress with `@SuppressLint("InvalidFragmentVersionForActivityResult")` on the `permissionLauncher` property
    -   address remaining lint warnings before submission

------------------------------------------------------------------------

# Phase 20 -- Future Enhancements

Potential upgrades:

-   offline maps
-   elevation tracking
-   cloud sync
-   iOS version

------------------------------------------------------------------------

# Development Milestone Checklist

## MVP

-   Location tracking
-   Local storage
-   Map display
-   Export data

## Version 1.0

-   polished UI
-   stable background tracking
-   Play Store release

## Version 2.0

-   advanced analytics
-   route stats
-   cross-platform support

------------------------------------------------------------------------

# Recommended Development Order

1.  Project setup
2.  Dependencies
3.  Permissions
4.  Location tracking
5.  Database storage
6.  Map rendering
7.  Background tracking
8.  Data export
9.  UI polish
10. Testing
11. Release