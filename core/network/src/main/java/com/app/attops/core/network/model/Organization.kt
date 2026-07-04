package com.app.attops.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("business_type") val businessType: String,
    @SerialName("org_code") val orgCode: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("address") val address: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
