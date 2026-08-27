package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TourPipelineStep
import com.example.ui.theme.ImmersiveAmberGold
import com.example.ui.theme.ImmersiveDarkBg
import com.example.ui.theme.ImmersiveEmeraldAccent
import com.example.ui.theme.ImmersiveGlassBorder
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePurpleLight
import com.example.ui.theme.ImmersivePurplePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceElevated
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextMuted
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun AiAnalysisLoadingOverlay(
    step: TourPipelineStep,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_anim")
    val pulseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_rot"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveDarkBg.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Rotating Hologram Scanner Ring
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .rotate(pulseRotation)
                    .border(
                        BorderStroke(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(
                                    ImmersivePurplePrimary,
                                    ImmersiveEmeraldAccent,
                                    ImmersiveAmberGold,
                                    ImmersivePurplePrimary
                                )
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (step) {
                        TourPipelineStep.RECOGNIZING -> Icons.Default.Visibility
                        TourPipelineStep.FETCHING_HISTORY -> Icons.Default.Search
                        TourPipelineStep.GENERATING_NARRATION -> Icons.Default.GraphicEq
                        else -> Icons.Default.AutoAwesome
                    },
                    contentDescription = null,
                    tint = when (step) {
                        TourPipelineStep.RECOGNIZING -> ImmersivePurplePrimary
                        TourPipelineStep.FETCHING_HISTORY -> ImmersiveEmeraldAccent
                        TourPipelineStep.GENERATING_NARRATION -> ImmersiveAmberGold
                        else -> ImmersiveTextPrimary
                    },
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = when (step) {
                    TourPipelineStep.RECOGNIZING -> "IDENTIFYING LANDMARK ARCHITECTURE"
                    TourPipelineStep.FETCHING_HISTORY -> "GROUNDING HISTORY WITH GOOGLE SEARCH"
                    TourPipelineStep.GENERATING_NARRATION -> "SYNTHESIZING AR AUDIO TOUR GUIDE"
                    else -> "PROCESSING AI TOUR..."
                },
                color = ImmersiveTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when (step) {
                    TourPipelineStep.RECOGNIZING -> "Analyzing multi-modal neural vision via gemini-3.1-pro-preview..."
                    TourPipelineStep.FETCHING_HISTORY -> "Fetching verified timelines and folklore via gemini-3.5-flash..."
                    TourPipelineStep.GENERATING_NARRATION -> "Composing narrated speech clip via gemini-3.1-flash-tts-preview..."
                    else -> "Please hold steady while the AI reconstructs the site..."
                },
                color = ImmersivePurpleLight.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step Progress Checklist
            Surface(
                color = ImmersiveSurfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ImmersiveGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepItem(
                        label = "1. AI Visual Recognition",
                        model = "gemini-3.1-pro-preview",
                        isCompleted = step.ordinal > TourPipelineStep.RECOGNIZING.ordinal,
                        isActive = step == TourPipelineStep.RECOGNIZING
                    )
                    StepItem(
                        label = "2. Google Search Grounding",
                        model = "gemini-3.5-flash",
                        isCompleted = step.ordinal > TourPipelineStep.FETCHING_HISTORY.ordinal,
                        isActive = step == TourPipelineStep.FETCHING_HISTORY
                    )
                    StepItem(
                        label = "3. AR Voice Tour Narration",
                        model = "gemini-3.1-flash-tts-preview",
                        isCompleted = step.ordinal > TourPipelineStep.GENERATING_NARRATION.ordinal,
                        isActive = step == TourPipelineStep.GENERATING_NARRATION
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    label: String,
    model: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                color = if (isCompleted) ImmersiveEmeraldAccent else if (isActive) ImmersivePurplePrimary else ImmersiveTextMuted,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = model,
                color = ImmersiveTextMuted.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (isCompleted) {
            Text(
                text = "✓ READY",
                color = ImmersiveEmeraldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        } else if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = ImmersivePurplePrimary
            )
        } else {
            Text(
                text = "PENDING",
                color = ImmersiveTextMuted.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
