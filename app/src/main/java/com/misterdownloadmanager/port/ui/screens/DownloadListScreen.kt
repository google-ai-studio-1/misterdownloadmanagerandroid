package com.misterdownloadmanager.port.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.port.R
import com.misterdownloadmanager.port.core.model.DownloadItem
import com.misterdownloadmanager.port.core.model.DownloadStatus
import com.misterdownloadmanager.port.ui.icons.MdmIcons

@Composable
fun DownloadListScreen(
    items: List<DownloadItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPause: (DownloadItem) -> Unit,
    onResume: (DownloadItem) -> Unit,
    onDelete: (DownloadItem) -> Unit,
    onSelect: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text(stringResource(R.string.action_search)) },
            leadingIcon = { Icon(MdmIcons.Search, contentDescription = stringResource(R.string.action_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (items.isEmpty()) {
            Text(text = stringResource(R.string.empty_downloads), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.empty_downloads_hint), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { item ->
                    DownloadRow(
                        item = item,
                        onPause = { onPause(item) },
                        onResume = { onResume(item) },
                        onDelete = { onDelete(item) },
                        onSelect = { onSelect(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadRow(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    Card(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = if (item.kind.name == "TORRENT") MdmIcons.Torrent else MdmIcons.Download,
                    contentDescription = null
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(text = item.fileName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(text = statusText(item), style = MaterialTheme.typography.bodySmall)
                }
                if (item.status.canPause()) {
                    IconButton(onClick = onPause) {
                        Icon(MdmIcons.Pause, contentDescription = stringResource(R.string.action_pause))
                    }
                }
                if (item.status.canResume()) {
                    IconButton(onClick = onResume) {
                        Icon(MdmIcons.Play, contentDescription = stringResource(R.string.action_resume))
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(MdmIcons.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { item.progress() }, modifier = Modifier.fillMaxWidth())
            Text(
                text = formatBytes(item.downloadedBytes) + " / " + formatBytes(item.totalBytes),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun statusText(item: DownloadItem): String {
    return when (item.status) {
        DownloadStatus.QUEUED -> stringResource(R.string.status_queued)
        DownloadStatus.DOWNLOADING -> stringResource(R.string.status_downloading) + " " + formatSpeed(item.speedBytesPerSecond)
        DownloadStatus.STOPPED -> stringResource(R.string.status_stopped)
        DownloadStatus.COMPLETED -> stringResource(R.string.status_completed)
        DownloadStatus.ERROR -> item.error ?: stringResource(R.string.status_failed)
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 0L) return "Unknown"
    if (bytes < 1024L) return "$bytes B"
    if (bytes < 1024L * 1024L) return "%.1f KB".format(bytes / 1024.0)
    if (bytes < 1024L * 1024L * 1024L) return "%.1f MB".format(bytes / (1024.0 * 1024.0))
    return "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}

fun formatSpeed(bytesPerSecond: Long): String {
    return formatBytes(bytesPerSecond) + "/s"
}
