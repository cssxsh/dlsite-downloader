package xyz.cssxsh.dlsite

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DLsiteToolTest {

    @BeforeAll
    fun login() = runBlocking {
        assertNotNull(DLsiteTool.login())
    }

    @Test
    fun purchases() = runBlocking {
        assertEquals(DLsiteTool.purchases().works.size, 440)
    }

    @Test
    fun download(): Unit = runBlocking {
        DLsiteTool.download("VJ013799")
    }
}