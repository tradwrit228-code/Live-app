package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "landmark_tours")
data class LandmarkTourEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val landmarkName: String,
    val city: String,
    val country: String,
    val style: String,
    val yearBuilt: String,
    val summary: String,
    val narrative: String,
    val timelineJson: String,
    val hotspotsJson: String,
    val folkloreJson: String,
    val audioScript: String,
    val searchSourcesJson: String,
    val imagePath: String,
    val scannedTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
