package xyz.cssxsh.dlsite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("style")
    val style: String,
    @SerialName("pattern")
    val pattern: String,
    @SerialName("login_id")
    val loginId: String,
    @SerialName("password")
    val password: String,
    @SerialName("hosts")
    val hosts: Map<String, List<String>>,
    @SerialName("dns")
    val dns: String,
    @SerialName("max_async_num")
    val maxAsyncNum: Int,
    @SerialName("block_size_MB")
    val blockSizeMB: Long
)