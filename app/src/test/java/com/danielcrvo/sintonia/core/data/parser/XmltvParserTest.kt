package com.danielcrvo.sintonia.core.data.parser

import java.io.StringReader
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XmltvParserTest {
    private fun parse(xml: String) = XmltvParser.parse(StringReader(xml)).toList()

    @Test
    fun `converts dates with and without a time zone`() {
        assertEquals(Instant.parse("2026-10-21T09:00:00Z").toEpochMilli(), XmltvParser.parseDate("20261021060000 -0300"))
        assertEquals(Instant.parse("2026-10-21T06:00:00Z").toEpochMilli(), XmltvParser.parseDate("20261021060000"))
        assertEquals(Instant.parse("2026-10-21T04:30:00Z").toEpochMilli(), XmltvParser.parseDate("20261021060000 +0130"))
        assertEquals(Instant.parse("2026-10-21T06:00:00Z").toEpochMilli(), XmltvParser.parseDate("202610210600"))
    }

    @Test
    fun `impossible dates return null`() {
        assertNull(XmltvParser.parseDate("20260231000000"))
        assertNull(XmltvParser.parseDate("20261301000000"))
        assertNull(XmltvParser.parseDate("texto"))
    }

    @Test
    fun `reads a programme with title and description`() {
        val xml = """<tv><programme start="20261021060000 +0000" stop="20261021070000 +0000" channel="faro.noticias">
            <title lang="pt">Jornal da Manhã</title><desc>Notícias &amp; clima</desc></programme></tv>"""
        val result = parse(xml)
        assertEquals(1, result.size)
        assertEquals("faro.noticias", result[0].channel)
        assertEquals("Jornal da Manhã", result[0].title)
        assertEquals("Notícias & clima", result[0].description)
    }

    @Test
    fun `numeric entities and CDATA`() {
        val xml = """<tv><programme start="20261021060000" stop="20261021070000" channel="a">
            <title><![CDATA[Rock & Roll]]></title><desc>A&#233; &#x41;</desc></programme></tv>"""
        val result = parse(xml)
        assertEquals("Rock & Roll", result[0].title)
        assertEquals("Aé A", result[0].description)
    }

    @Test
    fun `skips malformed items and continues`() {
        val xml = """<tv>
            <programme start="20261021060000" stop="20261021050000" channel="a"><title>Fim antes do início</title></programme>
            <programme start="20261021060000" stop="20261021070000"><title>Sem canal</title></programme>
            <programme start="20261021060000" stop="20261021070000" channel="a"></programme>
            <programme start="20261021060000" stop="20261021070000" channel="a"/>
            <programme start="20261021060000" stop="20261021070000" channel="a"><title>Válido</title></programme>
        </tv>"""
        assertEquals(listOf("Válido"), parse(xml).map { it.title })
    }

    @Test
    fun `reads a large file in chunks smaller than a programme`() {
        val programme = """<programme start="20261021060000" stop="20261021070000" channel="a"><title>Programa</title></programme>"""
        val xml = "<tv>" + programme.repeat(5000) + "</tv>"
        // A reader that returns few characters per read forces programmes split across chunks.
        val slow = object : java.io.Reader() {
            private val inner = StringReader(xml)
            override fun read(buffer: CharArray, offset: Int, length: Int) = inner.read(buffer, offset, minOf(length, 37))
            override fun close() = inner.close()
        }
        assertEquals(5000, XmltvParser.parse(slow).count())
    }

    @Test
    fun `unterminated block does not blow memory and the next programme is still read`() {
        val garbage = "x".repeat(600 * 1024)
        val valid = """<programme start="20261021060000" stop="20261021070000" channel="a"><title>Válido</title></programme>"""
        val xml = """<tv><programme start="20261021060000" stop="20261021070000" channel="a"><title>$garbage""" + valid + "</tv>"
        assertEquals(listOf("Válido"), parse(xml).map { it.title })
    }
}
