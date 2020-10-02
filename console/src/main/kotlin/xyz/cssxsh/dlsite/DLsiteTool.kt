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
import kotlinx.coroutines.sync.*
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.Jsoup
import org.slf4j.*
import xyz.cssxsh.dlsite.data.*
import java.io.File
import java.io.RandomAccessFile
import java.lang.Long.min
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTime

object DLsiteTool : CoroutineScope {
    private const val CONFIG_FILE = "config.json"
    private const val DATA_FILE = "data.json"
    private const val PURCHASES_URL = "https://play.dlsite.com/api/purchases"
    private const val DOWNLOAD_URL = "https://play.dlsite.com/api/download"
    private const val AUTHORIZE_URL = "https://play.dlsite.com/api/authorize"
    private const val PLAY_LOGIN_URL = "https://play.dlsite.com/login"
    private const val LOGIN_URL = "https://login.dlsite.com/login"
    private const val MYLIST_URL = "https://play.dlsite.com/api/mylist/mylists"

    val logger: Logger = requireNotNull(LoggerFactory.getLogger(this::class.java)) { "创建logger失败" }

    override val coroutineContext: CoroutineContext by lazy {
        Dispatchers.IO + CoroutineName("DListeTool")
    }

    var config: Config
        set(value) = File(CONFIG_FILE).writeText(Json.encodeToString(Config.serializer(), value))
        get() = File(CONFIG_FILE).run {
            if (exists()) {
                Json.decodeFromString(Config.serializer(), readText())
            } else {
                Config(
                    style = "LIGHT",
                    downloadListName = "DownLoad",
                    loginId = "",
                    password = "",
                    cname = emptyMap(),
                    chinaDns = "https://223.5.5.5/dns-query",
                    foreignDns = "https://cloudflare-dns.com/dns-query",
                    maxAsyncNum = 16,
                    blockSizeMB = 8
                ).also {
                    writeText(Json.encodeToString(Config.serializer(), it))
                }
            }
        }

