package com.example.ui

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class SimulatorView {
    FRONT, BACK
}

enum class ContextState {
    IDLE, CHARGING, MEDIA, NOTIFICATION, TIMER
}

@Composable
fun PhoneSimulator(
    settingsManager: SettingsManager,
    currentView: SimulatorView,
    activeState: ContextState,
    onStateTriggered: (String) -> Unit,
    modifier: Modifier = Modifier,
    notificationSender: String = "SARA",
    notificationText: String = "HEY THERE!",
    notificationApp: String = "WhatsApp",
    isReplying: Boolean = false,
    onReplyingChanged: (Boolean) -> Unit = {},
    onActiveStateChanged: (ContextState) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    // Settings State
    var islandWidth by remember { mutableStateOf(settingsManager.islandWidth) }
    var islandHeight by remember { mutableStateOf(settingsManager.islandHeight) }
    var islandYOffset by remember { mutableStateOf(settingsManager.islandYOffset) }
    var islandCornerRadius by remember { mutableStateOf(settingsManager.islandCornerRadius) }
    var brightness by remember { mutableStateOf(settingsManager.brightness) }
    var volume by remember { mutableStateOf(settingsManager.volume) }

    // Sync from settingsManager periodically
    LaunchedEffect(settingsManager.islandWidth, settingsManager.islandHeight, settingsManager.islandYOffset, settingsManager.islandCornerRadius) {
        islandWidth = settingsManager.islandWidth
        islandHeight = settingsManager.islandHeight
        islandYOffset = settingsManager.islandYOffset
        islandCornerRadius = settingsManager.islandCornerRadius
    }

    // Vibration helper
    val triggerVibration: (Long) -> Unit = { duration ->
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    // Media visualization values
    val infiniteTransition = rememberInfiniteTransition(label = "media_viz")
    val waveAnim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "wave1"
    )
    val waveAnim2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(animation = tween(550, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "wave2"
    )
    val waveAnim3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(animation = tween(320, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "wave3"
    )
    val waveAnim4 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(animation = tween(450, easing = FastOutLinearInEasing), repeatMode = RepeatMode.Reverse),
        label = "wave4"
    )

    // Breathing pulse for LEDs/Charging
    val breathingAnim by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "breathing"
    )

    // Running timer values
    var timerSeconds by remember { mutableStateOf(84) } // 1:24
    LaunchedEffect(activeState) {
        if (activeState == ContextState.TIMER) {
            timerSeconds = 84
            while (timerSeconds > 0 && activeState == ContextState.TIMER) {
                delay(1000)
                timerSeconds--
            }
        }
    }

    // Temporary toast trigger inside island
    var temporaryAlertText by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isReplying) {
        if (isReplying) {
            delay(150)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Safe fallback if requester is not attached yet
            }
        }
    }

    LaunchedEffect(isReplying, activeState) {
        if (!isReplying || activeState != ContextState.NOTIFICATION) {
            replyText = ""
        }
    }

    val showTemporaryAlert: (String) -> Unit = { text ->
        scope.launch {
            temporaryAlertText = text
            delay(3000)
            temporaryAlertText = null
        }
    }

    val appColor = when (notificationApp.lowercase()) {
        "whatsapp" -> Color(0xFF25D366)
        "telegram" -> Color(0xFF0088CC)
        "instagram" -> Color(0xFFE1306C)
        else -> Color(0xFFE11919)
    }

    // Simulated slider gestures
    var activeGestureSlider by remember { mutableStateOf<String?>(null) } // "brightness" or "volume" or null
    var activeGestureTimer by remember { mutableStateOf(0) }
    LaunchedEffect(activeGestureSlider) {
        if (activeGestureSlider != null) {
            activeGestureTimer = 3
            while (activeGestureTimer > 0) {
                delay(1000)
                activeGestureTimer--
            }
            activeGestureSlider = null
        }
    }

    Box(
        modifier = modifier
            .width(280.dp)
            .height(550.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xFF0F0F11))
            .border(3.dp, Color(0xFF2C2C32), RoundedCornerShape(36.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (currentView == SimulatorView.FRONT) {
            // FRONT VIEW (Screen and punch-hole camera)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF000000))
            ) {
                // Status Bar Indicators (Time, Battery, signal)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("13:37", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("5G", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text("100%", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }

                // Center camera punch-hole (represented as dark void)
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 12.dp)
                        .background(Color(0xFF141416), CircleShape)
                        .border(1.dp, Color(0xFF2E2E32), CircleShape)
                )

                // DYNAMIC ISLAND CONTAINER
                val isIdle = activeState == ContextState.IDLE && 
                             temporaryAlertText == null && 
                             activeGestureSlider == null

                val targetWidth = when {
                    isIdle -> 14.dp
                    temporaryAlertText != null -> 210.dp
                    activeState == ContextState.CHARGING -> 195.dp
                    activeState == ContextState.MEDIA -> 220.dp
                    activeState == ContextState.NOTIFICATION -> if (isReplying) 260.dp else 250.dp
                    activeState == ContextState.TIMER -> 160.dp
                    activeGestureSlider == "brightness" -> 200.dp
                    activeGestureSlider == "volume" -> 200.dp
                    else -> islandWidth.dp
                }

                val targetHeight = when {
                    isIdle -> 14.dp
                    temporaryAlertText != null -> 42.dp
                    activeState == ContextState.CHARGING -> 46.dp
                    activeState == ContextState.MEDIA -> 52.dp
                    activeState == ContextState.NOTIFICATION -> if (isReplying) 115.dp else 52.dp
                    activeState == ContextState.TIMER -> 42.dp
                    activeGestureSlider == "brightness" -> 44.dp
                    activeGestureSlider == "volume" -> 44.dp
                    else -> islandHeight.dp
                }

                val targetYOffset = if (isIdle) 12.dp else islandYOffset.dp
                val targetCornerRadius = if (isIdle) 7.dp else islandCornerRadius.dp
                val borderAlpha = if (isIdle) 0f else 0.2f
                val contentAlpha = if (isIdle) 0f else 1f

                // Custom tuned spring specs for organic, highly responsive, fluid Nothing OS/iOS island feel
                val dynamicSpringSpec = spring<Dp>(
                    dampingRatio = 0.65f, // Sweet spot for elegant physical bounce
                    stiffness = 350f      // Ultra responsive transition speed
                )

                val animWidth by animateDpAsState(targetValue = targetWidth, animationSpec = dynamicSpringSpec, label = "w")
                val animHeight by animateDpAsState(targetValue = targetHeight, animationSpec = dynamicSpringSpec, label = "h")
                val animYOffset by animateDpAsState(targetValue = targetYOffset, animationSpec = dynamicSpringSpec, label = "y")
                val animCornerRadius by animateDpAsState(targetValue = targetCornerRadius, animationSpec = dynamicSpringSpec, label = "radius")
                val animBorderAlpha by animateFloatAsState(targetValue = borderAlpha, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "border_alpha")
                val animContentAlpha by animateFloatAsState(targetValue = contentAlpha, animationSpec = tween(durationMillis = 150), label = "content_alpha")

                val gestureModifier = if (!isReplying) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    triggerVibration(60)
                                    val action = settingsManager.doubleTapAction
                                    onStateTriggered("Double Tap: Mapped to '$action'")
                                    showTemporaryAlert("TRIGGER: $action")
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val isLeftSide = change.position.x < (size.width / 2)
                                if (isLeftSide) {
                                    activeGestureSlider = "brightness"
                                    val newBrightness = (brightness + (dragAmount.x / 5f).toInt()).coerceIn(0, 100)
                                    if (newBrightness != brightness) {
                                        brightness = newBrightness
                                        settingsManager.brightness = newBrightness
                                        triggerVibration(10) // Light click tick
                                    }
                                } else {
                                    activeGestureSlider = "volume"
                                    val newVolume = (volume + (dragAmount.x / 5f).toInt()).coerceIn(0, 100)
                                    if (newVolume != volume) {
                                        volume = newVolume
                                        settingsManager.volume = newVolume
                                        triggerVibration(10) // Light click tick
                                    }
                                }
                            }
                        }
                } else Modifier

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = animYOffset)
                        .width(animWidth)
                        .height(animHeight)
                        .clip(RoundedCornerShape(animCornerRadius))
                        .background(Color(0xFF0F0F12))
                        .border(1.5.dp, Color.White.copy(alpha = animBorderAlpha), RoundedCornerShape(animCornerRadius))
                        .then(gestureModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = animContentAlpha },
                        contentAlignment = Alignment.Center
                    ) {
                        // Contents based on active state / action
                        when {
                            temporaryAlertText != null -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFE11919), CircleShape) // Nothing Red Dot accent
                                )
                                DotMatrixText(
                                    text = temporaryAlertText!!,
                                    dotSize = 1.2.dp,
                                    dotSpacing = 0.8.dp,
                                    activeColor = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                        }

                        activeGestureSlider == "brightness" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DotMatrixText("BRT", dotSize = 1.dp, activeColor = Color.White.copy(alpha = 0.5f))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(Color(0x22FFFFFF), CircleShape)
                                        .drawBehind {
                                            drawRoundRect(
                                                color = Color.White,
                                                size = Size(size.width * (brightness / 100f), size.height),
                                                cornerRadius = CornerRadius(2f, 2f)
                                            )
                                        }
                                )
                                DotMatrixText("$brightness%", dotSize = 1.dp, activeColor = Color.White)
                            }
                        }

                        activeGestureSlider == "volume" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DotMatrixText("VOL", dotSize = 1.dp, activeColor = Color.White.copy(alpha = 0.5f))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(Color(0x22FFFFFF), CircleShape)
                                        .drawBehind {
                                            drawRoundRect(
                                                color = Color.White,
                                                size = Size(size.width * (volume / 100f), size.height),
                                                cornerRadius = CornerRadius(2f, 2f)
                                            )
                                        }
                                )
                                DotMatrixText("$volume%", dotSize = 1.dp, activeColor = Color.White)
                            }
                        }

                        activeState == ContextState.CHARGING -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                Color(0xFFE11919).copy(alpha = breathingAnim),
                                                CircleShape
                                            )
                                    )
                                    DotMatrixText("CHARGING", dotSize = 1.1.dp, dotSpacing = 0.7.dp, activeColor = Color.White)
                                }
                                DotMatrixText("45W  78%", dotSize = 1.1.dp, dotSpacing = 0.7.dp, activeColor = Color(0xFFE11919))
                            }
                        }

                        activeState == ContextState.MEDIA -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DotMatrixText("LOFI BEATS", dotSize = 1.1.dp, activeColor = Color.White)
                                
                                // Beautiful Dot visualizer
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val barColor = Color(0xFFE11919)
                                    val inactiveBar = Color(0x1AFFFFFF)
                                    
                                    Box(modifier = Modifier.width(4.dp).height(24.dp).background(inactiveBar).drawBehind { drawRect(barColor, size = Size(size.width, size.height * waveAnim1)) })
                                    Box(modifier = Modifier.width(4.dp).height(24.dp).background(inactiveBar).drawBehind { drawRect(barColor, size = Size(size.width, size.height * waveAnim2)) })
                                    Box(modifier = Modifier.width(4.dp).height(24.dp).background(inactiveBar).drawBehind { drawRect(barColor, size = Size(size.width, size.height * waveAnim3)) })
                                    Box(modifier = Modifier.width(4.dp).height(24.dp).background(inactiveBar).drawBehind { drawRect(barColor, size = Size(size.width, size.height * waveAnim4)) })
                                }
                            }
                        }

                        activeState == ContextState.NOTIFICATION -> {
                            if (isReplying) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(modifier = Modifier.size(5.dp).background(appColor, CircleShape))
                                            Text(
                                                text = "REPLY TO ${notificationSender.uppercase()}",
                                                color = appColor,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        // Cancel/Close button
                                        Text(
                                            text = "CANCEL",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.clickable {
                                                onReplyingChanged(false)
                                                onActiveStateChanged(ContextState.IDLE)
                                            }
                                        )
                                    }

                                    // Input field and Send button row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Styled Text Field
                                        BasicTextField(
                                            value = replyText,
                                            onValueChange = { replyText = it },
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(appColor),
                                            keyboardOptions = KeyboardOptions(
                                                imeAction = ImeAction.Send
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onSend = {
                                                    if (replyText.isNotEmpty()) {
                                                        onStateTriggered("Reply sent to $notificationSender: \"$replyText\"")
                                                        showTemporaryAlert("REPLY SENT!")
                                                        onReplyingChanged(false)
                                                        onActiveStateChanged(ContextState.IDLE)
                                                        replyText = ""
                                                    }
                                                }
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(focusRequester)
                                                .background(Color(0xFF16161C), RoundedCornerShape(6.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            decorationBox = { innerTextField ->
                                                if (replyText.isEmpty()) {
                                                    Text(
                                                        text = "Type reply...",
                                                        color = Color.White.copy(alpha = 0.3f),
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )

                                        // Send Button
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(appColor)
                                                .clickable {
                                                    if (replyText.isNotEmpty()) {
                                                        onStateTriggered("Reply sent to $notificationSender: \"$replyText\"")
                                                        showTemporaryAlert("REPLY SENT!")
                                                        onReplyingChanged(false)
                                                        onActiveStateChanged(ContextState.IDLE)
                                                        replyText = ""
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "SEND",
                                                color = if (notificationApp.lowercase() == "whatsapp") Color.Black else Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Helper typing label instead of chips
                                    Text(
                                        text = "TYPE WITH YOUR PHONE KEYBOARD & TAP SEND",
                                        color = appColor.copy(alpha = 0.6f),
                                        fontSize = 6.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            onReplyingChanged(true)
                                        }
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Profile Avatar with app overlay badge
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        // Avatar circle
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF222226), CircleShape)
                                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = notificationSender.firstOrNull()?.toString()?.uppercase() ?: "?",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        // App Indicator badge overlay
                                        Box(
                                            modifier = Modifier
                                                .size(11.dp)
                                                .background(appColor, CircleShape)
                                                .border(1.dp, Color(0xFF0F0F12), CircleShape)
                                        )
                                    }

                                    // Message Text Details (Sender + Message Preview)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = notificationSender.uppercase(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(appColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = notificationApp.uppercase(),
                                                    color = appColor,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        Text(
                                            text = notificationText,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }

                                    // Right accent: Active glowing pulse (dot-matrix look)
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(appColor.copy(alpha = 0.8f), CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    )
                                }
                            }
                        }

                        activeState == ContextState.TIMER -> {
                            val mins = timerSeconds / 60
                            val secs = timerSeconds % 60
                            val timerStr = String.format("%02d:%02d", mins, secs)

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DotMatrixText("TMR", dotSize = 1.0.dp, activeColor = Color.White.copy(alpha = 0.5f))
                                DotMatrixText(timerStr, dotSize = 1.2.dp, activeColor = Color.White)
                            }
                        }

                        else -> {
                            // Collapsed/Camera Blend
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(0xFF141416), CircleShape)
                            )
                        }
                    }
                }
            }


                // Help overlay hints
                if (!isReplying || activeState != ContextState.NOTIFICATION) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("PUNCH-HOLE GLASS SCREEN", color = Color.White.copy(alpha = 0.25f), fontSize = 10.sp, letterSpacing = 1.sp)
                        Text("Swipe Left for Brightness  |  Right for Volume", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                        Text("Double Tap Island to trigger Action", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                    }
                }
            }
        } else {
            // BACK VIEW: Translucent Nothing Phone (2a) chassis details + Glyph LEDs!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF161619))
            ) {
                // Background grid/printed traces of the Nothing Phone (2a)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Central Camera Module outer rim
                    drawCircle(
                        color = Color(0xFF222226),
                        radius = 70.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 3f)
                    )

                    // Printed circuit traces lines (Nothing 2a tech-look)
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = Offset(size.width / 2f, size.height / 3f + 70.dp.toPx()),
                        end = Offset(size.width / 2f, size.height - 40.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                    
                    // Circular inductive coil traces below camera
                    drawCircle(
                        color = Color(0x12FFFFFF),
                        radius = 50.dp.toPx(),
                        center = Offset(size.width / 2f, size.height * 0.7f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Centered dual camera module circles ("eyes")
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (550.dp / 3f - 18.dp)),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF000000), CircleShape)
                            .border(2.dp, Color(0xFF323238), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF141416), CircleShape))
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF000000), CircleShape)
                            .border(2.dp, Color(0xFF323238), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF141416), CircleShape))
                    }
                }

                // GLYPH LED STRIPS (3 distinct zones)
                // We will represent their glows in real-time!
                
                // Active lights based on activeState
                val isGlyphEnabled = settingsManager.enableGlyphs
                val glyphColor1 by animateColorAsState(
                    targetValue = when {
                        !isGlyphEnabled -> Color(0x1A404040)
                        activeState == ContextState.NOTIFICATION -> Color.White
                        activeState == ContextState.MEDIA -> Color.White.copy(alpha = waveAnim1)
                        activeState == ContextState.CHARGING -> Color.White.copy(alpha = breathingAnim)
                        else -> Color(0x22FFFFFF)
                    }, label = "g1"
                )

                val glyphColor2 by animateColorAsState(
                    targetValue = when {
                        !isGlyphEnabled -> Color(0x1A404040)
                        activeState == ContextState.NOTIFICATION -> Color.White
                        activeState == ContextState.MEDIA -> Color.White.copy(alpha = waveAnim2)
                        activeState == ContextState.CHARGING -> Color.White.copy(alpha = breathingAnim)
                        else -> Color(0x22FFFFFF)
                    }, label = "g2"
                )

                val glyphColor3 by animateColorAsState(
                    targetValue = when {
                        !isGlyphEnabled -> Color(0x1A404040)
                        activeState == ContextState.NOTIFICATION -> Color.White
                        activeState == ContextState.MEDIA -> Color.White.copy(alpha = waveAnim3)
                        activeState == ContextState.CHARGING -> Color.White // Progress-driven
                        activeState == ContextState.TIMER -> Color.White // Progress-driven
                        else -> Color(0x22FFFFFF)
                    }, label = "g3"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 3f
                    val radius = 80.dp.toPx()

                    // LED 1: Top-Left Arch
                    drawArc(
                        color = glyphColor1,
                        startAngle = 145f,
                        sweepAngle = 60f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = Size(radius * 2f, radius * 2f),
                        topLeft = Offset(cx - radius, cy - radius)
                    )

                    // LED 2: Top-Right Arch
                    drawArc(
                        color = glyphColor2,
                        startAngle = 295f,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = Size(radius * 2f, radius * 2f),
                        topLeft = Offset(cx - radius, cy - radius)
                    )

                    // LED 3: Bottom-Right sloped indicator (shows volume, battery percent, or timer countdown)
                    // Let's draw it as a curved arc around the bottom right quadrant
                    val progressRatio = when (activeState) {
                        ContextState.CHARGING -> 0.78f // Charging is at 78%
                        ContextState.TIMER -> (timerSeconds / 84f).coerceIn(0f, 1f) // Timer depleting
                        else -> 1f
                    }
                    val maxSweep = 75f
                    val activeSweep = maxSweep * progressRatio

                    // Base background strip for LED 3
                    drawArc(
                        color = if (isGlyphEnabled) Color(0x22FFFFFF) else Color(0x1A404040),
                        startAngle = 35f,
                        sweepAngle = maxSweep,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = Size(radius * 2f, radius * 2f),
                        topLeft = Offset(cx - radius, cy - radius)
                    )

                    // Active progress part
                    if (isGlyphEnabled && progressRatio > 0) {
                        drawArc(
                            color = glyphColor3,
                            startAngle = 35f,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                            size = Size(radius * 2f, radius * 2f),
                            topLeft = Offset(cx - radius, cy - radius)
                        )
                    }
                }

                // Dot Matrix overlay showing backing stats
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("GLYPH INTERFACE", color = Color(0xFFE11919), fontSize = 10.sp, letterSpacing = 2.sp)
                    Text("3 Active LED zones (Nothing Phone 2a)", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                    if (activeState == ContextState.CHARGING) {
                        Text("Glyph 3 showing Charging Progress (78%)", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    } else if (activeState == ContextState.TIMER) {
                        Text("Glyph 3 showing Timer Progress Indicator", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    } else if (activeState == ContextState.MEDIA) {
                        Text("LEDs pulsing to equalizer frequency beat", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}


