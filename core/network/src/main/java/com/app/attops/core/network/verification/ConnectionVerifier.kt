package com.app.attops.core.network.verification

import android.util.Log
import com.app.attops.core.network.BuildConfig
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionVerifier @Inject constructor(
    private val postgrest: Postgrest
) {
    /**
     * Verifies the connection to Supabase.
     */
    suspend fun verify(): Result<Unit> {
        if (!BuildConfig.DEBUG) {
            return Result.success(Unit)
        }

        return try {
            // Using a query that bypasses RLS issues for basic heartbeat check
            postgrest.from("organizations")
                .select(columns = Columns.ALL) {
                    limit(1)
                }
            Log.d("AttOps_Verification", "SUPABASE CONNECTION SUCCESS")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AttOps_Verification", "SUPABASE CONNECTION FAILED: ${e.message}")
            Result.failure(e)
        }
    }
}
