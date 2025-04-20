// MainActivity.kt
package com.west.jnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.west.jnote.ui.theme.JnoteTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JnoteTheme {
                NotesApp()
            }
        }
    }
}

@Composable
fun NotesApp() {
    val navController = rememberNavController()
    val (folders, setFolders) = remember { mutableStateOf(listOf(Folder(id = "1", name = "Notes"))) }
    val notesMap = remember { mutableStateMapOf<String, List<Note>>() }

    NavHost(navController = navController, startDestination = "folders") {
        composable("folders") {
            FoldersScreen(
                navController = navController,
                folders = folders,
                notesMap = notesMap,
                onAddFolder = { newFolder ->
                    setFolders(folders + newFolder)
                },
                onDeleteFolder = { folderToDelete ->
                    setFolders(folders.filter { it.id != folderToDelete.id })
                    notesMap.remove(folderToDelete.id)
                }
            )
        }
        composable("notes/{folderId}") { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: "1"
            val folder = folders.firstOrNull { it.id == folderId } ?: folders[0]
            NotesScreen(
                navController = navController,
                folder = folder,
                notes = notesMap.getOrPut(folder.id) { mutableStateListOf() },
                onAddNote = { newNote ->
                    notesMap[folder.id] = (notesMap[folder.id] ?: emptyList()) + newNote
                },
                onDeleteNote = { noteToDelete ->
                    notesMap[folder.id] = (notesMap[folder.id] ?: emptyList()).filter { it.id != noteToDelete.id }
                }
            )
        }
        composable("note_editor/{folderId}/{noteId}") { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: "1"
            val noteId = backStackEntry.arguments?.getString("noteId")
            NoteEditorScreen(
                navController = navController,
                folderId = folderId,
                noteId = noteId,
                notesMap = notesMap,
                onSaveNote = { updatedNote ->
                    notesMap[folderId] = (notesMap[folderId] ?: emptyList()).map {
                        if (it.id == updatedNote.id) updatedNote else it
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    navController: NavHostController,
    folders: List<Folder>,
    notesMap: Map<String, List<Note>>,
    onAddFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { showAddFolderDialog = true }) {
                        Icon(Icons.Default.Create, contentDescription = "Add Folder")
                    }
                }
            )
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                LazyColumn {
                    items(folders) { folder ->
                        val noteCount = notesMap[folder.id]?.size ?: 0
                        FolderItem(
                            folder = folder.copy(count = noteCount),
                            onClick = {
                                navController.navigate("notes/${folder.id}")
                            },
                            onDelete = {
                                folderToDelete = folder
                            }
                        )
                    }
                }
            }
        }
    )

    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onAddFolder(Folder(
                                id = UUID.randomUUID().toString(),
                                name = newFolderName
                            ))
                            newFolderName = ""
                            showAddFolderDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddFolderDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder") },
            text = { Text("Are you sure you want to delete '${folder.name}' and all its notes?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFolder(folder)
                        folderToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { folderToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FolderItem(
    folder: Folder,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MailOutline,
            contentDescription = "Folder",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = folder.name,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            fontSize = 16.sp
        )
        Text(
            text = folder.count.toString(),
            color = Color.Gray,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Folder",
                tint = Color.Red
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavHostController,
    folder: Folder,
    notes: List<Note>,
    onAddNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notes yet. Tap + to create one.")
                    }
                } else {
                    LazyColumn {
                        items(notes) { note ->
                            NoteItem(
                                note = note,
                                onClick = {
                                    navController.navigate("note_editor/${folder.id}/${note.id}")
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val newNote = Note(
                        id = UUID.randomUUID().toString(),
                        title = "",
                        content = "",
                        date = currentDate
                    )
                    onAddNote(newNote)
                    navController.navigate("note_editor/${folder.id}/${newNote.id}")
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
            }
        }
    )
}

@Composable
fun NoteItem(note: Note, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        if (note.title.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isChecked) {
                    Checkbox(
                        checked = note.isChecked,
                        onCheckedChange = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = note.title,
                    fontWeight = if (note.isChecked) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        if (note.content.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.content,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = note.date,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
    Divider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    navController: NavHostController,
    folderId: String,
    noteId: String?,
    notesMap: Map<String, List<Note>>,
    onSaveNote: (Note) -> Unit
) {
    val folderNotes = notesMap[folderId] ?: emptyList()
    val existingNote = noteId?.let { id -> folderNotes.firstOrNull { it.id == id } }

    // Combine title and content for editing
    val initialText = remember(existingNote) {
        val note = existingNote ?: return@remember ""
        if (note.content.isEmpty()) note.title else "${note.title}\n${note.content}"
    }

    var text by remember { mutableStateOf(initialText) }
    val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(existingNote?.date ?: currentDate) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (text.isNotEmpty()) {
                            // Split text into title and content
                            val lines = text.lines()
                            val title = lines.firstOrNull() ?: ""
                            val content = if (lines.size > 1) lines.drop(1).joinToString("\n") else ""

                            val updatedNote = Note(
                                id = existingNote?.id ?: UUID.randomUUID().toString(),
                                title = title,
                                content = content,
                                date = currentDate,
                                isChecked = existingNote?.isChecked ?: false
                            )
                            onSaveNote(updatedNote)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    placeholder = { Text("Start typing...") }
                )
            }
        }
    )
}

// Data classes
data class Folder(
    val id: String,
    val name: String,
    val count: Int = 0
)

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val date: String,
    val isChecked: Boolean = false
)