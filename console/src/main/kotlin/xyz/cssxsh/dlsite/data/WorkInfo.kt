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
data class WorkInfo(
    @SerialName("age_category")
    val ageCategory: String,
    @SerialName("author_name")
    val authorName: String?,
    @SerialName("content_count")
    val contentCount: Long,
    @SerialName("content_length")
    val contentLength: Long,
    @SerialName("content_size")
    val contentSize: Long,
    @SerialName("dl_format")
    val dlFormat: Int,
    @SerialName("download_start_date")
    @Serializable(DateSerializer::class)
    val downloadStartDate: Date?,
    @SerialName("file_type")
    val fileType: String,
    @SerialName("inservice")
    val inservice: Int,
    @SerialName("is_playwork")
    val isPlaywork: Boolean,
    @SerialName("maker_id")
    val makerId: String,
    @SerialName("maker_name")
    val makerName: String,
    @SerialName("maker_name_kana")
    val makerNameKana: String,
    @SerialName("os")
    val os: List<String>,
    @SerialName("purchase_type")
    val purchaseType: Int,
    @SerialName("regist_date")
    val registDate: String,
    @SerialName("rental_activate_date")
    @Serializable(DateSerializer::class)
    val rentalActivateDate: Date?,
    @SerialName("rental_expired_date")
    @Serializable(DateSerializer::class)
    val rentalExpiredDate: Date?,
    @SerialName("rental_id")
    val rentalId: String?,
    @SerialName("rental_time")
    @Serializable(DateSerializer::class)
    val rentalTime: Date?,
    @SerialName("sales_date")
    val salesDate: String,
    @SerialName("site_id")
    val siteId: String,
    @SerialName("tags")
    val tags: List<TagInfo>?,
    @SerialName("touch_content_count")
    val touchContentCount: Long,
    @SerialName("touch_inservice")
    val touchInservice: Int,
    @SerialName("touch_site_id")
    val touchSiteId: String,
    @SerialName("upgrade_date")
    @Serializable(DateSerializer::class)
    val upgradeDate: Date?,
    @SerialName("work_files")
    val workFiles: WorkFiles,
    @SerialName("work_name")
    val workName: String,
    @SerialName("work_name_kana")
    val workNameKana: String?,
    @SerialName("work_type")
    val workType: String,
    @SerialName("workno")
    val workNO: String
) {
    @Serializable
    data class WorkFiles(
        @SerialName("main")
        val main: String,
        @SerialName("sam")
        val sam: String
    )

    @Serializable
    data class TagInfo(
        @SerialName("name")
        val name: String,
        @SerialName("class")
        val `class`: String,
        @SerialName("sub_class")
        val subClass: String?
    )

    companion object {
        object DateSerializer : KSerializer<Date> {
            private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000000Z'")

            override fun deserialize(decoder: Decoder): Date =
                dateFormat.parse(decoder.decodeString())

            override val descriptor: SerialDescriptor
                get() = PrimitiveSerialDescriptor("DateSerializerTo$dateFormat", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: Date) =
                encoder.encodeString(dateFormat.format(value))
        }
    }
}