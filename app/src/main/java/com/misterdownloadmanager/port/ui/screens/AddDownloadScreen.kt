package com.misterdownloadmanager.port.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.port.R

@Composable
fun AddDownloadScreen(
    onAdd: (String) -> Unit,
    onAddTorrent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf("") }
    var magnet by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.label_url)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { if (url.isNotBlank()) onAdd(url) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_add_link))
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = magnet,
            onValueChange = { magnet = it },
            label = { Text(stringResource(R.string.label_magnet)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { if (magnet.isNotBlank()) onAddTorrent(magnet) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_add_torrent))
        }
    }
}
