package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralCredit
import com.example.ui.theme.CyanNet
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintCash
import com.example.ui.theme.RoyalJadeAlpine
import com.example.ui.theme.RoyalJadeForest
import com.example.ui.theme.SolarChampagne

/**
 * Modern speed-dial floating action button featuring smooth spring rotation,
 * contextual backdrop dimming, and rapid actions for recording new debts, voice inputs,
 * and creating customer accounts.
 */
@Composable
fun SpeedDialFab(
    isBn: Boolean,
    onVoiceClick: () -> Unit,
    onManualDebtClick: () -> Unit,
    onAddCustomerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Expanded Sub-Actions with Staggered Spring Animation
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                        ),
                exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                        slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = spring(stiffness = Spring.StiffnessHigh)
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action 1: New Customer Account
                    SpeedDialItem(
                        label = if (isBn) "নতুন খরিদ্দার" else "New Customer",
                        icon = Icons.Default.PersonAdd,
                        containerColor = CyanNet,
                        contentColor = Color.White,
                        testTag = "fab_action_new_customer",
                        onClick = {
                            expanded = false
                            onAddCustomerClick()
                        }
                    )

                    // Action 2: Direct Quick Debt Form
                    SpeedDialItem(
                        label = if (isBn) "দ্রুত বাকি এন্ট্রি" else "Record Quick Debt",
                        icon = Icons.Default.EditNote,
                        containerColor = CoralCredit,
                        contentColor = Color.White,
                        testTag = "fab_action_quick_debt",
                        onClick = {
                            expanded = false
                            onManualDebtClick()
                        }
                    )

                    // Action 3: Voice Ledger
                    SpeedDialItem(
                        label = if (isBn) "মুখে বলে হিসাব (ভয়েস)" else "Voice Ledger",
                        icon = Icons.Default.Mic,
                        containerColor = MintCash,
                        contentColor = Color.White,
                        testTag = "fab_action_voice",
                        onClick = {
                            expanded = false
                            onVoiceClick()
                        }
                    )
                }
            }

            // Primary Toggle Button
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = Color.Transparent,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                RoyalJadeForest,
                                RoyalJadeAlpine,
                                EmeraldPrimary
                            )
                        )
                    )
                    .testTag("fab_speed_dial_main")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (expanded) "Close Quick Actions" else "Record New Debt",
                    tint = if (expanded) SolarChampagne else Color.White,
                    modifier = Modifier
                        .size(30.dp)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(end = 6.dp)
    ) {
        // Label Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            modifier = Modifier.padding(end = 10.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Circular Action Button
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = CircleShape,
            modifier = Modifier
                .size(46.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
