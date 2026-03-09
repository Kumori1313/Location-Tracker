package com.example.locationtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Help & Tutorial") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 50.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item { SectionHeader("Map Tab") }
                item {
                    HelpCard {
                        HelpParagraph(
                            "The Map tab is your main tracking screen. The map displays your recorded GPS " +
                            "trail as a blue line, with a blue marker showing the most recent fix."
                        )
                        HelpDivider()
                        HelpRow(title = "Zoom In / Zoom Out") {
                            FilledTonalIconButton(onClick = {}) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                            FilledTonalIconButton(onClick = {}) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            HelpNote("Tap + or − to zoom. You can also pinch with two fingers.")
                        }
                        HelpDivider()
                        HelpRow(title = "Center on My Location") {
                            FilledTonalIconButton(onClick = {}) {
                                Icon(Icons.Default.MyLocation, contentDescription = null)
                            }
                            HelpNote("Re-centers the map on your current device location. Located in the top-right corner.")
                        }
                        HelpDivider()
                        HelpParagraph(
                            "Session selector — the dropdown in the top-left corner. Choose a past " +
                            "session to view its trail, or select \"All Sessions\" to see everything."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Route selector — shown at the bottom before you start tracking. Optionally " +
                            "pick a named route so the session is counted toward that route's statistics."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Start / Stop Tracking — the large button at the bottom of the map. Tap once " +
                            "to begin recording your location in the background; tap again to stop and save."
                        )
                        HelpDivider()
                        HelpRow(title = "Help Button") {
                            FilledTonalIconButton(onClick = {}) {
                                Icon(Icons.Default.Help, contentDescription = null)
                            }
                            HelpNote("Opens this help screen. Located at the bottom-right of the map, across from the Google watermark.")
                        }
                    }
                }

                item { SectionHeader("History Tab") }
                item {
                    HelpCard {
                        HelpParagraph(
                            "The History tab lists all completed tracking sessions, sorted by date. " +
                            "Tap a session to expand it and view details such as duration and point count."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Search bar — type any part of a session name or date to filter the list."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Date filter chip — tap the \"Date\" chip to open a date picker. Use the " +
                            "Range / Single toggle next to the chip to switch between a date range and a " +
                            "single day. Tap the × icon to clear the date filter."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Time filter — the Min and Max fields on the right accept durations in " +
                            "hh:mm:ss format. Just type digits; colons are inserted automatically. " +
                            "Only sessions whose travel time falls within the range are shown."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Session menu — tap the ⋮ icon on any session row for Rename and Delete options."
                        )
                    }
                }

                item { SectionHeader("Routes Tab") }
                item {
                    HelpCard {
                        HelpParagraph(
                            "Routes are named journeys defined by an optional start point and end point. " +
                            "Associating a session with a route lets the app track your fastest and average " +
                            "travel times across multiple runs of that route."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Create route — tap the + button in the bottom-right corner. Give the route a " +
                            "name, then optionally set start and end coordinates either by typing lat/lng " +
                            "values or tapping \"Pick on Map\" to place a pin interactively."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Search bar — filters the route list by name."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Route stats — each route card shows the number of recorded runs, the fastest " +
                            "time, and the average time once at least one session has been linked to it."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Route menu — tap the ⋮ icon on any route card for Edit and Delete options."
                        )
                    }
                }

                item { SectionHeader("Map Coordinate Picker") }
                item {
                    HelpCard {
                        HelpParagraph(
                            "Used when setting start or end points for a route. Tap anywhere on the map " +
                            "to drop a pin, or type a location name in the search bar to find a place."
                        )
                        HelpDivider()
                        HelpRow(title = "Zoom In / Zoom Out") {
                            FilledTonalIconButton(onClick = {}) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                            FilledTonalIconButton(onClick = {}) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            HelpNote("Same zoom controls as the main map. Pinch to zoom also works here.")
                        }
                        HelpDivider()
                        HelpParagraph(
                            "Confirm — saves the pinned coordinate. Clear — removes the current pin " +
                            "without closing the picker. Cancel — closes without saving."
                        )
                    }
                }

                item { SectionHeader("Export Tab") }
                item {
                    HelpCard {
                        HelpParagraph(
                            "Export your session data to share or back up. Choose a format and optionally " +
                            "select a specific session, then tap Export."
                        )
                        HelpDivider()
                        HelpParagraph("CSV — spreadsheet-compatible, one row per GPS point with timestamp, latitude, longitude, and accuracy.")
                        HelpParagraph("GPX — standard GPS exchange format compatible with most mapping and fitness apps.")
                        HelpParagraph("JSON — machine-readable format including full session metadata.")
                    }
                }

                item { SectionHeader("Settings Tab") }
                item {
                    HelpCard {
                        HelpParagraph(
                            "Tracking Interval — controls how often a GPS point is recorded while " +
                            "tracking. A shorter interval gives more detail but uses more battery."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Dark Mode — forces the app and map into dark theme, overriding the system setting."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Show Latest Fix Marker — toggles the blue pin that marks your most recent " +
                            "recorded GPS point on the map."
                        )
                        HelpDivider()
                        HelpParagraph(
                            "Default Arrival Radius — the distance from a route's end point that counts " +
                            "as \"arrived\" for the auto-stop feature. Applied when creating new routes."
                        )
                    }
                }
            }
        }
    }
}

// ── Internal helpers ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun HelpCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun HelpParagraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun HelpNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun HelpDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun HelpRow(
    title: String,
    content: @Composable RowScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
