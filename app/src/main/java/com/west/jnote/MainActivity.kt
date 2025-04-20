// MainActivity.kt
package com.west.jnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
            JnoteTheme(darkTheme = true) { // Force dark theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1C1C1E) // Dark background
                ) {
                    NotesApp()
                }
            }
        }
    }
}

@Composable
fun NotesApp() {
    val navController = rememberNavController()
    val (folders, setFolders) = remember {
        mutableStateOf(listOf(Folder(id = "1", name = "Notes", isDefault = true)))
    }
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
                notes = notesMap.getOrPut(folder.id) { mutableStateListOf() }
                    .filter { it.content.isNotBlank() },
                onAddNote = { newNote ->
                    notesMap[folder.id] = (notesMap[folder.id] ?: emptyList()) + newNote
                },
                onDeleteNote = { noteToDelete ->
                    notesMap[folder.id] = (notesMap[folder.id] ?: emptyList())
                        .filter { it.id != noteToDelete.id }
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
                },
                onDeleteNote = { noteToDelete ->
                    notesMap[folderId] = (notesMap[folderId] ?: emptyList())
                        .filter { it.id != noteToDelete.id }
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
                title = {
                    Text(
                        "Folders",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E), // Dark background
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showAddFolderDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Folder",
                            tint = Color(0xFF0A84FF) // Light blue color for dark theme
                        )
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .background(Color(0xFF1C1C1E)) // Dark background
            ) {
                LazyColumn {
                    items(folders) { folder ->
                        val noteCount = (notesMap[folder.id] ?: emptyList())
                            .count { it.content.isNotBlank() }
                        FolderItem(
                            folder = folder.copy(count = noteCount),
                            onClick = {
                                navController.navigate("notes/${folder.id}")
                            },
                            onDelete = if (!folder.isDefault) {
                                { folderToDelete = folder }
                            } else {
                                null
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
            title = {
                Text(
                    "New Folder",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = {
                        Text(
                            "Folder name",
                            color = Color(0xFF8E8E93) // Light gray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onAddFolder(Folder(
                                id = UUID.randomUUID().toString(),
                                name = newFolderName,
                                isDefault = false
                            ))
                            newFolderName = ""
                            showAddFolderDialog = false
                        }
                    }
                ) {
                    Text(
                        "Add",
                        color = Color(0xFF0A84FF), // Light blue
                        fontSize = 17.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddFolderDialog = false }
                ) {
                    Text(
                        "Cancel",
                        fontSize = 17.sp,
                        color = Color(0xFF0A84FF) // Light blue
                    )
                }
            },
            containerColor = Color(0xFF2C2C2E), // Dark gray
            textContentColor = Color.White
        )
    }

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = {
                Text(
                    "Delete Folder",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete '${folder.name}' and all its notes?",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93) // Light gray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFolder(folder)
                        folderToDelete = null
                    }
                ) {
                    Text(
                        "Delete",
                        color = Color(0xFFFF3B30), // Red
                        fontSize = 17.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { folderToDelete = null }
                ) {
                    Text(
                        "Cancel",
                        fontSize = 17.sp,
                        color = Color(0xFF0A84FF) // Light blue
                    )
                }
            },
            containerColor = Color(0xFF2C2C2E), // Dark gray
            textContentColor = Color.White
        )
    }
}

@Composable
fun FolderItem(
    folder: Folder,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick)
            .background(Color(0xFF2C2C2E)) // Dark gray background
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Folder",
            tint = Color(0xFF0A84FF), // Light blue
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = folder.name,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            color = Color.White
        )
        Text(
            text = folder.count.toString(),
            color = Color(0xFF8E8E93), // Light gray
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Folder",
                    tint = Color(0xFF8E8E93) // Light gray
                )
            }
        }
    }
    Divider(
        color = Color(0xFF38383A), // Dark divider
        thickness = 0.5.dp
    )
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
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        folder.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0A84FF) // Light blue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E), // Dark background
                    titleContentColor = Color.White
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .background(Color(0xFF1C1C1E)) // Dark background
            ) {
                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No notes yet. Tap + to create one.",
                            fontSize = 15.sp,
                            color = Color(0xFF8E8E93) // Light gray
                        )
                    }
                } else {
                    LazyColumn {
                        items(notes) { note ->
                            if (note.content.isNotBlank()) {
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
                containerColor = Color(0xFF0A84FF) // Light blue
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New Note",
                    tint = Color.White
                )
            }
        }
    )
}

@Composable
fun NoteItem(note: Note, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2C2E)) // Dark gray background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val firstLine = note.content.lines().firstOrNull() ?: ""
                if (firstLine.isNotBlank()) {
                    Text(
                        text = firstLine,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = Color.White,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.date,
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93) // Light gray
                    )
                }
            }
        }
        Divider(
            color = Color(0xFF38383A), // Dark divider
            thickness = 0.5.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    navController: NavHostController,
    folderId: String,
    noteId: String?,
    notesMap: Map<String, List<Note>>,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit
) {
    val folderNotes = notesMap[folderId] ?: emptyList()
    val existingNote = noteId?.let { id -> folderNotes.firstOrNull { it.id == id } }

    var text by remember { mutableStateOf(existingNote?.content ?: "") }
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    BackHandler {
        if (text.isBlank()) {
            noteId?.let {
                onDeleteNote(Note(id = it, title = "", content = "", date = ""))
            }
        }
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (text.isNotBlank()) existingNote?.date ?: currentDate else "New Note",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93) // Light gray
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (text.isBlank()) {
                            noteId?.let {
                                onDeleteNote(Note(id = it, title = "", content = "", date = ""))
                            }
                        }
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0A84FF) // Light blue
                        )
                    }
                },
                actions = {
                    if (existingNote != null && text.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onDeleteNote(existingNote)
                                navController.popBackStack()
                            }
                        ) {
                            Text(
                                "Delete",
                                color = Color(0xFFFF3B30), // Red
                                fontSize = 17.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                val firstLine = text.lines().firstOrNull() ?: ""
                                val updatedNote = Note(
                                    id = existingNote?.id ?: UUID.randomUUID().toString(),
                                    title = firstLine,
                                    content = text,
                                    date = currentDate,
                                    isChecked = existingNote?.isChecked ?: false
                                )
                                onSaveNote(updatedNote)
                                navController.popBackStack()
                            }
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Text(
                            "Done",
                            color = if (text.isNotBlank()) Color(0xFF0A84FF) else Color(0xFF8E8E93),
                            fontSize = 17.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E), // Dark background
                    titleContentColor = Color.White
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .background(Color(0xFF2C2C2E)) // Dark gray background
            ){
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            placeholder = {
                                Text(
                                    "Start typing...",
                                    color = Color(0xFF8E8E93) // Light gray
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                color = Color.White
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF0A84FF) // Light blue
                            )
                        )
                    }
        }
    )
}

// Data classes remain the same
data class Folder(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val count: Int = 0
)

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val date: String,
    val isChecked: Boolean = false
)
