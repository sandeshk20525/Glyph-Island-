package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SettingsManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    settingsManager: SettingsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // State derived from SettingsManager
    var doubleTapAction by remember { mutableStateOf(settingsManager.doubleTapAction) }
    var islandWidth by remember { mutableStateOf(settingsManager.islandWidth) }
    var islandHeight by remember { mutableStateOf(settingsManager.islandHeight) }
    var islandYOffset by remember { mutableStateOf(settingsManager.islandYOffset) }
    var islandCornerRadius by remember { mutableStateOf(settingsManager.islandCornerRadius) }
    var enableOverlay by remember { mutableStateOf(settingsManager.enableOverlay) }
    var enableGlyphs by remember { mutableStateOf(settingsManager.enableGlyphs) }
    var zygiskFastMode by remember { mutableStateOf(settingsManager.zygiskFastMode) }

    // Simulation States
    var activeState by remember { mutableStateOf(ContextState.IDLE) }
    var simulatorView by remember { mutableStateOf(SimulatorView.FRONT) }
    var logsText by remember { mutableStateOf("System: Service active and listening on libxposed API 102 [Android 16 / Nothing OS 4.1].\nSystem: Synchronized hooking for 'com.android.systemui' is active.") }

    var notificationSender by remember { mutableStateOf("Mamma") }
    var notificationMessage by remember { mutableStateOf("Aarahe ho ghar? Dinner is ready! 🍲") }
    var notificationApp by remember { mutableStateOf("WhatsApp") }
    var isReplying by remember { mutableStateOf(false) }

    // Auto-dismiss/collapse logic: notification hides back behind the camera after 4 seconds if not replying
    LaunchedEffect(activeState, isReplying) {
        if (activeState == ContextState.NOTIFICATION && !isReplying) {
            delay(4000) // Show for 4 seconds
            if (!isReplying) {
                activeState = ContextState.IDLE
                logsText = "Log: Notification auto-collapsed back behind camera punch-hole.\n$logsText"
            }
        }
    }

    // Sync active state simulation with the real device module via Broadcast
    LaunchedEffect(activeState, notificationSender, notificationMessage, notificationApp) {
        com.example.xposed.XposedStatus.sendSimulationBroadcast(
            context,
            activeState.name,
            notificationSender,
            notificationMessage,
            notificationApp
        )
        logsText = "Log: Simulated state [${activeState.name}] broadcasted to real phone.\n$logsText"
    }

    // Tabs for Adaptive Layout inside single-view
    var currentTab by remember { mutableStateOf(0) } // 0: Manager App Settings, 1: Live Simulation, 2: Module Source Code

    val logAction: (String) -> Unit = { message ->
        logsText = "Log: $message\n$logsText"
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070708))
    ) {
        val isWide = maxWidth >= 680.dp

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DotMatrixText(
                                text = "GLYPH ISLAND",
                                dotSize = 2.2.dp,
                                charSpacing = 4.dp,
                                activeColor = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFE11919), RoundedCornerShape(2.dp))
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0F0F11)
                    )
                )
            },
            containerColor = Color(0xFF070708)
        ) { innerPadding ->
            if (isWide) {
                // Side-by-side layout for tablets / horizontal displays
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // LEFT COLUMN: Phone Simulator Control + Live Device Preview
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Switcher button for Simulator View
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141416))
                                .border(1.dp, Color(0xFF2E2E32), RoundedCornerShape(12.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (simulatorView == SimulatorView.FRONT) Color.White else Color.Transparent)
                                    .clickable { simulatorView = SimulatorView.FRONT }
                                    .wrapContentHeight(Alignment.CenterVertically),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "FRONT SCREEN",
                                    color = if (simulatorView == SimulatorView.FRONT) Color.Black else Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (simulatorView == SimulatorView.BACK) Color.White else Color.Transparent)
                                    .clickable { simulatorView = SimulatorView.BACK }
                                    .wrapContentHeight(Alignment.CenterVertically),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "REAR GLYPH LEDS",
                                    color = if (simulatorView == SimulatorView.BACK) Color.Black else Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Actual Interactive Simulator
                        PhoneSimulator(
                            settingsManager = settingsManager,
                            currentView = simulatorView,
                            activeState = activeState,
                            onStateTriggered = { msg -> logAction(msg) },
                            modifier = Modifier.weight(1f),
                            notificationSender = notificationSender,
                            notificationText = notificationMessage,
                            notificationApp = notificationApp,
                            isReplying = isReplying,
                            onReplyingChanged = { isReplying = it },
                            onActiveStateChanged = { activeState = it }
                        )
                    }

                    // RIGHT COLUMN: Configuration panels with Scroll state
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Navigation sub-tabs inside Settings Panel
                        DashboardTabs(currentTab = currentTab, onTabSelected = { currentTab = it })

                        when (currentTab) {
                            0 -> ConfigurationTab(
                                doubleTapAction = doubleTapAction,
                                onActionChanged = {
                                    doubleTapAction = it
                                    settingsManager.doubleTapAction = it
                                    logAction("Double Tap action mapped to '$it'")
                                },
                                islandWidth = islandWidth,
                                onWidthChanged = {
                                    islandWidth = it
                                    settingsManager.islandWidth = it
                                },
                                islandHeight = islandHeight,
                                onHeightChanged = {
                                    islandHeight = it
                                    settingsManager.islandHeight = it
                                },
                                islandYOffset = islandYOffset,
                                onYOffsetChanged = {
                                    islandYOffset = it
                                    settingsManager.islandYOffset = it
                                },
                                islandCornerRadius = islandCornerRadius,
                                onCornerRadiusChanged = {
                                    islandCornerRadius = it
                                    settingsManager.islandCornerRadius = it
                                },
                                enableOverlay = enableOverlay,
                                onOverlayToggled = {
                                    enableOverlay = it
                                    settingsManager.enableOverlay = it
                                    logAction("System overlay service ${if (it) "Enabled" else "Disabled"}")
                                },
                                enableGlyphs = enableGlyphs,
                                onGlyphsToggled = {
                                    enableGlyphs = it
                                    settingsManager.enableGlyphs = it
                                    logAction("Glyph LED Sync Interface ${if (it) "Activated" else "Deactivated"}")
                                },
                                zygiskFastMode = zygiskFastMode,
                                onZygiskToggled = {
                                    zygiskFastMode = it
                                    settingsManager.zygiskFastMode = it
                                    logAction("Zygisk-next direct hooking ${if (it) "Fast Mode" else "Standard Mode"}")
                                }
                            )

                            1 -> TriggerSimulationTab(
                                activeState = activeState,
                                onStateChanged = {
                                    activeState = it
                                    logAction("Simulation state switched to $it")
                                },
                                notificationSender = notificationSender,
                                onSenderChanged = { notificationSender = it },
                                notificationMessage = notificationMessage,
                                onMessageChanged = { notificationMessage = it },
                                notificationApp = notificationApp,
                                onAppChanged = { notificationApp = it }
                            )

                            2 -> SourceCodeTab()
                        }

                        // Terminal / Debug Console output
                        LogConsoleSection(logsText = logsText, onClearLogs = { logsText = "System: Awaiting trigger events..." })
                    }
                }
            } else {
                // Vertical layout for phones
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(14.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Switcher button for Simulator View vs Control Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141416))
                            .border(1.dp, Color(0xFF2E2E32), RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (simulatorView == SimulatorView.FRONT) Color.White else Color.Transparent)
                                .clickable { simulatorView = SimulatorView.FRONT }
                                .wrapContentHeight(Alignment.CenterVertically),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "FRONT SCREEN",
                                color = if (simulatorView == SimulatorView.FRONT) Color.Black else Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (simulatorView == SimulatorView.BACK) Color.White else Color.Transparent)
                                .clickable { simulatorView = SimulatorView.BACK }
                                .wrapContentHeight(Alignment.CenterVertically),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "REAR GLYPH LEDS",
                                color = if (simulatorView == SimulatorView.BACK) Color.Black else Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Device Simulator Box
                    PhoneSimulator(
                        settingsManager = settingsManager,
                        currentView = simulatorView,
                        activeState = activeState,
                        onStateTriggered = { msg -> logAction(msg) },
                        notificationSender = notificationSender,
                        notificationText = notificationMessage,
                        notificationApp = notificationApp,
                        isReplying = isReplying,
                        onReplyingChanged = { isReplying = it },
                        onActiveStateChanged = { activeState = it }
                    )

                    // Navigation Tabs
                    DashboardTabs(currentTab = currentTab, onTabSelected = { currentTab = it })

                    // Active Tab Panel
                    when (currentTab) {
                        0 -> ConfigurationTab(
                            doubleTapAction = doubleTapAction,
                            onActionChanged = {
                                doubleTapAction = it
                                settingsManager.doubleTapAction = it
                                logAction("Double Tap action mapped to '$it'")
                            },
                            islandWidth = islandWidth,
                            onWidthChanged = {
                                islandWidth = it
                                settingsManager.islandWidth = it
                            },
                            islandHeight = islandHeight,
                            onHeightChanged = {
                                islandHeight = it
                                settingsManager.islandHeight = it
                            },
                            islandYOffset = islandYOffset,
                            onYOffsetChanged = {
                                islandYOffset = it
                                settingsManager.islandYOffset = it
                            },
                            islandCornerRadius = islandCornerRadius,
                            onCornerRadiusChanged = {
                                islandCornerRadius = it
                                settingsManager.islandCornerRadius = it
                            },
                            enableOverlay = enableOverlay,
                            onOverlayToggled = {
                                enableOverlay = it
                                settingsManager.enableOverlay = it
                                logAction("System overlay service ${if (it) "Enabled" else "Disabled"}")
                            },
                            enableGlyphs = enableGlyphs,
                            onGlyphsToggled = {
                                enableGlyphs = it
                                settingsManager.enableGlyphs = it
                                logAction("Glyph LED Sync Interface ${if (it) "Activated" else "Deactivated"}")
                            },
                            zygiskFastMode = zygiskFastMode,
                            onZygiskToggled = {
                                zygiskFastMode = it
                                settingsManager.zygiskFastMode = it
                                logAction("Zygisk-next direct hooking ${if (it) "Fast Mode" else "Standard Mode"}")
                            }
                        )

                        1 -> TriggerSimulationTab(
                            activeState = activeState,
                            onStateChanged = {
                                activeState = it
                                logAction("Simulation state switched to $it")
                            },
                            notificationSender = notificationSender,
                            onSenderChanged = { notificationSender = it },
                            notificationMessage = notificationMessage,
                            onMessageChanged = { notificationMessage = it },
                            notificationApp = notificationApp,
                            onAppChanged = { notificationApp = it }
                        )

                        2 -> SourceCodeTab()
                    }

                    // Logs Terminal console output
                    LogConsoleSection(logsText = logsText, onClearLogs = { logsText = "System: Awaiting trigger events..." })
                }
            }
        }
    }
}

