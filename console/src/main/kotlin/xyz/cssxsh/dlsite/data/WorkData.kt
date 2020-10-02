package xyz.cssxsh.dlsite.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkData(
    @SerialName("works")
    val works: MutableList<WorkInfo>
)