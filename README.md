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

# Phase 19 -- Home Screen Consolidation - Complete

Remove the Home tab entirely and fold its functionality into the Map screen.

-   Move the **Start / Stop tracking** controls onto the Map screen (e.g., a FAB or bottom bar)
-   Move **route selection** (choose an active route before tracking) onto the Map screen
-   Remove `HomeScreen.kt` and its nav entry from `AppNavigation`
-   Update bottom navigation bar from 6 tabs to 5: Map, Routes, History, Export, Settings
-   Ensure the Map screen handles the "no active session" state gracefully (shows current location + route picker, not an empty map)

------------------------------------------------------------------------

# Phase 20 -- Search & Filter (Routes + History) - Complete

Add search bars and filter controls to the Routes and History screens.

### Search Bar

-   Persistent search field at the top of each screen (Routes, History)
-   Filters the displayed list in real-time as the user types
-   Matches against route/session names (case-insensitive)

### Date Filter

-   Calendar date-range picker popup (Material3 `DateRangePicker`)
-   Filters routes/sessions to only those whose date falls within the selected range
-   Clear button resets the date filter

### Travel Time Filter

-   Two text inputs (min / max) accepting durations (e.g., "0:30" or "45" for minutes)
-   Filters routes/sessions whose recorded travel time falls within the typed range
-   Applied alongside the date filter (AND logic)

------------------------------------------------------------------------

# Phase 21 -- Per-Route Travel Time Statistics - Complete

Record and aggregate travel time on a per-route basis, not just per session.

-   When a session is associated with a named route, its duration is attributed to that route
-   `RouteDao` gains queries for: all sessions for a route, fastest duration, average duration
-   Routes screen (or route detail view) displays:
    -   **Fastest time** across all sessions on this route
    -   **Average time** across all sessions on this route
    -   **Session count** for this route
-   Existing per-session stats remain; this adds an aggregated layer on top
-   No new schema columns required if sessions already carry `routeId` FK and `durationMs`; otherwise add `durationMs` to `Session` entity with a Room migration

------------------------------------------------------------------------

# Phase 22 -- Route Map: Center on Current Location - Complete

When the map picker used for selecting route start/end coordinates loads, automatically center on the user's current location.

-   On `MapPickerScreen` (or equivalent) `LaunchedEffect`, request a single location fix via `FusedLocationProviderClient.getCurrentLocation()`
-   Move the camera to the returned `LatLng` at a street-level zoom (e.g., zoom 16) before the user interacts
-   Fall back to a default city center if permission is denied or location is unavailable
-   No new permissions needed (fine location already granted by Phase 3)

------------------------------------------------------------------------

# Phase 23 -- Address & Location Search - Complete

Allow users to specify route start/end points by address or by searching for a place name, in addition to tapping the map.

### Address Input (Geocoding)

-   Text field on the route creation screen accepts a free-form address string
-   On submit, call Android's `Geocoder.getFromLocationName()` (or the Places SDK geocoding endpoint) to resolve to `LatLng`
-   Resolved coordinates are used the same way as a map tap — markers placed, fields populated
-   Show an error if the address cannot be resolved

### Location Search with Proximity Ranking (Fuzzy)

-   Search field triggers suggestions via the **Google Places Autocomplete API** (`places-autocomplete` endpoint) or the **Places SDK for Android** (`PlacesClient.findAutocompletePredictions`)
-   Results are ranked by proximity to the device's current location (pass `locationBias` / `origin` parameter)
-   Fuzzy matching is handled server-side by the Places API (handles typos, partial names)
-   Selecting a suggestion resolves to `LatLng` via `PlacesClient.fetchPlace` and populates the coordinate fields
-   Requires: `com.google.android.libraries.places:places` dependency, Places API key enabled in Google Cloud Console (same key as Maps)

------------------------------------------------------------------------

# Phase 24 -- Play Store Preparation

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

# Phase 25 -- Future Enhancements

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