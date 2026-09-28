package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ActionResult
import com.example.data.model.ActionType
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.data.model.EmotionState
import com.example.device.DeviceActionManager
import com.example.ui.theme.ArushiCardBg
import com.example.ui.theme.ArushiCardBorder
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
fun ArushiAvatarOrb(
    state: AssistantState,
    rmsLevel: Float,
    emotion: EmotionState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Dynamic responsive scale based on state and microphone RMS
    val activeScale = when (state) {
        AssistantState.LISTENING -> (1.0f + rmsLevel * 0.25f).coerceIn(1.0f, 1.3f)
        AssistantState.SPEAKING -> breathingScale * 1.08f
        AssistantState.THINKING -> breathingScale
        else -> breathingScale
    }

    val glowColors = when (state) {
        AssistantState.LISTENING -> listOf(ArushiNeonCyan, ArushiNeonViolet)
        AssistantState.SPEAKING -> listOf(ArushiNeonPink, ArushiNeonViolet, ArushiNeonCyan)
        AssistantState.THINKING -> listOf(ArushiGold, ArushiNeonViolet)
        AssistantState.ERROR -> listOf(Color.Red, ArushiNeonPink)
        AssistantState.IDLE -> listOf(ArushiNeonViolet, ArushiNeonPink.copy(alpha = 0.6f))
        AssistantState.CONNECTING -> listOf(ArushiNeonCyan, ArushiNeonPink)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(150.dp)
                .scale(activeScale)
                .clickable(onClick = onClick)
                .testTag("arushi_avatar_orb")
        ) {
            // Glowing outer gradient ring
            Box(
                modifier = Modifier
                    .size(146.dp)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(glowColors))
                    .padding(4.dp)
            ) {
                // Inner dark container
                Box(
                    modifier = Modifier
                        .size(138.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF140D24)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_arushi_avatar_1790579650544),
                        contentDescription = "Arushi AI Assistant Avatar",
                        modifier = Modifier
                            .size(126.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Emotion pill badge attached at bottom of avatar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ArushiSurfaceVariant,
                border = BorderStroke(1.dp, glowColors.first().copy(alpha = 0.8f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = emotion.emoji,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = when (state) {
                            AssistantState.LISTENING -> "Listening..."
                            AssistantState.SPEAKING -> "Speaking..."
                            AssistantState.THINKING -> "Thinking..."
                            else -> emotion.title
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ArushiTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic 7-bar audio waveform
        AudioWaveformVisualizer(
            state = state,
            rmsLevel = rmsLevel
        )
    }
}

@Composable
fun AudioWaveformVisualizer(
    state: AssistantState,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 7
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(28.dp)
            .testTag("audio_waveform")
    ) {
        for (i in 0 until barCount) {
            val animProgress by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300 + i * 80, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            val heightFactor = when (state) {
                AssistantState.LISTENING -> (0.2f + rmsLevel * 0.8f * ((i % 3 + 1) / 3f)).coerceIn(0.2f, 1f)
                AssistantState.SPEAKING -> animProgress
                AssistantState.THINKING -> 0.35f
                else -> 0.15f
            }

            val barColor = when (state) {
                AssistantState.LISTENING -> ArushiNeonCyan
                AssistantState.SPEAKING -> ArushiNeonPink
                AssistantState.THINKING -> ArushiGold
                else -> ArushiTextMuted.copy(alpha = 0.4f)
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((24 * heightFactor).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onReplayAudio: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.isUser

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            if (!isUser) {
                Image(
                    painter = painterResource(id = R.drawable.img_arushi_avatar_1790579650544),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(1.dp, ArushiNeonViolet, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ),
                    color = if (isUser) ArushiNeonViolet.copy(alpha = 0.3f) else ArushiCardBg,
                    border = BorderStroke(
                        1.dp,
                        if (isUser) ArushiNeonViolet.copy(alpha = 0.7f) else ArushiCardBorder
                    ),
                    modifier = Modifier.testTag(if (isUser) "user_message_bubble" else "arushi_message_bubble")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (!isUser) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Arushi ${message.emotion.emoji}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ArushiNeonPink
                                )
                                if (!message.audioBase64.isNullOrBlank()) {
                                    IconButton(
                                        onClick = { onReplayAudio(message.audioBase64) },
                                        modifier = Modifier.size(24.dp).testTag("replay_audio_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Replay voice",
                                            tint = ArushiNeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ArushiTextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }

                // If message triggered an action (Ola, Rapido, Amazon, etc.), show action card
                message.actionResult?.let { action ->
                    Spacer(modifier = Modifier.height(6.dp))
                    ActionResultCard(actionResult = action, context = context)
                }
            }
        }
    }
}

@Composable
fun ActionResultCard(
    actionResult: ActionResult,
    context: Context,
    modifier: Modifier = Modifier
) {
    val (badgeIcon, badgeColor) = when (actionResult.type) {
        ActionType.OLA -> Icons.Default.DirectionsCar to ArushiGold
        ActionType.RAPIDO -> Icons.Default.TwoWheeler to ArushiNeonCyan
        ActionType.AMAZON -> Icons.Default.ShoppingBag to ArushiNeonPink
        ActionType.FLIGHT -> Icons.Default.Flight to ArushiEmerald
        ActionType.WHATSAPP -> Icons.Default.OpenInNew to ArushiEmerald
        ActionType.CALL -> Icons.Default.Call to ArushiNeonCyan
        ActionType.ADDRESS -> Icons.Default.LocationOn to ArushiNeonPink
        else -> Icons.Default.Info to ArushiNeonViolet
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ArushiSurfaceVariant),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("action_result_card")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = actionResult.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ArushiTextPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = actionResult.status,
                        fontSize = 10.sp,
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = actionResult.summary,
                fontSize = 12.sp,
                color = ArushiTextSecondary
            )

            // Multiple contact disambiguation buttons if multiple matches found
            if (!actionResult.contacts.isNullOrEmpty() && actionResult.contacts.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap contact to call:",
                    fontSize = 11.sp,
                    color = ArushiTextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    actionResult.contacts.forEach { contact ->
                        Button(
                            onClick = { DeviceActionManager.makeCall(context, contact.number) },
                            colors = ButtonDefaults.buttonColors(containerColor = ArushiCardBg),
                            border = BorderStroke(1.dp, ArushiNeonCyan.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = contact.name,
                                    fontSize = 12.sp,
                                    color = ArushiTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = contact.number,
                                    fontSize = 11.sp,
                                    color = ArushiNeonCyan
                                )
                            }
                        }
                    }
                }
            }

            // Direct Action Trigger Button (e.g. Open App / Web Link)
            if (!actionResult.deepLink.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(actionResult.deepLink)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Open Link / App",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionChips(
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickActions = listOf(
        "Ola book karo 🚕" to "Mere liye Ola book kar do",
        "Rapido bike 🛵" to "Rapido auto ya bike book karo",
        "Amazon order 📦" to "Amazon par wireless earphones order karo",
        "Flight booking ✈️" to "Delhi se Mumbai ke liye flight tickets check karo",
        "Mom ko call karo 📞" to "Mom ko call karo",
        "WhatsApp kholo 💬" to "WhatsApp open karo",
        "Address update 📍" to "Mera delivery address save kar lo"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier.testTag("quick_action_chips")
    ) {
        items(quickActions) { (label, command) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ArushiCardBg,
                border = BorderStroke(1.dp, ArushiCardBorder),
                modifier = Modifier
                    .clickable { onActionSelected(command) }
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = ArushiTextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