    private val newDns: Dns = object : Dns {
        private val client = OkHttpClient()
        val chinaDoh = DnsOverHttps.Builder().apply {
            client(client)
            url(config.chinaDns.toHttpUrl())
            includeIPv6(false)
            post(true)
        }.build()
        val foreignDoh = DnsOverHttps.Builder().apply {
            client(client)
            url(config.foreignDns.toHttpUrl())
            includeIPv6(false)
            post(true)
        }.build()

        val host: MutableMap<String, List<InetAddress>> = mutableMapOf()

        override fun lookup(hostname: String): List<InetAddress> = host.getOrElse(hostname) {
            (config.cname[hostname]?.let { chinaDoh.lookup(it) } ?: foreignDoh.lookup(hostname)).also { list ->
                host[hostname] = list
                logger.info("域名 $hostname 查询, 结果${list}")
            }
        }

    }

    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(HttpTimeout) {
            socketTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 180_000
        }
        install(HttpCookies) {
            storage = LoggerCookiesStorage()
        }
        ContentEncoding {
            gzip()
            deflate()
            identity()
        }
        engine {
            config {
                dns(newDns)
            }
        }
    }

    suspend fun login(): String? {
        val config = config
        logger.info("开始登录 用户名: ${config.loginId} , 密码: ${config.password} ")
        val token = httpClient.get<HttpResponse>(LOGIN_URL).setCookie().let { list ->
            requireNotNull(list["XSRF-TOKEN"]?.value) { "XSRF-TOKEN 为空" }
        }
        httpClient.post<HttpResponse>(LOGIN_URL) {
            body = FormDataContent(Parameters.build {
                append("login_id", config.loginId.apply { require(isNotEmpty()) { "登录名为空" } })
                append("password", config.password.apply { require(isNotEmpty()) { "密码为空" } })
                append("_token", token)
            })
        }.setCookie()
        httpClient.get<HttpResponse>(PLAY_LOGIN_URL).setCookie()
        return httpClient.cookies("https://login.dlsite.com")["PHPSESSID"]?.value
    }

    suspend fun purchases(block: (List<WorkInfo>) -> Unit = {}): WorkData = withContext(coroutineContext) {
        suspend fun getPurchases(page: Int) = httpClient.get<Purchases>(PURCHASES_URL) {
            parameter("page", page)
            logger.info("Load WorkData form ${url.buildString()}")
        }.also {
            it.works.apply(block)
            logger.info("Load WorkData count ${it.works.size}")
        }

        getPurchases(1).run {
            WorkData(works = works.toMutableList()).also { data ->
                (if (offset > 0) (1..offset) else IntRange.EMPTY).forEach { index ->
                    data.works.addAll(getPurchases(index + 1).works)
                }
            }
        }.also {
            File(DATA_FILE).writeText(Json.encodeToString(WorkData.serializer(), it))
        }
    }

    suspend fun authorize() = httpClient.get<Authorize>(AUTHORIZE_URL)

    private suspend fun myLists(sync: Boolean = true): Map<String, MyList> = buildMap {
        httpClient.get<MyListsData>(MYLIST_URL) { parameter("sync", sync) }.let { data ->
            data.mylists.forEach { list ->
                this[list.myListName] = MyList(
                    id = list.id,
                    insertDate = list.insertDate,
                    name = list.myListName,
                    works = list.myListWorkId.map { index -> data.mylistWorks[index.toInt()] }
                )
            }
        }
    }

    fun downloadList(name: String): Job = launch {
        requireNotNull(myLists()[name.trim()]) { "未找到列表$name" }.let { list ->
            logger.info("尝试下载列表 ${list.name} ")
            list.works.forEach { workNO ->
                runCatching {
                    download(workNO)
                }.onFailure {
                    logger.error(it)
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun downloadFile(url: String, dir: File): Job = launch {
        val config = config

        val response = httpClient.get<HttpResponse>(url) {
            header(HttpHeaders.Range, "bytes=0-1")
        }

        val length = requireNotNull(response.headers[HttpHeaders.ContentRange]?.let { value ->
            // Content-Range: bytes 0-0/58231185
            Regex("""\d+$""").find(value)?.value
        }) { "文件长度获取失败" }.toLong()

        val filename = requireNotNull(response.headers[HttpHeaders.ContentDisposition]?.let { value ->
            Regex("""[A-Z]{2}[0-9]{6}[.\w]+""").find(value)?.value
        }) { "文件名获取失败" }

        val file = File(dir, filename).also {
            if (it.exists()) {
                logger.info("文件${it.toURI()}已存在，将跳过下载")
                this.cancel()
            }
        }

        val tempFile = File(dir, "$filename.temp")

        RandomAccessFile(tempFile, "rw").apply {
            setLength(length)
        }.close()

        val blockSize: Long = config.blockSizeMB * 1024 * 1024
        val blockNum: Int = ((length - 1) / blockSize + 1).toInt()

        var finishNum = 0
        val mutex = Mutex()
        val channel: Channel<Int> = Channel(config.maxAsyncNum)

        val downloadBlock: suspend (index: Int) -> Unit = { index ->
            val start = (index * blockSize)
            val end = min((index + 1) * blockSize, length) - 1
            httpClient.get<ByteArray>(url) {
                // header(HttpHeaders.Cookie, "AuthCookie=${config.cookie}")
                header(HttpHeaders.Range, "bytes=${start}-${end}")
                timeout {
                    requestTimeoutMillis = config.blockSizeMB * 300_000
                }
            }.let {
                RandomAccessFile(tempFile, "rw").apply {
                    seek(start)
                    write(it)
                }.close()
            }
        }

        measureTime {
            logger.info("文件 $filename (${length / (1024 * 1024)}M)开始下载")

            List(blockNum) { index ->
                async {
                    channel.send(index)
                    while (isActive)  {
                        runCatching {
                            downloadBlock(index)
                        }.onSuccess {
                            channel.receive()
                            mutex.withLock {
                                finishNum++
                                logger.debug("文件 $filename 块 $index 下载完成, 总完成度 $finishNum/$blockNum ")
                            }
                            return@async
                        }.onFailure {
                            logger.error("文件 $filename 块 $index 下载失败, 错误 ${it.message} ")
                        }.isSuccess
                    }
                }
            }.awaitAll()
        }.let {
            val size = length / (1024.0 * 1024.0)
            val speed = size / it.inSeconds
            logger.info("文件 ${file.toURI()} (${size}MB) 下载完毕 共计${it.inMinutes} 分钟 速度${speed} MB/s")
        }

        tempFile.renameTo(file)
    }

    suspend fun download(workNO: String): File {
        workNO.also {
            require(it.matches("""[A-Z]{2}[0-9]{6}""".toRegex())) { """work_不正确""" }
        }

        val dir = File(workNO).apply { mkdir() }

        logger.info(" 下载任务 $workNO 开始, 目录 ${dir.toURI()} ")

        httpClient.get<HttpResponse>(DOWNLOAD_URL) {
            parameter("workno", workNO)
            header(HttpHeaders.Range, "bytes=0-0")
            logger.info("开始链接 ${url.buildString()}")
        }.apply { setCookie() }.let { response ->
            when (response.request.url.host) {
                // 直接下载
                "download.dlsite.com" -> downloadFile(response.request.url.toString(), dir).join()
                // 分段下载 或者 需要许可证运行
                "www.dlsite.com" -> requireNotNull(Jsoup.parse(response.readText())).let { document ->
                    // License Key
                    document.select("td").map { it.text().trim() }.find {
                        it.matches("""\w{4}-\w{4}-\w{4}-\w{4}""".toRegex())
                    }?.let { license ->
                        File(dir, "License.txt").writeText(license)
                        logger.info("作品${workNO} 的许可证 $license 已保存")
                    }
                    // Download list
                    document.select(".work_download a").forEach { element ->
                        requireNotNull(element.attr("href")) { "下载链接获取失败" }.let { url ->
                            downloadFile(url, dir).join()
                        }
                    }
                }
                else -> throw IllegalStateException("跳转到了${response.request.url}，大概需要登陆。。。")
            }
        }
        return dir
    }

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        runCatching<Unit> {
            requireNotNull(login()) { "登陆失败" }

            if (args.isNotEmpty()) {
                args.forEach { workNO ->
                    runCatching {
                        download(workNO)
                    }.onFailure {
                        logger.error(it)
                    }
                }
            } else {
                downloadList(name = config.downloadListName).join()
            }
        }.onFailure {
            logger.error(it)
        }
    }
}