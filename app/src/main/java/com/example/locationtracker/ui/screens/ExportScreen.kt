package com.example.locationtracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.LocationPoint
import com.example.locationtracker.database.entities.Session
import com.example.locationtracker.export.CSVExporter
import com.example.locationtracker.export.GPXExporter
import com.example.locationtracker.export.JSONExporter
import com.example.locationtracker.importer.CSVParser
import com.example.locationtracker.importer.GPXParser
import com.example.locationtracker.importer.JSONParser
import com.example.locationtracker.importer.ParsedPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ExportScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Export state
    var exportStatus by remember { mutableStateOf("") }

    // Import state
    var importStatus by remember { mutableStateOf("") }
    var importFilename by remember { mutableStateOf("") }
    var parsedPoints by remember { mutableStateOf<List<ParsedPoint>>(emptyList()) }
    var showImportDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            importStatus = "Reading file…"
            val (filename, points) = withContext(Dispatchers.IO) {
                val name = getFilename(context, uri)
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: return@withContext name to emptyList()
                val format = detectFormat(name, context.contentResolver.getType(uri))
                val parsed = when (format) {
                    "csv"  -> CSVParser.parse(content)
                    "gpx"  -> GPXParser.parse(content)
                    "json" -> JSONParser.parse(content)
                    else   -> emptyList()
                }
                name to parsed
            }
            importFilename = filename
            if (points.isEmpty()) {
                importStatus = "No points found or unsupported format."
            } else {
                parsedPoints = points
                importStatus = ""
                showImportDialog = true
            }
        }
    }

    fun doImport(asSession: Boolean) {
        val pointsToImport = parsedPoints
        showImportDialog = false
        parsedPoints = emptyList()
        scope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(context)
                if (asSession && pointsToImport.isNotEmpty()) {
                    val startTime = pointsToImport.minOf { it.timestamp }
                    val endTime   = pointsToImport.maxOf { it.timestamp }
                    val sessionId = db.sessionDao().insert(Session(startTime = startTime))
                    pointsToImport.forEach { p ->
                        db.locationDao().insert(
                            LocationPoint(
                                sessionId = sessionId,
                                latitude  = p.latitude,
                                longitude = p.longitude,
                                timestamp = p.timestamp,
                                accuracy  = p.accuracy
                            )
                        )
                    }
                    db.sessionDao().update(
                        Session(
                            id         = sessionId,
                            startTime  = startTime,
                            endTime    = endTime,
                            durationMs = endTime - startTime
                        )
                    )
                } else {
                    pointsToImport.forEach { p ->
                        db.locationDao().insert(
                            LocationPoint(
                                latitude  = p.latitude,
                                longitude = p.longitude,
                                timestamp = p.timestamp,
                                accuracy  = p.accuracy
                            )
                        )
                    }
                }
            }
            importStatus = "Imported ${pointsToImport.size} points successfully."
        }
    }

    // Import dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                parsedPoints = emptyList()
            },
            title = { Text("Import \"$importFilename\"") },
            text  = {
                Text(
                    "${parsedPoints.size} points found.\n\n" +
                    "Import as a new session (appears in History and Map) " +
                    "or merge directly into your existing data?"
                )
            },
            confirmButton = {
                Button(onClick = { doImport(asSession = true) }) {
                    Text("As New Session")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(onClick = { doImport(asSession = false) }) {
                        Text("Merge")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        showImportDialog = false
                        parsedPoints = emptyList()
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Export ────────────────────────────────────────────────────────
        Text("Export Location History", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        ExportFormat.entries.forEach { format ->
            Button(
                onClick = {
                    scope.launch {
                        val points = AppDatabase.getInstance(context)
                            .locationDao().getAllPoints().first()
                        if (points.isEmpty()) {
                            exportStatus = "No location data to export."
                            return@launch
                        }
                        shareFile(context, points, format)
                        exportStatus = "${format.label} export shared."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export ${format.label}")
            }
            Spacer(Modifier.height(12.dp))
        }

        if (exportStatus.isNotEmpty()) {
            Text(exportStatus, style = MaterialTheme.typography.bodyMedium)
        }

        // ── Import ────────────────────────────────────────────────────────
        Spacer(Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Import File", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Import a previously exported CSV, GPX, or JSON file.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pick File to Import")
        }

        if (importStatus.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(importStatus, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

enum class ExportFormat(val label: String, val mimeType: String, val fileName: String) {
    CSV("CSV", "text/csv", "location_history.csv"),
    GPX("GPX", "application/gpx+xml", "location_history.gpx"),
    JSON("JSON", "application/json", "location_history.json")
}

private fun getFilename(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: uri.lastPathSegment
        ?: "file"

private fun detectFormat(filename: String, mimeType: String?): String? {
    val lower = filename.lowercase()
    return when {
        lower.endsWith(".csv")  -> "csv"
        lower.endsWith(".gpx")  -> "gpx"
        lower.endsWith(".json") -> "json"
        mimeType?.contains("csv")  == true -> "csv"
        mimeType?.contains("gpx")  == true -> "gpx"
        mimeType?.contains("json") == true -> "json"
        else -> null
    }
}

private fun shareFile(context: Context, points: List<LocationPoint>, format: ExportFormat) {
    val content = when (format) {
        ExportFormat.CSV  -> CSVExporter.export(points)
        ExportFormat.GPX  -> GPXExporter.export(points)
        ExportFormat.JSON -> JSONExporter.export(points)
    }
    val exportsDir = File(context.cacheDir, "exports").also { it.mkdirs() }
    val file = File(exportsDir, format.fileName).also { it.writeText(content) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share location history"))
}
