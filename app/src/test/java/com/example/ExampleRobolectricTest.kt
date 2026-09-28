package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DeliveryAddress
import com.example.device.DeviceActionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Arushi AI", appName)
    }

    @Test
    fun `save and load delivery address`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val address = DeliveryAddress(
            fullName = "Yogesh Kumar",
            street = "Sector 14, Ring Road",
            city = "New Delhi",
            pincode = "110001",
            phone = "9876543210"
        )
        DeviceActionManager.saveAddress(context, address)
        val loaded = DeviceActionManager.getSavedAddress(context)
        assertEquals("Yogesh Kumar", loaded.fullName)
        assertEquals("New Delhi", loaded.city)
        assertEquals("110001", loaded.pincode)
    }

    @Test
    fun `ola booking action result generated properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = DeviceActionManager.openOla(context, "Saket", "IGI Airport")
        assertNotNull(result)
        assertEquals(com.example.data.model.ActionType.OLA, result.type)
    }
}
