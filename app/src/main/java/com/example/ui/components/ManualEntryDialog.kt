package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.theme.EmeraldPrimary
import com.example.voice.BanglaVoiceParser

@Composable
fun ManualEntryDialog(
    isBn: Boolean,
    onSave: (name: String, phone: String, amount: Double, type: TransactionType, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.BAKI_SALE) }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = BakiStrings.manualEntry(isBn),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Type selection chips
                Text(
                    text = if (isBn) "লেনদেনের ধরন:" else "Transaction Type:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = type == TransactionType.BAKI_SALE,
                        onClick = { type = TransactionType.BAKI_SALE },
                        label = { Text(BakiStrings.bakiSale(isBn)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == TransactionType.REPAYMENT,
                        onClick = { type = TransactionType.REPAYMENT },
                        label = { Text(BakiStrings.repayment(isBn)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == TransactionType.CASH_SALE,
                        onClick = { type = TransactionType.CASH_SALE },
                        label = { Text(BakiStrings.cashSale(isBn)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(BakiStrings.customerName(isBn)) },
                    placeholder = { Text(if (isBn) "যেমন: রহিম ভাই" else "e.g. Rahim") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = BanglaVoiceParser.toAsciiDigits(it) },
                    label = { Text(BakiStrings.amountTaka(isBn)) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(BakiStrings.mobileNumber(isBn) + " (ঐচ্ছিক)") },
                    placeholder = { Text("017XXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isBn) "পণ্যের বিবরণ বা নোট" else "Item Note (Optional)") },
                    placeholder = { Text(if (isBn) "যেমন: চাল, ডাল" else "e.g. Rice, Oil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(BakiStrings.cancel(isBn))
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && amt > 0) {
                                onSave(name, phone, amt, type, note)
                            }
                        },
                        enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("submit_manual_entry_button")
                    ) {
                        Text(
                            text = BakiStrings.save(isBn),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
