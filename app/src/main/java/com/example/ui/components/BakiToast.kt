package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class BakiToastType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class BakiToastState(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: BakiToastType = BakiToastType.SUCCESS,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMs: Long = 4000L
)

/**
 * Beautiful, floating Material 3 Toast component with animated entry, status badges,
 * and optional tactile action buttons (such as "পূর্বাবস্থায় ফিরুন / Undo").
 */
@Composable
fun BakiFloatingToastHost(
    toast: BakiToastState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(toast.durationMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn(spring(dampingRatio = 0.8f, stiffness = 400f)) +
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ),
        exit = fadeOut(spring(stiffness = 500f)) +
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = 500f)
                ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("floating_toast_container")
    ) {
        if (toast != null) {
            val (icon, iconTint, badgeBg) = when (toast.type) {
                BakiToastType.SUCCESS -> Triple(
                    Icons.Default.CheckCircle,
                    Color(0xFF10B981),
                    Color(0xFF10B981).copy(alpha = 0.15f)
                )
                BakiToastType.ERROR -> Triple(
                    Icons.Default.Error,
                    Color(0xFFEF4444),
                    Color(0xFFEF4444).copy(alpha = 0.15f)
                )
                BakiToastType.WARNING -> Triple(
                    Icons.Default.Warning,
                    Color(0xFFF59E0B),
                    Color(0xFFF59E0B).copy(alpha = 0.15f)
                )
                BakiToastType.INFO -> Triple(
                    Icons.Default.Info,
                    Color(0xFF06B6D4),
                    Color(0xFF06B6D4).copy(alpha = 0.15f)
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("floating_toast_surface")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = toast.message,
                            color = Color(0xFFF8FAFC),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            modifier = Modifier.testTag("floating_toast_message")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (toast.actionLabel != null && toast.onAction != null) {
                            TextButton(
                                onClick = {
                                    toast.onAction.invoke()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFFF59E0B) // Warm gold
                                ),
                                modifier = Modifier.testTag("floating_toast_action_button")
                            ) {
                                Text(
                                    text = toast.actionLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("floating_toast_close_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
