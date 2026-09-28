package com.example.data.model

enum class AssistantState {
    IDLE,
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

enum class InteractionMode {
    VOICE,
    TEXT
}

enum class EmotionState(val title: String, val emoji: String) {
    FRIENDLY("Friendly & Warm", "✨"),
    WITTY("Witty & Sassy", "😏"),
    EXCITED("Energetic & Hyped", "🎉"),
    SUPPORTIVE("Supportive & Calm", "💖"),
    FOCUSED("Sharp & Focused", "🎯")
}

enum class ActionType {
    OLA,
    RAPIDO,
    AMAZON,
    FLIGHT,
    WHATSAPP,
    CALL,
    APP,
    URL,
    ADDRESS,
    GENERAL
}

data class ContactItem(
    val name: String,
    val number: String
)

data class ActionResult(
    val type: ActionType,
    val title: String,
    val summary: String,
    val deepLink: String? = null,
    val packageName: String? = null,
    val status: String = "Success",
    val contacts: List<ContactItem>? = null
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val audioBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val actionResult: ActionResult? = null,
    val emotion: EmotionState = EmotionState.FRIENDLY
)

data class DeliveryAddress(
    val fullName: String = "",
    val street: String = "",
    val city: String = "",
    val pincode: String = "",
    val phone: String = ""
) {
    fun isComplete(): Boolean = fullName.isNotBlank() && street.isNotBlank() && city.isNotBlank() && pincode.isNotBlank()
    
    fun formatted(): String = listOf(fullName, street, city, pincode, phone).filter { it.isNotBlank() }.joinToString(", ")
}
