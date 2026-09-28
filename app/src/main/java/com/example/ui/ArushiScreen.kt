package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantState
import com.example.data.model.InteractionMode
import com.example.ui.theme.ArushiCardBg
import com.example.ui.theme.ArushiCardBorder
import com.example.ui.theme.ArushiDeepBg
import com.example.ui.theme.ArushiEmerald
import com.example.ui.theme.ArushiGold
import com.example.ui.theme.ArushiNeonCyan
import com.example.ui.theme.ArushiNeonPink
import com.example.ui.theme.ArushiNeonViolet
import com.example.ui.theme.ArushiSurfaceVariant
import com.example.ui.theme.ArushiTextMuted
import com.example.ui.theme.ArushiTextPrimary
import com.example.ui.theme.ArushiTextSecondary

@Composable
fun ArushiScreen(
    viewModel: ArushiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launchers
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show error snackbar if any
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    fun onMicClick() {
        val hasMicPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPerm) {
            viewModel.toggleVoiceListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ArushiDeepBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = ArushiDeepBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                state = uiState.assistantState,
                mode = uiState.interactionMode,
                isBgServiceActive = uiState.isBackgroundServiceRunning,
                savedAddressSet = uiState.savedAddress.isComplete(),
                onModeToggle = {
                    viewModel.setInteractionMode(
                        if (uiState.interactionMode == InteractionMode.VOICE) InteractionMode.TEXT else InteractionMode.VOICE
                    )
                },
                onAddressClick = { viewModel.setAddressDialogVisible(true) },
                onSettingsClick = { viewModel.setSettingsDialogVisible(true) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero section: Avatar & Waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ArushiAvatarOrb(
                    state = uiState.assistantState,
                    rmsLevel = uiState.audioRmsLevel,
                    emotion = uiState.currentEmotion,
                    onClick = { onMicClick() }
                )
            }

            // Quick command chips
            QuickActionChips(
                onActionSelected = { command ->
                    viewModel.sendTextMessage(command)
                },
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Real-time transcript preview bubble when listening
            AnimatedVisibility(visible = uiState.partialTranscript.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ArushiSurfaceVariant,
                    border = BorderStroke(1.dp, ArushiNeonCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = ArushiNeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "\"${uiState.partialTranscript}\"",
                            fontSize = 12.sp,
                            color = ArushiTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Conversation history list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("conversation_list"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onReplayAudio = { audioBase64 ->
                            viewModel.replayAudio(audioBase64)
                        }
                    )
                }
            }

            // Bottom control bar (Voice Mode vs Text Mode)
            if (uiState.interactionMode == InteractionMode.VOICE) {
                VoiceBottomBar(
                    state = uiState.assistantState,
                    isSpeakerTesting = uiState.isSpeakerTestRunning,
                    onMicClick = { onMicClick() },
                    onSpeakerTestClick = { viewModel.runSpeakerTest() },
                    onSwitchToText = { viewModel.setInteractionMode(InteractionMode.TEXT) }
                )
            } else {
                TextBottomBar(
                    text = textInput,
                    onTextChange = { textInput = it },
                    onSend = {
                        viewModel.sendTextMessage(textInput)
                        textInput = ""
                    },
                    onSwitchToVoice = { viewModel.setInteractionMode(InteractionMode.VOICE) }
                )
            }
        }
    }

    // Address Dialog
    if (uiState.showAddressDialog) {
        DeliveryAddressDialog(
            currentAddress = uiState.savedAddress,
            onSave = { updated ->
                viewModel.saveAddress(updated)
            },
            onDismiss = { viewModel.setAddressDialogVisible(false) }
        )
    }

    // Settings & Diagnostics Dialog
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            customApiKey = uiState.customApiKey,
            selectedVoice = uiState.selectedVoice,
            isBgServiceRunning = uiState.isBackgroundServiceRunning,
            isSpeakerTestRunning = uiState.isSpeakerTestRunning,
            onApiKeyChange = { viewModel.updateApiKey(it) },
            onVoiceSelect = { viewModel.updateVoice(it) },
            onToggleBgService = { viewModel.toggleBackgroundService() },
            onRunSpeakerTest = { viewModel.runSpeakerTest() },
            onClearHistory = { viewModel.clearMessages() },
            onDismiss = { viewModel.setSettingsDialogVisible(false) }
        )
    }
}

