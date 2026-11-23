package com.misterdownloadmanager.port.core.tools

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object TreeUriPath {
    fun resolveTreeDocumentId(documentId: String, primaryRoot: String): String? {
        val volume = documentId.substringBefore(':')
        if (volume.isBlank()) return null
        val relative = documentId.substringAfter(':', "")
        val root = if (volume == "primary") primaryRoot else "/storage/$volume"
        return if (relative.isBlank()) root else "$root/$relative"
    }

    fun treeUriToPath(uri: Uri, context: Context): String? {
        if (uri.scheme != "content") return null
        if (uri.authority != "com.android.externalstorage.documents") return null
        val documentId = DocumentsContract.getTreeDocumentId(uri) ?: return null
        return resolveTreeDocumentId(documentId, Environment.getExternalStorageDirectory().absolutePath)
    }

    fun isFolderWritable(path: String): Boolean {
        val folder = File(path)
        if (!folder.exists() && !folder.mkdirs()) return false
        if (!folder.isDirectory) return false
        return try {
            val probe = File(folder, ".mdm-write-test")
            probe.createNewFile()
            probe.delete()
            true
        } catch (e: Exception) {
            false
        }
    }
}
