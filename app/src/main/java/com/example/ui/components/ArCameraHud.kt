package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.repository.CuratedCityLandmark
import com.example.model.TourPipelineStep
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
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceCard
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextMuted
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun ArCameraHud(
    pipelineStep: TourPipelineStep,
    sampleLandmarks: List<CuratedCityLandmark>,
    onCapturePhoto: (Bitmap) -> Unit,
    onSelectSample: (CuratedCityLandmark) -> Unit,
    onOpenPassport: () -> Unit,
    onOpenApiKeyDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        onCapturePhoto(bitmap)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hud_scan")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    val reticleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reticle_rot"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveDarkBg)
    ) {
        // Camera Viewfinder or Simulated AR View
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Simulated AR City Viewfinder Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF141218), Color(0xFF2B2930), Color(0xFF141218))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = sampleLandmarks.first().drawableRes),
                    contentDescription = "Simulated Viewfinder",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }
        }

        // AR HUD Overlay Graphics (Grid, Reticle, Laser Scanline)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // AR Corner Brackets
                    val strokeW = 3.dp.toPx()
                    val bracketLen = 38.dp.toPx()
                    val padding = 44.dp.toPx()

                    val left = padding
                    val right = size.width - padding
                    val top = padding + 60.dp.toPx()
                    val bottom = size.height - padding - 160.dp.toPx()

                    val bracketColor = ImmersivePurplePrimary.copy(alpha = 0.9f)

                    // Top Left
                    drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), strokeW)
                    drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLen), strokeW)

                    // Top Right
                    drawLine(bracketColor, Offset(right, top), Offset(right - bracketLen, top), strokeW)
                    drawLine(bracketColor, Offset(right, top), Offset(right, top + bracketLen), strokeW)

                    // Bottom Left
                    drawLine(bracketColor, Offset(left, bottom), Offset(left + bracketLen, bottom), strokeW)
                    drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - bracketLen), strokeW)

                    // Bottom Right
                    drawLine(bracketColor, Offset(right, bottom), Offset(right - bracketLen, bottom), strokeW)
                    drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - bracketLen), strokeW)

                    // Animated Laser Scanning Line
                    val currentLaserY = top + (bottom - top) * laserY
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, ImmersivePurplePrimary, ImmersiveRoseAccent, ImmersivePurplePrimary, Color.Transparent)
                        ),
                        start = Offset(left, currentLaserY),
                        end = Offset(right, currentLaserY),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }
        )

        // Center Rotating AR Reticle
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.Center)
                .rotate(reticleRotation)
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.sweepGradient(
                            listOf(ImmersivePurplePrimary, Color.Transparent, ImmersiveRoseAccent, Color.Transparent, ImmersivePurplePrimary)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Top Status Header HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ImmersiveGlassDark,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, ImmersiveGlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(ImmersiveLiveRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE AR LENS • ACTIVE",
                        color = ImmersivePurplePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Passport / Collection Button
                Surface(
                    color = ImmersiveGlassDark,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ImmersiveAmberGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onOpenPassport() }
                        .testTag("open_passport_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Explorer Passport",
                            tint = ImmersiveAmberGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // API Key Settings
                Surface(
                    color = ImmersiveGlassDark,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ImmersivePurplePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onOpenApiKeyDialog() }
                        .testTag("api_key_settings_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "AI",
                            color = ImmersivePurplePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Bottom Controls HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, ImmersiveDarkBg.copy(alpha = 0.9f), ImmersiveDarkBg)
                    )
                )
                .padding(bottom = 24.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick Sample Landmarks Carousel
            Text(
                text = "OR EXPLORE ICONIC WORLD MONUMENTS",
                color = ImmersiveTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sampleLandmarks) { sample ->
                    Surface(
                        color = ImmersiveSurfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ImmersiveGlassBorder),
                        modifier = Modifier
                            .width(135.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectSample(sample) }
                            .testTag("sample_landmark_${sample.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = sample.drawableRes),
                                contentDescription = sample.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = sample.name,
                                    color = ImmersiveTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = sample.city,
                                    color = ImmersiveAmberGold,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Shutter and Gallery Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pick Photo from Gallery Button
                Surface(
                    color = ImmersiveGlassDark,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ImmersivePurplePrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable { galleryLauncher.launch("image/*") }
                        .testTag("pick_gallery_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Pick Image",
                            tint = ImmersivePurplePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // AI Capture Shutter Button
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .border(
                            BorderStroke(2.dp, ImmersivePurplePrimary.copy(alpha = 0.4f)),
                            CircleShape
                        )
                        .padding(4.dp)
                        .border(
                            BorderStroke(3.dp, ImmersivePurplePrimary),
                            CircleShape
                        )
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (imageCapture != null) {
                                val executor = Executors.newSingleThreadExecutor()
                                imageCapture?.takePicture(
                                    executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val buffer = image.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            image.close()
                                            if (bitmap != null) {
                                                ContextCompat.getMainExecutor(context).execute {
                                                    onCapturePhoto(bitmap)
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            // Fallback to sample landmark bitmap if camera capture fails
                                            ContextCompat.getMainExecutor(context).execute {
                                                val fallback = BitmapFactory.decodeResource(
                                                    context.resources,
                                                    sampleLandmarks.first().drawableRes
                                                )
                                                onCapturePhoto(fallback)
                                            }
                                        }
                                    }
                                )
                            } else {
                                // Fallback photo capture
                                val fallback = BitmapFactory.decodeResource(
                                    context.resources,
                                    sampleLandmarks.first().drawableRes
                                )
                                onCapturePhoto(fallback)
                            }
                        }
                        .testTag("capture_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Take Photo",
                        tint = ImmersiveDarkBg,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Help/Info placeholder
                Surface(
                    color = ImmersiveGlassDark,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ImmersiveAmberGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable {
                            // Quick scan sample
                            onSelectSample(sampleLandmarks.random())
                        }
                        .testTag("quick_random_sample_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = "Quick Tour",
                            tint = ImmersiveAmberGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
