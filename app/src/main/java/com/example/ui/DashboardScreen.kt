package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyStepRecord
import com.example.data.Friend
import com.example.data.LocationPoint
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

private val AvatarColors = listOf(
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF4CAF50), // Green
    Color(0xFF2196F3), // Blue
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFFFFC107)  // Amber
)

@Composable
fun HighDensityHeader() {
    val dateFormat = remember { SimpleDateFormat("EEEE, d 'de' MMM", Locale("pt", "BR")) }
    val currentDate = remember { dateFormat.format(Date()).replaceFirstChar { it.uppercase() } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // JD Avatar Circle representation from prompt
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(HdCardHeroBg)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "JD",
                    color = HdDarkBlueText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Column {
                Text(
                    text = "Bom dia, João",
                    color = HdTextColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentDate,
                    color = HdSubtleText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        IconButton(
            onClick = { /* notification flow actions placeholder */ },
            modifier = Modifier
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notificações",
                tint = HdSubtleText
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    viewModel: StepMapViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Activity, 1: Map GPS, 2: Friends

    val userSteps by viewModel.userSteps.collectAsStateWithLifecycle()
    val userLat by viewModel.userLatitude.collectAsStateWithLifecycle()
    val friends by viewModel.friendsList.collectAsStateWithLifecycle()
    val pathPoints by viewModel.walkPath.collectAsStateWithLifecycle()
    val isTrackingState by viewModel.isTrackingLocation.collectAsStateWithLifecycle()
    val isSimulatingState by viewModel.isSimulatingMovement.collectAsStateWithLifecycle()
    val stepHistory by viewModel.stepHistory.collectAsStateWithLifecycle()

    var showAddFriendDialog by remember { mutableStateOf(false) }

    // Request necessary runtime permissions
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACTIVITY_RECOGNITION"
        )
    )

    // Synchronize GPS Tracker when permission changes or manual toggle is requested
    LaunchedEffect(isTrackingState) {
        if (isTrackingState) {
            if (permissionState.allPermissionsGranted) {
                viewModel.startGpsTracking()
            } else {
                viewModel.stopGpsTracking()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HdBackground,
        bottomBar = {
            NavigationBar(
                containerColor = HdCardGpsBg,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(width = 1.dp, color = HdGpsBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = "Pedometer") },
                    label = { Text("Activity", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HdDarkBlueText,
                        selectedTextColor = HdDarkBlueText,
                        indicatorColor = HdCardHeroBg,
                        unselectedIconColor = HdSubtleText,
                        unselectedTextColor = HdSubtleText
                    ),
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Radar GPS") },
                    label = { Text("Map", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HdDarkBlueText,
                        selectedTextColor = HdDarkBlueText,
                        indicatorColor = HdCardHeroBg,
                        unselectedIconColor = HdSubtleText,
                        unselectedTextColor = HdSubtleText
                    ),
                    modifier = Modifier.testTag("tab_map")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Social") },
                    label = { Text("Friends", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HdDarkBlueText,
                        selectedTextColor = HdDarkBlueText,
                        indicatorColor = HdCardHeroBg,
                        unselectedIconColor = HdSubtleText,
                        unselectedTextColor = HdSubtleText
                    ),
                    modifier = Modifier.testTag("tab_social")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HighDensityHeader()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> StepDashboardTab(
                        userSteps = userSteps,
                        history = stepHistory,
                        isSimulating = isSimulatingState,
                        onSimulateStep = { viewModel.simulateWalkAction(it) },
                        onToggleSimulation = { viewModel.toggleSocialSimulation(it) },
                        onResetStats = { viewModel.resetDailyStats() }
                    )
                    1 -> GpsRadarTab(
                        userLat = userLat,
                        userLon = viewModel.userLongitude.collectAsStateWithLifecycle().value,
                        friends = friends,
                        pathPoints = pathPoints,
                        isTracking = isTrackingState,
                        isSimulating = isSimulatingState,
                        onToggleTracking = {
                            if (!isTrackingState) {
                                if (permissionState.allPermissionsGranted) {
                                    viewModel.startGpsTracking()
                                } else {
                                    permissionState.launchMultiplePermissionRequest()
                                }
                            } else {
                                viewModel.stopGpsTracking()
                            }
                        },
                        onClearTrail = { viewModel.clearGpsTrail() },
                        permissionsGranted = permissionState.allPermissionsGranted
                    )
                    2 -> SocialFriendsTab(
                        friends = friends,
                        onAddFriendClick = { showAddFriendDialog = true },
                        onRemoveFriend = { viewModel.removeFriend(it) },
                        isSimulating = isSimulatingState,
                        onToggleSimulation = { viewModel.toggleSocialSimulation(it) }
                    )
                }
            }
        }
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onConfirm = { name, colorIdx ->
                viewModel.addNewFriend(name, colorIdx)
                showAddFriendDialog = false
            }
        )
    }
}

// ==========================================
// TAB 1: PASSOS / OUTCOME PEDOMETER SCREEN
// ==========================================
@Composable
fun StepDashboardTab(
    userSteps: Int,
    history: List<DailyStepRecord>,
    isSimulating: Boolean,
    onSimulateStep: (Int) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    onResetStats: () -> Unit
) {
    val stepGoal = 10000
    val progress = (userSteps.toFloat() / stepGoal).coerceIn(0f, 1f)

    // Animated progress ring
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "Steps Progressive Ring"
    )

    // Animated numerical counts
    val animatedSteps by animateIntAsState(
        targetValue = userSteps,
        animationSpec = tween(durationMillis = 800),
        label = "Steps Numeric Counter"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "ACOMPANHAMENTO ATIVO",
                    color = HdSubtleText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = "Progresso Diário",
                    color = HdTextColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Circular Pedometer Gauge Card (MD3 Elevated / High Density style)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = HdCardHeroBg),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(12.dp)
                    ) {
                        // Background concentric guide (opaque white ring)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.35f),
                                style = Stroke(width = 8.dp.toPx())
                            )
                        }

                        // Elegant Solid theme blue active progress ring
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = HdBrandBlueDef,
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }

                        // Text indicator center matching HTML style
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = HdDarkBlueText,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = animatedSteps.toString(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = HdDarkBlueText
                            )
                            Text(
                                text = "meta: 10k passos",
                                color = HdDarkBlueText.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary trackers elements (Distance, Calories, Time meters in high density layout)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "DISTÂNCIA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HdSubtleText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f km", (userSteps * 0.70) / 1000.0),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HdTextColor
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(28.dp),
                            color = HdDarkBlueText.copy(alpha = 0.1f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CALORIAS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HdSubtleText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.0f kcal", userSteps * 0.041),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HdTextColor
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(28.dp),
                            color = HdDarkBlueText.copy(alpha = 0.1f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TEMPO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HdSubtleText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%d min", (userSteps * 0.0058).toInt().coerceAtLeast(1)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HdTextColor
                            )
                        }
                    }
                }
            }
        }

        // Active Emulator Walking Quick Action Toggles (High Density light style)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HdCardGpsBg),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HdGpsBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Simulador de Passos / Teste",
                        fontWeight = FontWeight.Bold,
                        color = HdBrandBlueDef,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Gere passadas virtuais para testar a atualização do GPS com os amigos em tempo real e desenhar o rastro de caminhada no mapa.",
                        color = HdSubtleText,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSimulateStep(100) },
                            colors = ButtonDefaults.buttonColors(containerColor = HdBrandBlueDef, contentColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("sim_walk_100")
                        ) {
                            Text("+100p", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onSimulateStep(500) },
                            colors = ButtonDefaults.buttonColors(containerColor = HdBrandBlueDef, contentColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("sim_walk_500")
                        ) {
                            Text("+500p", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onSimulateStep(1000) },
                            colors = ButtonDefaults.buttonColors(containerColor = HdBrandBlueDef, contentColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("sim_walk_1000")
                        ) {
                            Text("+1k p", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Auto Passos (Simular Caminhada)", color = HdTextColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isSimulating,
                            onCheckedChange = { onToggleSimulation(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HdBrandBlueDef,
                                uncheckedThumbColor = SoftGrey,
                                uncheckedTrackColor = SoftGrey.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }

        // Historic stats
        if (history.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Histórico de Dias Anteriores",
                        fontWeight = FontWeight.Bold,
                        color = HdTextColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            items(history.take(7)) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HdCardGpsBg),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HdGpsBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(HdCardHeroBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = HdBrandBlueDef, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = record.date, fontWeight = FontWeight.Bold, color = HdTextColor, fontSize = 13.sp)
                                Text(text = String.format(Locale.getDefault(), "%.1f kcal \u2022 %.2f km", record.caloriesKcal, record.distanceMeters / 1000.0), color = HdSubtleText, fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = "${record.steps} passos",
                            fontWeight = FontWeight.Bold,
                            color = HdBrandBlueDef,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Reset Data Buttons
        item {
            TextButton(
                onClick = onResetStats,
                colors = ButtonDefaults.textButtonColors(contentColor = HdPulseRed),
                modifier = Modifier.padding(vertical = 4.dp).testTag("reset_stats_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Limpar Estatísticas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// TAB 2: INTERACTIVE VECTOR SOCIAL RADAR MAP
// ==========================================
@Composable
fun GpsRadarTab(
    userLat: Double,
    userLon: Double,
    friends: List<Friend>,
    pathPoints: List<LocationPoint>,
    isTracking: Boolean,
    isSimulating: Boolean,
    onToggleTracking: () -> Unit,
    onClearTrail: () -> Unit,
    permissionsGranted: Boolean
) {
    var zoomScale by remember { mutableStateOf(350000.0) } // ratio: pixels per lat/lon degree
    var focusOnFriendId by remember { mutableStateOf<String?>(null) }

    // Map drag panning offset coordinates
    var mapPanX by remember { mutableStateOf(0f) }
    var mapPanY by remember { mutableStateOf(0f) }

    // Animation flag for active radar sweeps
    val infiniteTransition = rememberInfiniteTransition(label = "Radar Sweep Trigger")
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Radar Rotation Vector"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tracker Panel header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GPS Social - Mapa Radar",
                    fontWeight = FontWeight.Bold,
                    color = HdTextColor,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Caminho Ativo e Localização dos Amigos",
                    color = HdSubtleText,
                    fontSize = 12.sp
                )
            }

            // High priority GPS tracking button
            Button(
                onClick = onToggleTracking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) HdPulseRed else HdBrandBlueDef,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp).testTag("gps_tracking_toggle")
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTracking) "Parar" else "Compartilhar GPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live coordinate status telemetry card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = HdCardGpsBg),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HdGpsBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sua Posição Temporal",
                        fontWeight = FontWeight.Bold,
                        color = HdBrandBlueDef,
                        fontSize = 11.sp
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "Lat: %.6f | Lon: %.6f", userLat, userLon),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = HdTextColor,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isTracking) "Conexão GPS Satelital ativa" else "Virtual (Dispositivo de Teste/Satélites)",
                        color = if (isTracking) HdBrandBlueDef else HdSubtleText,
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = {
                        mapPanX = 0f
                        mapPanY = 0f
                        focusOnFriendId = null
                    },
                    modifier = Modifier.background(HdCardHeroBg, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Centralizar",
                        tint = HdBrandBlueDef,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Custom Vector Map Canvas Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(HdCardGpsBg)
                .border(2.dp, HdGpsBorder, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        mapPanX += pan.x
                        mapPanY += pan.y
                        // Multiplies the current zoom relative
                        zoomScale = (zoomScale * zoom).coerceIn(100000.0, 1500000.0)
                    }
                }
        ) {
            // Radar Rendering Draw block
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasCenter = Offset(size.width / 2f + mapPanX, size.height / 2f + mapPanY)

                // 1. Draw circular radar target indicators background coordinates grid
                val radiusMax = min(size.width, size.height) * 0.9f / 2f
                val stepMax = radiusMax / 3f

                for (i in 1..3) {
                    val radiusCur = stepMax * i
                    drawCircle(
                        color = HdBrandBlueDef.copy(alpha = 0.03f + 0.015f * i),
                        radius = radiusCur,
                        center = canvasCenter,
                        style = Stroke(width = 1f)
                    )

                    // Draw rings text marker (estimated meters based on scale conversion)
                    val scaleConversion = radiusCur / zoomScale
                    val approxMeters = scaleConversion * 111319.9 // standard meters per coordinate degree
                    val textStr = if (approxMeters > 1000.0) {
                        String.format(Locale.getDefault(), "%.1f km", approxMeters / 1000.0)
                    } else {
                        String.format(Locale.getDefault(), "%.0f m", approxMeters)
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        textStr,
                        canvasCenter.x + radiusCur - 15f,
                        canvasCenter.y - 8f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(120, 0, 98, 161)
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }

                // Cross grid lines N-S E-W
                drawLine(
                    color = HdBrandBlueDef.copy(alpha = 0.08f),
                    start = Offset(canvasCenter.x, canvasCenter.y - radiusMax),
                    end = Offset(canvasCenter.x, canvasCenter.y + radiusMax),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = HdBrandBlueDef.copy(alpha = 0.08f),
                    start = Offset(canvasCenter.x - radiusMax, canvasCenter.y),
                    end = Offset(canvasCenter.x + radiusMax, canvasCenter.y),
                    strokeWidth = 1.5f
                )

                // High tech directional labels (using dark text colors now in light theme)
                drawContext.canvas.nativeCanvas.drawText("N", canvasCenter.x, canvasCenter.y - radiusMax - 10f, android.graphics.Paint().apply { color = android.graphics.Color.argb(255, 26, 28, 30); textSize = 28f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true })
                drawContext.canvas.nativeCanvas.drawText("S", canvasCenter.x, canvasCenter.y + radiusMax + 24f, android.graphics.Paint().apply { color = android.graphics.Color.argb(255, 26, 28, 30); textSize = 28f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true })
                drawContext.canvas.nativeCanvas.drawText("W", canvasCenter.x - radiusMax - 18f, canvasCenter.y + 10f, android.graphics.Paint().apply { color = android.graphics.Color.argb(255, 26, 28, 30); textSize = 28f; textAlign = android.graphics.Paint.Align.RIGHT; isFakeBoldText = true })
                drawContext.canvas.nativeCanvas.drawText("E", canvasCenter.x + radiusMax + 10f, canvasCenter.y + 10f, android.graphics.Paint().apply { color = android.graphics.Color.argb(255, 26, 28, 30); textSize = 28f; textAlign = android.graphics.Paint.Align.LEFT; isFakeBoldText = true })

                // Active sweeps gradient cone to make it look hyper digital
                val sweepAngleRad = Math.toRadians(radarSweepAngle.toDouble())
                val endLineX = canvasCenter.x + radiusMax * cos(sweepAngleRad).toFloat()
                val endLineY = canvasCenter.y + radiusMax * sin(sweepAngleRad).toFloat()
                drawLine(
                    color = HdBrandBlueDef.copy(alpha = 0.15f),
                    start = canvasCenter,
                    end = Offset(endLineX, endLineY),
                    strokeWidth = 3f
                )

                // 2. Draw user's WALK PATH trail points if exists
                if (pathPoints.size > 1) {
                    val pathToDraw = Path()
                    var isFirstPoint = true

                    pathPoints.forEach { pt ->
                        val dLat = pt.latitude - userLat
                        val dLon = (pt.longitude - userLon) * cos(Math.toRadians(userLat))

                        val xPx = canvasCenter.x + (dLon * zoomScale).toFloat()
                        val yPx = canvasCenter.y - (dLat * zoomScale).toFloat()

                        if (isFirstPoint) {
                            pathToDraw.moveTo(xPx, yPx)
                            isFirstPoint = false
                        } else {
                            pathToDraw.lineTo(xPx, yPx)
                        }
                    }

                    drawPath(
                        path = pathToDraw,
                        color = HdBrandBlueDef,
                        style = Stroke(width = 6f, miter = 1f),
                        alpha = 0.8f
                    )
                }

                // 3. Draw FRIENDS on the map radar relative coordinates
                friends.forEach { friend ->
                    if (friend.isOnline) {
                        val dLat = friend.latitude - userLat
                        val dLon = (friend.longitude - userLon) * cos(Math.toRadians(userLat))

                        val fX = canvasCenter.x + (dLon * zoomScale).toFloat()
                        val fY = canvasCenter.y - (dLat * zoomScale).toFloat()

                        // Calculate distance from user
                        val dist = calculateDistanceBetweenPoints(userLat, userLon, friend.latitude, friend.longitude)

                        // Render orange/blue marker circle
                        val markerColor = AvatarColors[friend.avatarColorIdx % AvatarColors.size]

                        drawCircle(
                            color = markerColor,
                            radius = 16f,
                            center = Offset(fX, fY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 18f,
                            center = Offset(fX, fY),
                            style = Stroke(width = 2f)
                        )

                        // Draw live pulse indicator representing real-time positioning connection
                        val isPulseActive = focusOnFriendId == friend.id
                        drawCircle(
                            color = markerColor.copy(alpha = if (isPulseActive) 0.3f else 0.15f),
                            radius = if (isPulseActive) 42f else 30f,
                            center = Offset(fX, fY)
                        )

                        // Draw Friend avatar Initials in the center of the marker
                        val initials = friend.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
                        drawContext.canvas.nativeCanvas.drawText(
                            initials,
                            fX,
                            fY + 6f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 16f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                        )

                        // Under light theme, draw gorgeous contrast overlays
                        drawContext.canvas.nativeCanvas.drawText(
                            "${friend.name.split(" ").firstOrNull() ?: friend.name} (${String.format(Locale.getDefault(), "%.0fm", dist)})",
                            fX,
                            fY - 24f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(255, 0, 98, 161)
                                textSize = 18f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                        )
                    }
                }

                // 4. Draw USER centered at the coordinate anchor point
                drawCircle(
                    color = HdBrandBlueDef.copy(alpha = 0.2f),
                    radius = 32f,
                    center = canvasCenter
                )
                drawCircle(
                    color = HdBrandBlueDef,
                    radius = 10f,
                    center = canvasCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = canvasCenter,
                    style = Stroke(width = 2.5f)
                )
            }

            // Floating Controls inside GpsRadar component (Zoom / Focus / Navigation actions)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { zoomScale = (zoomScale * 1.4f).coerceAtMost(1500000.0) },
                    containerColor = Color.White,
                    contentColor = HdBrandBlueDef,
                    modifier = Modifier.size(44.dp).border(1.dp, HdGpsBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                FloatingActionButton(
                    onClick = { zoomScale = (zoomScale / 1.4f).coerceAtLeast(100000.0) },
                    containerColor = Color.White,
                    contentColor = HdBrandBlueDef,
                    modifier = Modifier.size(44.dp).border(1.dp, HdGpsBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }

                FloatingActionButton(
                    onClick = {
                        mapPanX = 0f
                        mapPanY = 0f
                        focusOnFriendId = null
                    },
                    containerColor = Color.White,
                    contentColor = HdBrandBlueDef,
                    modifier = Modifier.size(44.dp).border(1.dp, HdGpsBorder, CircleShape)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Re-center User")
                }
            }

            // Legend or info popup
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HdGpsBorder)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(HdBrandBlueDef, CircleShape))
                    Text("Você", color = HdTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(8.dp).background(HdBrandBlueDef.copy(alpha = 0.6f), CircleShape))
                    Text("Caminho", color = HdTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(8.dp).background(ActiveOrange, CircleShape))
                    Text("Amigos", color = HdTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Action trail card triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${pathPoints.size} Marcadores de Trajeto gravados",
                color = HdSubtleText,
                fontSize = 11.sp
            )

            TextButton(
                onClick = onClearTrail,
                colors = ButtonDefaults.textButtonColors(contentColor = HdSubtleText)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Resetar Caminho", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Distance solver helper function
private fun calculateDistanceBetweenPoints(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}

// ==========================================
// TAB 3: SOCIAL, FRIENDS & LEADERBOARDS
// ==========================================
@Composable
fun SocialFriendsTab(
    friends: List<Friend>,
    onAddFriendClick: () -> Unit,
    onRemoveFriend: (String) -> Unit,
    isSimulating: Boolean,
    onToggleSimulation: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Social top bar actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Amigos & Competição",
                        fontWeight = FontWeight.Bold,
                        color = HdTextColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Compartilhamento de Localização e Passos",
                        color = HdSubtleText,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onAddFriendClick,
                    colors = ButtonDefaults.buttonColors(containerColor = HdBrandBlueDef, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Adicionar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Realtime switch panel (High density light themed)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HdCardGpsBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HdGpsBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GPS Social Tempo Real",
                            color = HdBrandBlueDef,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Amigos migram no mapa e incrementam passos conforme caminham autonomamente.",
                            color = HdSubtleText,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isSimulating,
                        onCheckedChange = onToggleSimulation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HdBrandBlueDef,
                            uncheckedThumbColor = SoftGrey,
                            uncheckedTrackColor = SoftGrey.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        // Leaderboard title
        item {
            Text(
                text = "Placar Geral (Passos de Hoje)",
                fontWeight = FontWeight.Bold,
                color = HdTextColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (friends.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = HdCardGpsBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HdGpsBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = HdSubtleText, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sem amigos conectados", color = HdTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Clique em 'Adicionar' para conectar novos contatos GPS temporários.", color = HdSubtleText, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            // Display ranking of active step walkers (Glassmorphic elevated card layouts)
            items(friends.sortedByDescending { it.steps }) { friend ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HdGpsBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Friend Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        AvatarColors[friend.avatarColorIdx % AvatarColors.size],
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = friend.name.split(" ").map { it.take(1) }.take(2).joinToString("").uppercase()
                                Text(
                                    text = initials,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                // Small online live connection dot
                                if (friend.isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .border(1.5.dp, Color.White, CircleShape)
                                            .background(HdBrandBlueDef, CircleShape)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = friend.name,
                                        fontWeight = FontWeight.Bold,
                                        color = HdTextColor,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = HdSubtleText, modifier = Modifier.size(11.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "Lat: %.4f | Lon: %.4f", friend.latitude, friend.longitude),
                                        color = HdSubtleText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${friend.steps} passos",
                                    fontWeight = FontWeight.Bold,
                                    color = HdBrandBlueDef,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.2f", (friend.steps * 0.70) / 1000.0)} km",
                                    color = HdSubtleText,
                                    fontSize = 10.sp
                                )
                            }

                            // Quick delete action
                            IconButton(onClick = { onRemoveFriend(friend.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remover Amigo",
                                    tint = HdPulseRed.copy(0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog window to add secondary friends to test multi-markers mapping coordinates
@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var nameState by remember { mutableStateOf("") }
    var colorSelectedIdx by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.5.dp, HdGpsBorder, RoundedCornerShape(20.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Conectar Novo Amigo",
                    fontWeight = FontWeight.Bold,
                    color = HdTextColor,
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Gere um amigo fictício nas proximidades para simular caminhada social e interagir com o radar GPS em tempo real.",
                    color = HdSubtleText,
                    fontSize = 11.sp
                )

                TextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text("Nome do Amigo") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = HdCardGpsBg,
                        unfocusedContainerColor = HdCardGpsBg,
                        focusedLabelColor = HdBrandBlueDef,
                        focusedIndicatorColor = HdBrandBlueDef,
                        unfocusedIndicatorColor = HdGpsBorder,
                        focusedTextColor = HdTextColor,
                        unfocusedTextColor = HdTextColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("friend_name_input")
                )

                Text("Cor do Marcador Radar", color = HdTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AvatarColors.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (colorSelectedIdx == idx) 3.dp else 0.dp,
                                    color = if (colorSelectedIdx == idx) HdTextColor else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorSelectedIdx = idx }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = HdSubtleText)) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nameState.isNotBlank()) {
                                onConfirm(nameState, colorSelectedIdx)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HdBrandBlueDef, contentColor = Color.White),
                        enabled = nameState.isNotBlank()
                    ) {
                        Text("Confirmar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
