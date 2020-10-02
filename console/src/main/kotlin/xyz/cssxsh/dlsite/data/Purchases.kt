package xyz.cssxsh.dlsite.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Purchases(
    @SerialName("last")
    val last: String,
    @SerialName("limit")
    val limit: Int,
    @SerialName("offset")
    val offset: Int,
    @SerialName("total")
    val total: Int,
    @SerialName("works")
    val works: List<WorkInfo>
)