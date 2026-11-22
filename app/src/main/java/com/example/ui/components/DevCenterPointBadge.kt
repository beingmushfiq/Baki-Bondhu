package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary

/**
 * Subtle, tasteful footer badge crediting DevCenterPoint (https://devcenterpoint.com)
 * as the engineering partner behind the application.
 */
@Composable
fun DevCenterPointSubtleFooter(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        onClick = {
            launchDevCenterPointUrl(context)
        },
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier
            .padding(vertical = 12.dp)
            .testTag("devcenterpoint_footer_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.Verified,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Crafted with DevCenterPoint",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "• devcenterpoint.com",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = EmeraldPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * Featured engineering partner card placed in the Settings sheet.
 */
@Composable
fun DevCenterPointPartnerCard(
    isBn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("devcenterpoint_partner_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "DevCenterPoint",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            contentColor = EmeraldPrimary
                        ) {
                            Text(
                                text = "Verified Partner",
                                color = EmeraldPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBn) "কারিগরি সহযোগী • devcenterpoint.com" else "Engineering & Architecture Partner",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isBn) {
                    "বাকী বন্ধু অ্যাপ্লিকেশনটি DevCenterPoint-এর আধুনিক অফলাইন-ফার্স্ট এবং ক্লাউড আর্কিটেকচার দ্বারা সুরক্ষিত ও পরিচালিত।"
                } else {
                    "Baki Bondhu is built with modern offline-first mobile architecture by DevCenterPoint."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { launchDevCenterPointUrl(context) },
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("visit_devcenterpoint_button")
            ) {
                Text(
                    text = if (isBn) "ওয়েবসাইট দেখুন: devcenterpoint.com" else "Visit devcenterpoint.com",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun launchDevCenterPointUrl(context: Context) {
    try {
        val uri = Uri.parse("https://devcenterpoint.com")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("DevCenterPoint", "Failed to launch URL: ${e.message}", e)
    }
}
