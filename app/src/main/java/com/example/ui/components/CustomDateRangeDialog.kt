package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldPrimary
import com.example.util.DateRangeFilterType
import com.example.util.DateRangeHelper
import com.example.util.DateRangeSelection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun CustomDateRangeDialog(
    initialSelection: DateRangeSelection,
    isBn: Boolean,
    onApplyRange: (DateRangeSelection) -> Unit,
    onDismiss: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val oneDayMs = remember { TimeUnit.DAYS.toMillis(1) }

    var selectedPreset by remember { mutableStateOf(initialSelection.type) }
    var startTimestamp by remember {
        mutableStateOf(initialSelection.startTimestamp ?: (now - 7 * oneDayMs))
    }
    var endTimestamp by remember {
        mutableStateOf(initialSelection.endTimestamp ?: now)
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("custom_date_range_dialog"),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBn) "তারিখ ফিল্টার নির্বাচন" else "Select Date Filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("date_range_dialog_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets
                Text(
                    text = if (isBn) "দ্রুত সময়সীমা (Presets):" else "Quick Ranges:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Today
                    DatePresetChip(
                        label = if (isBn) "আজকে" else "Today",
                        isSelected = selectedPreset == DateRangeFilterType.TODAY,
                        modifier = Modifier.weight(1f).testTag("preset_today_chip"),
                        onClick = {
                            selectedPreset = DateRangeFilterType.TODAY
                            startTimestamp = DateRangeHelper.getStartOfDay(now)
                            endTimestamp = DateRangeHelper.getEndOfDay(now)
                        }
                    )

                    // This Week
                    DatePresetChip(
                        label = if (isBn) "এই সপ্তাহে" else "This Week",
                        isSelected = selectedPreset == DateRangeFilterType.THIS_WEEK,
                        modifier = Modifier.weight(1f).testTag("preset_this_week_chip"),
                        onClick = {
                            selectedPreset = DateRangeFilterType.THIS_WEEK
                            startTimestamp = DateRangeHelper.getStartOfWeek(now)
                            endTimestamp = DateRangeHelper.getEndOfWeek(now)
                        }
                    )

                    // All
                    DatePresetChip(
                        label = if (isBn) "সব লেনদেন" else "All Time",
                        isSelected = selectedPreset == DateRangeFilterType.ALL,
                        modifier = Modifier.weight(1f).testTag("preset_all_chip"),
                        onClick = {
                            selectedPreset = DateRangeFilterType.ALL
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Range Adjusters
                Text(
                    text = if (isBn) "কাস্টম দিন নির্বাচন:" else "Custom Interval Selection:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Offset Buttons (Last 7 Days, Last 15 Days, Last 30 Days)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DatePresetChip(
                        label = if (isBn) "গত ৭ দিন" else "Last 7 Days",
                        isSelected = selectedPreset == DateRangeFilterType.CUSTOM && (endTimestamp - startTimestamp) <= 8 * oneDayMs,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedPreset = DateRangeFilterType.CUSTOM
                            startTimestamp = DateRangeHelper.getStartOfDay(now - 7 * oneDayMs)
                            endTimestamp = DateRangeHelper.getEndOfDay(now)
                        }
                    )
                    DatePresetChip(
                        label = if (isBn) "গত ১৫ দিন" else "Last 15 Days",
                        isSelected = selectedPreset == DateRangeFilterType.CUSTOM && (endTimestamp - startTimestamp) in (13 * oneDayMs)..(16 * oneDayMs),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedPreset = DateRangeFilterType.CUSTOM
                            startTimestamp = DateRangeHelper.getStartOfDay(now - 15 * oneDayMs)
                            endTimestamp = DateRangeHelper.getEndOfDay(now)
                        }
                    )
                    DatePresetChip(
                        label = if (isBn) "গত ৩০ দিন" else "Last 30 Days",
                        isSelected = selectedPreset == DateRangeFilterType.CUSTOM && (endTimestamp - startTimestamp) > 25 * oneDayMs,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedPreset = DateRangeFilterType.CUSTOM
                            startTimestamp = DateRangeHelper.getStartOfDay(now - 30 * oneDayMs)
                            endTimestamp = DateRangeHelper.getEndOfDay(now)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selected Range Display
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBn) "নির্বাচিত সময়কাল" else "Active Period",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when (selectedPreset) {
                                        DateRangeFilterType.ALL -> if (isBn) "সকল লেনদেন (শুরু থেকে আজ)" else "All Time History"
                                        DateRangeFilterType.TODAY -> if (isBn) "আজকের দিন: ${dateFormatter.format(Date(now))}" else "Today: ${dateFormatter.format(Date(now))}"
                                        DateRangeFilterType.THIS_WEEK -> if (isBn) "চলতি সপ্তাহ" else "Current Week"
                                        DateRangeFilterType.THIS_MONTH -> if (isBn) "চলতি মাস" else "Current Month"
                                        DateRangeFilterType.CUSTOM -> "${dateFormatter.format(Date(startTimestamp))} — ${dateFormatter.format(Date(endTimestamp))}"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = if (isBn) "বাতিল" else "Cancel")
                    }

                    Button(
                        onClick = {
                            val finalSelection = when (selectedPreset) {
                                DateRangeFilterType.ALL -> DateRangeSelection(DateRangeFilterType.ALL)
                                DateRangeFilterType.TODAY -> DateRangeSelection(
                                    DateRangeFilterType.TODAY,
                                    DateRangeHelper.getStartOfDay(now),
                                    DateRangeHelper.getEndOfDay(now)
                                )
                                DateRangeFilterType.THIS_WEEK -> DateRangeSelection(
                                    DateRangeFilterType.THIS_WEEK,
                                    DateRangeHelper.getStartOfWeek(now),
                                    DateRangeHelper.getEndOfWeek(now)
                                )
                                DateRangeFilterType.THIS_MONTH -> DateRangeSelection(
                                    DateRangeFilterType.THIS_MONTH,
                                    DateRangeHelper.getStartOfMonth(now),
                                    DateRangeHelper.getEndOfMonth(now)
                                )
                                DateRangeFilterType.CUSTOM -> DateRangeSelection(
                                    DateRangeFilterType.CUSTOM,
                                    DateRangeHelper.getStartOfDay(startTimestamp),
                                    DateRangeHelper.getEndOfDay(endTimestamp)
                                )
                            }
                            onApplyRange(finalSelection)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f).testTag("date_range_apply_btn")
                    ) {
                        Text(
                            text = if (isBn) "প্রয়োগ করুন" else "Apply Filter",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DatePresetChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
