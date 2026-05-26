package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.viewmodel.StreamRewardsViewModel
import java.text.SimpleDateFormat
import java.util.*

// Styling Color Palette - Streaming Movie Dark Theme
val CinemaDarkReal = Color(0xFF0F0E13)
val CardBackgroundPurple = Color(0xFF1B1824)
val NeonCyan = Color(0xFF00E5FF)
val ElectricPink = Color(0xFFFF2A6D)
val GlowPurple = Color(0xFF6200EE)
val WarningOrange = Color(0xFFFF9100)
val SoftGray = Color(0xFFA5A2B5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamRewardsApp(viewModel: StreamRewardsViewModel) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeRedemptions by viewModel.activeRedemptions.collectAsStateWithLifecycle()
    val accountsPool by viewModel.accountsPool.collectAsStateWithLifecycle()
    val pointsLogs by viewModel.pointsLogs.collectAsStateWithLifecycle()
    val dailyMissions by viewModel.dailyMissions.collectAsStateWithLifecycle()
    val misionesProgress by viewModel.misionesProgress.collectAsStateWithLifecycle()
    val tournamentsLeaderboard by viewModel.tournamentsLeaderboard.collectAsStateWithLifecycle()
    val raffleParticipants by viewModel.raffleParticipants.collectAsStateWithLifecycle()
    val adminLogs by viewModel.adminLogs.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val adsTodayPoints by viewModel.simulatedAdLimit.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("home") }
    var showPointLogsDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.notificationEvents.collect { (title, text) ->
            NotificationHelper.showNotification(context, title, text)
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    if (!isLoggedIn) {
        AuthScreen(viewModel = viewModel)
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ConnectedTv,
                            contentDescription = "Logo",
                            tint = ElectricPink,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "StreamRewards Pro",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                        )
                    }
                },
                actions = {
                    // Profile/Level Status Pill
                    Surface(
                        onClick = { showPointLogsDialog = true },
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stars,
                                contentDescription = "Puntos",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentUser.pointsCurrent} pts",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    // Notification Bell Button with dynamic badge
                    IconButton(
                        onClick = { showNotificationsDialog = true }
                    ) {
                        BadgedBox(
                            badge = {
                                val unreadCount = notifications.size
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = ElectricPink,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadCount", fontSize = 10.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Alertas",
                                tint = if (notifications.isNotEmpty()) NeonCyan else SoftGray
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.logoutUser() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = SoftGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CinemaDarkReal
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CinemaDarkReal,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
                    label = { Text("Inicio", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = GlowPurple.copy(alpha = 0.4f),
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )
                NavigationBarItem(
                    selected = selectedTab == "ganar",
                    onClick = { selectedTab = "ganar" },
                    icon = { Icon(Icons.Filled.LocalActivity, contentDescription = "Misiones") },
                    label = { Text("Misiones", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = GlowPurple.copy(alpha = 0.4f),
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    ),
                    modifier = Modifier.testTag("nav_tab_earn")
                )
                NavigationBarItem(
                    selected = selectedTab == "canjear",
                    onClick = { selectedTab = "canjear" },
                    icon = { Icon(Icons.Filled.Redeem, contentDescription = "Canjes") },
                    label = { Text("Canjear", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = GlowPurple.copy(alpha = 0.4f),
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    ),
                    modifier = Modifier.testTag("nav_tab_redeem")
                )
                NavigationBarItem(
                    selected = selectedTab == "leaderboard",
                    onClick = { selectedTab = "leaderboard" },
                    icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = "Torneos") },
                    label = { Text("Torneos", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = GlowPurple.copy(alpha = 0.4f),
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    ),
                    modifier = Modifier.testTag("nav_tab_compete")
                )
                if (currentUser.email == "blandonjose6788@gmail.com") {
                    NavigationBarItem(
                        selected = selectedTab == "admin",
                        onClick = { selectedTab = "admin" },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Admin") },
                        label = { Text("CTO Panel", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricPink,
                            selectedTextColor = ElectricPink,
                            indicatorColor = ElectricPink.copy(alpha = 0.2f),
                            unselectedIconColor = SoftGray,
                            unselectedTextColor = SoftGray
                        ),
                        modifier = Modifier.testTag("nav_tab_admin")
                    )
                }
            }
        },
        containerColor = CinemaDarkReal
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CinemaDarkReal, CardBackgroundPurple, CinemaDarkReal)
                    )
                )
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "navigationTransition"
            ) { tab ->
                when (tab) {
                    "home" -> HomeScreen(currentUser, activeRedemptions, viewModel)
                    "ganar" -> EarnAndMissionsScreen(currentUser, dailyMissions, misionesProgress, adsTodayPoints, viewModel)
                    "canjear" -> RedeemCatalogScreen(currentUser, viewModel)
                    "leaderboard" -> TournamentsAndRafflesScreen(tournamentsLeaderboard, raffleParticipants, currentUser, viewModel)
                    "admin" -> {
                        if (currentUser.email == "blandonjose6788@gmail.com") {
                            AdminCockpitScreen(accountsPool, activeRedemptions, adminLogs, viewModel)
                        } else {
                            HomeScreen(currentUser, activeRedemptions, viewModel)
                        }
                    }
                }
            }
        }
    }

    // --- POINTS MOVEMENT LOG DIALOG ---
    if (showPointLogsDialog) {
        AlertDialog(
            onDismissRequest = { showPointLogsDialog = false },
            title = {
                Text(
                    "Historial de Puntos",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (pointsLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se han registrado movimientos de puntos todavía",
                            color = SoftGray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(pointsLogs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.detail,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = log.source.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = SoftGray
                                        )
                                    )
                                }
                                Text(
                                    text = if (log.delta >= 0) "+${log.delta}" else "${log.delta}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (log.delta >= 0) NeonCyan else ElectricPink
                                    )
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPointLogsDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                ) {
                    Text("Cerrar")
                }
            },
            containerColor = CardBackgroundPurple
        )
    }

    // --- INTEGRATED SYSTEM NOTIFICATIONS DIALOG ---
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Alertas",
                        tint = ElectricPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Notificaciones Pro",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsOff,
                                contentDescription = "Sin notificaciones",
                                tint = SoftGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No tienes notificaciones por el momento.\n¡Tus alertas de premios aparecerán aquí!",
                                color = SoftGray,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                        items(notifications) { notif ->
                            val iconPair = when (notif.category) {
                                "points" -> Pair(Icons.Filled.Stars, NeonCyan)
                                "reward" -> Pair(Icons.Filled.Redeem, ElectricPink)
                                "system" -> Pair(Icons.Filled.VerifiedUser, GlowPurple)
                                else -> Pair(Icons.Filled.Info, SoftGray)
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = iconPair.first,
                                        contentDescription = notif.category,
                                        tint = iconPair.second,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = notif.text,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = SoftGray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showNotificationsDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                ) {
                    Text("De acuerdo")
                }
            },
            containerColor = CardBackgroundPurple
        )
    }
    }
}

