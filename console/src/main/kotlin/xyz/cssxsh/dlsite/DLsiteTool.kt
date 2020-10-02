package xyz.cssxsh.dlsite

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.internal.toHexString
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlin.time.measureTime

object DLsiteTool : CoroutineScope {
    private const val CONFIG_FILE = "config.json"
    private const val DATA_FILE = "data.json"
    private const val PURCHASES_URL = "https://play.dlsite.com/api/purchases"
    private const val DOWNLOAD_URL = "https://play.dlsite.com/api/download"

    // TODO {"customer_id":"SFT000002291632","production_id":null,"login_id":"cssxsh@gmail.com","is_super_user":false,"sid":"bj6rdd3keg8jf2qboqugp0748g"}
    private const val AUTHORIZE_URL = "https://play.dlsite.com/api/authorize"
    private const val PLAY_LOGIN_URL = "https://play.dlsite.com/login"
    private const val LOGIN_URL = "https://login.dlsite.com/login"

    private val logger = requireNotNull(LoggerFactory.getLogger(DLsiteTool::class.java))

    override val coroutineContext: CoroutineContext by lazy {
        Dispatchers.IO + CoroutineName("DListeTool")
    }

    var config: Config = File(CONFIG_FILE).run {
        if (canRead()) {
            Json.decodeFromString(Config.serializer(), readText())
        } else {
            Config(
                style = "LIGHT",
                pattern = "yyyy-MM-dd'T'HH:mm:ss'.000000Z'",
                loginId = "",
                password = "",
                hosts = emptyMap(),
                dns = "https://1.0.0.1/dns-query",
                maxAsyncNum = 16,
                blockSizeMB = 8
            ).also {
                writeText(Json.encodeToString(Config.serializer(), it))
            }
        }
    }

