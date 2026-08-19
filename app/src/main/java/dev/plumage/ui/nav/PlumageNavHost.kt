package dev.plumage.ui.nav

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.plumage.ui.collections.CollectionDetailScreen
import dev.plumage.ui.collections.CollectionsScreen
import dev.plumage.ui.settings.SettingsScreen
import dev.plumage.ui.swipe.SwipeScreen
import dev.plumage.ui.viewer.ViewerScreen

object Routes {
    const val SWIPE = "swipe"
    const val COLLECTIONS = "collections"
    const val COLLECTION_DETAIL = "collection/{name}"
    const val VIEWER = "viewer/{collection}/{postId}"
    const val SETTINGS = "settings"

    fun collectionDetail(name: String) = "collection/${Uri.encode(name)}"
    fun viewer(collection: String, postId: Long) = "viewer/${Uri.encode(collection)}/$postId"
}

@Composable
fun PlumageNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.SWIPE,
        enterTransition = { slideInHorizontally(tween(220)) { it / 6 } + fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { slideOutHorizontally(tween(220)) { it / 6 } + fadeOut(tween(160)) }
    ) {
        composable(Routes.SWIPE) {
            SwipeScreen(
                onOpenCollections = { navController.navigate(Routes.COLLECTIONS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.COLLECTIONS) {
            CollectionsScreen(
                onBack = { navController.popBackStack() },
                onOpenCollection = { navController.navigate(Routes.collectionDetail(it)) }
            )
        }

        composable(
            route = Routes.COLLECTION_DETAIL,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val name = entry.arguments?.getString("name").orEmpty()
            CollectionDetailScreen(
                collectionName = name,
                onBack = { navController.popBackStack() },
                onOpenPost = { postId -> navController.navigate(Routes.viewer(name, postId)) }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("collection") { type = NavType.StringType },
                navArgument("postId") { type = NavType.LongType }
            )
        ) { entry ->
            ViewerScreen(
                collectionName = entry.arguments?.getString("collection").orEmpty(),
                postId = entry.arguments?.getLong("postId") ?: 0L,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
