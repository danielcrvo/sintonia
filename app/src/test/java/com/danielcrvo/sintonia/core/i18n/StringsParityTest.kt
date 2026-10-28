package com.danielcrvo.sintonia.core.i18n

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsParityTest {
    private data class Entry(val kind: String, val placeholders: Set<String>)

    private val resDir = listOf("src/main/res", "app/src/main/res").map(::File).first { it.exists() }

    private fun load(folder: String): Map<String, Entry> {
        val file = File(resDir, "$folder/strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val result = linkedMapOf<String, Entry>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as? Element ?: continue
            val name = node.getAttribute("name")
            if (node.getAttribute("translatable") == "false") continue
            when (node.tagName) {
                "string" -> result[name] = Entry("string", placeholders(node.textContent))
                "plurals" -> {
                    val items = node.getElementsByTagName("item")
                    val all = (0 until items.length).map { placeholders(items.item(it).textContent) }
                    result[name] = Entry("plurals", all.flatten().toSet())
                }
            }
        }
        return result
    }

    private fun placeholders(text: String): Set<String> =
        Regex("%(\\d+\\$)?[sdf]").findAll(text).map { it.value }.toSet()

    private fun quantities(folder: String, plural: String): Set<String> {
        val file = File(resDir, "$folder/strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val plurals = doc.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val element = plurals.item(i) as Element
            if (element.getAttribute("name") != plural) continue
            val items = element.getElementsByTagName("item")
            return (0 until items.length).map { (items.item(it) as Element).getAttribute("quantity") }.toSet()
        }
        return emptySet()
    }

    @Test
    fun `the three languages have the same keys`() {
        val base = load("values")
        listOf("values-en", "values-es").forEach { folder ->
            val other = load(folder)
            assertEquals(base.keys - other.keys, emptySet(), "missing in $folder")
            assertEquals(other.keys - base.keys, emptySet(), "extra in $folder")
        }
    }

    @Test
    fun `placeholders are the same in every language`() {
        val base = load("values")
        listOf("values-en", "values-es").forEach { folder ->
            val other = load(folder)
            base.forEach { (name, entry) ->
                assertEquals(entry.kind, other[name]?.kind, "tipo de $name em $folder")
                assertEquals(entry.placeholders, other[name]?.placeholders, "placeholders de $name em $folder")
            }
        }
    }

    @Test
    fun `Spanish plurals have one, many and other`() {
        val plurals = load("values-es").filterValues { it.kind == "plurals" }.keys
        assertTrue(plurals.isNotEmpty())
        plurals.forEach { name ->
            assertEquals(setOf("one", "many", "other"), quantities("values-es", name), "quantidades de $name em es")
        }
    }

    @Test
    fun `no translation is empty`() {
        listOf("values", "values-en", "values-es").forEach { folder ->
            val file = File(resDir, "$folder/strings.xml")
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val strings = doc.getElementsByTagName("string")
            for (i in 0 until strings.length) {
                val element = strings.item(i) as Element
                if (element.getAttribute("translatable") == "false") continue
                assertTrue(element.textContent.isNotBlank(), "${element.getAttribute("name")} vazia em $folder")
            }
        }
    }
}
