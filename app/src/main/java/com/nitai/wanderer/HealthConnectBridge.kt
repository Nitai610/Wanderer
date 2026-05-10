package com.nitai.wanderer

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

// NOTE FOR BAGRUT: Why use a separate Kotlin class for this?
// "Google's official Health Connect library is written exclusively for Kotlin Coroutines.
// I created this bridge class so my Java code could easily ask for step and calorie data without breaking compatibility."
class HealthConnectBridge(private val context: Context) {

    private val client: HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null

    interface HealthCallback {
        fun onSuccess(total: Long)
        fun onFailure(errorMessage: String)
    }

    // NOTE: Tells the system we need both Step and Calorie permissions.
    fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        )
    }

    fun readTodaySteps(callback: HealthCallback) {
        if (client == null) {
            callback.onFailure("Health Connect is not installed.")
            return
        }
        // NOTE FOR BAGRUT: What is CoroutineScope(Dispatchers.IO)?
        // "It forces the data download to happen on a background thread. If I did this on the Main thread,
        // the app's UI would freeze completely while waiting for Google's database to respond."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS)
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now())
                )
                val response = client.readRecords(request)
                var totalSteps = 0L
                for (record in response.records) {
                    totalSteps += record.count
                }
                // NOTE: Switch back to the Main thread so we can safely update the TextViews
                CoroutineScope(Dispatchers.Main).launch { callback.onSuccess(totalSteps) }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch { callback.onFailure(e.message ?: "Error") }
            }
        }
    }

    fun readTodayCalories(callback: HealthCallback) {
        if (client == null) {
            callback.onFailure("Health Connect is not installed.")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS)
                val request = ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now())
                )
                val response = client.readRecords(request)
                var totalCalories = 0.0
                for (record in response.records) {
                    totalCalories += record.energy.inKilocalories
                }
                CoroutineScope(Dispatchers.Main).launch { callback.onSuccess(totalCalories.toLong()) }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch { callback.onFailure(e.message ?: "Error") }
            }
        }
    }
}