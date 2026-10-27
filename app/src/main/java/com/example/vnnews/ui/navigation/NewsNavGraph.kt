package com.example.vnnews.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vnnews.ui.bookmarks.BookmarksScreen
import com.example.vnnews.ui.detail.DetailScreen
import com.example.vnnews.ui.home.HomeScreen

@Composable
fun NewsApp(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        NewsDestination.Home.route,
        NewsDestination.Bookmarks.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == NewsDestination.Home.route,
                        onClick = {
                            navController.navigate(NewsDestination.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
                        label = { Text("Trang chủ") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NewsDestination.Bookmarks.route,
                        onClick = {
                            navController.navigate(NewsDestination.Bookmarks.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Đã lưu") },
                        label = { Text("Đã lưu") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NewsDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NewsDestination.Home.route) {
                HomeScreen(
                    onArticleClick = { url, title ->
                        navController.navigate(NewsDestination.Detail.createRoute(url, title))
                    }
                )
            }

            composable(NewsDestination.Bookmarks.route) {
                BookmarksScreen(
                    onArticleClick = { url, title ->
                        navController.navigate(NewsDestination.Detail.createRoute(url, title))
                    }
                )
            }

            composable(
                route = NewsDestination.Detail.route,
                arguments = listOf(
                    navArgument("encodedUrl") { type = NavType.StringType },
                    navArgument("encodedTitle") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("encodedUrl")
                val encodedTitle = backStackEntry.arguments?.getString("encodedTitle")
                val url = NewsDestination.Detail.decodeUrl(encodedUrl)
                val title = NewsDestination.Detail.decodeTitle(encodedTitle)

                DetailScreen(
                    url = url,
                    title = title,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
