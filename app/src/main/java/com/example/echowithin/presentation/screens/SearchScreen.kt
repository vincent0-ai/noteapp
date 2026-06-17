package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import com.example.echowithin.presentation.viewmodel.NotesViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import com.example.echowithin.ui.theme.BrandOrange
import com.example.echowithin.ui.theme.BrandAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: NotesViewModel,
    isLocked: Boolean,
    onNoteClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = viewModel.uiState.searchResults
    val isLoading = viewModel.uiState.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { EchoWithinTopBarTitle() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchNotes(it)
                },
                placeholder = { Text("Search your unspoken thoughts...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = BrandOrange
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            if (isLoading && results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandOrange)
                }
            } else {
                // Build a (hit, note) pair list so the locked-note filter
                // only runs once per query result set, not once per
                // recomposition per item.
                //
                // The server-side /personal_post/search endpoint already
                // filters out locked notes (see blueprints/notes.py in the
                // backend), and the offline fallback in NotesRepository
                // filters them out locally too. The check below is a
                // belt-and-braces defence — if anything ever slips through
                // (older server, partial sync, etc.) we still won't leak
                // locked note content into the search results list.
                val notePairs = remember(results) {
                    results.map { hit -> hit to viewModel.getNoteById(hit.id) }
                }
                val visiblePairs = remember(notePairs) {
                    notePairs.filter { (_, note) -> note?.isLocked != true }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Stable key on the hit id so LazyColumn can preserve
                    // item identity across result updates (typing a new
                    // query, a locked note being hidden, etc.). Without a
                    // key, every result-list mutation tears down and
                    // recomposes every visible card and can jump scroll.
                    items(visiblePairs, key = { (hit, _) -> hit.id }) { (hit, note) ->
                        val isNoteLocked = note?.isLocked == true
                        val hideContent = isNoteLocked && isLocked

                        // Memoize the markdown-stripped + highlight-parsed
                        // snippet per item so it's computed once per result,
                        // not on every scroll frame / recomposition.
                        val highlightedSnippet = remember(hit.snippet) {
                            parseSearchSnippet(hit.snippet.orEmpty(), BrandOrange)
                        }

                        Card(
                            onClick = { onNoteClick(hit.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (hideContent) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked Note",
                                            tint = BrandAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Locked Note",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandAmber
                                        )
                                    }
                                    Text(
                                        text = "🔒 Content is hidden. Verify PIN under the Locked tab to view.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = highlightedSnippet,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hit.created_at != null) {
                                        Text(
                                            text = "Created: ${hit.created_at.take(10)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    
                                    Text(
                                        text = "View details →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (visiblePairs.isEmpty() && query.isNotEmpty() && !isLoading) {
                        item {
                            val lockedWereHidden = results.isNotEmpty()
                            Text(
                                text = if (lockedWereHidden) {
                                    "Locked notes are hidden from search. Unlock them in the Locked tab to see them here."
                                } else {
                                    "No results found for \"$query\""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

fun parseSearchSnippet(snippet: String, highlightColor: Color): AnnotatedString {
    val cleanSnippet = stripMarkdown(snippet)

    return buildAnnotatedString {
        val markStartTag = "<mark class=\"search-highlight\">"
        val markEndTag = "</mark>"
        var currentIndex = 0

        while (true) {
            val startIdx = cleanSnippet.indexOf(markStartTag, currentIndex)
            if (startIdx == -1) {
                append(cleanSnippet.substring(currentIndex))
                break
            }
            append(cleanSnippet.substring(currentIndex, startIdx))
            val endIdx = cleanSnippet.indexOf(markEndTag, startIdx + markStartTag.length)
            if (endIdx == -1) {
                append(cleanSnippet.substring(startIdx))
                break
            }
            val matchText = cleanSnippet.substring(startIdx + markStartTag.length, endIdx)
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor))
            append(matchText)
            pop()
            currentIndex = endIdx + markEndTag.length
        }
    }
}

// Pre-compiled markdown-stripping regexes. Previously stripMarkdown()
// allocated 7 fresh Regex objects per call per visible card, which ran on
// every recomposition (every scroll frame / every keystroke) and caused
// noticeable GC pressure and frame drops on long search lists. Compiling
// them once at file scope (matching the pattern HomeScreen uses) removes
// that per-frame cost entirely.
private val searchMdHeadingRegex = Regex("(?m)^#+\\s+")
private val searchMdBlockquoteRegex = Regex("(?m)^[\\s*+-]*>\\s*")
private val searchMdListItemRegex = Regex("(?m)^[\\s]*[*+-]\\s+")
private val searchMdEmphasisRegex = Regex("\\*\\*|__|\\*|_|~~")
private val searchMdInlineCodeRegex = Regex("`+")
private val searchMdLinkRegex = Regex("\\[(.*?)\\]\\(.*?\\)")
private val searchMdImageRegex = Regex("!\\[(.*?)\\]\\(.*?\\)")

private fun stripMarkdown(text: String): String {
    var clean = text
    clean = clean.replace(searchMdHeadingRegex, "")
    clean = clean.replace(searchMdBlockquoteRegex, "")
    clean = clean.replace(searchMdListItemRegex, "")
    clean = clean.replace(searchMdEmphasisRegex, "")
    clean = clean.replace(searchMdInlineCodeRegex, "")
    clean = clean.replace(searchMdLinkRegex, "$1")
    clean = clean.replace(searchMdImageRegex, "$1")
    return clean
}
