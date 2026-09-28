package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DeliveryAddress
import com.example.ui.theme.ArushiCardBg
import com.example.ui.theme.ArushiCardBorder
import com.example.ui.theme.ArushiDeepBg
import com.example.ui.theme.ArushiEmerald
import com.example.ui.theme.ArushiNeonCyan
import com.example.ui.theme.ArushiNeonPink
import com.example.ui.theme.ArushiNeonViolet
import com.example.ui.theme.ArushiSurfaceVariant
import com.example.ui.theme.ArushiTextMuted
import com.example.ui.theme.ArushiTextPrimary
import com.example.ui.theme.ArushiTextSecondary

@Composable
fun DeliveryAddressDialog(
    currentAddress: DeliveryAddress,
    onSave: (DeliveryAddress) -> Unit,
    onDismiss: () -> Unit
) {
    var fullName by remember { mutableStateOf(currentAddress.fullName) }
    var street by remember { mutableStateOf(currentAddress.street) }
    var city by remember { mutableStateOf(currentAddress.city) }
    var pincode by remember { mutableStateOf(currentAddress.pincode) }
    var phone by remember { mutableStateOf(currentAddress.phone) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ArushiCardBg),
            border = BorderStroke(1.dp, ArushiNeonViolet.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("delivery_address_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ArushiNeonPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delivery Address",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ArushiTextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ArushiTextSecondary)
                    }
                }

                Text(
                    text = "Arushi uses this for your Amazon orders and Ola ride pickups automatically!",
                    fontSize = 12.sp,
                    color = ArushiTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("address_name_input")
                )

                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("House / Flat / Street / Area") },
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("address_street_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier.weight(1f).testTag("address_city_input")
                    )
                    OutlinedTextField(
                        value = pincode,
                        onValueChange = { pincode = it },
                        label = { Text("Pincode") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier.weight(1f).testTag("address_pincode_input")
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("address_phone_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSave(
                            DeliveryAddress(
                                fullName = fullName.trim(),
                                street = street.trim(),
                                city = city.trim(),
                                pincode = pincode.trim(),
                                phone = phone.trim()
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArushiNeonPink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("save_address_button")
                ) {
                    Text("Save Address", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    customApiKey: String,
    selectedVoice: String,
    isBgServiceRunning: Boolean,
    isSpeakerTestRunning: Boolean,
    onApiKeyChange: (String) -> Unit,
    onVoiceSelect: (String) -> Unit,
    onToggleBgService: () -> Unit,
    onRunSpeakerTest: () -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val voiceOptions = listOf(
        "Aoede" to "Warm & Expressive (Default)",
        "Kore" to "Youthful & Energetic",
        "Fenrir" to "Calm & Deep",
        "Puck" to "Playful & Vibrant"
    )

    var tempKey by remember { mutableStateOf(customApiKey) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ArushiCardBg),
            border = BorderStroke(1.dp, ArushiNeonViolet.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = ArushiNeonViolet,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Arushi Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ArushiTextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ArushiTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speaker Diagnostic Test Section (Requirement #15)
                Text(
                    text = "Audio Diagnostics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ArushiNeonCyan
                )
                Text(
                    text = "Verify phone speaker output using 440Hz sine wave tone.",
                    fontSize = 11.sp,
                    color = ArushiTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onRunSpeakerTest,
                    enabled = !isSpeakerTestRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = ArushiSurfaceVariant),
                    border = BorderStroke(1.dp, ArushiNeonCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("speaker_test_button")
                ) {
                    if (isSpeakerTestRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ArushiNeonCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Playing 440Hz Tone...", color = ArushiNeonCyan, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = ArushiNeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Speaker (440Hz Tone)", color = ArushiTextPrimary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Background Assistant Service Section (Requirement #46)
                Text(
                    text = "Background Assistant Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ArushiNeonPink
                )
                Text(
                    text = "Allows Arushi to run in the background with persistent notification while you use Ola, Rapido, or Amazon.",
                    fontSize = 11.sp,
                    color = ArushiTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ArushiSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = if (isBgServiceRunning) "Foreground Service Active" else "Service Stopped",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (isBgServiceRunning) ArushiEmerald else ArushiTextMuted
                        )
                        Text(
                            text = if (isBgServiceRunning) "Tap notification to talk anytime" else "Toggle ON to keep active in background",
                            fontSize = 10.sp,
                            color = ArushiTextSecondary
                        )
                    }
                    Switch(
                        checked = isBgServiceRunning,
                        onCheckedChange = { onToggleBgService() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ArushiNeonPink,
                            checkedTrackColor = ArushiNeonViolet.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("bg_service_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Selector Section
                Text(
                    text = "Voice Persona",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ArushiTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    voiceOptions.forEach { (voiceKey, voiceDesc) ->
                        val isSelected = selectedVoice.equals(voiceKey, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ArushiNeonViolet.copy(alpha = 0.25f) else ArushiSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) ArushiNeonViolet else ArushiCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVoiceSelect(voiceKey) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = voiceKey,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) ArushiNeonPink else ArushiTextPrimary
                                    )
                                    Text(
                                        text = voiceDesc,
                                        fontSize = 10.sp,
                                        color = ArushiTextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ArushiNeonPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom API Key Override
                Text(
                    text = "Gemini API Key (Optional Override)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ArushiTextPrimary
                )
                Text(
                    text = "Leave empty to use project's BuildConfig key",
                    fontSize = 11.sp,
                    color = ArushiTextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tempKey,
                    onValueChange = {
                        tempKey = it
                        onApiKeyChange(it)
                    },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Clear conversation history button
                OutlinedButton(
                    onClick = onClearHistory,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("clear_history_button")
                ) {
                    Text("Clear Chat History", color = Color.Red, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ArushiNeonViolet,
    unfocusedBorderColor = ArushiCardBorder,
    focusedLabelColor = ArushiNeonViolet,
    unfocusedLabelColor = ArushiTextSecondary,
    cursorColor = ArushiNeonPink,
    focusedTextColor = ArushiTextPrimary,
    unfocusedTextColor = ArushiTextPrimary
)
