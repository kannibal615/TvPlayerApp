package com.smartvision.svplayer.data.diagnostics

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceDiagnosticsApiService {
    @POST("api/app/device-diagnostics.php")
    suspend fun upsert(@Body request: DeviceDiagnosticsRequest): DeviceDiagnosticsResponse
}

data class DeviceDiagnosticsRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("public_device_code") val publicDeviceCode: String,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("android_version") val androidVersion: String,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("diagnostic_type") val diagnosticType: String,
    @SerializedName("payload") val payload: Map<String, @JvmSuppressWildcards Any?>,
)

data class DeviceDiagnosticsResponse(
    @SerializedName("success") val success: Boolean = false,
)
