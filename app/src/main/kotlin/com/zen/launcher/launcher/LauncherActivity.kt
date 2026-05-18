package com.zen.launcher.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zen.launcher.services.NotificationFilterService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BgTheme(val label: String) {
    BLACK("Pure black"),
    DARK_GRID("Dark grid"),
    DARK_DOTS("Dark dots"),
    WARM("Off-white warm"),
    WARM_GRID("Warm grid"),
    WARM_DOTS("Dotted paper"),
    CHARCOAL("Charcoal"),
    CHARCOAL_GRID("Charcoal grid"),
    CHARCOAL_DOTS("Charcoal dots"),
}

data class ThemeColors(
    val bg: Color,
    val text: Color,
    val textMuted: Color,
    val divider: Color
)

data class AppItem(val label: String, val packageName: String)
data class TodayStats(
    val opens: Int,
    val intentionalPauses: Int,
    val blockedNotifications: Int
)

enum class Screen { ONBOARDING, HOME, SETTINGS }

private val DefaultAllowedPackages = listOf(
    "com.spotify.music",
    "com.amazon.kindle",
    "com.whatsapp",
    "com.google.android.apps.maps"
)

fun BgTheme.colors() = when (this) {
    BgTheme.BLACK, BgTheme.DARK_GRID, BgTheme.DARK_DOTS -> ThemeColors(
        bg = Color(0xFF000000),
        text = Color.White,
        textMuted = Color.White.copy(alpha = 0.35f),
        divider = Color.White.copy(alpha = 0.08f)
    )

    BgTheme.WARM, BgTheme.WARM_GRID, BgTheme.WARM_DOTS -> ThemeColors(
        bg = Color(0xFFF5F0E8),
        text = Color(0xFF1A1A1A),
        textMuted = Color(0xFF1A1A1A).copy(alpha = 0.35f),
        divider = Color(0xFF1A1A1A).copy(alpha = 0.1f)
    )

    BgTheme.CHARCOAL, BgTheme.CHARCOAL_GRID, BgTheme.CHARCOAL_DOTS -> ThemeColors(
        bg = Color(0xFF1C1C1E),
        text = Color.White,
        textMuted = Color.White.copy(alpha = 0.35f),
        divider = Color.White.copy(alpha = 0.07f)
    )
}

fun Modifier.bgPattern(theme: BgTheme): Modifier = this.drawBehind {
    val c = theme.colors()
    drawRect(color = c.bg)
    when (theme) {
        BgTheme.DARK_GRID -> drawGrid(Color.White.copy(alpha = 0.06f), 32.dp.toPx())
        BgTheme.DARK_DOTS -> drawDots(Color.White.copy(alpha = 0.18f), 24.dp.toPx())
        BgTheme.WARM_GRID -> drawGrid(Color.Black.copy(alpha = 0.08f), 28.dp.toPx())
        BgTheme.WARM_DOTS -> drawDots(Color.Black.copy(alpha = 0.20f), 20.dp.toPx())
        BgTheme.CHARCOAL_GRID -> drawGrid(Color.White.copy(alpha = 0.05f), 32.dp.toPx())
        BgTheme.CHARCOAL_DOTS -> drawDots(Color.White.copy(alpha = 0.15f), 22.dp.toPx())
        else -> Unit
    }
}

fun DrawScope.drawGrid(color: Color, spacing: Float) {
    var x = 0f
    while (x <= size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += spacing
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += spacing
    }
}

fun DrawScope.drawDots(color: Color, spacing: Float) {
    var x = spacing / 2
    while (x <= size.width) {
        var y = spacing / 2
        while (y <= size.height) {
            drawCircle(color, radius = 1.5f, center = Offset(x, y))
            y += spacing
        }
        x += spacing
    }
}

fun Context.getPrefs() = getSharedPreferences("zen_prefs", Context.MODE_PRIVATE)

fun Context.getAllowedPackages(): MutableSet<String> =
    getPrefs().getStringSet("allowed_packages", DefaultAllowedPackages.toSet())!!.toMutableSet()

