package dev.plumage.domain

import dev.plumage.data.remote.dto.PostDto
import dev.plumage.domain.model.Post

/**
 * Client-side gate applied to every payload before anything reaches the deck.
 * Server-side tags do most of the work; this catches what they cannot.
 */
object PostFilter {

    /** Neither decodes in Coil, and neither belongs in a swipe deck. */
    val UNSUPPORTED_EXTENSIONS = setOf("webm", "swf", "mp4")

    /**
     * Backstop for the -ai_generated server exclusion. Booru tag vocabulary drifts,
     * aliases get created and retired, and a post can carry an AI marker that is not
     * the canonical tag. This list is a best effort, not an authoritative set, and it
     * is cheap to extend when a new marker shows up.
     */
    val AI_MARKERS = setOf(
        "ai_generated",
        "ai_art",
        "ai_assisted",
        "ai_generated_art",
        "ai_upscaled",
        "stable_diffusion",
        "novelai",
        "midjourney"
    )

    data class Criteria(
        val blockedTags: Set<String>,
        val filterAi: Boolean,
        val seenIds: Set<Long>
    )

    fun filter(dtos: List<PostDto>, criteria: Criteria): List<Post> =
        dtos.asSequence()
            .filter { it.rating.equals("s", ignoreCase = true) }
            .filterNot { it.flags.deleted }
            .filterNot { it.file.ext.lowercase() in UNSUPPORTED_EXTENSIONS }
            .mapNotNull { Post.from(it) }
            .filterNot { it.id in criteria.seenIds }
            .filterNot { post -> post.tags.any { it.lowercase() in criteria.blockedTags } }
            .filterNot { post ->
                criteria.filterAi && post.tags.any { it.lowercase() in AI_MARKERS }
            }
            .toList()
}
