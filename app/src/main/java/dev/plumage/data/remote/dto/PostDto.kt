package dev.plumage.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostListResponse(
    val posts: List<PostDto> = emptyList()
)

@Serializable
data class PostDto(
    val id: Long,
    val file: FileDto = FileDto(),
    val preview: PreviewDto = PreviewDto(),
    val sample: SampleDto = SampleDto(),
    val score: ScoreDto = ScoreDto(),
    val tags: TagsDto = TagsDto(),
    val rating: String = "s",
    val flags: FlagsDto = FlagsDto(),
    @SerialName("fav_count") val favCount: Int = 0,
    val description: String = "",
    val sources: List<String> = emptyList()
)

/**
 * [url] is nullable on purpose. e926 returns a null file URL for posts the
 * requesting client is not allowed to fetch (deleted posts, posts by artists
 * under a takedown). Treating it as non-null is the single most common way
 * these clients crash in the wild.
 */
@Serializable
data class FileDto(
    val width: Int = 0,
    val height: Int = 0,
    val ext: String = "",
    val size: Long = 0,
    val md5: String = "",
    val url: String? = null
)

@Serializable
data class PreviewDto(
    val width: Int = 0,
    val height: Int = 0,
    val url: String? = null
)

@Serializable
data class SampleDto(
    val has: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val url: String? = null
)

@Serializable
data class ScoreDto(
    val up: Int = 0,
    val down: Int = 0,
    val total: Int = 0
)

@Serializable
data class TagsDto(
    val general: List<String> = emptyList(),
    val species: List<String> = emptyList(),
    val character: List<String> = emptyList(),
    val artist: List<String> = emptyList(),
    val copyright: List<String> = emptyList(),
    val invalid: List<String> = emptyList(),
    val lore: List<String> = emptyList(),
    val meta: List<String> = emptyList()
) {
    fun flatten(): List<String> =
        general + species + character + artist + copyright + invalid + lore + meta
}

@Serializable
data class FlagsDto(
    val pending: Boolean = false,
    val flagged: Boolean = false,
    val deleted: Boolean = false
)