fun Context.getAllowedPackageOrder(allowedPackages: Set<String> = getAllowedPackages()): List<String> {
    val saved = getPrefs().getString("allowed_package_order", null)
        ?.split("|")
        ?.filter { it.isNotBlank() }
        ?: DefaultAllowedPackages
    return normalizeAllowedOrder(saved, allowedPackages)
}

fun Context.saveAllowedAppState(pkgs: Set<String>, order: List<String>) {
    val normalizedOrder = normalizeAllowedOrder(order, pkgs)
    getPrefs().edit()
        .putStringSet("allowed_packages", pkgs)
        .putString("allowed_package_order", normalizedOrder.joinToString("|"))
        .apply()
}

fun Context.getAllowedNotifPackages(): MutableSet<String> =
    getPrefs().getStringSet("allowed_notif_packages", mutableSetOf("com.whatsapp"))!!.toMutableSet()

fun Context.saveAllowedNotifPackages(pkgs: Set<String>) =
    getPrefs().edit().putStringSet("allowed_notif_packages", pkgs).apply()

fun Context.is24Hour(): Boolean = getPrefs().getBoolean("is_24_hour", true)
fun Context.save24Hour(v: Boolean) = getPrefs().edit().putBoolean("is_24_hour", v).apply()

fun Context.showAppIcons(): Boolean = getPrefs().getBoolean("show_app_icons", false)
fun Context.saveShowAppIcons(v: Boolean) = getPrefs().edit().putBoolean("show_app_icons", v).apply()

fun Context.useIntentionalPause(): Boolean = getPrefs().getBoolean("intentional_pause", true)
fun Context.saveIntentionalPause(v: Boolean) = getPrefs().edit().putBoolean("intentional_pause", v).apply()

fun Context.isOnboardingDone(): Boolean = getPrefs().getBoolean("onboarding_done", false)
fun Context.saveOnboardingDone() = getPrefs().edit().putBoolean("onboarding_done", true).apply()

fun Context.getBgTheme(): BgTheme {
    val name = getPrefs().getString("bg_theme", BgTheme.BLACK.name) ?: BgTheme.BLACK.name
    return runCatching { BgTheme.valueOf(name) }.getOrDefault(BgTheme.BLACK)
}

fun Context.saveBgTheme(t: BgTheme) = getPrefs().edit().putString("bg_theme", t.name).apply()

fun normalizeAllowedOrder(order: List<String>, allowedPackages: Set<String>): List<String> {
    val ordered = order.distinct().filter { it in allowedPackages }
    val missing = allowedPackages.filterNot { it in ordered }.sorted()
    return ordered + missing
}

fun Context.isNotificationListenerEnabled(): Boolean {
    val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        ?: return false
    val serviceName = ComponentName(this, NotificationFilterService::class.java).flattenToString()
    return enabled.split(":").any { it.equals(serviceName, ignoreCase = true) }
}

fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun Context.resetDailyStatsIfNeeded() {
    val prefs = getPrefs()
    val today = todayKey()
    if (prefs.getString("stats_day", null) == today) return
    prefs.edit()
        .putString("stats_day", today)
        .putInt("stats_opens", 0)
        .putInt("stats_intentional_pauses", 0)
        .putInt("stats_blocked_notifications", 0)
        .putString("notification_digest", "")
        .apply()
}

fun Context.getTodayStats(): TodayStats {
    resetDailyStatsIfNeeded()
    val prefs = getPrefs()
    return TodayStats(
        opens = prefs.getInt("stats_opens", 0),
        intentionalPauses = prefs.getInt("stats_intentional_pauses", 0),
        blockedNotifications = prefs.getInt("stats_blocked_notifications", 0)
    )
}

fun Context.recordAppOpen() {
    resetDailyStatsIfNeeded()
    val prefs = getPrefs()
    prefs.edit().putInt("stats_opens", prefs.getInt("stats_opens", 0) + 1).apply()
}

fun Context.recordIntentionalPause() {
    resetDailyStatsIfNeeded()
    val prefs = getPrefs()
    prefs.edit()
        .putInt("stats_intentional_pauses", prefs.getInt("stats_intentional_pauses", 0) + 1)
        .apply()
}

