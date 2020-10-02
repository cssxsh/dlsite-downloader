package xyz.cssxsh.dlsite.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Authorize(
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("is_super_user")
    val isSuperUser: Boolean,
    @SerialName("login_id")
    val loginId: String,
    @SerialName("production_id")
    val productionId: String?,
    @SerialName("sid")
    val sid: String
)