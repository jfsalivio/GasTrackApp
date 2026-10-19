package com.gastrack.app

import com.gastrack.app.core.common.Resource
import com.gastrack.app.core.network.networkCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class NetworkCallTest {
    @Test fun `timeout reports uncertainty without issuing a second transaction`() = runTest {
        var attempts = 0
        val result = networkCall<Unit>(Json) { attempts++; throw SocketTimeoutException() }
        assertEquals(1, attempts)
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("before retrying"))
    }
    @Test fun `cancellation propagates rather than becoming a screen error`() = runTest {
        try { networkCall<Unit>(Json) { throw CancellationException("Screen left") }; fail("Cancellation must propagate") }
        catch (_: CancellationException) { }
    }
}
