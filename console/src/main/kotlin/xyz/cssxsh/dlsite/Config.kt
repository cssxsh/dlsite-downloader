package xyz.cssxsh.dlsite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("style")
    val style: String,
    @SerialName("download_list_name")
    val downloadListName: String,
    @SerialName("login_id")
    val loginId: String,
    @SerialName("password")
    val password: String,
    @SerialName("china_dns")
    val chinaDns: String,
    @SerialName("foreign_dns")
    val foreignDns: String,
    @SerialName("cname")
    val cname:  Map<String, String>,
    @SerialName("max_async_num")
    val maxAsyncNum: Int,
    @SerialName("block_size_MB")
    val blockSizeMB: Long
)