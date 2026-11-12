package com.vibe.docsnotes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Live-renders Google Docs style markdown syntax inside the editor while preserving exact character offsets.
 */
fun parseLiveDocsText(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            val headingStyle = when {
                line.startsWith("### ") -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                line.startsWith("## ") -> SpanStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                line.startsWith("# ") -> SpanStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8))
                line.startsWith("> ") -> SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF5F6368))
                else -> null
            }

            if (headingStyle != null) {
                val prefixLen = when {
                    line.startsWith("### ") -> 4
                    line.startsWith("## ") -> 3
                    else -> 2
                }
                withStyle(SpanStyle(color = Color(0xFF9AA0A6), fontSize = 14.sp)) {
                    append(line.substring(0, prefixLen))
                }
                withStyle(headingStyle) {
                    appendStyledInline(line.substring(prefixLen))
                }
            } else if (line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- ")) {
                val isChecked = line.startsWith("- [x] ")
                val prefixLen = if (line.startsWith("- [")) 6 else 2
                withStyle(SpanStyle(color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)) {
                    append(line.substring(0, prefixLen))
                }
                val contentStyle = if (isChecked) {
                    SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color(0xFF80868B))
                } else {
                    SpanStyle()
                }
                withStyle(contentStyle) {
                    appendStyledInline(line.substring(prefixLen))
                }
            } else {
                appendStyledInline(line)
            }

            if (index < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendStyledInline(segment: String) {
    // Matches ***bold italic***, **bold**, *italic*, ~~strikethrough~~, ==highlight==, `code`
    val pattern = Regex("(\*\*\*(.+?)\*\*\*|\*\*(.+?)\*\*|\*(.+?)\*|~~(.+?)~~|==(.+?)==|`(.+?)`)")
    var lastIndex = 0

    for (match in pattern.findAll(segment)) {
        if (match.range.first > lastIndex) {
            append(segment.substring(lastIndex, match.range.first))
        }
        val full = match.value
        val markerColor = SpanStyle(color = Color(0xFFBDC1C6))

        when {
            full.startsWith("***") -> {
                withStyle(markerColor) { append("***") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[2])
                }
                withStyle(markerColor) { append("***") }
            }
            full.startsWith("**") -> {
                withStyle(markerColor) { append("**") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[3])
                }
                withStyle(markerColor) { append("**") }
            }
            full.startsWith("*") -> {
                withStyle(markerColor) { append("*") }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[4])
                }
                withStyle(markerColor) { append("*") }
            }
            full.startsWith("~~") -> {
                withStyle(markerColor) { append("~~") }
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color(0xFF5F6368))) {
                    append(match.groupValues[5])
                }
                withStyle(markerColor) { append("~~") }
            }
            full.startsWith("==") -> {
                withStyle(markerColor) { append("==") }
                withStyle(SpanStyle(background = Color(0xFFFEF08A), color = Color(0xFF1F1F1F))) {
                    append(match.groupValues[6])
                }
                withStyle(markerColor) { append("==") }
            }
            full.startsWith("`") -> {
                withStyle(markerColor) { append("`") }
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFF1F3F4), color = Color(0xFFD93025))) {
                    append(match.groupValues[7])
                }
                withStyle(markerColor) { append("`") }
            }
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < segment.length) {
        append(segment.substring(lastIndex))
    }
}
