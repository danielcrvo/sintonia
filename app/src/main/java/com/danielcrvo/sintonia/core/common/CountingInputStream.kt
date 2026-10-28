package com.danielcrvo.sintonia.core.common

import java.io.FilterInputStream
import java.io.InputStream

class CountingInputStream(input: InputStream) : FilterInputStream(input) {
    var bytesRead: Long = 0
        private set

    override fun read(): Int = super.read().also { if (it >= 0) bytesRead++ }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it > 0) bytesRead += it }

    override fun skip(count: Long): Long = super.skip(count).also { bytesRead += it }
}
