package com.app.attops.core.network.verification

import android.util.Log
import com.app.attops.core.network.BuildConfig
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionVerifier @Inject constructor(
    private val postgrest: Postgrest
) {
    /**
     * Verifies the connection to Supabase.
     * Only executes logic in DEBUG builds.
     */
    suspend fun verify(): Result<Unit> {
        if (!BuildConfig.DEBUG) {
            return Result.success(Unit)
        }

        return try {
            // Lightweight request to verify connectivity
            postgrest.from("organizations")
                .select(columns = Columns.list("id")) {
                    limit(1)
                }
            Log.d("AttOps_Verification", "SUPABASE CONNECTION SUCCESS")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(
                "AttOps_Verification",
                """
                SUPABASE CONNECTION FAILED
                
                Exception: ${e::class.simpleName}
                Message: ${e.message}
                """.trimIndent(),
                e
            )
            Result.failure(e)
        }
    }
}
