package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ArushiAudioPlayer
import com.example.audio.SpeechRecognizerHelper
import com.example.data.gemini.GeminiService
import com.example.data.model.ActionResult
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.data.model.DeliveryAddress
import com.example.data.model.EmotionState
import com.example.data.model.InteractionMode
import com.example.device.DeviceActionManager
import com.example.service.ArushiBackgroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArushiUiState(
    val assistantState: AssistantState = AssistantState.IDLE,
    val interactionMode: InteractionMode = InteractionMode.VOICE,
    val currentEmotion: EmotionState = EmotionState.FRIENDLY,
    val messages: List<ChatMessage> = emptyList(),
    val partialTranscript: String = "",
    val audioRmsLevel: Float = 0f,
    val isBackgroundServiceRunning: Boolean = false,
    val savedAddress: DeliveryAddress = DeliveryAddress(),
    val errorMessage: String? = null,
    val isSpeakerTestRunning: Boolean = false,
    val customApiKey: String = "",
    val selectedVoice: String = "Aoede",
    val showAddressDialog: Boolean = false,
    val showSettingsDialog: Boolean = false
)

class ArushiViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val geminiService = GeminiService(context)
    private val audioPlayer = ArushiAudioPlayer(context)

    private val _uiState = MutableStateFlow(ArushiUiState())
    val uiState: StateFlow<ArushiUiState> = _uiState.asStateFlow()

    private var speechRecognizerHelper: SpeechRecognizerHelper? = null

    init {
        // Load saved address
        val addr = DeviceActionManager.getSavedAddress(context)
        _uiState.update { it.copy(savedAddress = addr) }

        // Observe background service
        viewModelScope.launch {
            ArushiBackgroundService.isServiceRunning.collect { isRunning ->
                _uiState.update { it.copy(isBackgroundServiceRunning = isRunning) }
            }
        }

        // Add warm introductory greeting from Arushi
        val introMessage = ChatMessage(
            isUser = false,
            text = "Hey! Main hoon Arushi, aapki personal voice AI assistant. Ola ya Rapido book karni ho, Amazon se order karna ho, ya bas baat karni ho — just ask me!",
            emotion = EmotionState.FRIENDLY
        )
        _uiState.update { it.copy(messages = listOf(introMessage)) }

        // Setup speech recognizer
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizerHelper = SpeechRecognizerHelper(
            context = context,
            scope = viewModelScope,
            onSpeechResult = { recognizedText ->
                _uiState.update { it.copy(partialTranscript = "", assistantState = AssistantState.THINKING) }
                handleUserInput(recognizedText)
            },
            onSpeechPartial = { partial ->
                _uiState.update { it.copy(partialTranscript = partial) }
            },
            onRmsChangedCallback = { rms ->
                _uiState.update { it.copy(audioRmsLevel = rms) }
            },
            onErrorCallback = { errorMsg ->
                _uiState.update {
                    it.copy(
                        assistantState = AssistantState.IDLE,
                        errorMessage = errorMsg,
                        audioRmsLevel = 0f
                    )
                }
            }
        )
    }

    fun toggleVoiceListening() {
        val currentState = _uiState.value.assistantState
        if (currentState == AssistantState.SPEAKING) {
            // Natural interruption support!
            interruptAssistant()
        } else if (currentState == AssistantState.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        audioPlayer.stopPlayback()
        _uiState.update {
            it.copy(
                assistantState = AssistantState.LISTENING,
                errorMessage = null,
                partialTranscript = ""
            )
        }
        speechRecognizerHelper?.startListening()
    }

    fun stopListening() {
        speechRecognizerHelper?.stopListening()
        _uiState.update { it.copy(assistantState = AssistantState.IDLE, audioRmsLevel = 0f) }
    }

    fun interruptAssistant() {
        audioPlayer.stopPlayback()
        speechRecognizerHelper?.stopListening()
        _uiState.update {
            it.copy(
                assistantState = AssistantState.LISTENING,
                audioRmsLevel = 0f
            )
        }
        speechRecognizerHelper?.startListening()
    }

    fun setInteractionMode(mode: InteractionMode) {
        _uiState.update { it.copy(interactionMode = mode) }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        stopListening()
        audioPlayer.stopPlayback()
        _uiState.update { it.copy(assistantState = AssistantState.THINKING) }
        handleUserInput(text)
    }

    private fun handleUserInput(prompt: String) {
        val userMsg = ChatMessage(isUser = true, text = prompt)
        val updatedList = _uiState.value.messages + userMsg
        _uiState.update { it.copy(messages = updatedList) }

        viewModelScope.launch {
            val result = geminiService.processUserTurn(prompt, updatedList)
            
            val modelMsg = ChatMessage(
                isUser = false,
                text = result.replyText,
                audioBase64 = result.audioBase64,
                actionResult = result.actionResult,
                emotion = result.emotion
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + modelMsg,
                    currentEmotion = result.emotion,
                    assistantState = if (!result.audioBase64.isNullOrBlank()) AssistantState.SPEAKING else AssistantState.IDLE
                )
            }

            // Play native Gemini audio through speaker
            if (!result.audioBase64.isNullOrBlank()) {
                audioPlayer.playAudioBase64(result.audioBase64) {
                    _uiState.update { it.copy(assistantState = AssistantState.IDLE) }
                }
            }
        }
    }

    fun replayAudio(audioBase64: String?) {
        if (audioBase64.isNullOrBlank()) return
        audioPlayer.stopPlayback()
        _uiState.update { it.copy(assistantState = AssistantState.SPEAKING) }
        audioPlayer.playAudioBase64(audioBase64) {
            _uiState.update { it.copy(assistantState = AssistantState.IDLE) }
        }
    }

    fun runSpeakerTest() {
        _uiState.update { it.copy(isSpeakerTestRunning = true, assistantState = AssistantState.SPEAKING) }
        audioPlayer.play440HzDiagnosticTone {
            _uiState.update { it.copy(isSpeakerTestRunning = false, assistantState = AssistantState.IDLE) }
        }
    }

    fun toggleBackgroundService() {
        val isCurrentlyRunning = _uiState.value.isBackgroundServiceRunning
        if (isCurrentlyRunning) {
            ArushiBackgroundService.stopService(context)
        } else {
            ArushiBackgroundService.startService(context)
        }
    }

    fun saveAddress(address: DeliveryAddress) {
        DeviceActionManager.saveAddress(context, address)
        _uiState.update { it.copy(savedAddress = address, showAddressDialog = false) }
        val msg = ChatMessage(
            isUser = false,
            text = "Aapka delivery address save ho gaya hai: ${address.formatted()}. Ab Amazon ya Ola rides me ye automatically use ho jayega!",
            emotion = EmotionState.FOCUSED,
            actionResult = ActionResult(
                type = com.example.data.model.ActionType.ADDRESS,
                title = "Address Updated",
                summary = address.formatted()
            )
        )
        _uiState.update { it.copy(messages = it.messages + msg) }
    }

    fun setAddressDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddressDialog = visible) }
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = visible) }
    }

    fun updateApiKey(apiKey: String) {
        geminiService.userCustomApiKey = apiKey
        _uiState.update { it.copy(customApiKey = apiKey) }
    }

    fun updateVoice(voiceName: String) {
        geminiService.selectedVoiceName = voiceName
        _uiState.update { it.copy(selectedVoice = voiceName) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(messages = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerHelper?.stopListening()
        audioPlayer.stopPlayback()
    }
}
