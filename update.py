import re

with open('app/src/main/java/com/example/booktracker/app/ui/BookTrackerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add TimerMiniPlayer to imports if not there
if 'import com.example.booktracker.app.ui.TimerPhase' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Timer', 'import androidx.compose.material.icons.filled.Timer\nimport androidx.compose.material.icons.filled.Pause\nimport androidx.compose.material.icons.filled.Favorite\nimport com.example.booktracker.app.ui.TimerPhase')

# 2. Modify NavHost wrapping
old_navhost = r'''    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {'''
new_navhost = r'''    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {'''
content = content.replace(old_navhost, new_navhost)

# 3. Add composables to NavHost
old_search = r'''            composable("search") {
                LibrarySearchScreen(
                    onSearch = viewModel::searchLibrary,
                    onOpenBook = { bookId -> navController.navigate("book/$bookId") },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }'''

new_search = r'''            composable("search") {
                LibrarySearchScreen(
                    onSearch = viewModel::searchLibrary,
                    onOpenBook = { bookId -> navController.navigate("book/$bookId") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("focus/{bookId}") { entry ->
                val bookId = entry.arguments?.getString("bookId")
                if (bookId != null) {
                    val book = books.find { it.id == bookId }
                    if (book != null) {
                        FocusTimerScreen(
                            viewModel = viewModel,
                            book = book,
                            onBack = { navController.popBackStack() },
                            onStartSession = viewModel::startSession
                        )
                    }
                }
            }
            composable("tbr_swipe") {
                val tbrBooks by viewModel.tbrCurationBooks.collectAsState()
                TbrSwipeScreen(
                    books = tbrBooks,
                    onPromote = { b -> viewModel.promote(b) },
                    onStartReading = { b -> viewModel.promoteToReading(b) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        
        val activeTimerBook by viewModel.activeTimerBook.collectAsState()
        val timerIsRunning by viewModel.timerIsRunning.collectAsState()
        val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
        val timerPhase by viewModel.timerPhase.collectAsState()
        
        if (activeTimerBook != null && currentRoute != "focus/{bookId}") {
            TimerMiniPlayer(
                book = activeTimerBook!!,
                isRunning = timerIsRunning,
                timeLeftSeconds = timeLeftSeconds,
                phase = timerPhase,
                onPlayPause = { viewModel.setTimerRunning(!timerIsRunning) },
                onOpenFullTimer = { navController.navigate("focus/${activeTimerBook!!.id}") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
    }'''

content = content.replace(old_search, new_search)

# 4. Modify LibraryScreen definition
old_lib_def = r'''fun LibraryScreen(
    books: List<Book>,
    onPromote: (Book) -> Unit,
    onDelete: (Book) -> Unit,
    onOpenBook: (Book) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf<BookStatus?>(null) }
    
    val displayBooks = books.filter { 
        if (selectedFilter == null) {
            it.status != BookStatus.READING.name && it.status != BookStatus.FINISHED.name && it.status != BookStatus.DNF.name
        } else {
            it.status == selectedFilter!!.name
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {'''

new_lib_def = r'''fun LibraryScreen(
    books: List<Book>,
    onPromote: (Book) -> Unit,
    onDelete: (Book) -> Unit,
    onOpenBook: (Book) -> Unit = {},
    onCurationClick: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf<BookStatus?>(null) }
    
    val displayBooks = books.filter { 
        if (selectedFilter == null) {
            it.status != BookStatus.READING.name && it.status != BookStatus.FINISHED.name && it.status != BookStatus.DNF.name
        } else {
            it.status == selectedFilter!!.name
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {'''

content = content.replace(old_lib_def, new_lib_def)

# 5. Modify LibraryScreen end
old_lib_end = r'''                items(displayBooksWithStatus, key = { it.id }) { book ->
                    LibraryGridCard(
                        book = book,
                        modifier = Modifier.animateItem(),
                        onClick = { onOpenBook(book) },
                        onPromote = { onPromote(book) },
                        onDelete = { onDelete(book) }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryGridCard('''

new_lib_end = r'''                items(displayBooksWithStatus, key = { it.id }) { book ->
                    LibraryGridCard(
                        book = book,
                        modifier = Modifier.animateItem(),
                        onClick = { onOpenBook(book) },
                        onPromote = { onPromote(book) },
                        onDelete = { onDelete(book) }
                    )
                }
            }
        }

        // Curation FAB
        androidx.compose.material3.ExtendedFloatingActionButton(
            onClick = onCurationClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            text = { Text("Curate TBR") }
        )
    }
}

@Composable
fun LibraryGridCard('''

content = content.replace(old_lib_end, new_lib_end)

# 6. Add onCurationClick to LibraryScreen call in BookTrackerApp
old_lib_call = r'''            composable("library") {
                Column(modifier = Modifier.fillMaxSize()) {
                    LibraryScreen(
                        books = books,
                        onPromote = viewModel::promote,
                        onDelete = onDeleteWithUndo,
                        onOpenBook = { navController.navigate("book/${it.id}") }
                    )
                }
            }'''

new_lib_call = r'''            composable("library") {
                LibraryScreen(
                    books = books,
                    onPromote = viewModel::promote,
                    onDelete = onDeleteWithUndo,
                    onOpenBook = { navController.navigate("book/${it.id}") },
                    onCurationClick = { navController.navigate("tbr_swipe") }
                )
            }'''

content = content.replace(old_lib_call, new_lib_call)

# 7. Add TimerMiniPlayer composable at end
timer_mini_player = r'''

@Composable
fun TimerMiniPlayer(
    book: Book,
    isRunning: Boolean,
    timeLeftSeconds: Int,
    phase: TimerPhase,
    onPlayPause: () -> Unit,
    onOpenFullTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onOpenFullTimer
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (phase == TimerPhase.WORK) "Deep Focus" else "Break",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                val mins = timeLeftSeconds / 60
                val secs = timeLeftSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            FilledTonalIconButton(
                onClick = { onPlayPause() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play"
                )
            }
        }
    }
}
'''
if 'fun TimerMiniPlayer(' not in content:
    content += timer_mini_player

with open('app/src/main/java/com/example/booktracker/app/ui/BookTrackerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
