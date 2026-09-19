package com.tomasthrawat.cloudfileboxr2

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CloudFileBoxScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudFileBoxScreen() {
    val context = LocalContext.current
    val api = remember { CloudApi() }
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<CloudObject>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            busy = true
            try { files = withContext(Dispatchers.IO) { api.list() } }
            catch (t: Throwable) { message = t.message ?: "Request failed" }
            finally { busy = false }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            message = null
            try {
                withContext(Dispatchers.IO) {
                    for (uri in uris) {
                        val name = displayName(context, uri) ?: "file-" + System.currentTimeMillis()
                        val temp = File.createTempFile("upload-", ".tmp", context.cacheDir)
                        context.contentResolver.openInputStream(uri).use { input ->
                            requireNotNull(input).copyTo(temp.outputStream())
                        }
                        val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        api.upload(temp, name, type)
                        temp.delete()
                    }
                }
                refresh()
            } catch (t: Throwable) { message = t.message ?: "Upload failed" }
            finally { busy = false }
        }
    }

    MaterialTheme(colorScheme = dynamicLightColorScheme(context)) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("CloudFileBox R2") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { picker.launch(arrayOf("*/*")) }) { Icon(Icons.Default.Add, "Add files") }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                Text("Your files", style = MaterialTheme.typography.headlineSmall)
                Text("Large files are streamed directly to R2.", style = MaterialTheme.typography.bodyMedium)
                if (busy) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp)) }
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(files, key = { it.key }) { item ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.key, style = MaterialTheme.typography.titleMedium)
                                    Text(formatBytes(item.size), style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        busy = true
                                        try {
                                            val target = File(context.cacheDir, item.key.substringAfterLast('/'))
                                            withContext(Dispatchers.IO) { api.download(item.key, target) }
                                            message = "Downloaded: " + target.name
                                        } catch (t: Throwable) { message = t.message ?: "Download failed" }
                                        finally { busy = false }
                                    }
                                }) { Icon(Icons.Default.Download, "Download") }
                                IconButton(onClick = {
                                    scope.launch {
                                        busy = true
                                        try { withContext(Dispatchers.IO) { api.delete(item.key) }; refresh() }
                                        catch (t: Throwable) { message = t.message ?: "Delete failed" }
                                        finally { busy = false }
                                    }
                                }) { Icon(Icons.Default.Delete, "Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }

private fun formatBytes(value: Long): String = when {
    value >= 1073741824L -> "%.2f GB".format(value / 1073741824.0)
    value >= 1048576L -> "%.2f MB".format(value / 1048576.0)
    value >= 1024L -> "%.1f KB".format(value / 1024.0)
    else -> value.toString() + " B"
}