fun Context.recordBlockedNotification(packageName: String) {
    resetDailyStatsIfNeeded()
    val prefs = getPrefs()
    val digest = prefs.getString("notification_digest", "").orEmpty()
        .split("|")
        .filter { it.contains("=") }
        .associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        .toMutableMap()
    digest[packageName] = (digest[packageName] ?: 0) + 1
    prefs.edit()
        .putInt("stats_blocked_notifications", prefs.getInt("stats_blocked_notifications", 0) + 1)
        .putString("notification_digest", digest.entries.joinToString("|") { "${it.key}=${it.value}" })
        .apply()
}

fun Context.getNotificationDigest(): Map<String, Int> {
    resetDailyStatsIfNeeded()
    return getPrefs().getString("notification_digest", "").orEmpty()
        .split("|")
        .filter { it.contains("=") }
        .associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        .filterValues { it > 0 }
}

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenApp() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}
}

@Composable
fun ZenApp() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(if (context.isOnboardingDone()) Screen.HOME else Screen.ONBOARDING) }
    var allowedPackages by remember { mutableStateOf(context.getAllowedPackages()) }
    var allowedPackageOrder by remember { mutableStateOf(context.getAllowedPackageOrder(allowedPackages)) }
    var allowedNotifPackages by remember { mutableStateOf(context.getAllowedNotifPackages()) }
    var is24Hour by remember { mutableStateOf(context.is24Hour()) }
    var bgTheme by remember { mutableStateOf(context.getBgTheme()) }
    var showIcons by remember { mutableStateOf(context.showAppIcons()) }
    var intentionalPause by remember { mutableStateOf(context.useIntentionalPause()) }
    var todayStats by remember { mutableStateOf(context.getTodayStats()) }
    var notificationDigest by remember { mutableStateOf(context.getNotificationDigest()) }

    fun refreshStats() {
        todayStats = context.getTodayStats()
        notificationDigest = context.getNotificationDigest()
    }

    when (screen) {
        Screen.ONBOARDING -> OnboardingScreen(
            bgTheme = bgTheme,
            allowedPackages = allowedPackages,
            allowedPackageOrder = allowedPackageOrder,
            allowedNotifPackages = allowedNotifPackages,
            showIcons = showIcons,
            onAllowedAppsChange = { packages, order ->
                allowedPackages = packages.toMutableSet()
                allowedPackageOrder = normalizeAllowedOrder(order, packages)
                context.saveAllowedAppState(allowedPackages, allowedPackageOrder)
            },
            onAllowedNotifPackagesChange = {
                allowedNotifPackages = it.toMutableSet()
                context.saveAllowedNotifPackages(allowedNotifPackages)
            },
            onShowIconsChange = {
                showIcons = it
                context.saveShowAppIcons(it)
            },
            onFinish = {
                context.saveOnboardingDone()
                screen = Screen.HOME
            }
        )

        Screen.HOME -> ZenHome(
            allowedPackages = allowedPackages,
            allowedPackageOrder = allowedPackageOrder,
            is24Hour = is24Hour,
            bgTheme = bgTheme,
            showIcons = showIcons,
            intentionalPause = intentionalPause,
            onStatsChanged = { refreshStats() },
            onOpenSettings = { screen = Screen.SETTINGS }
        )

        Screen.SETTINGS -> SettingsScreen(
            allowedPackages = allowedPackages,
            allowedPackageOrder = allowedPackageOrder,
            allowedNotifPackages = allowedNotifPackages,
            is24Hour = is24Hour,
            bgTheme = bgTheme,
            showIcons = showIcons,
            intentionalPause = intentionalPause,
            todayStats = todayStats,
            notificationDigest = notificationDigest,
            onAllowedAppsChange = { packages, order ->
                allowedPackages = packages.toMutableSet()
                allowedPackageOrder = normalizeAllowedOrder(order, packages)
                context.saveAllowedAppState(allowedPackages, allowedPackageOrder)
            },
            onAllowedNotifPackagesChange = {
                allowedNotifPackages = it.toMutableSet()
                context.saveAllowedNotifPackages(allowedNotifPackages)
            },
            onClockChange = {
                is24Hour = it
                context.save24Hour(it)
            },
            onThemeChange = {
                bgTheme = it
                context.saveBgTheme(it)
            },
            onShowIconsChange = {
                showIcons = it
                context.saveShowAppIcons(it)
            },
            onIntentionalPauseChange = {
                intentionalPause = it
                context.saveIntentionalPause(it)
            },
            onRefreshStats = { refreshStats() },
            onBack = {
                refreshStats()
                screen = Screen.HOME
            }
        )
    }
}

