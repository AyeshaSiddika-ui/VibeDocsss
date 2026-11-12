package com.vibe.docsnotes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class NoteDoc(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1A73E8),
                    background = Color(0xFFF8F9FA),
                    surface = Color.White
                )
            ) {
                DocsNotesApp()
            }
        }
    }
}

@Composable
fun DocsNotesApp() {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(loadNotes(context)) }
    var activeNoteId by remember { mutableStateOf<String?>(null) }

    val activeNote = notes.find { it.id == activeNoteId }

    if (activeNote == null) {
        NotesHomeScreen(
            notes = notes,
            onOpenNote = { activeNoteId = it.id },
            onCreateNote = {
                val newNote = NoteDoc(
                    title = "Untitled Document",
                    content = "# Welcome to VibeDocs\n\nStart typing with live commands:\n- **Bold text** with `**text**`\n- *Italic text* with `*text*`\n- ==Highlighted text== with `==text==`\n- ~~Strikethrough~~ with `~~text~~`\n- `Inline code` with backticks\n- [ ] Interactive checklist items"
                )
                val updated = listOf(newNote) + notes
                notes = updated
                saveNotes(context, updated)
                activeNoteId = newNote.id
            },
            onDeleteNote = { noteToDelete ->
                val updated = notes.filterNot { it.id == noteToDelete.id }
                notes = updated
                saveNotes(context, updated)
            }
        )
    } else {
        DocEditorScreen(
            note = activeNote,
            onBack = { activeNoteId = null },
            onUpdateNote = { updatedTitle, updatedContent ->
                val updatedList = notes.map {
                    if (it.id == activeNote.id) {
                        it.copy(title = updatedTitle, content = updatedContent, updatedAt = System.currentTimeMillis())
                    } else it
                }
                notes = updatedList
                saveNotes(context, updatedList)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesHomeScreen(
    notes: List<NoteDoc>,
    onOpenNote: (NoteDoc) -> Unit,
    onCreateNote: () -> Unit,
    onDeleteNote: (NoteDoc) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF1A73E8))
                        Spacer(Modifier.width(8.dp))
                        Text("VibeDocs", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNote,
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "New Doc") },
                text = { Text("New Doc") }
            )
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tap + New Doc to start writing", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { doc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenNote(doc) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title.ifBlank { "Untitled Document" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(4.dp))
                                val dateStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(doc.updatedAt))
                                Text("Edited $dateStr", fontSize = 12.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { onDeleteNote(doc) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF9AA0A6))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEditorScreen(
    note: NoteDoc,
    onBack: () -> Unit,
    onUpdateNote: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(note.content)) }
    var showCheatSheet by remember { mutableStateOf(false) }

    val liveVisualTransformation = remember {
        VisualTransformation { text ->
            TransformedText(
                text = parseLiveDocsText(text.text),
                offsetMapping = OffsetMapping.Identity
            )
        }
    }

    fun applyWrapSyntax(prefix: String, suffix: String = prefix) {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val selectedText = text.substring(selection.min, selection.max)
        val replacement = "$prefix$selectedText$suffix"
        val newText = text.replaceRange(selection.min, selection.max, replacement)
        val newCursor = if (selectedText.isEmpty()) {
            selection.min + prefix.length
        } else {
            selection.min + replacement.length
        }
        textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        onUpdateNote(title, newText)
    }

    fun applyLinePrefix(prefix: String) {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.min
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        textFieldValue = TextFieldValue(text = newText, selection = TextRange(cursor + prefix.length))
        onUpdateNote(title, newText)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    BasicTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            onUpdateNote(title, textFieldValue.text)
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F1F1F)),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Saved",
                        tint = Color(0xFF1E8E3E),
                        modifier = Modifier.size(20.dp)
                    )
                    IconButton(onClick = { showCheatSheet = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Syntax Guide")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Google Docs Style Formatting Toolbar
            Surface(
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolbarChip("H1") { applyLinePrefix("# ") }
                    ToolbarChip("H2") { applyLinePrefix("## ") }
                    ToolbarChip("B", bold = true) { applyWrapSyntax("**") }
                    ToolbarChip("I") { applyWrapSyntax("*") }
                    ToolbarChip("U / S") { applyWrapSyntax("~~") }
                    ToolbarChip("Highlight") { applyWrapSyntax("==") }
                    ToolbarChip("Code") { applyWrapSyntax("`") }
                    ToolbarChip("Bullet") { applyLinePrefix("- ") }
                    ToolbarChip("Task") { applyLinePrefix("- [ ] ") }
                    ToolbarChip("Quote") { applyLinePrefix("> ") }
                }
            }
        }
    ) { padding ->
        // Paper Document Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F3F4))
                .padding(padding)
                .padding(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(2.dp, RoundedCornerShape(8.dp)),
                color = Color.White,
                shape = RoundedCornerShape(8.dp)
            ) {
                val wordCount = remember(textFieldValue.text) {
                    textFieldValue.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            onUpdateNote(title, it.text)
                        },
                        visualTransformation = liveVisualTransformation,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Color(0xFF202124)
                        ),
                        cursorBrush = SolidColor(Color(0xFF1A73E8)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        decorationBox = { innerTextField ->
                            if (textFieldValue.text.isEmpty()) {
                                Text("Type # for heading, **bold**, *italic*...", color = Color.LightGray)
                            }
                            innerTextField()
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$wordCount words • Live Markdown Active",
                        fontSize = 11.sp,
                        color = Color(0xFF80868B)
                    )
                }
            }
        }
    }

    if (showCheatSheet) {
        AlertDialog(
            onDismissRequest = { showCheatSheet = false },
            title = { Text("Command Cheat Sheet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("# Heading 1   |   ## Heading 2")
                    Text("**Bold Text**   |   *Italic Text*")
                    Text("***Bold & Italic***")
                    Text("==Highlighted Text==")
                    Text("~~Strikethrough~~")
                    Text("`Inline Code`")
                    Text("- Bullet List   |   - [ ] Task Item")
                    Text("> Blockquote")
                }
            },
            confirmButton = {
                TextButton(onClick = { showCheatSheet = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun ToolbarChip(label: String, bold: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F3F4)
    ) {
        Text(
            text = label,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            color = Color(0xFF3C4043),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

private fun saveNotes(context: Context, notes: List<NoteDoc>) {
    val prefs = context.getSharedPreferences("vibedocs_prefs", Context.MODE_PRIVATE)
    val jsonArray = JSONArray()
    notes.forEach { note ->
        val obj = JSONObject()
        obj.put("id", note.id)
        obj.put("title", note.title)
        obj.put("content", note.content)
        obj.put("updatedAt", note.updatedAt)
        jsonArray.put(obj)
    }
    prefs.edit().putString("notes_json", jsonArray.toString()).apply()
}

private fun loadNotes(context: Context): List<NoteDoc> {
    val prefs = context.getSharedPreferences("vibedocs_prefs", Context.MODE_PRIVATE)
    val raw = prefs.getString("notes_json", null)
    if (raw.isNullOrEmpty()) {
        return listOf(
            NoteDoc(
                title = "Getting Started Guide",
                content = "# VibeDocs Notes\n\nWelcome to your **Google Docs style** markdown editor!\n\n## Supported Commands\n- *Italic text* using `*text*`\n- **Bold text** using `**text**`\n- ***Bold & Italic*** using `***text***`\n- ==Highlight== using `==text==`\n- ~~Strikethrough~~ using `~~text~~`\n- `Code snippet` using backticks\n- [ ] Checklist item using `- [ ] `\n> Blockquote using `> `"
            )
        )
    }
    val arr = JSONArray(raw)
    val list = mutableListOf<NoteDoc>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
            NoteDoc(
                id = obj.getString("id"),
                title = obj.getString("title"),
                content = obj.getString("content"),
                updatedAt = obj.getLong("updatedAt")
            )
        )
    }
    return list
}
