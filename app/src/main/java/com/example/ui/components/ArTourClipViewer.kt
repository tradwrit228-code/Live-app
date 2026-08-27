package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.AudioPlayerState
import com.example.model.ArHotspot
import com.example.model.ArTourClip
import com.example.model.HistoricalDossier
import com.example.model.HistoricalEraView
import com.example.model.LandmarkRecognitionResult
import com.example.ui.theme.ImmersiveAmberGold
import com.example.ui.theme.ImmersiveDarkBg
import com.example.ui.theme.ImmersiveGlassBorder
import com.example.ui.theme.ImmersiveGlassCard
import com.example.ui.theme.ImmersiveGlassDark
import com.example.ui.theme.ImmersiveLiveRed
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePurpleDark
import com.example.ui.theme.ImmersivePurpleLight
import com.example.ui.theme.ImmersivePurplePrimary
import com.example.ui.theme.ImmersiveRoseAccent
import com.example.ui.theme.ImmersiveSecondary
import com.example.ui.theme.ImmersiveSecondaryContainer
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceCard
import com.example.ui.theme.ImmersiveSurfaceElevated
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextMuted
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun ArTourClipViewer(
    recognition: LandmarkRecognitionResult,
    dossier: HistoricalDossier,
    tourClip: ArTourClip,
    photoBitmap: Bitmap?,
    audioState: AudioPlayerState,
    selectedHotspot: ArHotspot?,
    selectedEra: HistoricalEraView,
    selectedVoice: String,
    averageRating: Double? = null,
    reviewCount: Int = 0,
    onSelectHotspot: (ArHotspot?) -> Unit,
    onSelectEra: (HistoricalEraView) -> Unit,
    onSelectVoice: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onReplayAudio: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOpenDossier: () -> Unit,
    onOpenReviews: () -> Unit = {},
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVoiceMenuOpen by remember { mutableStateOf(false) }

    // Subtle Ken Burns slow pan/zoom effect for cinematic tour clip feel
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")
    val zoomScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom"
    )

    // Pulse animation for AR hotspot pins
    val pinPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pin_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveDarkBg)
    ) {
        // Landmark Image with Era Travel Visual Filter
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight

            if (photoBitmap != null) {
                val colorFilter = when (selectedEra) {
                    HistoricalEraView.PRESENT -> null
                    HistoricalEraView.MID_CENTURY -> ColorFilter.tint(
                        Color(0xFFE8D8A0),
                        androidx.compose.ui.graphics.BlendMode.Color
                    )
                    HistoricalEraView.ORIGINAL_CONSTRUCTION -> ColorFilter.tint(
                        Color(0xFFC7A779),
                        androidx.compose.ui.graphics.BlendMode.Modulate
                    )
                }

                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = recognition.landmarkName,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(zoomScale)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                // Tap outside closes selected hotspot card
                                onSelectHotspot(null)
                            }
                        }
                )
            }

            // AR Holographic Grid / Vignette Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ImmersiveDarkBg.copy(alpha = 0.85f),
                                Color.Transparent,
                                ImmersiveDarkBg.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // AR Hotspot Pins Overlay positioned dynamically on photo coordinates
            recognition.arHotspots.forEach { hotspot ->
                val posX = containerWidth * hotspot.normalizedX.coerceIn(0.1f, 0.9f)
                val posY = containerHeight * hotspot.normalizedY.coerceIn(0.18f, 0.72f)
                val isSelected = selectedHotspot?.id == hotspot.id

                Box(
                    modifier = Modifier
                        .offset(x = posX - 24.dp, y = posY - 24.dp)
                        .size(48.dp)
                        .clickable {
                            onSelectHotspot(if (isSelected) null else hotspot)
                        }
                        .testTag("hotspot_${hotspot.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    // Concentric pulsing aura
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 44.dp else (32.dp * pinPulse))
                            .background(
                                (if (isSelected) ImmersiveAmberGold else ImmersivePurplePrimary).copy(alpha = 0.35f),
                                CircleShape
                            )
                    )

                    // Core Pin Badge
                    Surface(
                        color = if (isSelected) ImmersiveAmberGold else ImmersiveSurfaceVariant.copy(alpha = 0.95f),
                        shape = CircleShape,
                        border = BorderStroke(2.dp, if (isSelected) Color.White else ImmersivePurplePrimary),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (hotspot.category) {
                                    "Engineering" -> "⚙"
                                    "History" -> "📜"
                                    "Secret Fact" -> "✦"
                                    else -> "🏛"
                                },
                                fontSize = 12.sp,
                                color = if (isSelected) ImmersiveDarkBg else Color.White
                            )
                        }
                    }
                }
            }

            // Floating AR Info Card when a hotspot is tapped
            AnimatedVisibility(
                visible = selectedHotspot != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                selectedHotspot?.let { spot ->
                    Surface(
                        color = ImmersiveGlassDark,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, ImmersivePurplePrimary),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 450.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = ImmersivePurplePrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.5.dp, ImmersivePurplePrimary)
                                ) {
                                    Text(
                                        text = spot.category.uppercase(),
                                        color = ImmersivePurplePrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onSelectHotspot(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = ImmersiveTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = spot.title,
                                color = ImmersiveTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = spot.detail,
                                color = ImmersiveTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        }

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ImmersiveGlassDark,
                shape = CircleShape,
                border = BorderStroke(1.dp, ImmersiveGlassBorder),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onBackToCamera() }
                    .testTag("back_to_camera_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ImmersivePurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = ImmersiveGlassDark,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ImmersiveGlassBorder),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { onOpenReviews() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = recognition.landmarkName,
                            color = ImmersiveTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${recognition.city}",
                            color = ImmersiveAmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (averageRating != null || reviewCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = ImmersiveAmberGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ImmersiveAmberGold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f (%d)", averageRating ?: 5.0, reviewCount),
                                        color = ImmersiveAmberGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Right Action Buttons (Reviews & History Dossier)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reviews Trigger
                Surface(
                    color = ImmersiveGlassDark,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ImmersiveAmberGold.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onOpenReviews() }
                        .testTag("open_reviews_top_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Reviews & Ratings",
                            tint = ImmersiveAmberGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // History Dossier Trigger
                Surface(
                    color = ImmersiveGlassDark,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ImmersivePurplePrimary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onOpenDossier() }
                        .testTag("open_history_dossier_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History Dossier",
                            tint = ImmersivePurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom AR Player & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, ImmersiveDarkBg.copy(alpha = 0.92f), ImmersiveDarkBg)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Era Travel Mode Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ImmersiveSurfaceVariant, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HistoricalEraView.values().forEach { era ->
                    val isSelected = selectedEra == era
                    Surface(
                        color = if (isSelected) ImmersivePurplePrimary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectEra(era) }
                            .testTag("era_tab_${era.name}")
                    ) {
                        Text(
                            text = era.label,
                            color = if (isSelected) ImmersiveDarkBg else ImmersiveTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time AR Audio Wave Visualizer & Narration Subtitles
            Surface(
                color = ImmersiveSurfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ImmersiveGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Audio Waveform Dancing Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        audioState.visualizerAmplitudes.forEach { amp ->
                            val barHeight = (amp * 20.dp.value).dp.coerceIn(3.dp, 20.dp)
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(barHeight)
                                    .background(
                                        if (audioState.isPlaying) ImmersivePurplePrimary else ImmersiveOutline,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Highlighted Subtitle
                    Text(
                        text = tourClip.audioNarrationScript.ifBlank { recognition.shortSummary },
                        color = ImmersiveTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 19.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Seek Slider
                    Slider(
                        value = audioState.progress,
                        onValueChange = { onSeekAudio(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = ImmersivePurplePrimary,
                            activeTrackColor = ImmersivePurplePrimary,
                            inactiveTrackColor = ImmersiveOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )

                    // Audio Controls Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Voice Selector
                        Box {
                            Surface(
                                color = ImmersiveSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ImmersiveGlassBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isVoiceMenuOpen = true }
                                    .testTag("voice_selector_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Voice",
                                        tint = ImmersivePurplePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedVoice,
                                        color = ImmersiveTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isVoiceMenuOpen,
                                onDismissRequest = { isVoiceMenuOpen = false }
                            ) {
                                listOf("Puck", "Charon", "Kore", "Aoede", "Fenrir").forEach { voice ->
                                    DropdownMenuItem(
                                        text = { Text(voice) },
                                        onClick = {
                                             onSelectVoice(voice)
                                            isVoiceMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // Play/Pause/Replay Center Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onReplayAudio() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Replay",
                                    tint = ImmersiveTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Surface(
                                color = ImmersivePurplePrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable { onTogglePlayPause() }
                                    .testTag("play_pause_audio_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (audioState.isPlaying) "Pause" else "Play",
                                        tint = ImmersiveDarkBg,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        // Playback Speed Button
                        Surface(
                            color = ImmersiveSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ImmersiveAmberGold.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val nextSpeed = when (audioState.playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        else -> 1.0f
                                    }
                                    onSpeedChange(nextSpeed)
                                }
                                .testTag("playback_speed_button")
                        ) {
                            Text(
                                text = "${audioState.playbackSpeed}x",
                                color = ImmersiveAmberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Row: History Dossier & Community Reviews
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Community Reviews Button
                Surface(
                    color = ImmersiveSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ImmersiveAmberGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenReviews() }
                        .testTag("explore_reviews_button")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = ImmersiveAmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AVIS (${reviewCount})",
                            color = ImmersiveAmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Deep History Dossier Sheet Button
                Surface(
                    color = ImmersivePurplePrimary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenDossier() }
                        .testTag("explore_full_history_button")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = null,
                            tint = ImmersiveDarkBg,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DOSSIER HISTORIQUE",
                            color = ImmersiveDarkBg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
