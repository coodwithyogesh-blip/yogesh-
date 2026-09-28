package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.example.data.model.ActionResult
import com.example.data.model.ActionType
import com.example.data.model.ContactItem
import com.example.data.model.DeliveryAddress
import java.net.URLEncoder

object DeviceActionManager {
    private const val TAG = "DeviceActionManager"
    private const val PREFS_NAME = "arushi_prefs"
    private const val KEY_FULL_NAME = "addr_fullname"
    private const val KEY_STREET = "addr_street"
    private const val KEY_CITY = "addr_city"
    private const val KEY_PINCODE = "addr_pincode"
    private const val KEY_PHONE = "addr_phone"

    private val APP_PACKAGE_MAP = mapOf(
        "whatsapp" to "com.whatsapp",
        "ola" to "com.olacabs.customer",
        "rapido" to "com.rapido.passenger",
        "amazon" to "in.amazon.mShop.android.shopping",
        "youtube" to "com.google.android.youtube",
        "instagram" to "com.instagram.android",
        "spotify" to "com.spotify.music",
        "maps" to "com.google.android.apps.maps",
        "chrome" to "com.android.chrome",
        "makemytrip" to "com.makemytrip",
        "flipkart" to "com.flipkart.android"
    )

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openOla(context: Context, pickup: String?, drop: String?, cabType: String? = null): ActionResult {
        val dropText = drop ?: "Destination"
        val pickupText = pickup ?: "Current Location"
        val isInstalled = isAppInstalled(context, "com.olacabs.customer")
        
        val webUrl = "https://book.olacabs.com/?pickup_name=${URLEncoder.encode(pickupText, "UTF-8")}&drop_name=${URLEncoder.encode(dropText, "UTF-8")}"
        
        return try {
            if (isInstalled) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.olacabs.customer")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    ActionResult(
                        type = ActionType.OLA,
                        title = "Ola Cab App Opened",
                        summary = "Ready to book ride from '$pickupText' to '$dropText'.",
                        packageName = "com.olacabs.customer",
                        status = "App Launched"
                    )
                } else {
                    openBrowser(context, webUrl)
                    ActionResult(
                        type = ActionType.OLA,
                        title = "Ola Web Booking",
                        summary = "Opened Ola Booking web portal for '$pickupText' to '$dropText'.",
                        deepLink = webUrl,
                        status = "Web Portal Opened"
                    )
                }
            } else {
                openBrowser(context, webUrl)
                ActionResult(
                    type = ActionType.OLA,
                    title = "Ola Ride Booking",
                    summary = "Ola app not installed. Opened Ola web portal for '$dropText'.",
                    deepLink = webUrl,
                    status = "Web Fallback"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Ola", e)
            ActionResult(
                type = ActionType.OLA,
                title = "Ola Booking Error",
                summary = "Could not open Ola automatically: ${e.message}",
                status = "Failed"
            )
        }
    }

    fun openRapido(context: Context, pickup: String?, drop: String?, vehicleType: String? = null): ActionResult {
        val dropText = drop ?: "Destination"
        val pickupText = pickup ?: "Current Location"
        val isInstalled = isAppInstalled(context, "com.rapido.passenger")
        val webUrl = "https://www.rapido.bike/"

        return try {
            if (isInstalled) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.rapido.passenger")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    ActionResult(
                        type = ActionType.RAPIDO,
                        title = "Rapido Bike/Auto Opened",
                        summary = "Launched Rapido for ride to '$dropText'.",
                        packageName = "com.rapido.passenger",
                        status = "App Launched"
                    )
                } else {
                    openBrowser(context, webUrl)
                    ActionResult(
                        type = ActionType.RAPIDO,
                        title = "Rapido Web",
                        summary = "Opened Rapido web portal.",
                        deepLink = webUrl,
                        status = "Web Fallback"
                    )
                }
            } else {
                openBrowser(context, webUrl)
                ActionResult(
                    type = ActionType.RAPIDO,
                    title = "Rapido Bike Taxi",
                    summary = "Rapido app not installed. Opened Rapido official portal.",
                    deepLink = webUrl,
                    status = "Web Fallback"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Rapido", e)
            ActionResult(
                type = ActionType.RAPIDO,
                title = "Rapido Error",
                summary = "Could not launch Rapido: ${e.message}",
                status = "Failed"
            )
        }
    }

    fun orderAmazon(context: Context, productQuery: String, address: String? = null): ActionResult {
        val queryEncoded = URLEncoder.encode(productQuery, "UTF-8")
        val amazonUrl = "https://www.amazon.in/s?k=$queryEncoded"
        val isInstalled = isAppInstalled(context, "in.amazon.mShop.android.shopping")

        val savedAddress = getSavedAddress(context)
        val addressSummary = when {
            !address.isNullOrBlank() -> "Delivery address: $address"
            savedAddress.isComplete() -> "Using saved address: ${savedAddress.formatted()}"
            else -> "Address not saved yet (Arushi will ask if needed)"
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(amazonUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (isInstalled) {
                    setPackage("in.amazon.mShop.android.shopping")
                }
            }
            context.startActivity(intent)
            ActionResult(
                type = ActionType.AMAZON,
                title = "Amazon Shopping: $productQuery",
                summary = "Searching '$productQuery' on Amazon. $addressSummary",
                deepLink = amazonUrl,
                status = if (isInstalled) "App Search" else "Web Search"
            )
        } catch (e: Exception) {
            openBrowser(context, amazonUrl)
            ActionResult(
                type = ActionType.AMAZON,
                title = "Amazon Search",
                summary = "Opened Amazon for '$productQuery'. $addressSummary",
                deepLink = amazonUrl,
                status = "Web Fallback"
            )
        }
    }

    fun bookFlight(context: Context, fromCity: String, toCity: String, travelDate: String?): ActionResult {
        val query = "flights from $fromCity to $toCity ${travelDate ?: ""}".trim()
        val flightUrl = "https://www.google.com/travel/flights?q=${URLEncoder.encode(query, "UTF-8")}"
        
        return try {
            openBrowser(context, flightUrl)
            ActionResult(
                type = ActionType.FLIGHT,
                title = "Flight Search: $fromCity ✈️ $toCity",
                summary = "Comparing best flights from $fromCity to $toCity ${if (!travelDate.isNullOrBlank()) "on $travelDate" else ""}.",
                deepLink = flightUrl,
                status = "Search Opened"
            )
        } catch (e: Exception) {
            ActionResult(
                type = ActionType.FLIGHT,
                title = "Flight Search Error",
                summary = "Could not open flight search: ${e.message}",
                status = "Failed"
            )
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String? = null, message: String? = null): ActionResult {
        val isInstalled = isAppInstalled(context, "com.whatsapp")
        return try {
            if (isInstalled) {
                val intent = if (!phoneNumber.isNullOrBlank()) {
                    val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                    val url = "https://api.whatsapp.com/send?phone=$cleanNumber${if (!message.isNullOrBlank()) "&text=${URLEncoder.encode(message, "UTF-8")}" else ""}"
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                ActionResult(
                    type = ActionType.WHATSAPP,
                    title = "WhatsApp Opened",
                    summary = if (!phoneNumber.isNullOrBlank()) "Opening chat with $phoneNumber" else "WhatsApp opened.",
                    status = "Success"
                )
            } else {
                ActionResult(
                    type = ActionType.WHATSAPP,
                    title = "WhatsApp Not Installed",
                    summary = "WhatsApp is not installed on this Android device.",
                    status = "Not Installed"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WhatsApp", e)
            ActionResult(
                type = ActionType.WHATSAPP,
                title = "WhatsApp Error",
                summary = "Could not open WhatsApp: ${e.message}",
                status = "Failed"
            )
        }
    }

    fun openApp(context: Context, appName: String): ActionResult {
        val normalized = appName.trim().lowercase()
        val packageName = APP_PACKAGE_MAP[normalized] ?: APP_PACKAGE_MAP.entries.firstOrNull {
            normalized.contains(it.key) || it.key.contains(normalized)
        }?.value

        if (packageName == null) {
            return ActionResult(
                type = ActionType.APP,
                title = "App Not Supported",
                summary = "Arushi currently supports: WhatsApp, Ola, Rapido, Amazon, YouTube, Instagram, Spotify, Maps, Chrome, and Flipkart.",
                status = "Unsupported"
            )
        }

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ActionResult(
                    type = ActionType.APP,
                    title = "Opened $appName",
                    summary = "Successfully launched $appName on your device.",
                    packageName = packageName,
                    status = "Success"
                )
            } else {
                ActionResult(
                    type = ActionType.APP,
                    title = "$appName Not Found",
                    summary = "$appName is not installed on this device.",
                    packageName = packageName,
                    status = "Not Installed"
                )
            }
        } catch (e: Exception) {
            ActionResult(
                type = ActionType.APP,
                title = "Could Not Open $appName",
                summary = "Error launching app: ${e.message}",
                status = "Failed"
            )
        }
    }

    fun openUrl(context: Context, url: String): ActionResult {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        return try {
            openBrowser(context, cleanUrl)
            ActionResult(
                type = ActionType.URL,
                title = "Website Opened",
                summary = "Opened $cleanUrl",
                deepLink = cleanUrl,
                status = "Success"
            )
        } catch (e: Exception) {
            ActionResult(
                type = ActionType.URL,
                title = "URL Error",
                summary = "Could not open website: ${e.message}",
                status = "Failed"
            )
        }
    }

    fun makeCall(context: Context, phoneNumber: String): ActionResult {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            return ActionResult(
                type = ActionType.CALL,
                title = "Invalid Number",
                summary = "The phone number '$phoneNumber' is invalid.",
                status = "Invalid Number"
            )
        }
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                type = ActionType.CALL,
                title = "Dialing $cleanNumber",
                summary = "Opened phone dialer with number $cleanNumber.",
                status = "Dialer Ready"
            )
        } catch (e: Exception) {
            ActionResult(
                type = ActionType.CALL,
                title = "Call Error",
                summary = "Failed to launch phone dialer: ${e.message}",
                status = "Failed"
            )
        }
    }

    fun searchAndCallContact(context: Context, contactName: String): ActionResult {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return ActionResult(
                type = ActionType.CALL,
                title = "Contacts Permission Required",
                summary = "Please grant Contacts permission so Arushi can find '$contactName'.",
                status = "Permission Denied"
            )
        }

        val foundContacts = mutableListOf<ContactItem>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")

        try {
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: ""
                    val number = it.getString(numberIndex) ?: ""
                    if (name.isNotBlank() && number.isNotBlank() && foundContacts.none { c -> c.number == number }) {
                        foundContacts.add(ContactItem(name, number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Contacts query failed", e)
        }

        return when {
            foundContacts.size == 1 -> {
                val single = foundContacts.first()
                makeCall(context, single.number).copy(
                    title = "Calling ${single.name}",
                    summary = "Found contact '${single.name}' (${single.number}). Ready on dialer.",
                    contacts = foundContacts
                )
            }
            foundContacts.size > 1 -> {
                ActionResult(
                    type = ActionType.CALL,
                    title = "Multiple Contacts Found",
                    summary = "Found ${foundContacts.size} contacts for '$contactName': ${foundContacts.take(3).joinToString { it.name }}. Which one should I call?",
                    contacts = foundContacts,
                    status = "Ambiguous Match"
                )
            }
            else -> {
                ActionResult(
                    type = ActionType.CALL,
                    title = "Contact Not Found",
                    summary = "No contact found matching '$contactName' on this device.",
                    status = "Not Found"
                )
            }
        }
    }

    fun saveAddress(context: Context, address: DeliveryAddress) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FULL_NAME, address.fullName)
            .putString(KEY_STREET, address.street)
            .putString(KEY_CITY, address.city)
            .putString(KEY_PINCODE, address.pincode)
            .putString(KEY_PHONE, address.phone)
            .apply()
    }

    fun getSavedAddress(context: Context): DeliveryAddress {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return DeliveryAddress(
            fullName = prefs.getString(KEY_FULL_NAME, "") ?: "",
            street = prefs.getString(KEY_STREET, "") ?: "",
            city = prefs.getString(KEY_CITY, "") ?: "",
            pincode = prefs.getString(KEY_PINCODE, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: ""
        )
    }

    private fun openBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
