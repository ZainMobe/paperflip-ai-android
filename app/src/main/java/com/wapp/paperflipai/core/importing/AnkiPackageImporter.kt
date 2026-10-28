package com.wapp.paperflipai.core.importing

import android.database.sqlite.SQLiteDatabase
import com.wapp.paperflipai.core.data.CardDto
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Reads an Anki `.apkg` export: a zip holding a SQLite collection
 * (`collection.anki21` or `collection.anki2`). Each note's first two
 * fields become a card's front and back; HTML, cloze markup and media tags
 * are stripped. Counterpart of `AnkiPackageImporter.swift`, using the
 * platform's own zip and SQLite rather than a library.
 *
 * Anki's newest format (`collection.anki21b`, zstd-compressed) can't be read
 * here; the user is told to re-export with "Support older Anki versions".
 */
object AnkiPackageImporter {

    class ImportError(val reason: Reason) : Exception(reason.name) {
        enum class Reason { NewFormat, NoCollection, Database, Empty, NotAnArchive }
    }

    data class ImportedDeck(
        val title: String,
        val cards: List<CardDto>,
        val noteCount: Int,
        val skipped: Int,
    )

    private val collectionNames = listOf("collection.anki21", "collection.anki2")

    /** Blocking; call from Dispatchers.IO. */
    fun import(bytes: ByteArray, fileName: String, cacheDir: File): ImportedDeck {
        val entries = HashMap<String, ByteArray>()
        var sawNewFormat = false
        var sawAnyEntry = false
        runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    sawAnyEntry = true
                    when (entry.name) {
                        "collection.anki21b" -> sawNewFormat = true
                        in collectionNames -> entries[entry.name] = zip.readBytes()
                    }
                    zip.closeEntry()
                }
            }
        }.onFailure { throw ImportError(ImportError.Reason.NotAnArchive) }
        if (!sawAnyEntry) throw ImportError(ImportError.Reason.NotAnArchive)

        val collection = collectionNames.firstNotNullOfOrNull { entries[it] }
            ?: throw ImportError(
                if (sawNewFormat) ImportError.Reason.NewFormat else ImportError.Reason.NoCollection
            )

        val tmp = File.createTempFile("anki-", ".sqlite", cacheDir)
        try {
            tmp.writeBytes(collection)
            val cards = ArrayList<CardDto>()
            var notes = 0
            var skipped = 0
            val db = runCatching {
                SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY)
            }.getOrElse { throw ImportError(ImportError.Reason.Database) }
            db.use {
                runCatching {
                    it.rawQuery("SELECT flds FROM notes ORDER BY id", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            notes += 1
                            val fields = cursor.getString(0).orEmpty().split('\u001f')
                            if (fields.size < 2) { skipped += 1; continue }
                            val front = clean(fields[0])
                            val back = clean(fields[1])
                            if (front.isEmpty() || back.isEmpty()) { skipped += 1; continue }
                            cards += CardDto(front = front, back = back)
                        }
                    }
                }.onFailure { throw ImportError(ImportError.Reason.Database) }
            }
            if (cards.isEmpty()) throw ImportError(ImportError.Reason.Empty)

            val title = fileName.substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifEmpty { "Anki import" }
            return ImportedDeck(title, cards, notes, skipped)
        } finally {
            tmp.delete()
            File(tmp.path + "-journal").delete()
        }
    }

    private val entities = mapOf(
        "&nbsp;" to " ", "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
        "&quot;" to "\"", "&#39;" to "'", "&apos;" to "'",
    )

    /** Strips Anki's HTML down to readable plain text. */
    fun clean(raw: String): String {
        var s = raw
        s = s.replace(Regex("""\[sound:[^\]]*]"""), "")
        s = s.replace(Regex("""\{\{c\d+::(.*?)(::[^}]*)?\}\}"""), "$1")
        s = s.replace(Regex("""<br\s*/?>|</div>|</p>|</li>""", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("""<[^>]+>"""), "")
        entities.forEach { (k, v) -> s = s.replace(k, v) }
        s = s.replace(Regex("""&#\d+;"""), "")
        s = s.replace(Regex("""[ \t]+"""), " ")
        s = s.replace(Regex("""\n{3,}"""), "\n\n")
        return s.trim()
    }
}
