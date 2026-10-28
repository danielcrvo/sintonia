package com.danielcrvo.sintonia.core.common

import com.danielcrvo.sintonia.core.network.ResponseTooLargeException
import java.io.FilterInputStream
import java.io.InputStream

class LimitedInputStream(input: InputStream, private val maxBytes: Long) : FilterInputStream(input) {
    private var total = 0L

    private fun add(count: Long) {
        total += count
        if (total > maxBytes) throw ResponseTooLargeException()
    }

    override fun read(): Int = super.read().also { if (it >= 0) add(1) }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it > 0) add(it.toLong()) }
}
