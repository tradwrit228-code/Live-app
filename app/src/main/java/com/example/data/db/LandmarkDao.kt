package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LandmarkDao {
    @Query("SELECT * FROM landmark_tours ORDER BY scannedTimestamp DESC")
    fun getAllTours(): Flow<List<LandmarkTourEntity>>

    @Query("SELECT * FROM landmark_tours WHERE id = :id LIMIT 1")
    suspend fun getTourById(id: Long): LandmarkTourEntity?

    @Query("SELECT * FROM landmark_tours WHERE landmarkName = :name LIMIT 1")
    suspend fun getTourByName(name: String): LandmarkTourEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTour(tour: LandmarkTourEntity): Long

    @Update
    suspend fun updateTour(tour: LandmarkTourEntity)

    @Delete
    suspend fun deleteTour(tour: LandmarkTourEntity)

    @Query("UPDATE landmark_tours SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    // User Reviews and Ratings
    @Query("SELECT * FROM landmark_reviews WHERE landmarkName = :landmarkName ORDER BY timestamp DESC")
    fun getReviewsForLandmark(landmarkName: String): Flow<List<LandmarkReviewEntity>>

    @Query("SELECT * FROM landmark_reviews ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<LandmarkReviewEntity>>

    @Query("SELECT COUNT(*) FROM landmark_reviews WHERE landmarkName = :landmarkName")
    fun getReviewCountForLandmark(landmarkName: String): Flow<Int>

    @Query("SELECT AVG(rating) FROM landmark_reviews WHERE landmarkName = :landmarkName")
    fun getAverageRatingForLandmark(landmarkName: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: LandmarkReviewEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<LandmarkReviewEntity>)

    @Delete
    suspend fun deleteReview(review: LandmarkReviewEntity)
}
