package xyz.cssxsh.dlsite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkData(
    @SerialName("works")
    val works: MutableList<WorkInfo>
)