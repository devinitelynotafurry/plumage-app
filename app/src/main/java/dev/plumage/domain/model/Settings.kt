package dev.plumage.domain.model

data class Settings(
    val username: String = "",
    val blockedTags: String = DEFAULT_BLOCKED_TAGS,
    val filterAiContent: Boolean = true,
    val useDynamicColor: Boolean = true
) {
    val blockedTagSet: Set<String>
        get() = blockedTags.trim().lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .toSet()

    companion object {
        /**
         * Shipped on by default. The safe-rating check upstream is not a substitute
         * for this: "safe" on a booru means non-explicit, which still lets through
         * fetish and gore adjacent material that has no business in this app.
         */
        const val DEFAULT_BLOCKED_TAGS =
            "young cub diaper gore death blood scat urine vore inflation hyper bulge underwear bikini nipples"
    }
}
