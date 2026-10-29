package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.booktracker.shared.models.PurchaseLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** The currency of the phone's region (INR in India), so most purchases need no typing. */
fun defaultCurrencyCode(): String =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")

private val purchaseDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

fun formatPurchaseDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(purchaseDateFormat)

fun formatPrice(log: PurchaseLog): String = "${log.currency} ${"%.2f".format(log.price)}"

/**
 * Add or edit one purchase: where, how much, in which currency, when, and a
 * note. Everything but the price is optional, and the store can be left blank
 * for a gift card or a second-hand find.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseEditorDialog(
    initial: PurchaseLog?,
    onDismiss: () -> Unit,
    onSave: (PurchaseLog) -> Unit,
    onDelete: (() -> Unit)? = null,
    /** A prefilled new purchase (from the wishlist) isn't an edit, so it says so. */
    title: String = if (initial == null) "Add purchase" else "Edit purchase"
) {
    var store by remember { mutableStateOf(initial?.vendor.orEmpty()) }
    var price by remember { mutableStateOf(initial?.price?.takeIf { it > 0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()) }
    var currency by remember { mutableStateOf(initial?.currency?.takeIf { it.isNotBlank() } ?: defaultCurrencyCode()) }
    var date by remember { mutableLongStateOf(initial?.date ?: System.currentTimeMillis()) }
    var note by remember { mutableStateOf(initial?.memo.orEmpty()) }
    var pickingDate by remember { mutableStateOf(false) }
    val parsedPrice = price.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it.take(60) },
                    label = { Text("Store (optional)") },
                    placeholder = { Text("Crossword, Amazon, a friend…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { text -> price = text.filter { it.isDigit() || it == '.' || it == ',' }.take(10) },
                        label = { Text("Price") },
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
                Surface(
                    shape = AppShape.S,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    onClick = { pickingDate = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Bought on", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(formatPurchaseDate(date), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Signed copy, birthday gift, used…") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete this purchase")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedPrice != null && currency.length == 3,
                onClick = {
                    onSave(PurchaseLog(date = date, vendor = store.trim(), price = parsedPrice ?: 0.0, currency = currency, memo = note.trim()))
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (pickingDate) {
        // The picker works in UTC midnights; convert at the edges so the day never shifts.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        date = Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate()
                            .atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

/**
 * A list of purchases with add and tap-to-edit, for the add-book dialogs.
 * Entirely optional: with nothing added it is a single quiet row.
 */
@Composable
fun PurchaseSection(
    purchases: List<PurchaseLog>,
    onChange: (List<PurchaseLog>) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<Int?>(null) } // index, or -1 for a new one
    editing?.let { index ->
        PurchaseEditorDialog(
            initial = purchases.getOrNull(index),
            onDismiss = { editing = null },
            onSave = { log ->
                onChange(if (index in purchases.indices) purchases.toMutableList().also { it[index] = log } else purchases + log)
                editing = null
            },
            onDelete = if (index in purchases.indices) {
                { onChange(purchases.filterIndexed { i, _ -> i != index }); editing = null }
            } else null
        )
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Purchase", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Text("optional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        purchases.forEachIndexed { index, log ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editing = index }
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(log.vendor.ifBlank { "Purchase" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(formatPurchaseDate(log.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatPrice(log), style = MaterialTheme.typography.bodyMedium)
            }
        }
        TextButton(onClick = { editing = -1 }, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (purchases.isEmpty()) "Add where and what you paid" else "Add another purchase")
        }
    }
}
