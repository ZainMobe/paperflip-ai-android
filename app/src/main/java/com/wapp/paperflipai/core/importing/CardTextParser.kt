package com.wapp.paperflipai.core.importing

/**
 * Turns pasted term/definition text into cards. Counterpart of
 * `CardTextParser.swift`. Handles what the big apps export:
 *
 *  - Quizlet default: term <tab> definition, one card per line
 *  - comma or semicolon separated
 *  - "term - definition" and "term: definition"
 *  - Anki plain-text export (tab separated, optional "#" header lines)
 *
 * The separator is chosen from the whole paste, so one odd line does not
 * flip the parse. Each line splits on its FIRST separator, so a definition
 * may itself contain commas. Lines with no separator are skipped.
 */
object CardTextParser {

    data class ParsedCard(val front: String, val back: String)

    data class Result(
        val cards: List<ParsedCard>,
        val skippedLines: Int,
        val separatorName: String,
    )

    private data class Separator(val name: String, val regex: Regex)

    // Order matters on ties: the earlier, more specific separator wins.
    private val separators = listOf(
        Separator("tab", Regex("""\t+""")),
        Separator("dash", Regex("""\s+[-–—]\s+""")),
        Separator("colon", Regex("""\s*:\s+""")),
        Separator("semicolon", Regex("""\s*;\s*""")),
        Separator("comma", Regex("""\s*,\s*""")),
    )

    fun parse(text: String): Result {
        val lines = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        if (lines.isEmpty()) return Result(emptyList(), 0, "")

        var best: Separator? = null
        var bestHits = 0
        for (separator in separators) {
            val hits = lines.count { split(it, separator.regex) != null }
            if (hits > bestHits) {
                best = separator
                bestHits = hits
            }
        }
        val chosen = best ?: return Result(emptyList(), lines.size, "")

        val cards = ArrayList<ParsedCard>(lines.size)
        var skipped = 0
        for (line in lines) {
            val pair = split(line, chosen.regex)
            if (pair == null) skipped += 1 else cards += ParsedCard(pair.first, pair.second)
        }
        return Result(cards, skipped, chosen.name)
    }

    private fun split(line: String, regex: Regex): Pair<String, String>? {
        val match = regex.find(line) ?: return null
        val front = line.substring(0, match.range.first).trim()
        val back = line.substring(match.range.last + 1).trim()
        if (front.isEmpty() || back.isEmpty()) return null
        return unquote(front) to unquote(back)
    }

    private fun unquote(value: String): String =
        if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
            value.substring(1, value.length - 1).replace("\"\"", "\"")
        } else value
}