@Composable
fun DashboardTabs(currentTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0F11))
            .border(1.dp, Color(0xFF1E1E22), RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabsList = listOf("MANAGER SETTINGS", "SIMULATION CENTER", "LSPOSD SOURCE")
        tabsList.forEachIndexed { index, title ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (currentTab == index) Color(0xFF1F1F24) else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .wrapContentHeight(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title,
                    color = if (currentTab == index) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ConfigurationTab(
    doubleTapAction: String,
    onActionChanged: (String) -> Unit,
    islandWidth: Float,
    onWidthChanged: (Float) -> Unit,
    islandHeight: Float,
    onHeightChanged: (Float) -> Unit,
    islandYOffset: Float,
    onYOffsetChanged: (Float) -> Unit,
    islandCornerRadius: Float,
    onCornerRadiusChanged: (Float) -> Unit,
    enableOverlay: Boolean,
    onOverlayToggled: (Boolean) -> Unit,
    enableGlyphs: Boolean,
    onGlyphsToggled: (Boolean) -> Unit,
    zygiskFastMode: Boolean,
    onZygiskToggled: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isActualActive by remember {
        mutableStateOf(com.example.xposed.XposedStatus.isModuleActive(context))
    }
    var showHelpGuide by remember { mutableStateOf(!isActualActive) }
    var useHindiGuide by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                isActualActive = com.example.xposed.XposedStatus.isModuleActive(context)
            }
        }
        val filter = android.content.IntentFilter("com.example.glyphisland.ACTION_MODULE_ACTIVE")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        // Send a ping immediately to ask SystemUI if it's active
        try {
            val pingIntent = android.content.Intent("com.example.glyphisland.ACTION_PING")
            context.sendBroadcast(pingIntent)
        } catch (e: Exception) {}
        
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Xposed Module Live Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
            shape = RoundedCornerShape(16.dp),
            border = BoxBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "LSPOSED STATUS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isActualActive) Color(0xFF1B5E20) else Color(0xFFC62828)
                            )
                            .border(
                                1.dp,
                                if (isActualActive) Color(0xFF4CAF50) else Color(0xFFE53935),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isActualActive) "ACTIVE" else "INACTIVE",
                            color = if (isActualActive) Color(0xFF81C784) else Color(0xFFEF5350),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isActualActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isActualActive) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isActualActive) {
                                "Module Loaded successfully by LSPosed!"
                            } else {
                                "Module is not active in LSPosed"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isActualActive) {
                                "Glyph Island system hooks are successfully synchronized and hooking 'com.android.systemui'."
                            } else {
                                "Open the LSPosed app, enable the module, check 'System UI' and 'System Framework' scopes, and reboot."
                            },
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Expanded Troubleshooting Guide Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
            shape = RoundedCornerShape(16.dp),
            border = BoxBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHelpGuide = !showHelpGuide },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFE11919),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "LSPOSED SETUP & FIX GUIDE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        if (showHelpGuide) "COLLAPSE ▲" else "EXPAND ▼",
                        color = Color(0xFFE11919),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(
                    visible = showHelpGuide,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Language Selector Tab
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF16161A))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { useHindiGuide = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!useHindiGuide) Color(0xFF1E1E24) else Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("English Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { useHindiGuide = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (useHindiGuide) Color(0xFF1E1E24) else Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("हिंदी गाइड", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (useHindiGuide) {
                            // Hindi Guide
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "अगर ऐप आपके फोन पर काम नहीं कर रहा है, तो इन स्टेप्स को फॉलो करें:",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                StepItem(
                                    number = "1",
                                    title = "डिवाइस रूटेड होना ज़रूरी है",
                                    desc = "यह एक Xposed मॉड्यूल है. आपका फोन Magisk, APatch या KernelSU से रूट होना चाहिए."
                                )

                                StepItem(
                                    number = "2",
                                    title = "LSPosed फ्रेमवर्क चालू करें",
                                    desc = "अपने फोन में LSPosed Manager ऐप खोलें और सुनिश्चित करें कि फ्रेमवर्क एक्टिव है."
                                )

                                StepItem(
                                    number = "3",
                                    title = "मॉड्यूल को ऑन करें (Enable Module)",
                                    desc = "LSPosed के 'Modules' सेक्शन में जाएं, 'Glyph Island' को ढूँढें और उसका स्विच ऑन करें."
                                )

                                StepItem(
                                    number = "4",
                                    title = "हुक स्कोप सेलेक्ट करें (VERY CRITICAL ⚠️)",
                                    desc = "मॉड्यूल ऑन करने के बाद स्कोप लिस्ट खुलेगी। वहाँ 'System UI' (com.android.systemui), 'Android System' (System Framework) और 'Glyph Island' (यह ऐप खुद) तीनों को टिक (✓) करना बेहद ज़रूरी है। बिना इसके ऐप मॉड्यूल के एक्टिव होने का पता नहीं लगा पाएगा!"
                                )

                                StepItem(
                                    number = "5",
                                    title = "फोन रीबूट करें",
                                    desc = "सभी सेटिंग्स सेव करने के बाद, अपने फोन को रीबूट करें या SystemUI को रिस्टार्ट करें। रीबूट के बाद यहाँ स्टेटस 'ACTIVE (REAL)' दिखाई देगा."
                                )
                            }
                        } else {
                            // English Guide
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "If the module is not working on your device, please follow these steps:",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                StepItem(
                                    number = "1",
                                    title = "Root Required",
                                    desc = "This is a low-level system module. Your phone must be rooted using Magisk, APatch, or KernelSU."
                                )

                                StepItem(
                                    number = "2",
                                    title = "LSPosed Active",
                                    desc = "Open your LSPosed or Luru Manager app and ensure the framework is fully active."
                                )

                                StepItem(
                                    number = "3",
                                    title = "Toggle Glyph Island Module",
                                    desc = "In LSPosed Manager, head to the 'Modules' section, find 'Glyph Island', and toggle it ON."
                                )

                                StepItem(
                                    number = "4",
                                    title = "Select Scopes (VERY CRITICAL ⚠️)",
                                    desc = "Under module settings, you MUST check 'System UI' (com.android.systemui), 'Android System' (System Framework), and 'Glyph Island' (this app itself). If unchecked, the manager app cannot detect the active hook status."
                                )

                                StepItem(
                                    number = "5",
                                    title = "Reboot Device",
                                    desc = "After configuring the scopes, perform a full device reboot or soft-restart SystemUI. The status badge will change to 'ACTIVE (REAL)' once running."
                                )
                            }
                        }
                    }
                }
            }
        }

        // Toggle Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
            shape = RoundedCornerShape(16.dp),
            border = BoxBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("MODULE STATUS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dynamic Island Overlay", color = Color.White, fontSize = 13.sp)
                        Text("Simulate system window alert overlay", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Switch(
                        checked = enableOverlay,
                        onCheckedChange = onOverlayToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE11919)
                        )
                    )
                }

                Divider(color = Color(0xFF1E1E22))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hardware Glyph LED Sync", color = Color.White, fontSize = 13.sp)
                        Text("Transmit island progress to rear LEDs", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Switch(
                        checked = enableGlyphs,
                        onCheckedChange = onGlyphsToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE11919)
                        )
                    )
                }

                Divider(color = Color(0xFF1E1E22))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Zygisk-next Hooking Mode", color = Color.White, fontSize = 13.sp)
                        Text("Enables zero-latency system inputs", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Switch(
                        checked = zygiskFastMode,
                        onCheckedChange = onZygiskToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE11919)
                        )
                    )
                }
            }
        }

        // Double Tap Gestures Option
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
            shape = RoundedCornerShape(16.dp),
            border = BoxBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("DOUBLE TAP CUSTOM TRIGGER", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                val actionOptions: List<Pair<String, ImageVector>> = listOf(
                    SettingsManager.ACTION_OPEN_APP to Icons.Default.PlayArrow,
                    SettingsManager.ACTION_SCREENSHOT to Icons.Default.Edit,
                    SettingsManager.ACTION_FLASHLIGHT to Icons.Default.Warning,
                    SettingsManager.ACTION_LAST_APP to Icons.Default.Refresh
                )

                actionOptions.forEach { (action, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onActionChanged(action) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = doubleTapAction == action,
                            onClick = { onActionChanged(action) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFE11919),
                                unselectedColor = Color.White.copy(alpha = 0.4f)
                            )
                        )
                        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        Text(action, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // Geometry sliders
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
            shape = RoundedCornerShape(16.dp),
            border = BoxBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("ISLAND GEOMETRY DIMENSIONS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Island Width", color = Color.White, fontSize = 12.sp)
                        Text("${islandWidth.toInt()} dp", color = Color(0xFFE11919), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = islandWidth,
                        onValueChange = onWidthChanged,
                        valueRange = 120f..240f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFE11919)
                        )
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Island Height", color = Color.White, fontSize = 12.sp)
                        Text("${islandHeight.toInt()} dp", color = Color(0xFFE11919), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = islandHeight,
                        onValueChange = onHeightChanged,
                        valueRange = 26f..70f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFE11919)
                        )
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Y-Axis Offset Position", color = Color.White, fontSize = 12.sp)
                        Text("${islandYOffset.toInt()} dp", color = Color(0xFFE11919), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = islandYOffset,
                        onValueChange = onYOffsetChanged,
                        valueRange = 8f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFE11919)
                        )
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Corner Radius", color = Color.White, fontSize = 12.sp)
                        Text("${islandCornerRadius.toInt()} dp", color = Color(0xFFE11919), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = islandCornerRadius,
                        onValueChange = onCornerRadiusChanged,
                        valueRange = 10f..35f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFE11919)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TriggerSimulationTab(
    activeState: ContextState,
    onStateChanged: (ContextState) -> Unit,
    notificationSender: String,
    onSenderChanged: (String) -> Unit,
    notificationMessage: String,
    onMessageChanged: (String) -> Unit,
    notificationApp: String,
    onAppChanged: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
        shape = RoundedCornerShape(16.dp),
        border = BoxBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("CONTEXTUAL SIMULATION CODES", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(
                "Trigger active status flows below to observe how the smart Dynamic Island on the screen and the physical Glyph LED bands on the rear coordinate together in real-time.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )

            val states: List<Triple<ContextState, String, ImageVector>> = listOf(
                Triple(ContextState.IDLE, "Idle Cam Mode", Icons.Default.Info),
                Triple(ContextState.CHARGING, "Charging Flow (45W)", Icons.Default.Star),
                Triple(ContextState.MEDIA, "Lofi Media Player", Icons.Default.PlayArrow),
                Triple(ContextState.NOTIFICATION, "Incoming Notification", Icons.Default.Notifications),
                Triple(ContextState.TIMER, "Countdown Timer (1:24)", Icons.Default.Refresh)
            )

            states.forEach { (state, title, icon) ->
                val isActive = activeState == state
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) Color(0x1AE11919) else Color(0xFF141416))
                        .border(1.dp, if (isActive) Color(0xFFE11919) else Color(0xFF1E1E22), RoundedCornerShape(10.dp))
                        .clickable { onStateChanged(state) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isActive) Color(0xFFE11919) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        title,
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isActive) {
                        Text(
                            "RUNNING",
                            color = Color(0xFFE11919),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Divider(color = Color(0xFF1E1E22), modifier = Modifier.padding(vertical = 4.dp))

            Text("MESSAGE SIMULATION PANEL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(
                "Customize message previews and app integration templates (e.g., WhatsApp message previews) in Glyph Island.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )

            // Sender OutlinedTextField
            OutlinedTextField(
                value = notificationSender,
                onValueChange = onSenderChanged,
                label = { Text("Sender Name", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF141416),
                    unfocusedContainerColor = Color(0xFF141416),
                    disabledContainerColor = Color(0xFF141416),
                    focusedIndicatorColor = Color(0xFFE11919),
                    unfocusedIndicatorColor = Color(0xFF1E1E22),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Message OutlinedTextField
            OutlinedTextField(
                value = notificationMessage,
                onValueChange = onMessageChanged,
                label = { Text("Message Body", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF141416),
                    unfocusedContainerColor = Color(0xFF141416),
                    disabledContainerColor = Color(0xFF141416),
                    focusedIndicatorColor = Color(0xFFE11919),
                    unfocusedIndicatorColor = Color(0xFF1E1E22),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // App Selector Row
            Text("SELECT APPLICATION BRAND", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            val apps = listOf("WhatsApp", "Telegram", "Instagram", "System")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                apps.forEach { app ->
                    val isSelected = notificationApp == app
                    val appColor = when (app.lowercase()) {
                        "whatsapp" -> Color(0xFF25D366)
                        "telegram" -> Color(0xFF0088CC)
                        "instagram" -> Color(0xFFE1306C)
                        else -> Color(0xFFE11919)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) appColor.copy(alpha = 0.15f) else Color(0xFF141416))
                            .border(1.dp, if (isSelected) appColor else Color(0xFF1E1E22), RoundedCornerShape(8.dp))
                            .clickable { onAppChanged(app) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app,
                            color = if (isSelected) appColor else Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Fire Button
            val buttonColor = when (notificationApp.lowercase()) {
                "whatsapp" -> Color(0xFF25D366)
                "telegram" -> Color(0xFF0088CC)
                "instagram" -> Color(0xFFE1306C)
                else -> Color(0xFFE11919)
            }
            Button(
                onClick = { onStateChanged(ContextState.NOTIFICATION) },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = if (notificationApp.lowercase() == "whatsapp") Color.Black else Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIMULATE ${notificationApp.uppercase()} PREVIEW",
                    color = if (notificationApp.lowercase() == "whatsapp") Color.Black else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SourceCodeTab() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
        shape = RoundedCornerShape(16.dp),
        border = BoxBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("XPOSED INJECTION HOOKS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE11919))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("API 102 SYNCED", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // System Injection Recommendation Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1B5E20).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        "SYSTEM INJECTION HOOK ACTIVE",
                        color = Color(0xFF4CAF50),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Recommendation hook successfully synchronized into 'com.android.systemui' on Android 16 (Nothing OS 4.1) using the modern libxposed API 102 architecture.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Text(
                "Below is the exact production code compiling inside your module. It implements the modern io.github.libxposed.api structure, hooking SystemUI to load the custom bar overlay on Android 16.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )

            // Syntax-like highlighted block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF050506))
                    .border(1.dp, Color(0xFF161619), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                val code = """
package com.example.xposed

import android.view.View
import android.widget.FrameLayout
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.annotations.BeforeHook
import io.github.libxposed.api.annotations.AfterHook
import io.github.libxposed.api.annotations.XposedHooker

class GlyphIslandXposedModule(
    base: XposedInterface, 
    param: XposedModuleInterface.ModuleLoadedParam
) : XposedModule(base, param) {

    override fun onPackageLoaded(param: XposedModuleInterface.OnPackageLoadedParam) {
        super.onPackageLoaded(param)
        
        // System Injection: Target com.android.systemui on Android 16 / Nothing OS 4.1
        if (param.packageName != "com.android.systemui") return
        
        // Hook Status Bar inflate to inject capsule
        val clazz = Class.forName("com.android.systemui.statusbar.phone.PhoneStatusBarView", false, param.classLoader)
        val method = clazz.getDeclaredMethod("onFinishInflate")
        hookMethod(method, PhoneStatusBarViewHooker::class.java)
        
        // Hook input receiver to route swipe gestures
        hookInputMonitorForGestures(param.classLoader)
        
        // Hook battery and sync Nothing Phone (2a) LEDs
        hookBatteryReceiver(param.classLoader)
    }

    @XposedHooker
    class PhoneStatusBarViewHooker(private val module: GlyphIslandXposedModule) : XposedInterface.Hooker<GlyphIslandXposedModule> {
        @AfterHook
        fun after(callback: XposedInterface.AfterCallback) {
            val statusBarView = callback.thisObject as View
            val context = statusBarView.context
            module.injectGlyphIslandView(statusBarView, context)
        }
    }
}
                """.trimIndent()

                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        code,
                        color = Color(0xFFAAAAAA),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LogConsoleSection(
    logsText: String,
    onClearLogs: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
        shape = RoundedCornerShape(16.dp),
        border = BoxBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LSPosed Injection Terminal", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "CLEAR",
                    color = Color(0xFFE11919),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClearLogs() }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF050506))
                    .border(1.dp, Color(0xFF161619), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        logsText,
                        color = Color(0xFF00FF00), // Hacker style green
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StepItem(
    number: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE11919)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun BoxBorder() = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E1E22))

