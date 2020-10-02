package xyz.cssxsh.dlsite

import io.ktor.client.features.cookies.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.cssxsh.dlsite.DLsiteTool.logger
import kotlin.math.min


class LoggerCookiesStorage : CookiesStorage {
    val container: MutableList<Cookie> = mutableListOf()
    private val oldestCookie: AtomicLong = atomic(0L)
    private val mutex = Mutex()

    @InternalAPI
    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val date = GMTDate()
        if (date.timestamp >= oldestCookie.value) cleanup(date.timestamp)

        container.filter { it.matches(requestUrl) }
    }

    @InternalAPI
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie): Unit = mutex.withLock {
        with(cookie) {
            if (name.isBlank()) return@withLock
        }

        val cookieNow = cookie.let {
            if (it.path?.startsWith("/") != true) it.copy(path = requestUrl.encodedPath) else it
        }.let {
            if (it.domain.isNullOrBlank()) it.copy(domain = requestUrl.host) else it
        }

        val isOld= container.removeAll { it.name == cookie.name && it.matches(requestUrl) }
        container.add(cookieNow)
        cookie.expires?.timestamp?.let { expires ->
            if (oldestCookie.value > expires) {
                oldestCookie.value = expires
            }
        }
        if (isOld.not()) logger.info("${cookieNow.domain} 已添加 Cookie(${cookie.name}=${cookie.value.let { 
            if(it.length <= 64) it else "${it.substring(0, 64)}..." 
        }})")
    }

    override fun close() {
    }

    private fun cleanup(timestamp: Long) {
        container.removeAll { cookie ->
            val expires = cookie.expires?.timestamp ?: return@removeAll false
            expires < timestamp
        }

        val newOldest = container.fold(Long.MAX_VALUE) { acc, cookie ->
            cookie.expires?.timestamp?.let { min(acc, it) } ?: acc
        }

        oldestCookie.value = newOldest
    }

    @InternalAPI
    fun Cookie.matches(requestUrl: Url): Boolean {
        val domain = domain?.toLowerCasePreservingASCIIRules()?.trimStart('.')
            ?: error("Domain field should have the default value")

        val path = with(path) {
            val current = path ?: error("Path field should have the default value")
            if (current.endsWith('/')) current else "$path/"
        }

        val host = requestUrl.host.toLowerCasePreservingASCIIRules()
        val requestPath = let {
            val pathInRequest = requestUrl.encodedPath
            if (pathInRequest.endsWith('/')) pathInRequest else "$pathInRequest/"
        }

        if (host != domain && (hostIsIp(host) || !host.endsWith(".$domain"))) {
            return false
        }

        if (path != "/" &&
            requestPath != path &&
            !requestPath.startsWith(path)
        ) return false

        return !(secure && !requestUrl.protocol.isSecure())
    }
}
