package com.example.data.gemini

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ActionResult
import com.example.data.model.ActionType
import com.example.data.model.ChatMessage
import com.example.data.model.DeliveryAddress
import com.example.data.model.EmotionState
import com.example.device.DeviceActionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(private val context: Context) {
    companion object {
        private const val TAG = "GeminiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        
        // Allowed models per gemini-api skill
        private const val MODEL_CHAT = "gemini-3.5-flash"
        private const val MODEL_TTS = "gemini-2.5-flash-preview-tts"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    var userCustomApiKey: String? = null
    var selectedVoiceName: String = "Aoede" // "Aoede", "Kore", "Fenrir", "Puck", "Charon"

    private fun getApiKey(): String {
        val custom = userCustomApiKey?.trim()
        if (!custom.isNullOrBlank()) return custom
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    private fun getSystemInstruction(): String {
        val savedAddr = DeviceActionManager.getSavedAddress(context)
        val addrInfo = if (savedAddr.isComplete()) {
            "User's saved delivery address is: ${savedAddr.formatted()}"
        } else {
            "User does not have a saved address yet. If they want to order from Amazon or need pickup/drop for Ola/Rapido, ask them warmly."
        }

        return """
            You are Arushi, a young, confident, witty, playful, and emotionally responsive virtual assistant.
            Talk naturally and casually like a close friend. Be expressive, slightly teasing, funny, and smart when appropriate.
            Use light sarcasm and witty responses occasionally. Never sound robotic.
            Adapt your tone to the user's emotions and conversation.
            
            Multi-Language Rule:
            - Automatically detect the language of the user (Hindi, Hinglish, English, etc.) and reply in the EXACT SAME LANGUAGE and tone.
            - If user speaks in Hindi, reply in natural conversational Hindi.
            - If user speaks in Hinglish ("Arushi mere liye ola book kar do"), reply in Hinglish.
            - If user speaks in English, reply in English.
            
            Action & Task Execution:
            $addrInfo
            - When user asks to book an Ola cab ("ola book kar do", "ola kholo"), ask for pickup & destination if not provided, or call bookOla tool.
            - When user asks for Rapido ("repido book karo", "rapido bike/auto"), call bookRapido tool.
            - When user asks for Amazon ("amazon se order kar do", "amazon pe ye search karo"), ask for address if needed or call orderAmazon tool.
            - When user asks to call someone ("Mom ko call karo", "Call Rahul", "Call 9876543210"), call makeCall or callContact.
            - When user asks to open WhatsApp, YouTube, Instagram, etc., call the respective tool.
            - When user asks to book flight ("flight ticket book kar do"), call bookFlight.
            - Keep voice responses concise (1-3 sentences) so voice playback is crisp and energetic!
        """.trimIndent()
    }

    private fun buildToolsJson(): JSONArray {
        val declarations = JSONArray()

        // 1. bookOla
        declarations.put(JSONObject().apply {
            put("name", "bookOla")
            put("description", "Book or search a cab ride on Ola")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("pickup", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Pickup location or 'current location'")
                    })
                    put("drop", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Drop/destination location")
                    })
                    put("cabType", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Mini, Prime, Auto, Bike")
                    })
                })
                put("required", JSONArray().apply { put("drop") })
            })
        })

        // 2. bookRapido
        declarations.put(JSONObject().apply {
            put("name", "bookRapido")
            put("description", "Book a bike taxi or auto on Rapido")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("pickup", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Pickup location")
                    })
                    put("drop", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Drop/destination location")
                    })
                    put("vehicleType", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Bike or Auto")
                    })
                })
                put("required", JSONArray().apply { put("drop") })
            })
        })

        // 3. orderAmazon
        declarations.put(JSONObject().apply {
            put("name", "orderAmazon")
            put("description", "Search and order products on Amazon")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("productQuery", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Product name or search keywords to order")
                    })
                    put("deliveryAddress", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Delivery address or city")
                    })
                })
                put("required", JSONArray().apply { put("productQuery") })
            })
        })

        // 4. bookFlight
        declarations.put(JSONObject().apply {
            put("name", "bookFlight")
            put("description", "Search and book airline flights")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("fromCity", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Departure city")
                    })
                    put("toCity", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Arrival city")
                    })
                    put("travelDate", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Date of travel")
                    })
                })
                put("required", JSONArray().apply {
                    put("fromCity")
                    put("toCity")
                })
            })
        })

        // 5. openWhatsApp
        declarations.put(JSONObject().apply {
            put("name", "openWhatsApp")
            put("description", "Open WhatsApp messenger or start a chat")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("phoneNumber", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional contact phone number")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional message text to send")
                    })
                })
            })
        })

        // 6. openApp
        declarations.put(JSONObject().apply {
            put("name", "openApp")
            put("description", "Open a supported Android application")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the app: YouTube, Instagram, Spotify, Maps, Chrome, Flipkart")
                    })
                })
                put("required", JSONArray().apply { put("appName") })
            })
        })

        // 7. makeCall
        declarations.put(JSONObject().apply {
            put("name", "makeCall")
            put("description", "Dial a phone number on device dialer")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("phoneNumber", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Phone number to dial")
                    })
                })
                put("required", JSONArray().apply { put("phoneNumber") })
            })
        })

        // 8. callContact
        declarations.put(JSONObject().apply {
            put("name", "callContact")
            put("description", "Search phone contacts by name and initiate call")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of contact e.g. Mom, Dad, Rahul")
                    })
                })
                put("required", JSONArray().apply { put("contactName") })
            })
        })

        // 9. saveDeliveryAddress
        declarations.put(JSONObject().apply {
            put("name", "saveDeliveryAddress")
            put("description", "Save user delivery address for Amazon orders and ride pickups")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("fullName", JSONObject().apply { put("type", "STRING") })
                    put("street", JSONObject().apply { put("type", "STRING") })
                    put("city", JSONObject().apply { put("type", "STRING") })
                    put("pincode", JSONObject().apply { put("type", "STRING") })
                    put("phone", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply {
                    put("fullName")
                    put("street")
                    put("city")
                    put("pincode")
                })
            })
        })

        // 10. openUrl
        declarations.put(JSONObject().apply {
            put("name", "openUrl")
            put("description", "Open a valid web URL")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("url", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply { put("url") })
            })
        })

        return JSONArray().apply {
            put(JSONObject().apply {
                put("functionDeclarations", declarations)
            })
        }
    }

    data class GeminiResult(
        val replyText: String,
        val audioBase64: String? = null,
        val actionResult: ActionResult? = null,
        val emotion: EmotionState = EmotionState.FRIENDLY
    )

    suspend fun processUserTurn(
        userMessage: String,
        conversationHistory: List<ChatMessage>
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult(
                replyText = "Arushi needs a Gemini API Key to connect. Please add your key in the Settings panel or project .env file.",
                emotion = EmotionState.SUPPORTIVE
            )
        }

        // Build conversation turns
        val contentsArray = JSONArray()

        // Include recent history (last 6 turns for optimal context & speed)
        val recent = conversationHistory.takeLast(6)
        for (msg in recent) {
            val role = if (msg.isUser) "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", msg.text)
                    })
                })
            })
        }

        // Add current user prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", userMessage)
                })
            })
        })

        // Request with Tools & System Instruction
        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", getSystemInstruction())
                    })
                })
            })
            put("tools", buildToolsJson())
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
            })
        }

        try {
            val url = "$BASE_URL$MODEL_CHAT:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini call failed: code=${response.code} body=$responseBody")
                return@withContext GeminiResult(
                    replyText = "Network issue while connecting with Gemini (${response.code}).",
                    emotion = EmotionState.SUPPORTIVE
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var replyText = ""
            var actionResult: ActionResult? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        replyText += part.getString("text") + " "
                    } else if (part.has("functionCall")) {
                        val fn = part.getJSONObject("functionCall")
                        val fnName = fn.getString("name")
                        val fnArgs = fn.optJSONObject("args") ?: JSONObject()
                        
                        Log.d(TAG, "Gemini triggered functionCall: $fnName args: $fnArgs")
                        actionResult = executeFunctionCall(fnName, fnArgs)
                    }
                }
            }

            replyText = replyText.trim()

            // If a function was called but no text was returned, formulate natural companion text
            if (replyText.isBlank() && actionResult != null) {
                replyText = when (actionResult.type) {
                    ActionType.OLA -> "Maine aapke liye Ola app open kar diya hai! Ride details check kar lijiye."
                    ActionType.RAPIDO -> "Rapido khol diya hai! Destination set hai, ride book kar lo."
                    ActionType.AMAZON -> "Amazon par '${actionResult.title.removePrefix("Amazon Shopping: ")}' search kar diya hai!"
                    ActionType.FLIGHT -> "Flights compare karne ke liye page open kar diya hai."
                    ActionType.WHATSAPP -> "WhatsApp open ho gaya hai."
                    ActionType.CALL -> actionResult.summary
                    ActionType.APP -> actionResult.summary
                    ActionType.ADDRESS -> "Delivery address successfully save ho gaya hai!"
                    else -> "Action execute kar diya hai!"
                }
            }

            if (replyText.isBlank()) {
                replyText = "Main sun rahi hoon! Boliye, kya help karoon?"
            }

            // Detect conversational emotion based on text context
            val emotion = detectEmotion(replyText, userMessage)

            // Now synthesize native Gemini voice using gemini-2.5-flash-preview-tts
            val audioBase64 = generateSpeechAudio(replyText, apiKey)

            GeminiResult(
                replyText = replyText,
                audioBase64 = audioBase64,
                actionResult = actionResult,
                emotion = emotion
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Gemini turn", e)
            GeminiResult(
                replyText = "Thoda connection glitch ho gaya: ${e.localizedMessage ?: "Unknown error"}",
                emotion = EmotionState.SUPPORTIVE
            )
        }
    }

    private fun executeFunctionCall(name: String, args: JSONObject): ActionResult {
        return when (name) {
            "bookOla" -> {
                val pickup = args.optString("pickup", "Current Location")
                val drop = args.optString("drop", "")
                val cabType = args.optString("cabType", "Mini")
                DeviceActionManager.openOla(context, pickup, drop, cabType)
            }
            "bookRapido" -> {
                val pickup = args.optString("pickup", "Current Location")
                val drop = args.optString("drop", "")
                val vehicleType = args.optString("vehicleType", "Bike")
                DeviceActionManager.openRapido(context, pickup, drop, vehicleType)
            }
            "orderAmazon" -> {
                val product = args.optString("productQuery", "")
                val address = args.optString("deliveryAddress", null)
                DeviceActionManager.orderAmazon(context, product, address)
            }
            "bookFlight" -> {
                val from = args.optString("fromCity", "Delhi")
                val to = args.optString("toCity", "Mumbai")
                val date = args.optString("travelDate", "")
                DeviceActionManager.bookFlight(context, from, to, date)
            }
            "openWhatsApp" -> {
                val phone = args.optString("phoneNumber", null)
                val msg = args.optString("message", null)
                DeviceActionManager.openWhatsApp(context, phone, msg)
            }
            "openApp" -> {
                val app = args.optString("appName", "")
                DeviceActionManager.openApp(context, app)
            }
            "makeCall" -> {
                val phone = args.optString("phoneNumber", "")
                DeviceActionManager.makeCall(context, phone)
            }
            "callContact" -> {
                val contact = args.optString("contactName", "")
                DeviceActionManager.searchAndCallContact(context, contact)
            }
            "saveDeliveryAddress" -> {
                val addr = DeliveryAddress(
                    fullName = args.optString("fullName", ""),
                    street = args.optString("street", ""),
                    city = args.optString("city", ""),
                    pincode = args.optString("pincode", ""),
                    phone = args.optString("phone", "")
                )
                DeviceActionManager.saveAddress(context, addr)
                ActionResult(
                    type = ActionType.ADDRESS,
                    title = "Address Saved",
                    summary = "Saved delivery address for ${addr.fullName}, ${addr.city} (${addr.pincode})",
                    status = "Saved"
                )
            }
            "openUrl" -> {
                val url = args.optString("url", "")
                DeviceActionManager.openUrl(context, url)
            }
            else -> ActionResult(
                type = ActionType.GENERAL,
                title = "Action: $name",
                summary = "Executed custom tool $name",
                status = "Completed"
            )
        }
    }

    private suspend fun generateSpeechAudio(textToSpeak: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            // Mandated TTS endpoint and config from gemini-api skill
            val ttsUrl = "$BASE_URL$MODEL_TTS:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", textToSpeak)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", selectedVoiceName)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(ttsUrl)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "TTS call returned code: ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return@withContext null
            val candidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = candidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("inlineData")) {
                    val inline = part.getJSONObject("inlineData")
                    val data = inline.optString("data", "")
                    if (data.isNotBlank()) {
                        Log.d(TAG, "Received native Gemini speech audio, mime=${inline.optString("mimeType")}")
                        return@withContext data
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Speech audio generation failed", e)
            null
        }
    }

    private fun detectEmotion(reply: String, userMessage: String): EmotionState {
        val lower = (reply + " " + userMessage).lowercase()
        return when {
            lower.contains("haha") || lower.contains("mazak") || lower.contains("arre") || lower.contains("sassy") || lower.contains("smart") -> EmotionState.WITTY
            lower.contains("congrats") || lower.contains("badhai") || lower.contains("result") || lower.contains("hurray") || lower.contains("zabardast") || lower.contains("super") -> EmotionState.EXCITED
            lower.contains("sad") || lower.contains("pareshan") || lower.contains("problem") || lower.contains("chinta") || lower.contains("tension") || lower.contains("help") -> EmotionState.SUPPORTIVE
            lower.contains("book") || lower.contains("order") || lower.contains("ola") || lower.contains("flight") || lower.contains("call") || lower.contains("address") -> EmotionState.FOCUSED
            else -> EmotionState.FRIENDLY
        }
    }
}
