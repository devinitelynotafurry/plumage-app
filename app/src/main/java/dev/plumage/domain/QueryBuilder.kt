package dev.plumage.domain

import dev.plumage.domain.model.SortMode

/**
 * Assembles the tag string sent to e926, inside a hard six-tag ceiling.
 *
 * The naive rule "reserve one slot for rating:s, allow five user tags" is wrong:
 * order: and the AI exclusion are also tags and also count. With shuffle plus the
 * AI filter on, three slots are already spoken for and only three are left. So the
 * budget is computed rather than assumed, and anything that does not fit is
 * reported back so the UI can tell the user which tags were dropped instead of
 * silently returning results that do not match what they typed.
 */
object QueryBuilder {

    const val MAX_TAGS = 6
    const val SAFETY_TAG = "rating:s"
    const val AI_EXCLUSION = "-ai_generated"

    data class Query(
        val tagString: String,
        val userTagsUsed: List<String>,
        val droppedTags: List<String>
    ) {
        val budgetExceeded: Boolean get() = droppedTags.isNotEmpty()
    }

    fun build(rawInput: String, sort: SortMode, filterAi: Boolean): Query {
        val reserved = buildList {
            // Redundant on e926, which only serves safe posts, and it costs a slot.
            // Kept anyway so that pointing BASE_URL at a different host cannot
            // silently widen what this app will display.
            add(SAFETY_TAG)
            if (filterAi) add(AI_EXCLUSION)
            sort.orderTag?.let { add(it) }
        }

        val userTags = rawInput.trim().lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .distinct()
            .filterNot { it in reserved }
            .filterNot { it.startsWith("rating:") }

        val budget = (MAX_TAGS - reserved.size).coerceAtLeast(0)
        val used = userTags.take(budget)
        val dropped = userTags.drop(budget)

        return Query(
            tagString = (reserved + used).joinToString(" "),
            userTagsUsed = used,
            droppedTags = dropped
        )
    }

    /** Stable key for the paging cursor, independent of tag order. */
    fun cursorKey(rawInput: String, sort: SortMode, filterAi: Boolean): String {
        val q = build(rawInput, sort, filterAi)
        return q.tagString.split(" ").sorted().joinToString(" ")
    }

    /** The label a swipe-right collection gets. Falls back for an empty search. */
    fun collectionNameFor(rawInput: String): String =
        rawInput.trim().lowercase().replace(Regex("\\s+"), " ").ifBlank { "everything" }
}
