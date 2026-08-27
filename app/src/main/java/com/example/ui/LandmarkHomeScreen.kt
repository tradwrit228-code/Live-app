package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.TourPipelineStep
import com.example.ui.components.AiAnalysisLoadingOverlay
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.ArCameraHud
import com.example.ui.components.ArTourClipViewer
import com.example.ui.components.ExplorerPassportSheet
import com.example.ui.components.HistoryDossierSheet
import com.example.ui.components.LandmarkReviewsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandmarkHomeScreen(
    viewModel: LandmarkViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val pipelineStep by viewModel.pipelineStep.collectAsState()
    val currentRecognition by viewModel.currentRecognition.collectAsState()
    val currentDossier by viewModel.currentDossier.collectAsState()
    val currentTourClip by viewModel.currentTourClip.collectAsState()
    val currentPhotoBitmap by viewModel.currentPhotoBitmap.collectAsState()
    val selectedHotspot by viewModel.selectedHotspot.collectAsState()
    val selectedEra by viewModel.selectedEra.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val savedTours by viewModel.savedTours.collectAsState()
    val currentReviews by viewModel.currentReviews.collectAsState()
    val currentAverageRating by viewModel.currentAverageRating.collectAsState()
    val currentReviewCount by viewModel.currentReviewCount.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showHistoryDossier by remember { mutableStateOf(false) }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var showPassportSheet by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            pipelineStep == TourPipelineStep.IDLE || currentRecognition == null || currentDossier == null || currentTourClip == null -> {
                ArCameraHud(
                    pipelineStep = pipelineStep,
                    sampleLandmarks = viewModel.sampleLandmarks,
                    onCapturePhoto = { bitmap ->
                        viewModel.processCapturedPhoto(bitmap)
                    },
                    onSelectSample = { sample ->
                        viewModel.selectSampleLandmark(sample)
                    },
                    onOpenPassport = { showPassportSheet = true },
                    onOpenApiKeyDialog = { showApiKeyDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // AR Tour Clip Viewer
                ArTourClipViewer(
                    recognition = currentRecognition!!,
                    dossier = currentDossier!!,
                    tourClip = currentTourClip!!,
                    photoBitmap = currentPhotoBitmap,
                    audioState = audioState,
                    selectedHotspot = selectedHotspot,
                    selectedEra = selectedEra,
                    selectedVoice = selectedVoice,
                    averageRating = currentAverageRating,
                    reviewCount = currentReviewCount,
                    onSelectHotspot = { viewModel.selectHotspot(it) },
                    onSelectEra = { viewModel.setHistoricalEra(it) },
                    onSelectVoice = { viewModel.setVoice(it) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onReplayAudio = { viewModel.playAudioTour() },
                    onSeekAudio = { viewModel.seekAudio(it) },
                    onSpeedChange = { viewModel.setSpeed(it) },
                    onOpenDossier = { showHistoryDossier = true },
                    onOpenReviews = { showReviewsSheet = true },
                    onBackToCamera = { viewModel.resetToCamera() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Loading Overlay during Vision AI / Search Grounding / Audio Generation
        if (pipelineStep == TourPipelineStep.RECOGNIZING ||
            pipelineStep == TourPipelineStep.FETCHING_HISTORY ||
            pipelineStep == TourPipelineStep.GENERATING_NARRATION
        ) {
            AiAnalysisLoadingOverlay(
                step = pipelineStep,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Search-Grounded History Dossier Modal Sheet
        if (showHistoryDossier && currentRecognition != null && currentDossier != null) {
            HistoryDossierSheet(
                recognition = currentRecognition!!,
                dossier = currentDossier!!,
                onDismiss = { showHistoryDossier = false },
                onOpenReviews = { showReviewsSheet = true }
            )
        }

        // Community Reviews & Rating Sheet
        if (showReviewsSheet && currentRecognition != null) {
            LandmarkReviewsSheet(
                landmarkName = currentRecognition!!.landmarkName,
                city = currentRecognition!!.city,
                reviews = currentReviews,
                averageRating = currentAverageRating,
                onDismiss = { showReviewsSheet = false },
                onSubmitReview = { rating, author, text, tag ->
                    viewModel.submitReview(
                        landmarkName = currentRecognition!!.landmarkName,
                        authorName = author,
                        rating = rating,
                        reviewText = text,
                        tag = tag
                    )
                },
                onDeleteReview = { review ->
                    viewModel.deleteReview(review)
                }
            )
        }

        // Explorer Tourism Passport Sheet
        if (showPassportSheet) {
            ExplorerPassportSheet(
                savedTours = savedTours,
                sampleLandmarks = viewModel.sampleLandmarks,
                onSelectTour = { tour ->
                    viewModel.loadTourFromEntity(tour)
                },
                onToggleFavorite = { id ->
                    viewModel.toggleFavorite(id)
                },
                onDeleteTour = { tour ->
                    viewModel.deleteTour(tour)
                },
                onDismiss = { showPassportSheet = false }
            )
        }

        // API Key Dialog
        if (showApiKeyDialog) {
            ApiKeyDialog(
                currentApiKey = customApiKey,
                onSaveKey = { viewModel.setApiKey(it) },
                onDismiss = { showApiKeyDialog = false }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
