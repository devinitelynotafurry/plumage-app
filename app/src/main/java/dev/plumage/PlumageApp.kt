package dev.plumage

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import dev.plumage.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class PlumageApp : Application(), ImageLoaderFactory {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var imageLoaderProvider: Provider<ImageLoader>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Keeps the username hot for the User-Agent interceptor, which runs on an
        // OkHttp thread and cannot suspend to read DataStore.
        appScope.launch { settingsRepository.observeUsernameIntoCache() }
    }

    override fun newImageLoader(): ImageLoader = imageLoaderProvider.get()
}
