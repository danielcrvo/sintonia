package com.danielcrvo.sintonia.core.data.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class M3uParserTest {
    private fun parse(text: String): Pair<M3uParser, List<M3uEntry>> {
        val parser = M3uParser()
        val entries = parser.parse(text.lineSequence()).toList()
        return parser to entries
    }

    private val basic = listOf(
        "#EXTM3U url-tvg=\"http://guia.exemplo/xmltv.xml\"",
        "#EXTINF:-1 tvg-id=\"faro.noticias\" tvg-name=\"Faro Notícias\" tvg-logo=\"http://img.exemplo/faro.png\" group-title=\"Notícias\",Faro Notícias",
        "http://servidor.exemplo/live/ana/123/101.m3u8",
    ).joinToString("\n")

    @Test
    fun `reads header, attributes and URL`() {
        val (parser, entries) = parse(basic)
        assertEquals("http://guia.exemplo/xmltv.xml", parser.epgUrl)
        assertEquals(
            listOf(
                M3uEntry(
                    tvgId = "faro.noticias",
                    tvgName = "Faro Notícias",
                    name = "Faro Notícias",
                    logo = "http://img.exemplo/faro.png",
                    group = "Notícias",
                    url = "http://servidor.exemplo/live/ana/123/101.m3u8",
                ),
            ),
            entries,
        )
    }

    @Test
    fun `accepts x-tvg-url in the header`() {
        val (parser, _) = parse("#EXTM3U x-tvg-url=\"http://g.exemplo/a.xml\"\n")
        assertEquals("http://g.exemplo/a.xml", parser.epgUrl)
    }

    @Test
    fun `strips BOM and accepts CRLF and blank lines`() {
        val (_, entries) = parse("﻿#EXTM3U\r\n\r\n#EXTINF:-1,Rede Horizonte\r\n\r\nhttp://x.exemplo/1\r\n".replace("\r\n", "\n"))
        assertEquals(1, entries.size)
        assertEquals("Rede Horizonte", entries[0].name)
        assertEquals("http://x.exemplo/1", entries[0].url)
    }

    @Test
    fun `keeps a comma inside the name`() {
        val (_, entries) = parse("#EXTM3U\n#EXTINF:-1 tvg-id=\"a\",Cine Aurora, Sessão da Tarde\nhttp://x.exemplo/1")
        assertEquals("Cine Aurora, Sessão da Tarde", entries[0].name)
    }

    @Test
    fun `keeps a comma inside a quoted attribute`() {
        val (_, entries) = parse("#EXTM3U\n#EXTINF:-1 group-title=\"Esportes, Ao vivo\" tvg-id=\"arena\",Arena Sport 1\nhttp://x.exemplo/1")
        assertEquals("Esportes, Ao vivo", entries[0].group)
        assertEquals("Arena Sport 1", entries[0].name)
    }

    @Test
    fun `empty name falls back to tvg-name, then to the URL`() {
        val (_, entries) = parse("#EXTM3U\n#EXTINF:-1 tvg-name=\"Docs Terra\",\nhttp://x.exemplo/1\n#EXTINF:-1,\nhttp://x.exemplo/2")
        assertEquals("Docs Terra", entries[0].name)
        assertEquals("http://x.exemplo/2", entries[1].name)
    }

    @Test
    fun `uses EXTGRP without leaking into the next item`() {
        val (_, entries) = parse("#EXTM3U\n#EXTINF:-1,Kids Planeta\n#EXTGRP:Infantil\nhttp://x.exemplo/1\n#EXTINF:-1,Música Viva\nhttp://x.exemplo/2")
        assertEquals("Infantil", entries[0].group)
        assertNull(entries[1].group)
    }

    @Test
    fun `group-title wins over EXTGRP`() {
        val (_, entries) = parse("#EXTM3U\n#EXTINF:-1 group-title=\"Filmes\",Eco\n#EXTGRP:Outro\nhttp://x.exemplo/1")
        assertEquals("Filmes", entries[0].group)
    }

    @Test
    fun `keeps two items with the same tvg-id`() {
        val text = "#EXTM3U\n#EXTINF:-1 tvg-id=\"faro\",Faro Notícias HD\nhttp://x.exemplo/1\n#EXTINF:-1 tvg-id=\"faro\",Faro Notícias SD\nhttp://x.exemplo/2"
        val (_, entries) = parse(text)
        assertEquals(listOf("Faro Notícias HD", "Faro Notícias SD"), entries.map { it.name })
    }

    @Test
    fun `ignores lines that are not addresses`() {
        val (_, entries) = parse("#EXTM3U\n#EXTINF:-1,Texto\nnao-e-url\n#EXTINF:-1,Ok\nhttp://x.exemplo/ok")
        assertEquals(listOf("http://x.exemplo/ok"), entries.map { it.url })
    }

    @Test
    fun `binary or empty file yields no items`() {
        assertEquals(0, parse("").second.size)
        assertEquals(0, parse("\u0000\u0001garbage\n\u0002").second.size)
    }
}
