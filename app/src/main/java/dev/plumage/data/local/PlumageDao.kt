package dev.plumage.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(post: SavedPostEntity)

    @Query("DELETE FROM saved_posts WHERE postId = :postId AND collectionName = :collection")
    suspend fun remove(postId: Long, collection: String)

    @Query("DELETE FROM saved_posts WHERE collectionName = :collection")
    suspend fun removeCollection(collection: String)

    @Query(
        """
        SELECT s.collectionName AS name,
               COUNT(*) AS itemCount,
               (SELECT s2.previewUrl FROM saved_posts s2
                 WHERE s2.collectionName = s.collectionName
                 ORDER BY s2.savedAt DESC LIMIT 1) AS previewUrl,
               MAX(s.savedAt) AS lastSavedAt
          FROM saved_posts s
         GROUP BY s.collectionName
         ORDER BY lastSavedAt DESC
        """
    )
    fun observeCollections(): Flow<List<CollectionSummary>>

    @Query("SELECT * FROM saved_posts WHERE collectionName = :collection ORDER BY savedAt DESC")
    fun observeCollection(collection: String): Flow<List<SavedPostEntity>>

    @Query("SELECT * FROM saved_posts WHERE postId = :postId AND collectionName = :collection LIMIT 1")
    suspend fun find(postId: Long, collection: String): SavedPostEntity?

    @Query("DELETE FROM saved_posts")
    suspend fun clearAll()
}

@Dao
interface SeenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mark(entry: SeenPostEntity)

    @Query("DELETE FROM seen_posts WHERE postId = :postId")
    suspend fun unmark(postId: Long)

    @Query("SELECT postId FROM seen_posts")
    suspend fun allSeenIds(): List<Long>

    @Query("SELECT COUNT(*) FROM seen_posts")
    fun observeSeenCount(): Flow<Int>

    @Query("DELETE FROM seen_posts")
    suspend fun clearAll()
}

@Dao
interface CursorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(cursor: QueryCursorEntity)

    @Query("SELECT nextPage FROM query_cursors WHERE query = :query LIMIT 1")
    suspend fun nextPage(query: String): Int?

    @Query("DELETE FROM query_cursors")
    suspend fun clearAll()
}
