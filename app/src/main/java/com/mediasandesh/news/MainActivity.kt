package com.mediasandesh.news

import android.content.Context
import android.content.Intent
import com.mediasandesh.news.widget.HeadlinesWidget
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mediasandesh.news.ui.AboutScreen
import com.mediasandesh.news.ui.ArticleScreen
import com.mediasandesh.news.ui.BrandRed
import com.mediasandesh.news.ui.CategoriesScreen
import com.mediasandesh.news.ui.CategoryPostsScreen
import com.mediasandesh.news.ui.ContactScreen
import com.mediasandesh.news.ui.HomeScreen
import com.mediasandesh.news.ui.Ink
import com.mediasandesh.news.ui.LibraryScreen
import com.mediasandesh.news.ui.LocalScreen
import com.mediasandesh.news.ui.QuickScreen
import com.mediasandesh.news.ui.MediaSandeshTheme
import com.mediasandesh.news.ui.MenuScreen
import com.mediasandesh.news.ui.Muted
import com.mediasandesh.news.ui.PrivacyScreen
import com.mediasandesh.news.ui.SearchScreen
import com.mediasandesh.news.ui.SettingsScreen
import com.mediasandesh.news.ui.SubmitScreen
import com.mediasandesh.news.ui.ShortsScreen
import com.mediasandesh.news.ui.VideoPlayerScreen
import com.mediasandesh.news.ui.VideosScreen
import com.mediasandesh.news.ui.isAppDark

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    private var openPost by mutableStateOf<Int?>(null)

    private fun Intent.postId() = getIntExtra(HeadlinesWidget.EXTRA_POST, 0).takeIf { it > 0 }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openPost = intent.postId()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openPost = intent.postId()
        setContent {
            val dark = isAppDark()
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            MediaSandeshTheme(dark) { App(openPost) { openPost = null } }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo("home") { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(openPost: Int?, onPostOpened: () -> Unit) {
    val nav = rememberNavController()
    LaunchedEffect(openPost) {
        openPost?.let {
            nav.navigate("article/$it")
            onPostOpened()
        }
    }
    val tabs = listOf(
        Tab("home", stringResource(R.string.tab_home), Icons.Default.Home),
        Tab("quick", stringResource(R.string.tab_quick), Icons.Default.Bolt),
        Tab("local", stringResource(R.string.tab_local), Icons.Default.LocationCity),
        Tab("videos", stringResource(R.string.tab_videos), Icons.Default.SmartDisplay),
        Tab("library", stringResource(R.string.tab_library), Icons.Default.Bookmark)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val showBars = route in tabs.map { it.route }
    val title = when {
        route == "home" -> stringResource(R.string.app_name)
        route == "categories" -> stringResource(R.string.tab_categories)
        route == "quick" -> stringResource(R.string.tab_quick)
        route == "local" -> stringResource(R.string.tab_local)
        route == "videos" -> stringResource(R.string.tab_videos)
        route == "library" -> stringResource(R.string.tab_library)
        route.startsWith("article") -> stringResource(R.string.title_article)
        route.startsWith("category") -> Uri.decode(backStack?.arguments?.getString("name") ?: stringResource(R.string.title_category))
        route == "search" -> stringResource(R.string.search_title)
        route == "submit" -> stringResource(R.string.submit_title)
        route.startsWith("video/") -> stringResource(R.string.tab_videos)
        route.startsWith("shorts") -> stringResource(R.string.video_shorts)
        route == "about" -> stringResource(R.string.menu_about)
        route == "contact" -> stringResource(R.string.menu_contact)
        route == "privacy" -> stringResource(R.string.menu_privacy)
        route == "settings" -> stringResource(R.string.settings_title)
        else -> stringResource(R.string.app_name)
    }
    val openArticle: (Int) -> Unit = { nav.navigate("article/$it") }
    val openCategory: (Int, String) -> Unit = { id, name -> nav.navigate("category/$id/${Uri.encode(name)}") }

    val immersive = route.startsWith("shorts")
    Scaffold(
        contentWindowInsets = if (immersive) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (!immersive && !route.startsWith("article") && !route.startsWith("category/") && route != "menu") TopAppBar(
                title = {
                    if (route == "home" || route == "library") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painterResource(R.mipmap.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colorResource(R.color.ic_launcher_background))
                                    .graphicsLayer { scaleX = 1.45f; scaleY = 1.45f }
                            )
                            Text(
                                stringResource(R.string.brand_name),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 21.sp,
                                color = BrandRed,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    } else if (route == "quick") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, fontWeight = FontWeight.Bold, color = Ink)
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.padding(start = 4.dp).size(22.dp)
                            )
                            Text(
                                stringResource(R.string.quick_badge),
                                color = Color(0xFFE69A00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = Ink,
                    actionIconContentColor = Ink
                ),
                navigationIcon = {
                    if (!showBars) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (showBars) {
                        IconButton(onClick = { nav.navigate("search") }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_title))
                        }
                        IconButton(onClick = { nav.navigate("menu") { launchSingleTop = true } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu))
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBars) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.openTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    tab.label,
                                    maxLines = 1,
                                    fontWeight = if (route == tab.route) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = BrandRed,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = BrandRed
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    onOpen = openArticle,
                    onCategory = openCategory,
                    onLibrary = { nav.openTab("library") },
                    onQuick = { nav.openTab("quick") },
                    onLocal = { nav.openTab("local") },
                    onSubmit = { nav.navigate("submit") }
                )
            }
            composable("submit") { SubmitScreen(onHome = { nav.openTab("home") }) }
            composable("quick") { QuickScreen(onOpen = openArticle, onHome = { nav.openTab("home") }) }
            composable("local") { LocalScreen(onOpen = openArticle) }
            composable("categories") {
                CategoriesScreen { cat -> openCategory(cat.id, cat.name) }
            }
            composable("videos") {
                VideosScreen(onPlay = { nav.navigate("video/$it") }, onShort = { nav.navigate("shorts/$it") })
            }
            composable("video/{id}") { entry -> VideoPlayerScreen(entry.arguments?.getString("id").orEmpty()) }
            composable(
                "shorts/{index}",
                arguments = listOf(navArgument("index") { type = NavType.IntType })
            ) { entry -> ShortsScreen(entry.arguments?.getInt("index") ?: 0, onBack = { nav.popBackStack() }) }
            composable("library") { LibraryScreen(onOpen = openArticle, onHome = { nav.openTab("home") }) }
            composable("search") {
                SearchScreen(onOpen = openArticle, onCategory = openCategory, onVideo = { nav.navigate("video/$it") })
            }
            composable("menu") {
                MenuScreen(
                    onClose = { nav.popBackStack() },
                    onTab = { tab -> nav.popBackStack(); nav.openTab(tab) },
                    onRoute = { target -> nav.popBackStack(); nav.navigate(target) },
                    onCategory = { id, name -> nav.popBackStack(); openCategory(id, name) }
                )
            }
            composable("about") { AboutScreen(onHome = { nav.openTab("home") }, onCategory = openCategory) }
            composable("contact") { ContactScreen() }
            composable("privacy") { PrivacyScreen() }
            composable("settings") { SettingsScreen(onSaved = { nav.openTab("library") }) }
            composable(
                "article/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                ArticleScreen(
                    entry.arguments?.getInt("id") ?: 0,
                    onOpen = openArticle,
                    onBack = { nav.popBackStack() },
                    onSubmit = { nav.navigate("submit") }
                )
            }
            composable(
                "category/{id}/{name}",
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { entry ->
                CategoryPostsScreen(
                    categoryId = entry.arguments?.getInt("id") ?: 0,
                    categoryName = Uri.decode(entry.arguments?.getString("name").orEmpty()),
                    onOpen = openArticle,
                    onBack = { nav.popBackStack() },
                    onAllCategories = { nav.navigate("categories") },
                    onLibrary = { nav.openTab("library") },
                    onSubmit = { nav.navigate("submit") }
                )
            }
        }
    }
}
