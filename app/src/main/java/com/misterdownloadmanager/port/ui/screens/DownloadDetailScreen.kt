package com.misterdownloadmanager.port.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.port.R
import com.misterdownloadmanager.port.core.model.DownloadItem
import com.misterdownloadmanager.port.core.model.DownloadStatus

@Composable
fun DownloadDetailScreen(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = item.fileName, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = item.url, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(progress = { item.progress() }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.label_status) + ": " + statusText(item))
        Text(text = stringResource(R.string.label_size) + ": " + formatBytes(item.totalBytes))
        Text(text = stringResource(R.string.label_downloaded) + ": " + formatBytes(item.downloadedBytes))
        Text(text = stringResource(R.string.label_speed) + ": " + formatSpeed(item.speedBytesPerSecond))
        Text(text = stringResource(R.string.label_connections) + ": " + item.connections + " / " + item.maxConnections)
        if (item.checksum != null) {
            Text(text = stringResource(R.string.label_checksum) + ": " + item.checksum)
        }
        if (item.error != null) {
            Text(text = item.error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (item.status.canPause()) {
            Button(onClick = onPause, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_pause))
            }
        }
        if (item.status.canResume()) {
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_resume))
            }
        }
        if (item.status == DownloadStatus.ERROR) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_retry))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_delete))
        }
    }
}
