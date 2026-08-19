package dev.plumage.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response element from /tags/autocomplete.json.
 *
 * [antecedentName] is populated when the match came in via an alias: the user
 * typed a deprecated tag and the site is telling you the canonical one. Show
 * the alias, search the canonical name.
 */
@Serializable
data class TagSuggestionDto(
    val id: Long = 0,
    val name: String = "",
    @SerialName("post_count") val postCount: Int = 0,
    val category: Int = 0,
    @SerialName("antecedent_name") val antecedentName: String? = null
)
