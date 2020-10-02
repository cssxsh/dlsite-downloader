package xyz.cssxsh.dlsite.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class MyList(
    @SerialName("id")
    val id: Int,
    @SerialName("insert_date")
    @Serializable(MyListsData.Companion.DateSerializer::class)
    val insertDate: Date,
    @SerialName("name")
    val name: String,
    @SerialName("works")
    val works: List<String>
)