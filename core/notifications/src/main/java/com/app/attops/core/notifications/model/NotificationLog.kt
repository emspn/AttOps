package com.app.attops.core.notifications.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationLog(
    @SerialName("id") val id: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("data") val data: Map<String, String>? = null,
    @SerialName("status") val status: String = "SENT",
    @SerialName("created_at") val createdAt: String
)
