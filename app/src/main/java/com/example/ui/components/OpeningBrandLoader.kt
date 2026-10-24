package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RoyalJadeAlpine
import com.example.ui.theme.RoyalJadeForest
import com.example.ui.theme.RoyalJadeObsidian
import com.example.ui.theme.RoyalJadeTealAura
import com.example.ui.theme.SolarChampagne
import kotlinx.coroutines.delay

/**
 * Opening brand loader presenting a crisp, polished entrance animation with Royal Jade
 * ambient halo and Solar Champagne insignia before smoothly fading into the dashboard.
 */
@Composable
fun OpeningBrandLoader(
    visible: Boolean,
    isBn: Boolean,
    onComplete: () -> Unit
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(1200)
            onComplete()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(400, easing = FastOutSlowInEasing))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "brand_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            RoyalJadeObsidian,
                            RoyalJadeForest,
                            Color(0xFF031D18)
                        )
                    )
                )
                .testTag("opening_brand_loader"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Glowing Ambient Halo & Icon
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    RoyalJadeTealAura.copy(alpha = 0.35f),
                                    RoyalJadeAlpine.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        RoyalJadeForest,
                                        RoyalJadeAlpine
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baki_logo),
                            contentDescription = "Baki Bondhu",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Brand Name
                Text(
                    text = if (isBn) "বাকী বন্ধু" else "Baki Bondhu",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SolarChampagne,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = if (isBn) "স্মার্ট ডিজিটাল বাকির হিসাব খাতা" else "Smart Digital Voice Ledger",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Crisp Minimal Spinner
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = SolarChampagne,
                    strokeWidth = 2.5.dp
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Attribution
                Text(
                    text = "DevCenterPoint",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
