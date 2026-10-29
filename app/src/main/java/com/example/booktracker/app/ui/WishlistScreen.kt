package com.example.booktracker.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.WishlistEngine
import com.example.booktracker.app.ui.animations.swipeToAct
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.PurchaseLog
import com.example.booktracker.shared.models.WishInfo
import com.example.booktracker.shared.models.WishPriority
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Currency

private val releaseFormat = DateTimeFormatter.ofPattern("MMM d")
private val releaseYearFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

private enum class WishFilter(val label: String) {
    ALL("All"), MUST("Must have"), SERIES("In a series"), SOON("Coming soon"), FITS("Fits the budget")
}

/**
 * The buying side of the library: what's wished for, what it would cost against
 * this month's budget, what a series is missing, and "Got it" to move a book
 * onto the shelf with its purchase logged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    books: List<Book>,
    ownerName: String,
    onBack: () -> Unit,
    onOpenBook: (Book) -> Unit,
    onSetWish: (Book, WishInfo) -> Unit,
    onGotIt: (Book, PurchaseLog?) -> Unit,
    onRemove: (Book) -> Unit,
    onSetBudget: (Int) -> Unit,
    /** Opens the book search, with [query] already typed when there is one. */
    onSearch: (query: String) -> Unit
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val currency = remember { defaultCurrencyCode() }
    val budget = LocalAppPrefs.current.monthlyBookBudget

    var sortName by rememberSaveable { mutableStateOf(WishlistEngine.Sort.PRIORITY.name) }
    val sort = WishlistEngine.Sort.valueOf(sortName)
    var filterName by rememberSaveable { mutableStateOf(WishFilter.ALL.name) }
    val filter = WishFilter.valueOf(filterName)
    var sortMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Book?>(null) }
    var gettingIt by remember { mutableStateOf<Book?>(null) }
    var editingBudget by remember { mutableStateOf(false) }

    val wished = remember(books) { WishlistEngine.wishlist(books) }
    val total = remember(wished) { WishlistEngine.total(wished) }
    val spent = remember(books) { WishlistEngine.spentIn(books, YearMonth.now(zone), zone)[currency] ?: 0.0 }
    val left = (budget - spent).coerceAtLeast(0.0)
    val gaps = remember(books) { WishlistEngine.seriesGaps(books).take(3) }
    val shown = remember(wished, books, sort, filter, left) {
        WishlistEngine.sorted(
            wished.filter { book ->
                val wish = WishlistEngine.info(book)
                when (filter) {
                    WishFilter.ALL -> true
                    WishFilter.MUST -> wish.priority == WishPriority.MUST
                    WishFilter.SERIES -> WishlistEngine.inStartedSeries(book, books)
                    WishFilter.SOON -> WishlistEngine.isUpcoming(book, today, zone)
                    WishFilter.FITS -> wish.currency.ifBlank { currency } == currency && (wish.price?.let { it <= left } == true)
                }
            },
            sort
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wishlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            WishlistEngine.Sort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    trailingIcon = { if (option == sort) Icon(Icons.Filled.Check, contentDescription = "Selected") },
                                    onClick = {
                                        sortName = option.name
                                        sortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, WishlistEngine.shareText(WishlistEngine.sorted(wished, WishlistEngine.Sort.PRIORITY), ownerName))
                            }
                            context.safeStartActivity(Intent.createChooser(send, "Share your wishlist"))
                        },
                        enabled = wished.isNotEmpty()
                    ) { Icon(Icons.Filled.Share, contentDescription = "Share list") }
                    IconButton(onClick = { onSearch("") }) { Icon(Icons.Filled.Add, contentDescription = "Add a book") }
                }
            )
        }
    ) { padding ->
        val side = 16.dp + windowCentredPadding()
        LazyColumn(
            contentPadding = PaddingValues(start = side, end = side, top = padding.calculateTopPadding(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "summary") {
                Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    Text(
                        "${total.count} ${if (total.count == 1) "book" else "books"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (total.priced > 0) {
                        Text(
                            buildString {
                                append("About ")
                                append(total.byCurrency.entries.joinToString(" + ") { (code, sum) -> money(sum, code.ifBlank { currency }) })
                                val unpriced = total.count - total.priced
                                if (unpriced > 0) append(" · $unpriced without a price")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "budget") {
                BudgetCard(budget = budget, spent = spent, currency = currency, onEdit = { editingBudget = true })
            }

            if (gaps.isNotEmpty()) {
                item(key = "series") {
                    SeriesGapsCard(gaps = gaps, onFind = { gap -> onSearch("${gap.seriesName} book ${gap.nextPosition}") })
                }
            }

            if (wished.isNotEmpty()) {
                item(key = "filters") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        WishFilter.entries.filter { it != WishFilter.FITS || budget > 0 }.forEach { option ->
                            TickFilterChip(
                                selected = filter == option,
                                onClick = { filterName = option.name },
                                label = { Text(option.label) }
                            )
                        }
                    }
                }
            }

            if (shown.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.BookmarkAdd,
                        title = if (wished.isEmpty()) "Nothing on your wishlist" else "Nothing matches",
                        message = if (wished.isEmpty()) {
                            "Search for a book and choose Wishlist, long-press any cover, or set a book's ownership to Wishlist."
                        } else "Try another filter.",
                        action = if (wished.isEmpty()) {
                            { Button(onClick = { onSearch("") }) { Text("Find a book") } }
                        } else null,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(shown, key = { it.id }) { book ->
                    WishRow(
                        book = book,
                        today = today,
                        onOpen = { onOpenBook(book) },
                        onGotIt = { gettingIt = book },
                        onEdit = { editing = book },
                        onFind = {
                            context.safeStartActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(WishlistEngine.findQuery(book))))
                            )
                        },
                        onRemove = { onRemove(book) },
                        modifier = Modifier.animateItem()
                    )
                }
                item(key = "hint") {
                    Text(
                        "Swipe right when you've got it, left to remove",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    editing?.let { book ->
        WishEditorDialog(
            title = book.title,
            initial = WishlistEngine.info(book),
            onDismiss = { editing = null },
            onSave = {
                onSetWish(book, it)
                editing = null
            }
        )
    }
    gettingIt?.let { book ->
        GotItFlow(book = book, onDismiss = { gettingIt = null }, onGotIt = { purchase ->
            onGotIt(book, purchase)
            gettingIt = null
        })
    }
    if (editingBudget) {
        BudgetDialog(
            current = budget,
            currency = currency,
            onDismiss = { editingBudget = false },
            onSave = {
                onSetBudget(it)
                editingBudget = false
            }
        )
    }
}

/** "₹5,200" in the phone's style; the code itself when the currency is unknown. */
internal fun money(amount: Double, code: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(code)
        maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    }.format(amount)
}.getOrElse { "$code ${"%.2f".format(amount)}" }

@Composable
private fun BudgetCard(budget: Int, spent: Double, currency: String, onEdit: () -> Unit) {
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (budget <= 0) {
                    Text("Set a monthly book budget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Spent this month: ${money(spent, currency)}. A budget shows what's left for the wishlist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val over = spent > budget
                    Text(
                        "${money(spent, currency)} of ${money(budget.toDouble(), currency)} this month",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (spent / budget).toFloat().coerceIn(0f, 1f) },
                        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (over) "${money(spent - budget, currency)} over budget" else "${money(budget - spent, currency)} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesGapsCard(gaps: List<WishlistEngine.SeriesGap>, onFind: (WishlistEngine.SeriesGap) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Text("Next in your series", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            gaps.forEach { gap ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${gap.seriesName} #${gap.nextPosition}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "After ${gap.lastRead.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilledTonalButton(onClick = { onFind(gap) }) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Find it")
                    }
                }
            }
        }
    }
}

