package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.ShopSettingsEntity
import com.example.ui.BakiStrings
import com.example.ui.theme.EmeraldPrimary
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun SettingsSheet(
    settings: ShopSettingsEntity,
    isBn: Boolean,
    currentUser: FirebaseUser? = null,
    isSyncingCloud: Boolean = false,
    lastCloudBackupAt: Long? = null,
    autoBackupEnabled: Boolean = true,
    isRestorePreviewOpen: Boolean = false,
    restorePreviewData: CloudRestorePreviewData? = null,
    isFetchingRestorePreview: Boolean = false,
    onSignInWithGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onSetAutoBackup: (Boolean) -> Unit = {},
    onBackupToCloud: () -> Unit = {},
    onRestoreFromCloud: () -> Unit = {},
    onDismissRestorePreview: () -> Unit = {},
    onConfirmCloudRestore: (mergeWithLocal: Boolean) -> Unit = {},
    onSave: (shopName: String, ownerName: String, bkash: String, nagad: String, autoReadBack: Boolean) -> Unit,
    onClearDatabase: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var shopName by remember { mutableStateOf(settings.shopName) }
    var ownerName by remember { mutableStateOf(settings.ownerName) }
    var bkash by remember { mutableStateOf(settings.bkashNumber) }
    var nagad by remember { mutableStateOf(settings.nagadNumber) }
    var autoReadBack by remember { mutableStateOf(settings.autoReadBack) }
    var showConfirmClear by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = BakiStrings.settingsTab(isBn),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GOOGLE ACCOUNT & CLOUD BACKUP CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_google_auth_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentUser != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentUser != null) EmeraldPrimary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_google),
                                    contentDescription = "Google",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (currentUser != null) (currentUser.displayName ?: currentUser.email ?: "Google Account")
                                        else (if (isBn) "Google ক্লাউড ব্যাকআপ" else "Google Cloud Backup"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (currentUser != null) {
                                        Text(
                                            text = currentUser.email ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = if (isBn) "অ্যাকাউন্টে ডাটা নিরাপদ রাখুন" else "Sign in to keep ledger safe",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (currentUser != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isBn) "সংযুক্ত ✓" else "Connected ✓",
                                        color = EmeraldPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        if (currentUser == null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isBn) "Google দিয়ে সাইন ইন করলে ফোন নষ্ট বা পরিবর্তন হলেও আপনার দোকানের খাতা ও কাস্টমারদের বাকি কখনো হারাবে না।"
                                else "Sign in with Google to backup your credit accounts and transaction history securely to Google Cloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onSignInWithGoogle,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                if (isSyncingCloud) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isBn) "সংযুক্ত হচ্ছে..." else "Signing in...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_google),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isBn) "Google অ্যাকাউন্ট দিয়ে সাইন ইন করুন" else "Sign in with Google",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Authenticated controls
                            Spacer(modifier = Modifier.height(12.dp))

                            // Auto Backup Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) "স্বয়ংক্রিয় ক্লাউড ব্যাকআপ" else "Auto Cloud Backup",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBn) "লেনদেন হওয়ার সাথে সাথে ক্লাউডে স্বয়ংক্রিয় ব্যাকআপ" else "Automatically syncs after each entry",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoBackupEnabled,
                                    onCheckedChange = { onSetAutoBackup(it) },
                                    modifier = Modifier.testTag("switch_auto_backup")
                                )
                            }

                            if (lastCloudBackupAt != null) {
                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(lastCloudBackupAt))
                                Text(
                                    text = if (isBn) "সর্বশেষ ক্লাউড ব্যাকআপ: $dateStr" else "Last Cloud Backup: $dateStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Backup & Restore Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onBackupToCloud,
                                    enabled = !isSyncingCloud,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("manual_backup_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBn) "ব্যাকআপ নিন" else "Backup Now",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = onRestoreFromCloud,
                                    enabled = !isSyncingCloud && !isFetchingRestorePreview,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("restore_cloud_button")
                                ) {
                                    if (isFetchingRestorePreview) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "যাচাই..." else "Checking...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "রিস্টোর করুন" else "Restore",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Sign Out option
                            TextButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "লগআউট / সাইন আউট" else "Sign Out",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isBn) "দোকানের সাধারণ সেটিংস" else "Store Information",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text(if (isBn) "দোকানের নাম" else "Shop Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text(if (isBn) "দোকানদারের নাম" else "Owner Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bkash,
                    onValueChange = { bkash = it },
                    label = { Text(if (isBn) "ব্যক্তিগত বিকাশ নম্বর (তাগাদা SMS-এর জন্য)" else "Personal bKash Number (for SMS)") },
                    placeholder = { Text("018XXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_bkash_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nagad,
                    onValueChange = { nagad = it },
                    label = { Text(if (isBn) "ব্যক্তিগত নগদ নম্বর" else "Personal Nagad Number") },
                    placeholder = { Text("017XXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_nagad_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Auto readback toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBn) "স্বয়ংক্রিয় ভয়েস নিশ্চিতকরণ (Read-Back)" else "Automatic Voice Read-Back",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isBn) "কথা বলার পর বাংলায় পড়ে শোনাবে" else "Reads back entry aloud before saving",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoReadBack,
                        onCheckedChange = { autoReadBack = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Featured Engineering Attribution Card
                DevCenterPointPartnerCard(isBn = isBn)

                Spacer(modifier = Modifier.height(16.dp))

                // Clear Database Action Button
                OutlinedButton(
                    onClick = { showConfirmClear = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("settings_clear_database_btn")
                ) {
                    Text(if (isBn) "সকল খাতা ও লেনদেন ক্লিয়ার করুন" else "Clear All Customers & Ledger")
                }

                if (showConfirmClear) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showConfirmClear = false },
                        title = { Text(if (isBn) "ডাটাবেজ ক্লিয়ার নিশ্চিতকরণ" else "Confirm Clear Database") },
                        text = { Text(if (isBn) "আপনি কি নিশ্চিত যে সকল খরিদ্দার ও বকেয়া হিসাব মুছে ফেলতে চান?" else "Are you sure you want to delete all customers and transaction records?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showConfirmClear = false
                                    onClearDatabase()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(if (isBn) "হ্যাঁ, মুছে ফেলুন" else "Yes, Clear All", color = Color.White)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showConfirmClear = false }) {
                                Text(if (isBn) "না" else "Cancel")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            onSave(shopName, ownerName, bkash, nagad, autoReadBack)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("save_settings_button")
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

    if (isRestorePreviewOpen) {
        CloudRestorePreviewDialog(
            previewData = restorePreviewData,
            isLoading = isFetchingRestorePreview,
            isBn = isBn,
            onConfirmRestore = { mergeWithLocal ->
                onConfirmCloudRestore(mergeWithLocal)
            },
            onDismiss = onDismissRestorePreview
        )
    }
}
