package xyz.cssxsh.dlsite.data


import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import xyz.cssxsh.dlsite.DLsiteTool
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class MyListsData(
    @SerialName("mylist_works")
    val mylistWorks: List<String>,
    @SerialName("mylists")
    val mylists: List<MyLists>
) {
    @Serializable
    data class MyLists(
        @SerialName("id")
        val id: Int,
        @SerialName("insert_date")
        @Serializable(DateSerializer::class)
        val insertDate: Date,
        @SerialName("mylist_name")
        val myListName: String,
        @SerialName("mylist_work_id")
        val myListWorkId: List<String>
    )

    companion object {
        object DateSerializer : KSerializer<Date> {
            private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

            override fun deserialize(decoder: Decoder): Date =
                dateFormat.parse(decoder.decodeString())

            override val descriptor: SerialDescriptor
                get() = PrimitiveSerialDescriptor("DateSerializerTo$dateFormat", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: Date) =
                encoder.encodeString(dateFormat.format(value))
        }
    }
}