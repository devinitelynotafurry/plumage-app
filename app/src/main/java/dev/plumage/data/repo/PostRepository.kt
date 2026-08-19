package dev.plumage.data.repo

import dev.plumage.data.local.CursorDao
import dev.plumage.data.local.QueryCursorEntity
import dev.plumage.data.local.SeenDao
import dev.plumage.data.local.SeenPostEntity
import dev.plumage.data.remote.E926Api
import dev.plumage.data.remote.RateLimiter
import dev.plumage.domain.PostFilter
import dev.plumage.domain.QueryBuilder
import dev.plumage.domain.model.Post
import dev.plumage.domain.model.Settings
import dev.plumage.domain.model.SortMode
import dev.plumage.domain.model.TagSuggestion
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val api: E926Api,
    private val seenDao: SeenDao,
    private val cursorDao: CursorDao,
    private val rateLimiter: RateLimiter
) {

    data class Page(
        val posts: List<Post>,
        val nextPage: Int,
        val exhausted: Boolean,
        val droppedTags: List<String>
    )

    /**
     * Fetches until it has something to show or runs out of patience.
     *
     * The retry loop exists because the seen ledger is applied client side. On a
     * heavily-swiped tag an entire page can come back already-seen, and returning an
     * empty deck at that point would look like "no results" when the truth is "you
     * have already been through these". [MAX_PAGE_ATTEMPTS] caps how much request
     * budget one refill is allowed to spend before giving up honestly.
     */
    suspend fun fetch(
        rawQuery: String,
        sort: SortMode,
        settings: Settings,
        startPage: Int? = null
    ): Page {
        val query = QueryBuilder.build(rawQuery, sort, settings.filterAiContent)
        val cursorKey = QueryBuilder.cursorKey(rawQuery, sort, settings.filterAiContent)
        val seen = seenDao.allSeenIds().toSet()

        var page = startPage ?: cursorDao.nextPage(cursorKey) ?: 1
        val collected = mutableListOf<Post>()
        var attempts = 0
        var exhausted = false

        while (collected.size < MIN_BATCH && attempts < MAX_PAGE_ATTEMPTS) {
            attempts++
            val response = rateLimiter.gate {
                api.getPosts(tags = query.tagString, limit = PAGE_SIZE, page = page)
            }

            if (response.posts.isEmpty()) {
                exhausted = true
                break
            }

            collected += PostFilter.filter(
                dtos = response.posts,
                criteria = PostFilter.Criteria(
                    blockedTags = settings.blockedTagSet,
                    filterAi = settings.filterAiContent,
                    seenIds = seen
                )
            )
            page++
        }

        // order:random re-rolls server side every request, so a saved page number is
        // meaningless for it and would just pin the user to one arbitrary slice.
        if (sort != SortMode.SHUFFLE) {
            cursorDao.put(QueryCursorEntity(cursorKey, page, System.currentTimeMillis()))
        }

        return Page(
            posts = collected.distinctBy { it.id },
            nextPage = page,
            exhausted = exhausted,
            droppedTags = query.droppedTags
        )
    }

    suspend fun suggestTags(prefix: String): List<TagSuggestion> {
        val cleaned = prefix.trim().removePrefix("-").removePrefix("~")
        if (cleaned.length < MIN_AUTOCOMPLETE_CHARS) return emptyList()
        return try {
            rateLimiter.gate { api.autocompleteTags(cleaned) }
                .map { TagSuggestion(it.name, it.postCount, it.category, it.antecedentName) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPopularTags(): List<TagSuggestion> {
        return try {
            rateLimiter.gate { api.getTags(order = "count", limit = 15) }
                .map { TagSuggestion(it.name, it.postCount, it.category, it.antecedentName) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun markSeen(postId: Long, kept: Boolean) =
        seenDao.mark(SeenPostEntity(postId, kept, System.currentTimeMillis()))

    suspend fun unmarkSeen(postId: Long) = seenDao.unmark(postId)

    fun observeSeenCount() = seenDao.observeSeenCount()

    suspend fun clearSeenHistory() {
        seenDao.clearAll()
        cursorDao.clearAll()
    }

    companion object {
        const val PAGE_SIZE = 80
        const val MIN_BATCH = 12
        const val MAX_PAGE_ATTEMPTS = 4
        const val MIN_AUTOCOMPLETE_CHARS = 2
    }
}
