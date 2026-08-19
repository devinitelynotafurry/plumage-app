package dev.plumage.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * A post kept by swiping right. The composite key means the same post can live
 * in more than one collection (found under "fox", found again under "corvid")
 * without duplicating rows inside a collection.
 */
@Entity(
    tableName = "saved_posts",
    primaryKeys = ["postId", "collectionName"],
    indices = [Index("collectionName"), Index("savedAt")]
)
data class SavedPostEntity(
    val postId: Long,
    val collectionName: String,
    val fileUrl: String,
    val sampleUrl: String,
    val previewUrl: String,
    val ext: String,
    val width: Int,
    val height: Int,
    val score: Int,
    val artists: String,
    val tags: String,
    val savedAt: Long
)

/**
 * Every post the user has ruled on, in either direction. This is the "never show
 * me this again" ledger; it is filtered client side because the API has no way to
 * exclude an arbitrary set of IDs without burning tag slots.
 */
@Entity(tableName = "seen_posts")
data class SeenPostEntity(
    @androidx.room.PrimaryKey val postId: Long,
    val kept: Boolean,
    val seenAt: Long
)

/**
 * Where paging left off for a given normalised query. Without this, resuming a
 * search restarts at page 1 and burns request budget re-fetching pages that are
 * entirely filtered out by the seen ledger.
 */
@Entity(tableName = "query_cursors")
data class QueryCursorEntity(
    @androidx.room.PrimaryKey val query: String,
    val nextPage: Int,
    val updatedAt: Long
)

/** Projection for the Collections grid. */
data class CollectionSummary(
    val name: String,
    val itemCount: Int,
    val previewUrl: String,
    val lastSavedAt: Long
)
