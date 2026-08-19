package dev.plumage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedPostEntity::class, SeenPostEntity::class, QueryCursorEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PlumageDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun seenDao(): SeenDao
    abstract fun cursorDao(): CursorDao

    companion object {
        const val NAME = "plumage.db"
    }
}