@Composable
fun OnboardingScreen(
    bgTheme: BgTheme,
    allowedPackages: Set<String>,
    allowedPackageOrder: List<String>,
    allowedNotifPackages: Set<String>,
    showIcons: Boolean,
    onAllowedAppsChange: (Set<String>, List<String>) -> Unit,
    onAllowedNotifPackagesChange: (Set<String>) -> Unit,
    onShowIconsChange: (Boolean) -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val colors = bgTheme.colors()
    val apps = remember { launcherApps(context) }
    var filter by remember { mutableStateOf("") }
    val visibleApps = remember(apps, filter, allowedPackages, allowedPackageOrder) {
        val appByPackage = apps.associateBy { it.packageName }
        val allowed = allowedPackageOrder.mapNotNull { appByPackage[it] }
        val rest = apps.filterNot { it.packageName in allowedPackages }
        val query = filter.trim()
        (allowed + rest).filter {
            query.isEmpty() || it.label.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .bgPattern(bgTheme)
            .padding(horizontal = 40.dp)
            .padding(top = 72.dp, bottom = 42.dp)
    ) {
        Text("Zen", color = colors.text, fontSize = 36.sp, fontWeight = FontWeight.Thin)
        Spacer(Modifier.height(10.dp))
        Text("Choose what belongs on your home screen.", color = colors.textMuted, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))

        SettingsRow(label = "Show app icons", colors = colors) {
            Switch(checked = showIcons, onCheckedChange = onShowIconsChange, colors = zenSwitchColors(colors))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            if (context.isNotificationListenerEnabled()) {
                "Notification access is on."
            } else {
                "Notification filtering needs access."
            },
            color = colors.textMuted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "open notification access",
            color = colors.text.copy(alpha = 0.75f),
            fontSize = 13.sp,
            modifier = Modifier.clickable {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        )
        Spacer(Modifier.height(20.dp))
        ZenSearchField(value = filter, onValueChange = { filter = it }, colors = colors)
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(visibleApps, key = { it.packageName }) { app ->
                AppPermissionRow(
                    app = app,
                    isAllowed = app.packageName in allowedPackages,
                    colors = colors,
                    showIcon = showIcons,
                    onToggle = {
                        if (app.packageName in allowedPackages) {
                            val newPackages = allowedPackages - app.packageName
                            onAllowedAppsChange(newPackages, allowedPackageOrder - app.packageName)
                            onAllowedNotifPackagesChange(allowedNotifPackages - app.packageName)
                        } else {
                            val newPackages = allowedPackages + app.packageName
                            onAllowedAppsChange(newPackages, allowedPackageOrder + app.packageName)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "finish setup",
            color = colors.text,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFinish() }
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ZenClock(is24Hour: Boolean, textColor: Color, mutedColor: Color) {
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    LaunchedEffect(is24Hour) {
        while (true) {
            val now = Date()
            time = SimpleDateFormat(if (is24Hour) "HH:mm" else "hh:mm a", Locale.getDefault()).format(now)
            date = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now)
            delay(1000)
        }
    }
    Column(horizontalAlignment = Alignment.Start) {
        Text(time, color = textColor, fontSize = 72.sp, fontWeight = FontWeight.Thin)
        Text(date, color = mutedColor, fontSize = 16.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
fun ZenHome(
    allowedPackages: Set<String>,
    allowedPackageOrder: List<String>,
    is24Hour: Boolean,
    bgTheme: BgTheme,
    showIcons: Boolean,
    intentionalPause: Boolean,
    onStatsChanged: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val colors = bgTheme.colors()
    var pendingPackage by remember { mutableStateOf<String?>(null) }
    var remainingPause by remember { mutableStateOf(0) }

    val apps = remember(allowedPackages, allowedPackageOrder) {
        val orderIndex = allowedPackageOrder.withIndex().associate { it.value to it.index }
        launcherApps(context)
            .filter { it.packageName in allowedPackages }
            .sortedWith(compareBy<AppItem> { orderIndex[it.packageName] ?: Int.MAX_VALUE }.thenBy { it.label })
    }

    LaunchedEffect(pendingPackage) {
        while (pendingPackage != null && remainingPause > 0) {
            delay(1000)
            remainingPause -= 1
        }
    }

    fun openApp(app: AppItem) {
        pm.getLaunchIntentForPackage(app.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.recordAppOpen()
            onStatsChanged()
            context.startActivity(it)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .bgPattern(bgTheme)
            .padding(horizontal = 48.dp),
        contentPadding = PaddingValues(top = 100.dp, bottom = 64.dp)
    ) {
        item {
            ZenClock(is24Hour, colors.text, colors.textMuted)
            Spacer(Modifier.height(64.dp))
        }
        items(apps, key = { it.packageName }) { app ->
            val isPending = pendingPackage == app.packageName
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!intentionalPause) {
                            openApp(app)
                        } else if (isPending && remainingPause == 0) {
                            pendingPackage = null
                            openApp(app)
                        } else {
                            pendingPackage = app.packageName
                            remainingPause = 3
                            context.recordIntentionalPause()
                            onStatsChanged()
                        }
                    }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcons) {
                    AppIcon(app.packageName, Modifier.size(24.dp))
                    Spacer(Modifier.width(14.dp))
                }
                Text(
                    text = when {
                        isPending && remainingPause > 0 -> "${app.label}  $remainingPause"
                        isPending -> "${app.label}  open"
                        else -> app.label
                    },
                    color = if (isPending) colors.text else colors.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraLight
                )
            }
        }
        item {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "...",
                color = colors.textMuted,
                fontSize = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSettings() }
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsScreen(
    allowedPackages: Set<String>,
    allowedPackageOrder: List<String>,
    allowedNotifPackages: Set<String>,
    is24Hour: Boolean,
    bgTheme: BgTheme,
    showIcons: Boolean,
    intentionalPause: Boolean,
    todayStats: TodayStats,
    notificationDigest: Map<String, Int>,
    onAllowedAppsChange: (Set<String>, List<String>) -> Unit,
    onAllowedNotifPackagesChange: (Set<String>) -> Unit,
    onClockChange: (Boolean) -> Unit,
    onThemeChange: (BgTheme) -> Unit,
    onShowIconsChange: (Boolean) -> Unit,
    onIntentionalPauseChange: (Boolean) -> Unit,
    onRefreshStats: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) }
    var appFilter by remember { mutableStateOf("") }
    var notificationAccessEnabled by remember { mutableStateOf(context.isNotificationListenerEnabled()) }
    val colors = bgTheme.colors()

    val allApps = remember { launcherApps(context) }
    val orderedApps = remember(allApps, allowedPackages, allowedPackageOrder, appFilter) {
        val appByPackage = allApps.associateBy { it.packageName }
        val allowed = allowedPackageOrder.mapNotNull { appByPackage[it] }
        val rest = allApps.filterNot { it.packageName in allowedPackages }
        val query = appFilter.trim()
        (allowed + rest).filter {
            query.isEmpty() ||
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .bgPattern(bgTheme)
            .padding(horizontal = 40.dp)
            .padding(top = 72.dp, bottom = 48.dp)
    ) {
        Text("Settings", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Thin)
        Spacer(Modifier.height(28.dp))

        SettingsRow(label = "24-hour clock", colors = colors) {
            Switch(
                checked = is24Hour,
                onCheckedChange = onClockChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.bg,
                    checkedTrackColor = colors.text,
                    uncheckedThumbColor = colors.text,
                    uncheckedTrackColor = colors.text.copy(alpha = 0.2f)
                )
            )
        }
        Spacer(Modifier.height(14.dp))
        SettingsRow(label = "App icons", colors = colors) {
            Switch(
                checked = showIcons,
                onCheckedChange = onShowIconsChange,
                colors = zenSwitchColors(colors)
            )
        }
        Spacer(Modifier.height(14.dp))
        SettingsRow(label = "Intentional open pause", colors = colors) {
            Switch(
                checked = intentionalPause,
                onCheckedChange = onIntentionalPauseChange,
                colors = zenSwitchColors(colors)
            )
        }

        Spacer(Modifier.height(22.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(22.dp))

        Text("Background", color = colors.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(BgTheme.entries.toList()) { theme ->
                val tc = theme.colors()
                val isSelected = theme == bgTheme
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onThemeChange(theme) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .bgPattern(theme)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, colors.text, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier.border(1.dp, colors.text.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("21:00", color = tc.text, fontSize = 9.sp, fontWeight = FontWeight.Thin)
                            Spacer(Modifier.height(3.dp))
                            Text("App", color = tc.text.copy(alpha = 0.5f), fontSize = 7.sp)
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = theme.label.split(" ").last(),
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            listOf("Apps", "Notifications", "Today").forEachIndexed { i, label ->
                Text(
                    text = label,
                    color = if (activeTab == i) colors.text else colors.textMuted,
                    fontSize = 15.sp,
                    fontWeight = if (activeTab == i) FontWeight.Medium else FontWeight.Light,
                    modifier = Modifier
                        .clickable {
                            activeTab = i
                            appFilter = ""
                            notificationAccessEnabled = context.isNotificationListenerEnabled()
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }

        if (activeTab != 2) {
            Spacer(Modifier.height(12.dp))
            ZenSearchField(
                value = appFilter,
                onValueChange = { appFilter = it },
                colors = colors
            )
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(18.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (activeTab) {
                0 -> {
                    items(orderedApps, key = { it.packageName }) { app ->
                        AppPermissionRow(
                            app = app,
                            isAllowed = app.packageName in allowedPackages,
                            colors = colors,
                            showIcon = showIcons,
                            orderControls = if (app.packageName in allowedPackages) {
                                {
                                    AppOrderControls(
                                        app = app,
                                        order = allowedPackageOrder,
                                        colors = colors,
                                        onMove = { newOrder -> onAllowedAppsChange(allowedPackages, newOrder) }
                                    )
                                }
                            } else {
                                null
                            },
                            onToggle = {
                                if (app.packageName in allowedPackages) {
                                    val newPackages = allowedPackages - app.packageName
                                    onAllowedAppsChange(newPackages, allowedPackageOrder - app.packageName)
                                } else {
                                    val newPackages = allowedPackages + app.packageName
                                    onAllowedAppsChange(newPackages, allowedPackageOrder + app.packageName)
                                }
                            }
                        )
                    }
                }

                1 -> {
                    item {
                        NotificationAccessRow(
                            enabled = notificationAccessEnabled,
                            colors = colors,
                            onOpenSettings = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                notificationAccessEnabled = context.isNotificationListenerEnabled()
                            },
                            onRefresh = {
                                notificationAccessEnabled = context.isNotificationListenerEnabled()
                                onRefreshStats()
                            }
                        )
                    }
                    if (notificationDigest.isNotEmpty()) {
                        item {
                            NotificationDigest(digest = notificationDigest, allApps = allApps, colors = colors)
                        }
                    }
                    items(orderedApps, key = { it.packageName }) { app ->
                        AppPermissionRow(
                            app = app,
                            isAllowed = app.packageName in allowedNotifPackages,
                            colors = colors,
                            showIcon = showIcons,
                            onToggle = {
                                val newPackages = if (app.packageName in allowedNotifPackages) {
                                    allowedNotifPackages - app.packageName
                                } else {
                                    allowedNotifPackages + app.packageName
                                }
                                onAllowedNotifPackagesChange(newPackages)
                            }
                        )
                    }
                }

                else -> {
                    item {
                        TodayStatsPanel(
                            stats = todayStats,
                            colors = colors,
                            onRefresh = onRefreshStats
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "back",
            color = colors.textMuted,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onBack() }
        )
    }
}

@Composable
fun SettingsRow(label: String, colors: ThemeColors, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.text.copy(alpha = 0.8f), fontSize = 16.sp)
        trailing()
    }
}

@Composable
fun zenSwitchColors(colors: ThemeColors) = SwitchDefaults.colors(
    checkedThumbColor = colors.bg,
    checkedTrackColor = colors.text,
    uncheckedThumbColor = colors.text,
    uncheckedTrackColor = colors.text.copy(alpha = 0.2f)
)

@Composable
fun ZenSearchField(value: String, onValueChange: (String) -> Unit, colors: ThemeColors) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Search apps", color = colors.textMuted) },
        textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 15.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.text.copy(alpha = 0.35f),
            unfocusedBorderColor = colors.divider,
            cursorColor = colors.text,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text
        )
    )
}

@Composable
fun AppPermissionRow(
    app: AppItem,
    isAllowed: Boolean,
    colors: ThemeColors,
    showIcon: Boolean,
    orderControls: (@Composable () -> Unit)? = null,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            AppIcon(app.packageName, Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(
            app.label,
            modifier = Modifier.weight(1f),
            color = if (isAllowed) colors.text else colors.textMuted,
            fontSize = 16.sp,
            fontWeight = if (isAllowed) FontWeight.Normal else FontWeight.Light
        )
        orderControls?.invoke()
        if (isAllowed) {
            Spacer(Modifier.width(6.dp))
            Text("ok", color = colors.textMuted, fontSize = 12.sp)
        }
    }
}

@Composable
fun AppOrderControls(
    app: AppItem,
    order: List<String>,
    colors: ThemeColors,
    onMove: (List<String>) -> Unit
) {
    val index = order.indexOf(app.packageName)
    Row {
        TextButton(
            enabled = index > 0,
            contentPadding = PaddingValues(horizontal = 6.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = colors.textMuted,
                disabledContentColor = colors.divider
            ),
            onClick = { onMove(order.swap(index, index - 1)) }
        ) {
            Text("up", fontSize = 12.sp)
        }
        TextButton(
            enabled = index >= 0 && index < order.lastIndex,
            contentPadding = PaddingValues(horizontal = 6.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = colors.textMuted,
                disabledContentColor = colors.divider
            ),
            onClick = { onMove(order.swap(index, index + 1)) }
        ) {
            Text("down", fontSize = 12.sp)
        }
    }
}

@Composable
fun NotificationAccessRow(
    enabled: Boolean,
    colors: ThemeColors,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            if (enabled) "Notification access is on." else "Notification access is off.",
            color = if (enabled) colors.text.copy(alpha = 0.8f) else colors.textMuted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "open access settings",
                color = colors.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onOpenSettings() }
            )
            Text(
                "refresh",
                color = colors.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onRefresh() }
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = colors.divider)
    }
}

@Composable
fun NotificationDigest(digest: Map<String, Int>, allApps: List<AppItem>, colors: ThemeColors) {
    val labels = allApps.associate { it.packageName to it.label }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text("Blocked today", color = colors.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        digest.entries.sortedByDescending { it.value }.take(5).forEach { (packageName, count) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(labels[packageName] ?: packageName.substringAfterLast("."), color = colors.text, fontSize = 14.sp)
                Text(count.toString(), color = colors.textMuted, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = colors.divider)
    }
}

@Composable
fun TodayStatsPanel(stats: TodayStats, colors: ThemeColors, onRefresh: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Today", color = colors.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        StatRow("Apps opened", stats.opens, colors)
        StatRow("Intentional pauses", stats.intentionalPauses, colors)
        StatRow("Notifications blocked", stats.blockedNotifications, colors)
        Spacer(Modifier.height(14.dp))
        Text(
            "refresh",
            color = colors.textMuted,
            fontSize = 13.sp,
            modifier = Modifier.clickable { onRefresh() }
        )
    }
}

@Composable
fun StatRow(label: String, value: Int, colors: ThemeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Light)
        Text(value.toString(), color = colors.textMuted, fontSize = 16.sp)
    }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(5.dp)),
        factory = {
            ImageView(it).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(context.packageManager.getApplicationIcon(packageName))
            }
        },
        update = {
            it.setImageDrawable(context.packageManager.getApplicationIcon(packageName))
        }
    )
}

fun List<String>.swap(from: Int, to: Int): List<String> {
    if (from !in indices || to !in indices) return this
    return toMutableList().also {
        val moving = it[from]
        it[from] = it[to]
        it[to] = moving
    }
}

fun launcherApps(context: Context): List<AppItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(intent, 0)
        .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase(Locale.getDefault()) }
}