// ==========================================
// SCREEN 1: HOME DASHBOARD SCREEN
// ==========================================
@Composable
fun HomeScreen(
    user: User,
    activeLeases: List<ActiveRedemption>,
    viewModel: StreamRewardsViewModel
) {
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card / Level indicator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonCyan, GlowPurple)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hola, ${user.name}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Estrella de Streaming • Nivel ${user.level}",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Level thresholds indicator
                        val progress = when (user.level) {
                            1 -> user.pointsHistoric / 500f
                            2 -> (user.pointsHistoric - 500) / 1500f
                            3 -> (user.pointsHistoric - 2000) / 3000f
                            else -> 1.0f
                        }
                        val infoText = when (user.level) {
                            1 -> "${user.pointsHistoric}/500 puntos para Nvl 2"
                            2 -> "${user.pointsHistoric}/2000 puntos para Nvl 3"
                            3 -> "${user.pointsHistoric}/5000 puntos para Nvl 4"
                            else -> "¡Nivel Máximo Leyenda alcanzado!"
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonCyan,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = infoText,
                            style = MaterialTheme.typography.bodySmall.copy(color = SoftGray)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Surface(
                        color = ElectricPink.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(64.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(ElectricPink, GlowPurple)))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (user.level) {
                                    1 -> Icons.Filled.StarOutline
                                    2 -> Icons.Filled.StarHalf
                                    3 -> Icons.Filled.Stars
                                    else -> Icons.Filled.EmojiEvents
                                },
                                contentDescription = "Medalla Nivel",
                                tint = ElectricPink,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // Daily Streak Reclaiming Header (Login Bonus)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Recompensa Diaria",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Racha actual: ${user.rachaCheckIn} días",
                                color = SoftGray,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.claimDailyReward() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("claim_daily_checkin")
                        ) {
                            Text("Reclamar", fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1..7 Grid Visual Day items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..7).forEach { day ->
                            val isCompleted = day <= user.rachaCheckIn
                            val isCurrentAndClaimable = day == user.rachaCheckIn + 1 || (user.rachaCheckIn == 7 && day == 1)
                            val dayPoints = when (day) {
                                1 -> 5
                                2 -> 10
                                3 -> 15
                                4 -> 20
                                5 -> 25
                                6 -> 30
                                7 -> "50+"
                                else -> 5
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isCompleted -> GlowPurple
                                                isCurrentAndClaimable -> ElectricPink.copy(alpha = 0.4f)
                                                else -> Color.White.copy(alpha = 0.06f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Completado",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = dayPoints.toString(),
                                            color = if (isCurrentAndClaimable) Color.White else SoftGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Día $day",
                                    color = if (isCurrentAndClaimable) NeonCyan else SoftGray,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrentAndClaimable) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Leases Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tus accesos activos (${activeLeases.size}/3)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                if (activeLeases.isNotEmpty()) {
                    Text(
                        text = "Vencimiento dinámico",
                        color = SoftGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (activeLeases.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ConnectedTv,
                            contentDescription = "Sin Canjes",
                            tint = SoftGray.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tienes cuentas activas",
                            color = SoftGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Canjea puntos ganados por acceso a Netflix, Disney+, Prime o Max",
                            color = SoftGray.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeLeases, key = { it.id }) { lease ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = when (lease.serviceType) {
                                        ServiceType.NETFLIX -> Color(0xFFE50914)
                                        ServiceType.PRIME_VIDEO -> Color(0xFF00A8E1)
                                        ServiceType.DISNEY_PLUS -> Color(0xFF113CCF)
                                        ServiceType.HBO_MAX -> Color(0xFF002BE7)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = lease.serviceType.name.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = lease.serviceType.name.replace("_", " "),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Cuenta asignada automáticamente",
                                        color = SoftGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Interactive Status
                            Surface(
                                color = if (lease.reported) WarningOrange.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (lease.reported) "Bajo Reporte" else "Verificado",
                                    color = if (lease.reported) WarningOrange else NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Credentials Viewer (Copy buttons)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Usuario: ${lease.email}", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Clave: ${lease.passwordDecoded}", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("${lease.email} / ${lease.passwordDecoded}"))
                                    viewModel.recalculateRaffleTickets() // small refresh
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copiar",
                                    tint = NeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "El acceso expira pronto (Demo timer)",
                                color = SoftGray,
                                fontSize = 12.sp
                            )

                            Button(
                                onClick = { viewModel.reportFallenAccount(lease.id) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ElectricPink
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(ElectricPink, WarningOrange))),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp).testTag("report_fallen_${lease.id}")
                            ) {
                                Text("Reportar caída", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: EARN POINTS & MISSIONS SCREEN
// ==========================================
@Composable
fun EarnAndMissionsScreen(
    user: User,
    missions: List<DailyMission>,
    progressMap: Map<String, MissionProgress>,
    adsTodayPoints: Int,
    viewModel: StreamRewardsViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Points visual bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Ganar Puntos Gratis",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Realiza las acciones de abajo para acumular puntos canjeables por cuentas.",
                        color = SoftGray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Action controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.watchRewardedAd() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NeonCyan, GlowPurple)))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonCyan.copy(alpha = 0.12f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.LocalActivity, contentDescription = "Video Ad", tint = NeonCyan)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Ver Vídeo Ad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+5 Pts • Limite: $adsTodayPoints/100", color = SoftGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.simulateFriendReferral() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(ElectricPink, GlowPurple)))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricPink.copy(alpha = 0.12f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Group, contentDescription = "Refer เพื่อน", tint = ElectricPink)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Invitar Amigo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+50 Pts ganados", color = SoftGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Daily Missions
        item {
            Text(
                "Misiones Diarias (Resetean 00:00)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(missions) { mission ->
            val progress = progressMap[mission.id] ?: MissionProgress(mission.id)
            val isCompleted = progress.currentCount >= mission.targetCount
            val ratio = progress.currentCount.toFloat() / mission.targetCount.toFloat()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mission.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = mission.description,
                            color = SoftGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress Bar Indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isCompleted) GlowPurple else NeonCyan,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${progress.currentCount}/${mission.targetCount}",
                                color = if (isCompleted) GlowPurple else SoftGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${mission.rewardPoints} Pts",
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { viewModel.claimMissionReward(mission.id) },
                            enabled = isCompleted && !progress.isClaimed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (progress.isClaimed) SoftGray.copy(alpha = 0.2f) else GlowPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("claim_mission_${mission.id}")
                        ) {
                            Text(
                                text = if (progress.isClaimed) "Cobrado" else "Cobrar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: REDEEM SERVICES CATALOG SCREEN
// ==========================================
@Composable
fun RedeemCatalogScreen(
    user: User,
    viewModel: StreamRewardsViewModel
) {
    val items = listOf(
        Triple(ServiceType.NETFLIX, "Netflix Premium UHD", "Acceso ideal para ver películas en 4K UHD. Alta disponibilidad."),
        Triple(ServiceType.PRIME_VIDEO, "Amazon Prime Video", "Soporte estable para series y películas originales de Amazon."),
        Triple(ServiceType.DISNEY_PLUS, "Disney+ Premium (Lock Nvl 3)", "Canje Premium. Requiere Nivel 3 para desbloquear acceso."),
        Triple(ServiceType.HBO_MAX, "HBO Max Series (Lock Nvl 3)", "Canje Premium. Requiere Nivel 3 para desbloquear acceso.")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory card of dynamic level benefits
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Beneficio de Nivel Activo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when (user.level) {
                            1 -> "Nivel 1: Costo normal de cuentas sin descuento. ¡Llega a 500 para el 10%!"
                            2 -> "Nivel 2: ¡Se te aplica un 10% de descuento en todos los canjes!"
                            3 -> "Nivel 3: ¡Tienes 20% de descuento y acceso a servicios premium libre!"
                            else -> "Nivel 4: ¡Beneficio Leyenda! 30% descuento aplicado."
                        },
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Text(
                "Catálogo de Plataformas de Streaming",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(items) { (serviceType, title, desc) ->
            val isLocked = (serviceType == ServiceType.DISNEY_PLUS || serviceType == ServiceType.HBO_MAX) && user.level < 3

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocked) CardBackgroundPurple.copy(alpha = 0.5f) else CardBackgroundPurple
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isLocked) SoftGray.copy(alpha = 0.3f) else when (serviceType) {
                                    ServiceType.NETFLIX -> Color(0xFFE50914)
                                    ServiceType.PRIME_VIDEO -> Color(0xFF00A8E1)
                                    ServiceType.DISNEY_PLUS -> Color(0xFF113CCF)
                                    ServiceType.HBO_MAX -> Color(0xFF002BE7)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isLocked) {
                                        Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = Color.White)
                                    } else {
                                        Text(
                                            text = title.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = title,
                                    color = if (isLocked) SoftGray else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isLocked) "Bloqueado para Nivel 1 y 2" else "Selecciona duración de acceso",
                                    color = if (isLocked) ElectricPink else NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(desc, color = SoftGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 7).forEach { days ->
                            val finalCost = viewModel.calculateRedeemCost(serviceType, days, user.level)
                            val isPurchasable = user.pointsCurrent >= finalCost && !isLocked

                            Button(
                                onClick = { viewModel.redeemStreamingService(serviceType, days) },
                                enabled = isPurchasable,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("redeem_${serviceType}_${days}days"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlowPurple,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.White.copy(alpha = 0.05f),
                                    disabledContentColor = SoftGray.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$days ${if (days == 1) "Día" else "Días"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "$finalCost pts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isPurchasable) NeonCyan else SoftGray.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: LEADERBOARDS & RAFFLES SCREEN
// ==========================================
@Composable
fun TournamentsAndRafflesScreen(
    leaderboard: List<Contestant>,
    participants: List<RaffleTicketHolder>,
    user: User,
    viewModel: StreamRewardsViewModel
) {
    var sectionTab by remember { mutableStateOf("torneo") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(
            selectedTabIndex = if (sectionTab == "torneo") 0 else 1,
            containerColor = CinemaDarkReal,
            contentColor = NeonCyan
        ) {
            Tab(
                selected = sectionTab == "torneo",
                onClick = { sectionTab = "torneo" },
                text = { Text("Ligas y Torneos", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = sectionTab == "sorteo",
                onClick = { sectionTab = "sorteo" },
                text = { Text("Sorteos Activos", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sectionTab == "torneo") {
            // Tournament Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Torneo Semanal de Puntos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Gana puntos viendo anuncios o invitando referidos durante estos 3 días. El Top 10 gana bono Premium especial al cierre de la temporada.",
                        color = SoftGray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Leaderboard content List
            Text("Tabla de posiciones de la Liga", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(leaderboard.size) { index ->
                    val contestant = leaderboard[index]
                    val isUserMe = contestant.isMe

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUserMe) GlowPurple.copy(alpha = 0.2f) else CardBackgroundPurple
                        ),
                        border = if (isUserMe) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonCyan, ElectricPink))) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Position indicator
                                Surface(
                                    color = when (index) {
                                        0 -> Color(0xFFFFD700) // Gold
                                        1 -> Color(0xFFC0C0C0) // Silver
                                        2 -> Color(0xFFCD7F32) // Bronze
                                        else -> Color.White.copy(alpha = 0.08f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (index + 1).toString(),
                                            color = if (index < 3) Color.Black else Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = contestant.name + if (isUserMe) " (Tú)" else "",
                                    color = Color.White,
                                    fontWeight = if (isUserMe) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Text(
                                text = "${contestant.pointsInTournament} pts liga",
                                color = if (isUserMe) NeonCyan else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else {
            val rafflesList by viewModel.rafflesList.collectAsStateWithLifecycle()

            if (rafflesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CardGiftcard,
                            contentDescription = null,
                            tint = SoftGray,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No hay sorteos activos creados.",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Los administradores habilitarán sorteos próximamente con excelentes bolsas de puntos.",
                            color = SoftGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(rafflesList) { raffle ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                            border = if (raffle.isActive) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonCyan, ElectricPink))) else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (raffle.isActive) Icons.Filled.LocalActivity else Icons.Filled.EmojiEvents,
                                            contentDescription = null,
                                            tint = if (raffle.isActive) NeonCyan else SoftGray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = raffle.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (raffle.isActive) ElectricPink.copy(alpha = 0.15f) else SoftGray.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (raffle.isActive) "ACTIVO" else "CONCLUIDO",
                                            color = if (raffle.isActive) ElectricPink else SoftGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = raffle.description, color = SoftGray, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Premio", color = SoftGray, fontSize = 11.sp)
                                        Text("🪙 ${raffle.prizePoints} pts", color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Precio Boleto", color = SoftGray, fontSize = 11.sp)
                                        Text("🎟️ ${raffle.ticketCost} pts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }

                                if (raffle.isActive) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { viewModel.buyRaffleTicket(raffle.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("buy_ticket_${raffle.id}")
                                    ) {
                                        Text("Adquirir Boleto para Sorteo", fontWeight = FontWeight.Black)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Filled.Stars, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Ganador central: ${raffle.winnerName} (${raffle.winnerEmail ?: "anonimo"})",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 5: DEV CTO ADMIN COCKPIT & SIMULATION PANEL
// ==========================================
@Composable
fun AdminCockpitScreen(
    pool: List<StreamingAccount>,
    leases: List<ActiveRedemption>,
    logs: List<ActivityLog>,
    viewModel: StreamRewardsViewModel
) {
    var expandedSection by remember { mutableStateOf("") } // "", "prices", "admins", "broadcast", "users", "raffles", "accounts"

    // Streaming account input fields
    var accEmail by remember { mutableStateOf("") }
    var accPassword by remember { mutableStateOf("") }
    var accService by remember { mutableStateOf(ServiceType.NETFLIX) }

    // Whitelist admin
    var newAdminEmail by remember { mutableStateOf("") }

    // Sent alert fields
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMsg by remember { mutableStateOf("") }

    // Create raffle fields
    var raffleTitle by remember { mutableStateOf("") }
    var raffleDesc by remember { mutableStateOf("") }
    var rafflePrize by remember { mutableStateOf("1000") }
    var raffleCost by remember { mutableStateOf("20") }

    // Adjust points select user
    var pointsAdjustmentUserId by remember { mutableStateOf("") }
    var pointsDeltaStr by remember { mutableStateOf("") }
    val pointsReason = "Bono de Administración"

    // Service prices edit state
    var netflixPrice by remember { mutableStateOf("100") }
    var primePrice by remember { mutableStateOf("250") }
    var disneyPrice by remember { mutableStateOf("500") }
    var hboPrice by remember { mutableStateOf("500") }

    // Fetch dynamic configs from viewmodel
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val adminEmails by viewModel.adminEmails.collectAsStateWithLifecycle()
    val servicePrices by viewModel.servicePrices.collectAsStateWithLifecycle()
    val rafflesList by viewModel.rafflesList.collectAsStateWithLifecycle()

    var userSearchQuery by remember { mutableStateOf("") }

    val currentUnityGameId by viewModel.unityGameId.collectAsStateWithLifecycle()
    val currentUnityPlacementId by viewModel.unityPlacementId.collectAsStateWithLifecycle()

    var unityGameIdInput by remember { mutableStateOf("") }
    var unityPlacementIdInput by remember { mutableStateOf("") }

    LaunchedEffect(currentUnityGameId, currentUnityPlacementId) {
        unityGameIdInput = currentUnityGameId
        unityPlacementIdInput = currentUnityPlacementId
    }

    // Initialize inputs with current dynamic prices value once they are loaded
    LaunchedEffect(servicePrices) {
        netflixPrice = (servicePrices[ServiceType.NETFLIX] ?: 100).toString()
        primePrice = (servicePrices[ServiceType.PRIME_VIDEO] ?: 250).toString()
        disneyPrice = (servicePrices[ServiceType.DISNEY_PLUS] ?: 500).toString()
        hboPrice = (servicePrices[ServiceType.HBO_MAX] ?: 500).toString()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricPink, WarningOrange)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, contentDescription = "Admin Area", tint = ElectricPink, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Panel de Control de Admin Real",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Administra usuarios, precios, sorteos, listas de correo administrativo y cuentas de streaming en sincronía real con la base de datos de Firebase.",
                        color = SoftGray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ==========================================
        // SECTOR 1: CRON & ROTATIONS
        // ==========================================
        item {
            Text("Tareas Rápidas de Backend", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.runDailyResetCron() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reiniciar Misiones", fontSize = 11.sp, color = NeonCyan)
                }
                Button(
                    onClick = { viewModel.rotateAllPasswords() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Rotar Claves Cuentas", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==========================================
        // ACCORDION 2: DYNAMIC SERVICE PRICES
        // ==========================================
        item {
            val isExpanded = expandedSection == "prices"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                border = if (isExpanded) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricPink, NeonCyan))) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (isExpanded) "" else "prices" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajustar Precios de Canje", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = SoftGray
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Precios dinámicos asignados en puntos para canjes de usuarios:", color = SoftGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val priceInputs = listOf(
                            Triple("Netflix", netflixPrice, ServiceType.NETFLIX),
                            Triple("Prime Video", primePrice, ServiceType.PRIME_VIDEO),
                            Triple("Disney Plus", disneyPrice, ServiceType.DISNEY_PLUS),
                            Triple("HBO Max", hboPrice, ServiceType.HBO_MAX)
                        )

                        priceInputs.forEach { (label, valStr, service) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1.2f))
                                OutlinedTextField(
                                    value = valStr,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() }) {
                                            when (service) {
                                                ServiceType.NETFLIX -> netflixPrice = newValue
                                                ServiceType.PRIME_VIDEO -> primePrice = newValue
                                                ServiceType.DISNEY_PLUS -> disneyPrice = newValue
                                                ServiceType.HBO_MAX -> hboPrice = newValue
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f).height(50.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val amt = valStr.toIntOrNull() ?: 100
                                        viewModel.updateServicePrice(service, amt)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Guardar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // ACCORDION 3: BROADCAST ALERTS
        // ==========================================
        item {
            val isExpanded = expandedSection == "broadcast"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                border = if (isExpanded) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricPink, NeonCyan))) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (isExpanded) "" else "broadcast" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Campaign, contentDescription = null, tint = ElectricPink)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mandar Notificación a Todos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = SoftGray
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            label = { Text("Título de Notificación") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = broadcastMsg,
                            onValueChange = { broadcastMsg = it },
                            label = { Text("Cuerpo del mensaje") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (broadcastTitle.isNotBlank() && broadcastMsg.isNotBlank()) {
                                    viewModel.sendGlobalNotification(broadcastTitle, broadcastMsg)
                                    broadcastTitle = ""
                                    broadcastMsg = ""
                                }
                            },
                            enabled = broadcastTitle.isNotBlank() && broadcastMsg.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enviar Despacho Global (Push Nativas)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // ACCORDION 3.5: CONFIGURAR UNITY ADS
        // ==========================================
        item {
            val isExpanded = expandedSection == "unityads"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                border = if (isExpanded) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricPink, NeonCyan))) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (isExpanded) "" else "unityads" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LiveTv, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configurar Unity Ads Monetization", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = SoftGray
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Configura dinámicamente tu Game ID de Unity y el ID de Placement para los anuncios bonificados (Rewarded Ads).",
                            color = SoftGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = unityGameIdInput,
                            onValueChange = { unityGameIdInput = it },
                            label = { Text("Game ID de Unity (7 dígitos)") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = unityPlacementIdInput,
                            onValueChange = { unityPlacementIdInput = it },
                            label = { Text("ID de Placement (Ej: Rewarded_Android)") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (unityGameIdInput.isNotBlank() && unityPlacementIdInput.isNotBlank()) {
                                    viewModel.updateUnityAdsConfig(unityGameIdInput.trim(), unityPlacementIdInput.trim())
                                }
                            },
                            enabled = unityGameIdInput.isNotBlank() && unityPlacementIdInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guardar Configuración en Firebase", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // ACCORDION 4: WHITE-LISTED ADMINS
        // ==========================================
        item {
            val isExpanded = expandedSection == "admins"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                border = if (isExpanded) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricPink, NeonCyan))) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (isExpanded) "" else "admins" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SupervisorAccount, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Administrar Correos Administrativos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = SoftGray
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Whitelistea correos electrónicos de confianza para otorgarles acceso total al panel administrativo:", color = SoftGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newAdminEmail,
                                onValueChange = { newAdminEmail = it },
                                label = { Text("Correo de nuevo Administrador") },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newAdminEmail.isNotBlank()) {
                                        viewModel.addAdminEmail(newAdminEmail)
                                        newAdminEmail = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Añadir", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Administradores Activos en Firebase:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Hardcoded super admin
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("blandonjose6788@gmail.com (Propietario)", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.Green, modifier = Modifier.size(18.dp))
                        }

                        // Loaded database whitelisted emails
                        adminEmails.forEach { email ->
                            if (!email.equals("blandonjose6788@gmail.com", ignoreCase = true)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(email, color = Color.White, fontSize = 13.sp)
                                    IconButton(
                                        onClick = { viewModel.removeAdminEmail(email) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // ACCORDION 5: CREATE SORTEOS / RAFFLES
        // ==========================================
        item {
            val isExpanded = expandedSection == "raffles"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                border = if (isExpanded) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricPink, NeonCyan))) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (isExpanded) "" else "raffles" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = WarningOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Programar Sorteos de Puntos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = SoftGray
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = raffleTitle,
                            onValueChange = { raffleTitle = it },
                            label = { Text("Título del Sorteo (Ej: Sorteo de Fin de Mes)") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = raffleDesc,
                            onValueChange = { raffleDesc = it },
                            label = { Text("Descripción o Reglas del sorteo") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rafflePrize,
                                onValueChange = { if (it.all { c -> c.isDigit() }) rafflePrize = it },
                                label = { Text("Bolsa Premio (pts)") },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = raffleCost,
                                onValueChange = { if (it.all { c -> c.isDigit() }) raffleCost = it },
                                label = { Text("Costo Boleto (pts)") },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val prize = rafflePrize.toIntOrNull() ?: 1000
                                val cost = raffleCost.toIntOrNull() ?: 20
                                if (raffleTitle.isNotBlank()) {
                                    viewModel.createRaffle(raffleTitle, raffleDesc, prize, cost)
                                    raffleTitle = ""
                                    raffleDesc = ""
                                }
                            },
                            enabled = raffleTitle.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Iniciar Sorteo en Base de Datos", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Monitoreo de Sorteos Guardados:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        rafflesList.forEach { raffle ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(raffle.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Premio: ${raffle.prizePoints} pts • Boletos: ${raffle.participantsCount}", color = SoftGray, fontSize = 11.sp)
                                        if (!raffle.isActive) {
                                            Text("Ganador: ${raffle.winnerName}", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (raffle.isActive) {
                                        Button(
                                            onClick = { viewModel.endRaffleAndPickWinner(raffle.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Elegir Ganador", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // ACCORDION 6: EDIT & DELETE USERS INDEX
        // ==========================================
        item {
            val isExpanded = expandedSection == "users"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                border = if (isExpanded) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricPink, NeonCyan))) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (isExpanded) "" else "users" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajustar Usuarios / Modificar Puntos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = SoftGray
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            label = { Text("Buscar usuario por correo o nombre") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SoftGray) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Usuarios sincronizados en Firebase:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        val filteredUsers = allUsers.filter {
                            userSearchQuery.isBlank() ||
                            it.email.contains(userSearchQuery, ignoreCase = true) ||
                            it.name.contains(userSearchQuery, ignoreCase = true)
                        }

                        filteredUsers.forEach { userObj ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text(userObj.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(userObj.email, color = SoftGray, fontSize = 11.sp)
                                            Text("Puntos: 🪙 ${userObj.pointsCurrent} pts • Nivel: ${userObj.level}", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteUserAccount(userObj.id) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar de base de datos", tint = ElectricPink)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = if (pointsAdjustmentUserId == userObj.id) pointsDeltaStr else "",
                                            onValueChange = {
                                                pointsAdjustmentUserId = userObj.id
                                                pointsDeltaStr = it
                                            },
                                            placeholder = { Text("Cant +/- (Ex: 50 o -30)", color = SoftGray.copy(alpha = 0.5f), fontSize = 11.sp) },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.weight(1.3f).height(48.dp)
                                        )

                                        Button(
                                            onClick = {
                                                val amt = pointsDeltaStr.toIntOrNull()
                                                if (amt != null && pointsAdjustmentUserId == userObj.id) {
                                                    viewModel.adjustUserPointsDirectly(userObj.id, amt, pointsReason)
                                                    pointsDeltaStr = ""
                                                    pointsAdjustmentUserId = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Text("Modificar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // ACCORDION 7: MANAGE AND LIST STREAMING ACCOUNTS
        // ==========================================
        item {
            val isExpanded = expandedSection == "accounts"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                border = if (isExpanded) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricPink, NeonCyan))) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (isExpanded) "" else "accounts" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ConnectedTv, contentDescription = null, tint = ElectricPink)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir / Eliminar Cuentas Streaming", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = SoftGray
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Servicio de Streaming a agregar:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            ServiceType.values().forEach { service ->
                                val selected = accService == service
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (selected) ElectricPink else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(1.dp, if (selected) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { accService = service }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        service.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = accEmail,
                            onValueChange = { accEmail = it },
                            label = { Text("Correo de la Cuenta de Streaming") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = accPassword,
                            onValueChange = { accPassword = it },
                            label = { Text("Contraseña de Acceso") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (accEmail.isNotBlank() && accPassword.isNotBlank()) {
                                    viewModel.addStreamingAccount(accService, accEmail, accPassword)
                                    accEmail = ""
                                    accPassword = ""
                                }
                            },
                            enabled = accEmail.isNotBlank() && accPassword.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Añadir Cuenta de Streaming", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTOR 8: CURRENT ACCOUNT POOL INDEX
        // ==========================================
        item {
            Text("Cuentas de Acceso Registradas (${pool.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(pool) { acc ->
            val isInRent = leases.any { it.accountId == acc.id }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = when (acc.serviceType) {
                                    ServiceType.NETFLIX -> Color(0xFFE50914)
                                    ServiceType.PRIME_VIDEO -> Color(0xFF00A8E1)
                                    ServiceType.DISNEY_PLUS -> Color(0xFF113CCF)
                                    ServiceType.HBO_MAX -> Color(0xFF002BE7)
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(14.dp)
                            ) { }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(acc.email, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Clave: ${acc.passwordDecoded} • Reportes 24h: ${acc.reports24h}/3",
                            color = SoftGray,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = when {
                                acc.status == AccountStatus.DOWN -> ElectricPink.copy(alpha = 0.2f)
                                isInRent -> GlowPurple.copy(alpha = 0.2f)
                                else -> NeonCyan.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = when {
                                    acc.status == AccountStatus.DOWN -> "CON PROBLEMAS"
                                    isInRent -> "Rentada"
                                    else -> "Libre"
                                },
                                color = when {
                                    acc.status == AccountStatus.DOWN -> ElectricPink
                                    isInRent -> SoftGray
                                    else -> NeonCyan
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.deleteStreamingAccount(acc.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = ElectricPink, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTOR 9: RECENT AUDITED LOGS
        // ==========================================
        item {
            Text("Registros de Logs de Firebase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        val dateString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        Text(
                            text = "[$dateString] ${log.action}",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
