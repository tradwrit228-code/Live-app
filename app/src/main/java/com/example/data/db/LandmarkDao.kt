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
}