@Composable
private fun TopAppBar(
    state: AssistantState,
    mode: InteractionMode,
    isBgServiceActive: Boolean,
    savedAddressSet: Boolean,
    onModeToggle: () -> Unit,
    onAddressClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val statusText = when (state) {
        AssistantState.LISTENING -> "Listening 🎧"
        AssistantState.SPEAKING -> "Speaking 💬"
        AssistantState.THINKING -> "Thinking 🧠"
        AssistantState.ERROR -> "Offline ⚠️"
        else -> "Ready ✨"
    }

    val statusColor = when (state) {
        AssistantState.LISTENING -> ArushiNeonCyan
        AssistantState.SPEAKING -> ArushiNeonPink
        AssistantState.THINKING -> ArushiGold
        AssistantState.ERROR -> Color.Red
        else -> ArushiEmerald
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // App brand & status badge
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Arushi",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    text = " AI",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = ArushiNeonPink
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }

        // Action controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Mode toggle pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ArushiCardBg,
                border = BorderStroke(1.dp, ArushiNeonViolet.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clickable { onModeToggle() }
                    .testTag("mode_toggle_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (mode == InteractionMode.VOICE) Icons.Default.Mic else Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = ArushiNeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (mode == InteractionMode.VOICE) "Voice" else "Text",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArushiTextPrimary
                    )
                }
            }

            // Saved address icon button
            IconButton(
                onClick = onAddressClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (savedAddressSet) ArushiNeonPink.copy(alpha = 0.15f) else ArushiCardBg)
                    .border(1.dp, if (savedAddressSet) ArushiNeonPink else ArushiCardBorder, CircleShape)
                    .testTag("top_address_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Address",
                    tint = if (savedAddressSet) ArushiNeonPink else ArushiTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ArushiCardBg)
                    .border(1.dp, ArushiCardBorder, CircleShape)
                    .testTag("top_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = ArushiTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceBottomBar(
    state: AssistantState,
    isSpeakerTesting: Boolean,
    onMicClick: () -> Unit,
    onSpeakerTestClick: () -> Unit,
    onSwitchToText: () -> Unit
) {
    val isListening = state == AssistantState.LISTENING
    val isSpeaking = state == AssistantState.SPEAKING

    val infiniteTransition = rememberInfiniteTransition(label = "mic_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val micBorderColor by animateColorAsState(
        targetValue = when {
            isListening -> ArushiNeonCyan
            isSpeaking -> ArushiNeonPink
            else -> ArushiNeonViolet
        },
        label = "border_color"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Speaker diagnostic test button (Requirement #15)
        Surface(
            shape = CircleShape,
            color = ArushiCardBg,
            border = BorderStroke(1.dp, ArushiNeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .clickable { onSpeakerTestClick() }
                .testTag("diagnostic_test_shortcut")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Test Speaker",
                    tint = ArushiNeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isSpeakerTesting) "Testing..." else "Test 440Hz",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ArushiNeonCyan
                )
            }
        }

        // Center primary Mic / Interruption button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .scale(if (isListening || isSpeaking) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            micBorderColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .clickable { onMicClick() }
                .testTag("mic_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (isListening) listOf(ArushiNeonCyan, ArushiNeonViolet)
                            else listOf(ArushiNeonViolet, ArushiNeonPink)
                        )
                    )
                    .border(2.dp, micBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = when {
                        isSpeaking -> Icons.Default.Stop
                        isListening -> Icons.Default.MicOff
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Microphone Button",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // Switch to text mode button
        Surface(
            shape = CircleShape,
            color = ArushiCardBg,
            border = BorderStroke(1.dp, ArushiCardBorder),
            modifier = Modifier
                .clickable { onSwitchToText() }
                .testTag("switch_to_text_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Text Mode",
                    tint = ArushiTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Text",
                    fontSize = 11.sp,
                    color = ArushiTextSecondary
                )
            }
        }
    }
}

@Composable
private fun TextBottomBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onSwitchToVoice: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSwitchToVoice,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ArushiCardBg)
                .border(1.dp, ArushiNeonViolet, CircleShape)
                .testTag("switch_to_voice_button")
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Switch to voice",
                tint = ArushiNeonViolet,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Ask Arushi anything (e.g. 'Ola book karo')...", fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ArushiNeonViolet,
                unfocusedBorderColor = ArushiCardBorder,
                focusedContainerColor = ArushiCardBg,
                unfocusedContainerColor = ArushiCardBg,
                cursorColor = ArushiNeonPink,
                focusedTextColor = ArushiTextPrimary,
                unfocusedTextColor = ArushiTextPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("text_input_field")
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank(),
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (text.isNotBlank()) ArushiNeonPink else ArushiSurfaceVariant)
                .testTag("send_text_button")
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send message",
                tint = if (text.isNotBlank()) Color.White else ArushiTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
