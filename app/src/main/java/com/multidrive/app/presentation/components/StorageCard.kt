package com.multidrive.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multidrive.app.domain.model.Account
import com.multidrive.app.presentation.theme.StorageGreen
import com.multidrive.app.presentation.theme.StorageRed
import com.multidrive.app.presentation.theme.StorageYellow

@Composable
fun StorageCard(account: Account) {
    val usedGB = account.storageUsed.toDouble() / (1024.0 * 1024 * 1024)
    val totalGB = if (account.storageQuota > 0) account.storageQuota.toDouble() / (1024.0 * 1024 * 1024) else 15.0
    val rawProgress = (usedGB / totalGB).toFloat().coerceIn(0f, 1f)

    // Animate the progress bar on first compose
    var targetProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(rawProgress) { targetProgress = rawProgress }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800),
        label = "storageProgress"
    )

    val storageColor = when {
        rawProgress > 0.90f -> StorageRed
        rawProgress > 0.70f -> StorageYellow
        else -> StorageGreen
    }

    val percentText = "${(rawProgress * 100).toInt()}%"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = account.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = percentText,
                    style = MaterialTheme.typography.titleMedium,
                    color = storageColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = storageColor,
                trackColor = storageColor.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used: ${"%.2f".format(usedGB)} GB",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Total: ${"%.2f".format(totalGB)} GB",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
