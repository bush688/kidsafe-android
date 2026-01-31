package com.kidsafe.probe.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownContent(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.replace("\r\n", "\n").split('\n')
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var inList = false
        lines.forEach { raw ->
            val line = raw.trimEnd()
            if (line.isBlank()) {
                inList = false
                return@forEach
            }

            when {
                line.startsWith("### ") -> {
                    inList = false
                    Text(
                        text = inlineBold(line.removePrefix("### ").trim()),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                line.startsWith("## ") -> {
                    inList = false
                    Text(
                        text = inlineBold(line.removePrefix("## ").trim()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                line.startsWith("# ") -> {
                    inList = false
                    Text(
                        text = inlineBold(line.removePrefix("# ").trim()),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                line.startsWith("- ") -> {
                    inList = true
                    Text(
                        text = inlineBold("• " + line.removePrefix("- ").trim()),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                else -> {
                    val topPad = if (inList) 0.dp else 0.dp
                    Text(
                        text = inlineBold(line),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = topPad),
                    )
                }
            }
        }
    }
}

private fun inlineBold(text: String): AnnotatedString {
    val out = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val start = text.indexOf("**", startIndex = i)
            if (start < 0) {
                append(text.substring(i))
                break
            }
            val end = text.indexOf("**", startIndex = start + 2)
            if (end < 0) {
                append(text.substring(i))
                break
            }
            append(text.substring(i, start))
            val boldText = text.substring(start + 2, end)
            val spanStart = length
            append(boldText)
            addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), spanStart, spanStart + boldText.length)
            i = end + 2
        }
    }
    return out
}

