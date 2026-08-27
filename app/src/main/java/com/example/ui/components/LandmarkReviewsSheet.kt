package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.LandmarkReviewEntity
import com.example.ui.theme.ImmersiveAmberGold
import com.example.ui.theme.ImmersiveDarkBg
import com.example.ui.theme.ImmersiveEmeraldAccent
import com.example.ui.theme.ImmersiveGlassBorder
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePurpleDark
import com.example.ui.theme.ImmersivePurpleLight
import com.example.ui.theme.ImmersivePurplePrimary
import com.example.ui.theme.ImmersiveRoseAccent
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceElevated
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextMuted
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LandmarkReviewsSheet(
    landmarkName: String,
    city: String,
    reviews: List<LandmarkReviewEntity>,
    averageRating: Double?,
    onDismiss: () -> Unit,
    onSubmitReview: (rating: Int, author: String, text: String, tag: String) -> Unit,
    onDeleteReview: (LandmarkReviewEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var showAddForm by remember { mutableStateOf(false) }
    var inputRating by remember { mutableIntStateOf(5) }
    var inputAuthor by remember { mutableStateOf("") }
    var inputReviewText by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("🏛 Architecture") }
    var formError by remember { mutableStateOf<String?>(null) }

    val tags = listOf(
        "🏛 Architecture",
        "📸 Photography",
        "🌅 Sunset View",
        "📜 Histoire",
        "🎒 Solo Explorer",
        "👨‍👩‍👧 Family",
        "✨ Atmosphere"
    )

    val avgScore = averageRating ?: if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 5.0
    val totalCount = reviews.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ImmersiveDarkBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .background(ImmersiveOutline, RoundedCornerShape(2.dp))
            )
        },
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = null,
                                tint = ImmersiveAmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "COMMUNITY REVIEWS & RATINGS",
                                color = ImmersiveAmberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = landmarkName,
                            color = ImmersiveTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Visitor feedback • $city",
                            color = ImmersiveTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_reviews_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ImmersiveTextSecondary
                        )
                    }
                }
            }

            // Rating & Stats Breakdown Card
            item {
                Surface(
                    color = ImmersiveSurfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ImmersiveAmberGold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Score & Star Display
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(0.4f)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", avgScore),
                                color = ImmersiveAmberGold,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black
                            )

                            StarRatingDisplay(rating = avgScore, starSize = 16.dp)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "$totalCount avis",
                                color = ImmersiveTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right: 5-to-1 Star Distribution Progress Bars
                        Column(
                            modifier = Modifier.weight(0.6f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            (5 downTo 1).forEach { star ->
                                val countForStar = reviews.count { it.rating == star }
                                val ratio = if (totalCount > 0) countForStar.toFloat() / totalCount else 0f

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "$star★",
                                        color = ImmersiveTextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(22.dp)
                                    )
                                    LinearProgressIndicator(
                                        progress = ratio,
                                        trackColor = ImmersiveSurfaceElevated,
                                        color = ImmersiveAmberGold,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$countForStar",
                                        color = ImmersiveTextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Write a Review Toggle Button
            item {
                Surface(
                    color = if (showAddForm) ImmersiveSurfaceElevated else ImmersivePurplePrimary,
                    shape = RoundedCornerShape(14.dp),
                    border = if (showAddForm) BorderStroke(1.dp, ImmersivePurplePrimary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showAddForm = !showAddForm }
                        .testTag("toggle_add_review_button")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.AddComment,
                            contentDescription = null,
                            tint = if (showAddForm) ImmersivePurplePrimary else ImmersiveDarkBg,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showAddForm) "FERMER LE FORMULAIRE" else "ÉCRIRE UN AVIS & NOTER",
                            color = if (showAddForm) ImmersivePurplePrimary else ImmersiveDarkBg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Review Submission Form
            item {
                AnimatedVisibility(visible = showAddForm) {
                    Surface(
                        color = ImmersiveSurfaceVariant,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ImmersivePurplePrimary.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "VOTRE NOTE GLOBALE",
                                color = ImmersivePurplePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            // Interactive Star Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { inputRating = star },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .testTag("star_select_$star")
                                    ) {
                                        Icon(
                                            imageVector = if (star <= inputRating) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "$star stars",
                                            tint = if (star <= inputRating) ImmersiveAmberGold else ImmersiveTextMuted,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            // Author Name Field
                            OutlinedTextField(
                                value = inputAuthor,
                                onValueChange = { inputAuthor = it },
                                label = { Text("Votre nom ou pseudo", color = ImmersiveTextSecondary) },
                                placeholder = { Text("Ex: Thomas V.", color = ImmersiveTextMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ImmersivePurplePrimary,
                                    unfocusedBorderColor = ImmersiveOutline,
                                    focusedTextColor = ImmersiveTextPrimary,
                                    unfocusedTextColor = ImmersiveTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("review_author_input")
                            )

                            // Tag Selection Chips
                            Text(
                                text = "THÈME DE VISITE",
                                color = ImmersivePurpleLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tags.forEach { tag ->
                                    val isSelected = selectedTag == tag
                                    Surface(
                                        color = if (isSelected) ImmersivePurplePrimary else ImmersiveSurfaceElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            0.5.dp,
                                            if (isSelected) ImmersivePurplePrimary else ImmersiveGlassBorder
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedTag = tag }
                                    ) {
                                        Text(
                                            text = tag,
                                            color = if (isSelected) ImmersiveDarkBg else ImmersiveTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // Review Text Input
                            OutlinedTextField(
                                value = inputReviewText,
                                onValueChange = {
                                    inputReviewText = it
                                    if (formError != null) formError = null
                                },
                                label = { Text("Votre avis détaillé", color = ImmersiveTextSecondary) },
                                placeholder = {
                                    Text(
                                        "Partagez vos impressions, points de vue et conseils sur ce monument...",
                                        color = ImmersiveTextMuted
                                    )
                                },
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ImmersivePurplePrimary,
                                    unfocusedBorderColor = ImmersiveOutline,
                                    focusedTextColor = ImmersiveTextPrimary,
                                    unfocusedTextColor = ImmersiveTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("review_text_input")
                            )

                            formError?.let { err ->
                                Text(
                                    text = err,
                                    color = ImmersiveRoseAccent,
                                    fontSize = 11.sp
                                )
                            }

                            // Submit Button
                            Button(
                                onClick = {
                                    if (inputReviewText.isBlank()) {
                                        formError = "Veuillez saisir un commentaire pour publier votre avis."
                                        return@Button
                                    }
                                    onSubmitReview(
                                        inputRating,
                                        inputAuthor.ifBlank { "Explorateur Passionné" },
                                        inputReviewText,
                                        selectedTag
                                    )
                                    inputReviewText = ""
                                    showAddForm = false
                                    formError = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ImmersivePurplePrimary,
                                    contentColor = ImmersiveDarkBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_review_button")
                            ) {
                                Text(
                                    text = "PUBLIER L'AVIS",
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Reviews List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVIS DES VISITEURS ($totalCount)",
                        color = ImmersivePurpleLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (reviews.isEmpty()) {
                item {
                    Surface(
                        color = ImmersiveSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Aucun avis pour le moment. Soyez le premier explorateur à partager votre expérience!",
                            color = ImmersiveTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(reviews, key = { it.id }) { review ->
                    ReviewItemCard(
                        review = review,
                        onDelete = { onDeleteReview(review) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ReviewItemCard(
    review: LandmarkReviewEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatted = remember(review.timestamp) {
        val diff = System.currentTimeMillis() - review.timestamp
        when {
            diff < 60_000 -> "À l'instant"
            diff < 3600_000 -> "Il y a ${(diff / 60_000)} min"
            diff < 86400_000 -> "Il y a ${(diff / 3600_000)} h"
            diff < 86400_000L * 7 -> "Il y a ${(diff / 86400_000L)} j"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(review.timestamp))
        }
    }

    Surface(
        color = ImmersiveSurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ImmersiveGlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Circle
                    Surface(
                        color = ImmersivePurpleDark,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ImmersivePurplePrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = review.authorName.take(1).uppercase(),
                                color = ImmersivePurplePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = review.authorName,
                            color = ImmersiveTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateFormatted,
                            color = ImmersiveTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Tag Chip
                Surface(
                    color = ImmersiveSurfaceElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, ImmersiveGlassBorder)
                ) {
                    Text(
                        text = review.tag,
                        color = ImmersiveAmberGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Star Rating Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (star <= review.rating) ImmersiveAmberGold else ImmersiveTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Review",
                        tint = ImmersiveTextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = review.reviewText,
                color = ImmersiveTextPrimary.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun StarRatingDisplay(
    rating: Double,
    modifier: Modifier = Modifier,
    starSize: androidx.compose.ui.unit.Dp = 14.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { index ->
            val icon = when {
                rating >= index -> Icons.Default.Star
                rating >= index - 0.5 -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ImmersiveAmberGold,
                modifier = Modifier.size(starSize)
            )
        }
    }
}
