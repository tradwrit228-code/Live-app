package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "landmark_reviews")
data class LandmarkReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val landmarkName: String,
    val authorName: String,
    val rating: Int,
    val reviewText: String,
    val tag: String = "Explorer",
    val timestamp: Long = System.currentTimeMillis()
)
