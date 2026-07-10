package com.app.attops.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class OrganizationSite(
    @SerialName("id") val id: String? = null,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("name") val name: String,
    @SerialName("address") val address: String? = null,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("created_at") val createdAt: String? = null
)
