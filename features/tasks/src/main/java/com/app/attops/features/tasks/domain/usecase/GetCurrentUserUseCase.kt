package com.app.attops.features.tasks.domain.usecase

import com.app.attops.core.network.model.User
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    operator fun invoke(): Flow<User?> = flow {
        val userId = auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                val user = postgrest.from("users")
                    .select(columns = Columns.ALL) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<User>()
                emit(user)
            } catch (e: Exception) {
                emit(null)
            }
        } else {
            emit(null)
        }
    }
}
