package dev.plumage.data.remote

import dev.plumage.data.remote.dto.PostListResponse
import dev.plumage.data.remote.dto.TagSuggestionDto
import retrofit2.http.GET
import retrofit2.http.Query

interface E926Api {

    /**
     * @param tags space-separated, six maximum. See [dev.plumage.domain.QueryBuilder].
     * @param limit capped at 320 by the API.
     */
    @GET("posts.json")
    suspend fun getPosts(
        @Query("tags") tags: String,
        @Query("limit") limit: Int = 80,
        @Query("page") page: Int = 1
    ): PostListResponse

    @GET("tags/autocomplete.json")
    suspend fun autocompleteTags(
        @Query("search[name_matches]") prefix: String,
        @Query("expiry") expiryDays: Int = 7
    ): List<TagSuggestionDto>

    @GET("tags.json")
    suspend fun getTags(
        @Query("search[order]") order: String = "count",
        @Query("limit") limit: Int = 20
    ): List<TagSuggestionDto>

    companion object {
        const val BASE_URL = "https://e926.net/"
    }
}
