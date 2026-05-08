package com.nitai.wanderer

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectBridge(private val context: Context) {

    // 1. Connect to the Health Connect app on the user's phone
    private val client: HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null

    // 2. An Interface so our Kotlin code can talk back to our Java code
    interface StepsCallback {
        fun onSuccess(totalSteps: Long)
        fun onFailure(errorMessage: String)
    }

    // 3. Helper to give Java the exact security string it needs
    fun getStepReadPermission(): String {
        return HealthPermission.getReadPermission(StepsRecord::class)
    }

    // 4. The actual data downloading logic
    fun readTodaySteps(callback: StepsCallback) {
        if (client == null) {
            callback.onFailure("Health Connect is not installed on this phone.")
            return
        }

        // BAGRUT NOTE: Health Connect uses Kotlin Coroutines. This launches a background
        // thread so the phone doesn't freeze while talking to Google's database.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get exactly 12:00 AM today to Right Now
                val startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS)
                val rightNow = Instant.now()

                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, rightNow)
                )

                val response = client.readRecords(request)
                var totalSteps = 0L

                // Loop through all the walking sessions today and add up the steps
                for (record in response.records) {
                    totalSteps += record.count
                }

                // Switch back to the Main UI Thread to update the screen
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onSuccess(totalSteps)
                }

            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onFailure(e.message ?: "Unknown Health Connect Error")
                }
            }
        }
    }
}