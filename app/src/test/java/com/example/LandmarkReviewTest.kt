package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LandmarkDatabase
import com.example.data.db.LandmarkReviewEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LandmarkReviewTest {

    private lateinit var database: LandmarkDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LandmarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndQueryReviews() = runBlocking {
        val dao = database.landmarkDao()

        val review1 = LandmarkReviewEntity(
            landmarkName = "Eiffel Tower",
            authorName = "Alice",
            rating = 5,
            reviewText = "Magical view from the top summit!",
            tag = "🌅 Sunset View",
            timestamp = System.currentTimeMillis()
        )
        val review2 = LandmarkReviewEntity(
            landmarkName = "Eiffel Tower",
            authorName = "Bob",
            rating = 4,
            reviewText = "Great architecture, expect some queues.",
            tag = "🏛 Architecture",
            timestamp = System.currentTimeMillis()
        )

        dao.insertReview(review1)
        dao.insertReview(review2)

        val reviews = dao.getReviewsForLandmark("Eiffel Tower").first()
        assertEquals(2, reviews.size)

        val avg = dao.getAverageRatingForLandmark("Eiffel Tower").first()
        assertNotNull(avg)
        assertEquals(4.5, avg!!, 0.01)

        val count = dao.getReviewCountForLandmark("Eiffel Tower").first()
        assertEquals(2, count)
    }

    @Test
    fun deleteReview() = runBlocking {
        val dao = database.landmarkDao()

        val id = dao.insertReview(
            LandmarkReviewEntity(
                landmarkName = "Colosseum",
                authorName = "Charlie",
                rating = 5,
                reviewText = "Incredible gladiator history.",
                tag = "📜 Histoire",
                timestamp = System.currentTimeMillis()
            )
        )

        val inserted = dao.getReviewsForLandmark("Colosseum").first().first()
        assertEquals(id, inserted.id)

        dao.deleteReview(inserted)
        val remaining = dao.getReviewsForLandmark("Colosseum").first()
        assertTrue(remaining.isEmpty())
    }
}
