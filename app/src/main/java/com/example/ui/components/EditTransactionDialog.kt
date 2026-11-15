package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.theme.BakiRed
import com.example.ui.theme.CashBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JomaGreen
import com.example.voice.BanglaVoiceParser

/**
 * Dialog for editing an existing transaction's amount, type, or item details.
 */
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    isBn: Boolean,
    onSave: (oldTx: Transaction, newTx: Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember {
        val formatted = if (isBn) {
            BanglaVoiceParser.toBanglaDigits(transaction.amount.toInt().toString())
        } else {
            transaction.amount.toInt().toString()
        }
        mutableStateOf(formatted)
    }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var noteText by remember { mutableStateOf(transaction.note) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_transaction_dialog")
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
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = if (isBn) "লেনদেন সংশোধন" else "Edit Transaction",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${transaction.customerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Type Chips
                Text(
                    text = if (isBn) "লেনদেনের ধরন:" else "Transaction Type:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedType == TransactionType.BAKI_SALE,
                        onClick = { selectedType = TransactionType.BAKI_SALE },
                        label = { Text(BakiStrings.bakiSale(isBn)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BakiRed.copy(alpha = 0.2f),
                            selectedLabelColor = BakiRed
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.REPAYMENT,
                        onClick = { selectedType = TransactionType.REPAYMENT },
                        label = { Text(BakiStrings.repayment(isBn)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JomaGreen.copy(alpha = 0.2f),
                            selectedLabelColor = JomaGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.CASH_SALE,
                        onClick = { selectedType = TransactionType.CASH_SALE },
                        label = { Text(BakiStrings.cashSale(isBn)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CashBlue.copy(alpha = 0.2f),
                            selectedLabelColor = CashBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(BakiStrings.amountTaka(isBn)) },
                    leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = EmeraldPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_tx_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(if (isBn) "বিবরণ বা পণ্যের তালিকা" else "Note / Items") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_tx_note_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val parsed = BanglaVoiceParser.toAsciiDigits(amountText).toDoubleOrNull()
                        if (parsed == null || parsed <= 0) {
                            errorMessage = if (isBn) "সঠিক টাকার পরিমাণ প্রদান করুন" else "Enter a valid amount"
                            return@Button
                        }
                        val updated = transaction.copy(
                            amountPaisa = com.example.util.Money.takaToPaisa(parsed),
                            type = selectedType,
                            note = noteText.trim()
                        )
                        onSave(transaction, updated)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_edited_transaction_button")
                ) {
                    Text(
                        text = if (isBn) "পরিবর্তন সংরক্ষণ করুন" else "Save Changes",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
