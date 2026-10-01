package com.multidrive.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multidrive.app.domain.model.DriveFile

@Composable
fun FileRow(
    file: DriveFile,
    accountEmail: String?,
    onFileClick: (DriveFile) -> Unit,
    onRename: (DriveFile) -> Unit,
    onDelete: (DriveFile) -> Unit,
    onShare: (DriveFile) -> Unit,
    onDownload: (DriveFile) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFileClick(file) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                file.isFolder -> Icons.Default.Folder
                file.mimeType.contains("image") -> Icons.Default.Image
                file.mimeType.contains("video") -> Icons.Default.Movie
                file.mimeType.contains("audio") -> Icons.Default.MusicNote
                file.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
                else -> Icons.Default.InsertDriveFile
            },
            contentDescription = null,
            tint = if (file.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(36.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccountBadge(email = accountEmail)
                if (!file.isFolder) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = { menuExpanded = false; onRename(file) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Share Link") },
                    onClick = { menuExpanded = false; onShare(file) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
                if (!file.isFolder) {
                    DropdownMenuItem(
                        text = { Text("Download") },
                        onClick = { menuExpanded = false; onDownload(file) },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuExpanded = false; onDelete(file) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
