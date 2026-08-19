package dev.plumage.domain.model

import dev.plumage.data.local.SavedPostEntity
import dev.plumage.data.remote.dto.PostDto

data class Post(
    val id: Long,
    val fileUrl: String,
    val sampleUrl: String,
    val previewUrl: String,
    val ext: String,
    val width: Int,
    val height: Int,
    val score: Int,
    val artists: List<String>,
    val tags: List<String>
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height else 1f

    val artistLabel: String
        get() = artists.firstOrNull()?.replace('_', ' ') ?: "unknown artist"

    val isAnimated: Boolean get() = ext.equals("gif", ignoreCase = true)

    fun toEntity(collection: String, savedAt: Long = System.currentTimeMillis()) = SavedPostEntity(
        postId = id,
        collectionName = collection,
        fileUrl = fileUrl,
        sampleUrl = sampleUrl,
        previewUrl = previewUrl,
        ext = ext,
        width = width,
        height = height,
        score = score,
        artists = artists.joinToString(","),
        tags = tags.joinToString(" "),
        savedAt = savedAt
    )

    companion object {
        /** Returns null when the payload has no usable file, which the API does emit. */
        fun from(dto: PostDto): Post? {
            val file = dto.file.url?.takeIf { it.isNotBlank() } ?: return null
            return Post(
                id = dto.id,
                fileUrl = file,
                sampleUrl = dto.sample.url?.takeIf { it.isNotBlank() } ?: file,
                previewUrl = dto.preview.url?.takeIf { it.isNotBlank() } ?: file,
                ext = dto.file.ext,
                width = dto.file.width,
                height = dto.file.height,
                score = dto.score.total,
                artists = dto.tags.artist,
                tags = dto.tags.flatten()
            )
        }

        fun from(entity: SavedPostEntity) = Post(
            id = entity.postId,
            fileUrl = entity.fileUrl,
            sampleUrl = entity.sampleUrl,
            previewUrl = entity.previewUrl,
            ext = entity.ext,
            width = entity.width,
            height = entity.height,
            score = entity.score,
            artists = entity.artists.split(",").filter { it.isNotBlank() },
            tags = entity.tags.split(" ").filter { it.isNotBlank() }
        )
    }
}

data class TagSuggestion(
    val name: String,
    val postCount: Int,
    val category: Int,
    val aliasOf: String?
)

enum class SortMode(val label: String, val orderTag: String?) {
    NEWEST("Newest", null),
    TOP_RATED("Top rated", "order:score"),
    SHUFFLE("Shuffle", "order:random")
}