@Composable
private fun WishRow(
    book: Book,
    today: LocalDate,
    onOpen: () -> Unit,
    onGotIt: () -> Unit,
    onEdit: () -> Unit,
    onFind: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wish = WishlistEngine.info(book)
    val zone = remember { ZoneId.systemDefault() }
    var menu by remember { mutableStateOf(false) }
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth().swipeToAct(onSwipeRight = onGotIt, onSwipeLeft = onRemove)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            BookCover(book.coverUrl, Modifier.size(width = 56.dp, height = 84.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    PriorityPill(wish.priority)
                }
                book.authors.firstOrNull()?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val buying = listOfNotNull(
                    wish.price?.let { "~${money(it, wish.currency.ifBlank { defaultCurrencyCode() })}" },
                    wish.store.takeIf { it.isNotBlank() }
                )
                if (buying.isNotEmpty()) {
                    Text(buying.joinToString(" · "), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                wish.releaseDate?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                    Text(
                        when {
                            date.isAfter(today) -> "Out ${date.format(if (date.year == today.year) releaseFormat else releaseYearFormat)}"
                            date == today -> "Out today"
                            else -> "Out now"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val why = listOfNotNull(
                    wish.recommendedBy.takeIf { it.isNotBlank() }?.let { "Recommended by $it" },
                    wish.note.takeIf { it.isNotBlank() }
                )
                if (why.isNotEmpty()) {
                    Text(
                        why.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = onGotIt, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), modifier = Modifier.height(36.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Got it")
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit details") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = { menu = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("Find it online") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                onClick = { menu = false; onFind() }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove from wishlist", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; onRemove() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityPill(priority: WishPriority) {
    val strong = priority == WishPriority.MUST
    Surface(
        shape = RoundedCornerShape(50),
        color = if (strong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (strong) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(priority.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

/** Priority, price, store, who recommended it, a note and the release day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WishEditorDialog(
    title: String,
    initial: WishInfo,
    onDismiss: () -> Unit,
    onSave: (WishInfo) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    var priority by remember { mutableStateOf(initial.priority) }
    var price by remember { mutableStateOf(initial.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()) }
    var currency by remember { mutableStateOf(initial.currency.ifBlank { defaultCurrencyCode() }) }
    var store by remember { mutableStateOf(initial.store) }
    var recommendedBy by remember { mutableStateOf(initial.recommendedBy) }
    var note by remember { mutableStateOf(initial.note) }
    var release by remember { mutableStateOf(initial.releaseDate) }
    var pickingDate by remember { mutableStateOf(false) }
    val parsedPrice = price.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wishlist details", maxLines = 1) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WishPriority.entries.forEach { option ->
                        FilterChip(
                            selected = priority == option,
                            onClick = { priority = option },
                            label = { Text(option.label, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { text -> price = text.filter { it.isDigit() || it == '.' || it == ',' }.take(10) },
                        label = { Text("Expected price") },
                        singleLine = true,
                        isError = price.isNotEmpty() && parsedPrice == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { text -> currency = text.filter(Char::isLetter).uppercase().take(3) },
                        label = { Text("Currency") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.width(110.dp)
                    )
                }
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it.take(60) },
                    label = { Text("Where to buy") },
                    placeholder = { Text("Amazon, a local shop…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = recommendedBy,
                    onValueChange = { recommendedBy = it.take(60) },
                    label = { Text("Recommended by") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(280) },
                    label = { Text("Why you want it") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    onClick = { pickingDate = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            release?.let { "Comes out ${Instant.ofEpochMilli(it).atZone(zone).toLocalDate().format(releaseYearFormat)}" } ?: "Not out yet? Add the release day",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (release != null) {
                            TextButton(onClick = { release = null }) { Text("Clear") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = price.isEmpty() || parsedPrice != null,
                onClick = {
                    onSave(
                        initial.copy(
                            priority = priority,
                            price = parsedPrice,
                            currency = currency,
                            store = store.trim(),
                            recommendedBy = recommendedBy.trim(),
                            note = note.trim(),
                            releaseDate = release
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (release?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: LocalDate.now(zone).plusWeeks(1))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker speaks UTC midnight; stored as the start of that local day.
                    state.selectedDateMillis?.let { millis ->
                        release = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }
}

/** "Got it": bought (log the purchase, prefilled from the wishlist) or a gift. */
@Composable
internal fun GotItFlow(book: Book, onDismiss: () -> Unit, onGotIt: (PurchaseLog?) -> Unit) {
    var buying by remember { mutableStateOf(false) }
    val wish = WishlistEngine.info(book)
    if (!buying) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Got it!") },
            text = { Text("How did “${book.title}” reach your shelf? It moves off the wishlist either way.") },
            confirmButton = { TextButton(onClick = { buying = true }) { Text("I bought it") } },
            dismissButton = { TextButton(onClick = { onGotIt(null) }) { Text("It was a gift") } }
        )
    } else {
        PurchaseEditorDialog(
            initial = PurchaseLog(
                date = System.currentTimeMillis(),
                vendor = wish.store,
                price = wish.price ?: 0.0,
                currency = wish.currency.ifBlank { defaultCurrencyCode() },
                memo = ""
            ),
            title = "Log the purchase",
            onDismiss = onDismiss,
            onSave = onGotIt
        )
    }
}

@Composable
private fun BudgetDialog(current: Int, currency: String, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.takeIf { it > 0 }?.toString().orEmpty()) }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly book budget") },
        text = {
            Column {
                Text(
                    "What you mean to spend on books each month. Purchases you log count against it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(7) },
                    label = { Text("Budget ($currency)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value ?: 0) }) { Text("Save") } },
        dismissButton = {
            Row {
                if (current > 0) TextButton(onClick = { onSave(0) }) { Text("No budget") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
