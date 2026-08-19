package dev.plumage.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.plumage.data.local.CollectionDao
import dev.plumage.data.local.CursorDao
import dev.plumage.data.local.PlumageDatabase
import dev.plumage.data.local.SeenDao
import dev.plumage.data.prefs.SettingsRepository
import dev.plumage.data.remote.E926Api
import dev.plumage.data.remote.UserAgentInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context) =
        SettingsRepository(context)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @Named("api")
    fun provideApiClient(settings: SettingsRepository): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor { settings.cachedUsername })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    /**
     * Separate client for images. It carries the same User-Agent (the CDN is still
     * e926's) but shares no rate limiting, and gets a bigger connection pool because
     * the deck prefetches several cards ahead.
     */
    @Provides
    @Singleton
    @Named("image")
    fun provideImageClient(settings: SettingsRepository): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor { settings.cachedUsername })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(@Named("api") client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(E926Api.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): E926Api = retrofit.create(E926Api::class.java)

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        @Named("image") client: OkHttpClient
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(client)
        // minSdk 29 means ImageDecoderDecoder is always available; it handles
        // animated GIF and WebP without the legacy GifDecoder fallback.
        .components { add(ImageDecoderDecoder.Factory()) }
        .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlumageDatabase =
        Room.databaseBuilder(context, PlumageDatabase::class.java, PlumageDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideCollectionDao(db: PlumageDatabase): CollectionDao = db.collectionDao()
    @Provides fun provideSeenDao(db: PlumageDatabase): SeenDao = db.seenDao()
    @Provides fun provideCursorDao(db: PlumageDatabase): CursorDao = db.cursorDao()
}
