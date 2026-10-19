package com.gastrack.app.core.common

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Bounded import compatible with the minimum Android API (26). */
fun InputStream.readLimited(limit: Int): ByteArray {
    val result = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(result.size() + count <= limit) { "The selected file is too large." }
        result.write(buffer, 0, count)
    }
    return result.toByteArray()
}
