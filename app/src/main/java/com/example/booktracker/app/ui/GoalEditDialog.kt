package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

enum class GoalType(val label: String) {
    YEARLY_BOOKS("Yearly Books"),
    MONTHLY_PAGES("Monthly Pages"),
    MONTHLY_BOOKS("Monthly Books"),
    DAILY_PAGES("Daily Pages")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GoalEditDialog(
    initialType: GoalType = GoalType.YEARLY_BOOKS,
    currentYearlyGoal: Int,
    currentMonthlyPagesGoal: Int,
    currentMonthlyBooksGoal: Int,
    currentDailyGoal: Int,
    onSaveYearlyGoal: (Int) -> Unit,
    onSaveMonthlyPagesGoal: (Int) -> Unit,
    onSaveMonthlyBooksGoal: (Int) -> Unit,
    onSaveDailyGoal: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }

    var yearlyVal by remember { mutableIntStateOf(currentYearlyGoal) }
    var monthlyPagesVal by remember { mutableIntStateOf(currentMonthlyPagesGoal) }
    var monthlyBooksVal by remember { mutableIntStateOf(currentMonthlyBooksGoal) }
    var dailyVal by remember { mutableIntStateOf(currentDailyGoal) }

    val currentVal = when (selectedType) {
        GoalType.YEARLY_BOOKS -> yearlyVal
        GoalType.MONTHLY_PAGES -> monthlyPagesVal
        GoalType.MONTHLY_BOOKS -> monthlyBooksVal
        GoalType.DAILY_PAGES -> dailyVal
    }

    val updateCurrentVal: (Int) -> Unit = { newVal ->
        val safe = newVal.coerceAtLeast(1)
        when (selectedType) {
            GoalType.YEARLY_BOOKS -> yearlyVal = safe
            GoalType.MONTHLY_PAGES -> monthlyPagesVal = safe
            GoalType.MONTHLY_BOOKS -> monthlyBooksVal = safe
            GoalType.DAILY_PAGES -> dailyVal = safe
        }
    }

    val presets = when (selectedType) {
        GoalType.YEARLY_BOOKS -> listOf(6, 12, 24, 36, 52)
        GoalType.MONTHLY_PAGES -> listOf(200, 300, 500, 800, 1000)
        GoalType.MONTHLY_BOOKS -> listOf(1, 2, 3, 4, 5)
        GoalType.DAILY_PAGES -> listOf(10, 20, 30, 50)
    }

    val stepSmall = when (selectedType) {
        GoalType.MONTHLY_PAGES -> 25
        else -> 1
    }

    val unitLabel = when (selectedType) {
        GoalType.YEARLY_BOOKS -> "books / year"
        GoalType.MONTHLY_PAGES -> "pages / month"
        GoalType.MONTHLY_BOOKS -> "books / month"
        GoalType.DAILY_PAGES -> "pages / day"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("Reading Targets", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Goal type tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedType.ordinal,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    GoalType.entries.forEach { type ->
                        Tab(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            text = {
                                Text(
                                    type.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Big Stepper Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "$currentVal",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            unitLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = { updateCurrentVal(currentVal - stepSmall) },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                            }

                            FilledTonalIconButton(
                                onClick = { updateCurrentVal(currentVal + stepSmall) },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Presets
                Text(
                    "Quick Presets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = currentVal == preset,
                            onClick = { updateCurrentVal(preset) },
                            label = { Text("$preset") },
                            shape = CircleShape
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveYearlyGoal(yearlyVal)
                    onSaveMonthlyPagesGoal(monthlyPagesVal)
                    onSaveMonthlyBooksGoal(monthlyBooksVal)
                    onSaveDailyGoal(dailyVal)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
