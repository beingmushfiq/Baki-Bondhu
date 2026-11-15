package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary

/**
 * First-run merchant setup dialog to personalize shop name, owner name,
 * mobile payments, and language preference for professional receipts.
 */
@Composable
fun MerchantOnboardingDialog(
    initialShopName: String,
    initialOwnerName: String,
    initialBkash: String,
    initialNagad: String,
    isBn: Boolean,
    onSaveAndStart: (shopName: String, ownerName: String, bkash: String, nagad: String, isBangla: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var shopName by remember { mutableStateOf(if (initialShopName == "আমার দোকান") "" else initialShopName) }
    var ownerName by remember { mutableStateOf(initialOwnerName) }
    var bkashNumber by remember { mutableStateOf(initialBkash) }
    var nagadNumber by remember { mutableStateOf(initialNagad) }
    var isBanglaSelected by remember { mutableStateOf(isBn) }
    var isError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
                .testTag("merchant_onboarding_dialog"),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Hero
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(EmeraldPrimary, EmeraldDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isBanglaSelected) "স্বাগতম বাকী বন্ধুতে!" else "Welcome to Baki Bondhu!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isBanglaSelected) "আপনার ডিজিটাল খাতা প্রস্তুত করতে দোকানের তথ্য দিন"
                    else "Enter your business details to setup your ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Language Selection Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = isBanglaSelected,
                        onClick = { isBanglaSelected = true },
                        label = { Text("বাংলা (Bengali)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isBanglaSelected,
                        onClick = { isBanglaSelected = false },
                        label = { Text("English", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Shop Name
                OutlinedTextField(
                    value = shopName,
                    onValueChange = {
                        shopName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text(if (isBanglaSelected) "দোকানের নাম *" else "Shop Name *") },
                    placeholder = { Text(if (isBanglaSelected) "যেমন: ভাই ভাই স্টোর" else "e.g. Bhai Bhai Store") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(if (isBanglaSelected) "দোকানের নাম প্রয়োজন" else "Shop name is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_shop_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Owner Name
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text(if (isBanglaSelected) "স্বত্বাধিকারীর নাম" else "Owner Name") },
                    placeholder = { Text(if (isBanglaSelected) "যেমন: মোঃ মুশফিকুর রহমান" else "e.g. Md. Mushfiqur Rahman") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_owner_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // bKash Number (for SMS receipts)
                OutlinedTextField(
                    value = bkashNumber,
                    onValueChange = { bkashNumber = it },
                    label = { Text(if (isBanglaSelected) "বিকাশ নম্বর (ঐচ্ছিক)" else "bKash Number (Optional)") },
                    placeholder = { Text("01XXXXXXXXX") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_bkash_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Nagad Number (for SMS receipts)
                OutlinedTextField(
                    value = nagadNumber,
                    onValueChange = { nagadNumber = it },
                    label = { Text(if (isBanglaSelected) "নগদ নম্বর (ঐচ্ছিক)" else "Nagad Number (Optional)") },
                    placeholder = { Text("01XXXXXXXXX") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_nagad_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Start button
                Button(
                    onClick = {
                        val cleanedShopName = shopName.trim()
                        if (cleanedShopName.isBlank()) {
                            isError = true
                        } else {
                            onSaveAndStart(
                                cleanedShopName,
                                ownerName.trim(),
                                bkashNumber.trim(),
                                nagadNumber.trim(),
                                isBanglaSelected
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("onboarding_submit_btn")
                ) {
                    Text(
                        text = if (isBanglaSelected) "খাতা শুরু করুন ➔" else "Start Ledger ➔",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DevCenterPointSubtleFooter()
            }
        }
    }
}
