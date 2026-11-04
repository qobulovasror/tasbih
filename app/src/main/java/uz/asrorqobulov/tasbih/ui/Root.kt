package uz.asrorqobulov.tasbih.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import uz.asrorqobulov.tasbih.data.Stats
import java.time.LocalDate
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.asrorqobulov.tasbih.R
import uz.asrorqobulov.tasbih.TasbihApp
import uz.asrorqobulov.tasbih.data.AppSettings
import uz.asrorqobulov.tasbih.session.State

@Composable
fun TasbihRoot(app: TasbihApp) {
    val settings by app.settings.flow.collectAsState(initial = null)
    val session by app.session.ui.collectAsState()
    val s = settings ?: return
    TasbihTheme(s) {
        when {
            !s.onboardingDone -> OnboardingScreen(app, s)
            session.state != State.IDLE -> SessionScreen(app, session, s)
            else -> MainNav(app, s)
        }
        session.result?.let { r ->
            val min = (r.durationMs / 60_000).toInt()
            // shu sessiyada kunlik maqsad bajarildimi (yozuv bazaga tushishini kutib)
            val goalDone by produceState(false, r) {
                if (s.dailyGoal <= 0) return@produceState
                delay(300)
                val today = LocalDate.now()
                val t = Stats(app.dao.sessionsOnce()).total(today, today)
                value = t >= s.dailyGoal && t - r.counted < s.dailyGoal
            }
            AlertDialog(
                onDismissRequest = app.session::dismissResult,
                title = { Text(stringResource(R.string.result_title)) },
                text = {
                    Text(
                        stringResource(R.string.result_line, fmt(r.counted), min) +
                            (if (r.reached) " · " + stringResource(R.string.target_reached) + " ✓" else "") +
                            (if (goalDone) "\n🎉 " + stringResource(R.string.goal_done) else ""),
                    )
                },
                confirmButton = { TextButton(onClick = app.session::dismissResult) { Text(stringResource(R.string.ok)) } },
            )
        }
    }
}

private data class Tab(val route: String, val label: Int, val outlined: ImageVector, val filled: ImageVector)

private val tabs = listOf(
    Tab("home", R.string.nav_home, Icons.Outlined.RadioButtonChecked, Icons.Filled.RadioButtonChecked),
    Tab("stats", R.string.nav_stats, Icons.Outlined.BarChart, Icons.Filled.BarChart),
    Tab("settings", R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
)
private fun isTab(route: String?) = tabs.any { it.route == route }

@Composable
private fun MainNav(app: TasbihApp, s: AppSettings) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val quick by app.quickStart.collectAsState()
    LaunchedEffect(quick) { if (quick && route != "home") nav.navigate("home") { popUpTo("home"); launchSingleTop = true } }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTab(route)) BottomBar(route) { r ->
                nav.navigate(r) {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
    ) { pad ->
        // consumeWindowInsets — ekranlardagi statusBarsPadding ikki marta qo'shilmasin
        NavHost(
            nav, startDestination = "home", modifier = Modifier.padding(pad).consumeWindowInsets(pad),
            enterTransition = {
                if (isTab(targetState.destination.route)) fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.97f)
                else slideInHorizontally(tween(280)) { it / 5 } + fadeIn(tween(280))
            },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { slideOutHorizontally(tween(240)) { it / 5 } + fadeOut(tween(200)) },
        ) {
            composable("home") {
                HomeScreen(app, s, onEdit = { id -> nav.navigate("edit?id=${id ?: ""}") }, onStats = { nav.navigate("stats") }, onPermissions = { nav.navigate("permissions") })
            }
            composable("stats") { StatsScreen(app) }
            composable("settings") {
                SettingsScreen(app, s, onTemplates = { nav.navigate("templates") }, onBackup = { nav.navigate("backup") }, onPermissions = { nav.navigate("permissions") })
            }
            composable("edit?id={id}", arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" })) {
                TemplateEditScreen(app, it.arguments?.getString("id").orEmpty(), onDone = { nav.popBackStack() })
            }
            composable("templates") { TemplatesScreen(app, onBack = { nav.popBackStack() }, onEdit = { id -> nav.navigate("edit?id=${id ?: ""}") }) }
            composable("backup") { BackupScreen(app, s, onBack = { nav.popBackStack() }) }
            composable("permissions") { PermissionsScreen(app, s, onBack = { nav.popBackStack() }) }
        }
    }
}

/** Pastki menyu: aktiv tab — kengayuvchi "pill", to'ldirilgan ikonka va accent rang. */
@Composable
private fun BottomBar(route: String?, onSelect: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.background(c.surfaceContainer)) {
        HorizontalDivider(color = c.outlineVariant)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp)) {
            tabs.forEach { t ->
                val on = route == t.route
                val w by animateDpAsState(if (on) 60.dp else 32.dp, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "pill")
                val bg by animateColorAsState(if (on) c.primary.copy(alpha = 0.16f) else Color.Transparent, label = "bg")
                val fg by animateColorAsState(if (on) c.primary else c.onSurfaceVariant, label = "fg")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(t.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.width(w).height(32.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                        Icon(if (on) t.filled else t.outlined, null, tint = fg)
                    }
                    Text(
                        stringResource(t.label), fontSize = 12.sp, color = fg,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
