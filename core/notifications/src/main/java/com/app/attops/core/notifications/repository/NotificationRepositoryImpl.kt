package com.app.attops.core.notifications.repository

import android.os.Build
import android.util.Log
import com.app.attops.core.common.result.Result
import com.app.attops.core.notifications.model.NotificationLog
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) : NotificationRepository {

    override fun getNotifications(): Flow<Result<List<NotificationLog>>> = flow {
        emit(Result.Loading)
        try {
            val userId = auth.currentUserOrNull()?.id ?: throw Exception("User not logged in")
            val notifications = postgrest.from("notifications_log")
                .select(columns = Columns.ALL) {
                    filter { eq("recipient_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<NotificationLog>()
            emit(Result.Success(notifications))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("notifications_log").update(buildJsonObject {
                put("status", "READ")
            }) {
                filter { eq("id", notificationId) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun uploadToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUserOrNull()?.id ?: return@withContext Result.Error(message = "User not logged in")
            
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
            
            val data = buildJsonObject {
                put("user_id", userId)
                put("token", token)
                put("device_name", deviceName)
                put("last_seen", java.time.Instant.now().toString())
            }

            postgrest.from("fcm_tokens").upsert(data) {
                onConflict = "user_id,token"
            }
            
            Log.d("NotificationRepo", "FCM Token uploaded successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Failed to upload FCM token", e)
            Result.Error(e)
        }
    }

    override suspend fun deleteToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("fcm_tokens").delete {
                filter { eq("token", token) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
