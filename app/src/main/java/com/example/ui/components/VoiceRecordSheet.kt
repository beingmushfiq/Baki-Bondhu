package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.BakiViewModel
import com.example.ui.theme.BakiRed
import com.example.ui.theme.CashBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JomaGreen
import com.example.voice.BanglaVoiceParser
import com.example.voice.SpeechState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceRecordSheet(
    viewModel: BakiViewModel,
    onLaunchSystemVoice: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()
    val speechState by viewModel.voiceManager.speechState.collectAsState()
    val audioLevel by viewModel.voiceManager.audioLevel.collectAsState()
    val isBn = uiState.isBangla
    val pending = uiState.pendingConfirmation
    val isEditing = uiState.isEditingParsedResult

    // Animation for pulse effect while listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Title and Reset/Cancel Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pending == null) BakiStrings.speakNow(isBn) else BakiStrings.confirmEntry(isBn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset & Retry Button
                    IconButton(
                        onClick = { viewModel.resetVoiceConfirmation() },
                        modifier = Modifier.testTag("voice_reset_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = if (isBn) "রিসেট করুন" else "Reset",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Cancel & Close Button
                    IconButton(
                        onClick = { viewModel.cancelVoiceAndClose() },
                        modifier = Modifier.testTag("voice_cancel_button")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (isBn) "বাতিল" else "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (pending == null) {
                // 1. LISTENING & RECORDING MODE
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (viewModel.voiceManager.hasApiKey) {
                            if (isBn) "Google Cloud Speech (বাংলা)" else "Google Cloud Speech (bn-BD)"
                        } else {
                            if (isBn) "🎙️ ফ্রি অ্যান্ড্রয়েড স্পিচ (কোনো কী লাগে না)" else "🎙️ Free Native Speech (No Key Needed)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .padding(8.dp)
                        .clickable {
                            if (speechState is SpeechState.Listening) {
                                viewModel.voiceManager.stopListening()
                            } else {
                                if (viewModel.voiceManager.hasApiKey) {
                                    viewModel.voiceManager.startListening()
                                } else {
                                    onLaunchSystemVoice()
                                }
                            }
                        }
                ) {
                    // Outer pulsing ring
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(if (speechState is SpeechState.Listening) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f))
                    )
                    // Inner glowing circle
                    Box(
                        modifier = Modifier
                            .size((80 + (audioLevel * 30)).dp)
                            .clip(CircleShape)
                            .background(if (speechState is SpeechState.Listening) EmeraldPrimary else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (speechState is SpeechState.Transcribing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (speechState is SpeechState.Listening) Icons.Default.Mic else Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time audio waveform visualizer (responds to microphone input levels)
                AudioWaveformVisualizer(
                    audioLevel = audioLevel,
                    isListening = speechState is SpeechState.Listening,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Status text
                val statusText = when (val s = speechState) {
                    is SpeechState.Listening -> if (isBn) "শুনছি... কথা বলা শেষ হলে 'হিসাব দেখুন' চাপুন" else "Listening... speak now, then tap 'Done'"
                    is SpeechState.Transcribing -> s.message
                    is SpeechState.PartialResult -> s.text
                    is SpeechState.Error -> s.message
                    else -> if (isBn) "বলুন: 'রহিম ৪৫০ বাকী' বা 'করিম ভাই ২০০ জমা'" else "Say: 'Rahim 450 credit'"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (speechState is SpeechState.Error) BakiRed else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // High visibility manual entry fallback if speech error occurred
                if (speechState is SpeechState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.openManualScreen((speechState as SpeechState.Error).message)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("voice_error_fallback_button")
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "ভয়েস ব্যর্থ হয়েছে • কীবোর্ডে লিখুন" else "Voice Failed • Switch to Manual Entry",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // If currently listening, show Cancel / Retry / Stop controls row
                if (speechState is SpeechState.Listening) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.cancelVoiceAndClose() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BakiRed),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("voice_listening_cancel_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBn) "বাতিল" else "Cancel", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.voiceManager.restartListening() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("voice_listening_retry_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBn) "আবার বলুন" else "Retry", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.voiceManager.stopListening() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("stop_and_transcribe_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBn) "কথা শেষ" else "Done", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fast test phrases (Bangla simulation bench)
                Text(
                    text = if (isBn) "দ্রুত পরীক্ষার ভয়েস স্যাম্পল:" else "Quick Voice Test Phrases:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val samplePhrases = listOf(
                    "রহিম চাচা ৪৫০ বাকী",
                    "করিম ভাই ২০০ জমা",
                    "সুমন ৭০০ টাকা বাকি",
                    "কামাল ৫০০ টাকা পরিশোধ",
                    "জাহিদ ৩৫০ নগদ বিক্রি"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    samplePhrases.forEach { phrase ->
                        SuggestionChip(
                            onClick = {
                                viewModel.simulateVoicePhrase(phrase)
                            },
                            label = { Text(phrase, fontSize = 13.sp) },
                            modifier = Modifier.testTag("phrase_chip_${phrase.take(4)}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Voice Action Button
                Button(
                    onClick = {
                        if (viewModel.voiceManager.hasApiKey) {
                            viewModel.voiceManager.startListening()
                        } else {
                            onLaunchSystemVoice()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("launch_system_voice_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (viewModel.voiceManager.hasApiKey) {
                            if (isBn) "গুগল ক্লাউড ভয়েস দিয়ে শুনুন" else "Listen with Cloud Voice"
                        } else {
                            if (isBn) "🎙️ ফ্রি গুগল ভয়েস ইনপুট (কোনো কী প্রয়োজন নেই)" else "🎙️ Free Voice Input (No Key Needed)"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (!viewModel.voiceManager.hasApiKey) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBn) "💡 সম্পূর্ণ ফ্রি: গুগল স্পিচ দিয়ে কোনো এপিআই কী ছাড়া সরাসরি বাংলায় বলুন।" else "💡 Free Service: Speak directly in Bangla without requiring any API key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onLaunchSystemVoice,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("listen_again_button")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "সিস্টেম ভয়েস ডায়ালগ ব্যবহার করুন" else "Use System Voice Dialog")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FilledTonalButton(
                    onClick = {
                        val snippet = when (val s = speechState) {
                            is SpeechState.Error -> s.message
                            is SpeechState.PartialResult -> s.text
                            else -> null
                        }
                        viewModel.openManualScreen(snippet)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("fallback_manual_entry_button")
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBn) "কীবোর্ড দিয়ে হিসাব লিখুন (ম্যানুয়াল ফলব্যাক)" else "Type Manually (Fallback)",
                        fontWeight = FontWeight.Bold
                    )
                }

            } else {
                // 2. READ-BACK CONFIRMATION & EDIT MODE
                if (!isEditing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Spoken phrase badge
                            Text(
                                text = "\"${pending.rawText}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Customer Name
                            Text(
                                text = pending.customerName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Transaction Type Badge
                            val typeColor = when (pending.type) {
                                TransactionType.BAKI_SALE -> BakiRed
                                TransactionType.REPAYMENT -> JomaGreen
                                TransactionType.CASH_SALE -> CashBlue
                            }
                            val typeText = when (pending.type) {
                                TransactionType.BAKI_SALE -> BakiStrings.bakiSale(isBn)
                                TransactionType.REPAYMENT -> BakiStrings.repayment(isBn)
                                TransactionType.CASH_SALE -> BakiStrings.cashSale(isBn)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(typeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = typeText,
                                    color = typeColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (pending.isAmountMissing || (pending.amountPaisa ?: 0L) <= 0L) {
                                // Missing Amount Warning Alert
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBn) "টাকার পরিমাণ স্পষ্ট নয়! নিচে টাকার পরিমাণ বেছে নিন বা লিখুন:"
                                            else "Amount not clearly heard. Select amount below or type:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Amount Chips
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    listOf(50, 100, 200, 500, 1000, 2000).forEach { quickAmt ->
                                        val chipText = if (isBn) "৳\u00A0${BanglaVoiceParser.toBanglaDigits("$quickAmt")}" else "৳\u00A0$quickAmt"
                                        SuggestionChip(
                                            onClick = {
                                                viewModel.updatePendingResult(
                                                    customerName = pending.customerName,
                                                    amount = quickAmt.toDouble(),
                                                    type = pending.type
                                                )
                                            },
                                            label = { Text(chipText, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                            modifier = Modifier.padding(horizontal = 3.dp)
                                        )
                                    }
                                }
                            } else {
                                // Amount in large bold
                                val amountStr = BakiStrings.formatMoney(pending.amount, isBn)
                                Text(
                                    text = amountStr,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = typeColor
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Read back audio button
                                OutlinedButton(
                                    onClick = {
                                        val phrase = pending.toReadBackPhrase(isBn)
                                        viewModel.voiceManager.speak(phrase, isBn)
                                    },
                                    modifier = Modifier.testTag("hear_readback_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBn) "পুনরায় ভয়েস শুনুন" else "Hear Read-Back Again")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons (Discard / Edit / Confirm)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetVoiceConfirmation() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BakiRed),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("voice_discard_retry_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBn) "বাতিল" else "Retry", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.toggleEditMode(true) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("correct_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(BakiStrings.correctEntry(isBn), fontSize = 12.sp)
                        }

                        val canConfirm = !pending.isAmountMissing && (pending.amountPaisa ?: 0L) > 0L
                        Button(
                            onClick = { viewModel.confirmPendingTransaction() },
                            enabled = canConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                                .testTag("confirm_save_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = BakiStrings.confirmAndSave(isBn),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                } else {
                    // EDIT MODE (Correction Keypad & Inputs)
                    var editName by remember { mutableStateOf(pending.customerName) }
                    var editAmount by remember { mutableStateOf(pending.amount.toInt().toString()) }
                    var editType by remember { mutableStateOf(pending.type) }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text(BakiStrings.customerName(isBn)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_name_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { editAmount = BanglaVoiceParser.toAsciiDigits(it) },
                            label = { Text(BakiStrings.amountTaka(isBn)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_amount_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Transaction Type Selector
                        Text(
                            text = if (isBn) "লেনদেনের ধরন:" else "Transaction Type:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = editType == TransactionType.BAKI_SALE,
                                onClick = { editType = TransactionType.BAKI_SALE },
                                label = { Text(BakiStrings.bakiSale(isBn)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = editType == TransactionType.REPAYMENT,
                                onClick = { editType = TransactionType.REPAYMENT },
                                label = { Text(BakiStrings.repayment(isBn)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = editType == TransactionType.CASH_SALE,
                                onClick = { editType = TransactionType.CASH_SALE },
                                label = { Text(BakiStrings.cashSale(isBn)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.toggleEditMode(false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(BakiStrings.cancel(isBn))
                            }

                            Button(
                                onClick = {
                                    val amt = editAmount.toDoubleOrNull() ?: 100.0
                                    viewModel.updatePendingParsed(editName, amt, editType)
                                    viewModel.toggleEditMode(false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("apply_edit_button")
                            ) {
                                Text(
                                    text = if (isBn) "ঠিক আছে" else "Done",
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
