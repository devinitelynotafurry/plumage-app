package dev.plumage.data.repo

import dev.plumage.data.local.CollectionDao
import dev.plumage.data.local.CollectionSummary
import dev.plumage.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val dao: CollectionDao
) {
    fun observeCollections(): Flow<List<CollectionSummary>> = dao.observeCollections()

    fun observeCollection(name: String): Flow<List<Post>> =
        dao.observeCollection(name).map { rows -> rows.map(Post::from) }

    suspend fun save(post: Post, collection: String) = dao.save(post.toEntity(collection))

    suspend fun remove(postId: Long, collection: String) = dao.remove(postId, collection)

    suspend fun removeCollection(name: String) = dao.removeCollection(name)

    suspend fun clearAll() = dao.clearAll()
}