    private val dns: Dns = object : Dns {
        val doh by lazy {
            DnsOverHttps.Builder().apply {
                client(OkHttpClient())
                url(config.dns.toHttpUrl())
                includeIPv6(false)
                post(true)
            }.build()
        }

        override fun lookup(hostname: String): List<InetAddress> = config.hosts[hostname]?.map { ip ->
            InetAddress.getByName(ip)
        }.takeUnless { it.isNullOrEmpty() } ?: doh.lookup(hostname).also { list ->
            config = config.copy(
                hosts = config.hosts + mapOf(hostname to list.mapNotNull { it.hostAddress })
            )
            File(CONFIG_FILE).writeText(
                Json.encodeToString(
                    Config.serializer(),
                    config
                )
            )
            logger.info("$hostname hosts 已刷新")
        }.also {
            logger.info("域名 $hostname 查询")
        }
    }

    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(HttpTimeout) {
            socketTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 30_000
        }
        install(HttpCookies) {
            // Will keep an in-memory map with all the cookies from previous requests.
            storage = AcceptAllCookiesStorage()
        }
        ContentEncoding {
            gzip()
            deflate()
            identity()
        }
        engine {
            config {
                dns(dns)
            }
        }
    }

    suspend fun login(): String? {
        val token = httpClient.get<HttpResponse>(LOGIN_URL).setCookie().let { list ->
            list.forEach { logger.info(it.toString()) }
            requireNotNull(list["XSRF-TOKEN"]?.value) { "XSRF-TOKEN 为空" }
        }
        return httpClient.post<HttpResponse>(LOGIN_URL) {
            body = FormDataContent(Parameters.build {
                append("login_id", config.loginId.also { require(!isEmpty()) { "登录名为空" } })
                append("password", config.password.also { require(!isEmpty()) { "密码为空" } })
                append("_token", token)
            })
        }.setCookie()["PHPSESSID"]?.value
    }

    suspend fun purchases(block: (List<WorkInfo>) -> Unit = {}): WorkData = withContext(coroutineContext) {
        suspend fun getPurchases(page: Int) = httpClient.get<Purchases>(PURCHASES_URL) {
            parameter("page", page)
            // header(HttpHeaders.Cookie, "__DLsite_SID=${config.sid}")
            logger.info("Load WorkData form ${url.buildString()}")
        }.also { logger.info("Load WorkData count ${it.works.size}") }

        getPurchases(1).run {
            WorkData(works = works.toMutableList()).also { data ->
                (if (offset > 0) (1..offset) else IntRange.EMPTY).forEach { index ->
                    data.works.addAll(getPurchases(index + 1).works.apply(block))
                }
            }
        }.also {
            File(DATA_FILE).writeText(Json.encodeToString(WorkData.serializer(), it))
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun downloadFile(url: String, dir: File): Job = launch {
        val response = httpClient.get<HttpResponse>(url) {
            // header(HttpHeaders.Cookie, "AuthCookie=${config.cookie}")
            header(HttpHeaders.Range, "bytes=0-0")
        }

        val length = requireNotNull(response.headers[HttpHeaders.ContentRange]?.let { value ->
            // Content-Range: bytes 0-0/58231185
            Regex("""\d+$""").find(value)?.value
        }).toLong()

        val filename = requireNotNull(response.headers[HttpHeaders.ContentDisposition]?.let { value ->
            Regex("""[A-Z]{2}[0-9]{6}[.\w]+""").find(value)?.value
        })

        RandomAccessFile(File(dir, filename), "rw").apply {
            setLength(length)
        }.close()

        val blockSize = config.blockSizeMB * 1024 * 1024

        val offsets = (0 until (length - 1) / blockSize + 1).map { index ->
            (index * blockSize) to min((index + 1) * blockSize, length) - 1
        }

        var num = 0

        val channel = Channel<Int>(config.maxAsyncNum)

        val jobs = Array<Job?>(config.maxAsyncNum) { null }

        (0 until config.maxAsyncNum).forEach {
            channel.send(it)
        }

        measureTime {
            logger.info("文件 $filename (${length / (1024 * 1024)}M)开始下载")

            offsets.forEachIndexed { index, pair ->
                val no = channel.receive()
                jobs[no] = launch {
                    val blockName = "${filename}[bytes=${pair.first.toHexString()}-${pair.second.toHexString()}]"
                    kotlin.runCatching {
                        httpClient.get<ByteArray>(url) {
                            // header(HttpHeaders.Cookie, "AuthCookie=${config.cookie}")
                            header(HttpHeaders.Range, "bytes=${pair.first}-${pair.second}")
                            timeout {
                                requestTimeoutMillis = 3600_000
                            }
                        }.let {
                            RandomAccessFile(File(dir, filename), "rw").apply {
                                seek(pair.first)
                                write(it)
                            }.close()
                        }
                    }.onSuccess {
                        num++
                        logger.info("文件块 $index 下载完成， 完成度 ${num}/${offsets.size}")
                    }.onFailure {
                        logger.error("文件块 $blockName 下载失败 ${it.message}")
                    }
                    jobs[index] = null
                    channel.send(index)
                }
            }

            jobs.forEach { it?.join() }
        }.let {
            val speed = length / (it.inSeconds * 1024 * 1024)
            logger.info("文件 $filename (${length / (1024 * 1024)}M) 下载完毕 共计${it.inMinutes} 分钟 速度${speed} MB/s")
        }
    }

    suspend fun download(workNO: String): File {
        val dir = File(workNO).apply { mkdir() }
        httpClient.get<HttpResponse>(PLAY_LOGIN_URL).setCookie()
        httpClient.get<HttpResponse>(DOWNLOAD_URL) {
            parameter("workno", workNO)
        }.apply { setCookie() }.let { response ->
            when (response.request.url.host) {
                // 直接下载
                "download.dlsite.com" -> downloadFile(response.request.url.toString(), dir).join()

                // 分段下载 或者 需要许可证
                "www.dlsite.com" -> requireNotNull(Jsoup.parse(response.readText())).let { document ->
                    // License Key
                    document.select(".table_inframe_box_fix strong").text().let { license ->
                        File(dir, "License.txt").writeText(requireNotNull(license))
                        logger.info("作品${workNO} 的许可证 $license 已保存")
                    }
                    // Download list
                    document.select(".work_download a").forEach { element ->
                        requireNotNull(element.attr("href")).let { url ->
                            downloadFile(url, dir).join()
                        }
                    }
                }
                else -> throw IllegalStateException("大概需要登陆。。。")
            }
        }
        return dir
    }

    @JvmStatic
    fun main(args: Array<String>) = runCatching<Unit> {
        runBlocking {
            login()
            args.forEach { download(it) }
        }
    }.onFailure {
        logger.error(it)
    }.getOrDefault(Unit)
}