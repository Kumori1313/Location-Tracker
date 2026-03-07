package com.example.locationtracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.LocationPoint
import com.example.locationtracker.export.CSVExporter
import com.example.locationtracker.export.GPXExporter
import com.example.locationtracker.export.JSONExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ExportScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("") }

    fun export(format: ExportFormat) {
        scope.launch {
            val points = AppDatabase.getInstance(context).locationDao().getAllPoints().first()
            if (points.isEmpty()) {
                statusMessage = "No location data to export."
                return@launch
            }
            shareFile(context, points, format)
            statusMessage = "${format.label} export shared."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Export Location History", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))

        ExportFormat.entries.forEach { format ->
            Button(
                onClick = { export(format) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export ${format.label}")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

enum class ExportFormat(val label: String, val mimeType: String, val fileName: String) {
    CSV("CSV", "text/csv", "location_history.csv"),
    GPX("GPX", "application/gpx+xml", "location_history.gpx"),
    JSON("JSON", "application/json", "location_history.json")
}

private fun shareFile(context: Context, points: List<LocationPoint>, format: ExportFormat) {
    val content = when (format) {
        ExportFormat.CSV -> CSVExporter.export(points)
        ExportFormat.GPX -> GPXExporter.export(points)
        ExportFormat.JSON -> JSONExporter.export(points)
    }

    val exportsDir = File(context.cacheDir, "exports").also { it.mkdirs() }
    val file = File(exportsDir, format.fileName).also { it.writeText(content) }

    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share location history"))
}
