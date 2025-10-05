package org.android.tripowe.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.android.tripowe.models.AppRepository
import org.android.tripowe.models.Participant
import org.android.tripowe.models.format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repo: AppRepository = remember { AppRepository() }) {
    val trips by repo.trips.collectAsState()
    val currentTrip by repo.currentTrip.collectAsState()
    val participants by repo.participants.collectAsState()
    val payments by remember { derivedStateOf { repo.getPaymentsByParticipant() } }
    val totalAmount by remember { derivedStateOf { repo.totalAmount } }
    val debtSummary by remember { derivedStateOf { repo.getUserDebtSummary() } }

    // Dynamic color list for pie chart and table legends
    val colors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFF44336), // Red
        Color(0xFFFFC107), // Yellow
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown
        Color(0xFF607D8B), // Blue Grey
        Color(0xFFE91E63)  // Pink
    )

    // Dropdown state
    var expanded by remember { mutableStateOf(false) }
    var showAddTrip by remember { mutableStateOf(false) }
    var newTripName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // Standard MD3 margin
                .semantics { contentDescription = "Trip expense overview" }
        ) {
            // Top row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp), // MD3 spacer
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Modern OutlinedTextField with rounded corners
                Box(modifier = Modifier.weight(2f)) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = currentTrip.name,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Select Trip") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary, // Adaptive
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)) // MD3 standard radius
                                .semantics { contentDescription = "Select trip dropdown" }
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            trips.forEach { trip ->
                                DropdownMenuItem(
                                    text = { Text(trip.name) },
                                    onClick = {
                                        repo.setCurrentTrip(trip.id)
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Add New Trip") },
                                onClick = {
                                    showAddTrip = true
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Settings button
                IconButton(
                    onClick = { /* Navigate to settings */ },
                    modifier = Modifier
                        .size(48.dp) // MD3 min touch target 48.dp
                        .semantics { contentDescription = "Settings" }
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Add trip input
            if (showAddTrip) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp), // MD3 spacer
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTripName,
                        onValueChange = { newTripName = it },
                        label = { Text("New Trip Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp)) // MD3 standard
                            .semantics { contentDescription = "New trip name" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newTripName.isNotBlank()) {
                                repo.addTrip(newTripName)
                                newTripName = ""
                                showAddTrip = false
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Add trip" }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Pie Chart (dynamic height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 200.dp) // Dynamic, responsive to screen
                    .padding(vertical = 8.dp), // MD3 spacer
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(180.dp) // Slightly smaller for better fit
                        .semantics { contentDescription = "Expense pie chart" }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerX = canvasWidth / 2
                    val centerY = canvasHeight / 2
                    val radius = (canvasWidth / 2) - 20

                    val total = totalAmount
                    var startAngle = 0f
                    participants.forEachIndexed { index, participant ->
                        val amount = payments[participant] ?: 0.0
                        if (total > 0) {
                            val sweepAngle = (amount / total * 360f).toFloat()
                            val color = colors[index % colors.size]
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset(centerX - radius, centerY - radius),
                                size = Size(radius * 2, radius * 2)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }
            }

            // Total text
            Text(
                text = "Total ${totalAmount.format(0)}$",
                style = MaterialTheme.typography.headlineSmall, // MD3 typography
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp) // MD3 spacer
                    .semantics { contentDescription = "Total amount: ${totalAmount.format(0)}$" }
            )

            // Debt summary text
            val userBalance = debtSummary.contains("owe you")
            Text(
                text = debtSummary,
                style = MaterialTheme.typography.bodyMedium, // MD3 typography
                color = if (userBalance) MaterialTheme.colorScheme.primary else if (debtSummary.contains("owes")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp) // MD3 spacer
                    .semantics { contentDescription = "Debt summary: $debtSummary" }
            )

            // Payments table (dynamic height, improved spacing)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp), // MD3 margin
                shape = RoundedCornerShape(12.dp), // MD3 standard radius
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // Adaptive surface
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // MD3 depth
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 12.dp) // MD3 padding (16.dp horizontal, 12.dp vertical)
                    ) {
                        Text(
                            "Participant",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium, // MD3 header
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Paid",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        // Info button in the header row, rightmost column
                        IconButton(
                            onClick = { /* TODO: Navigate to info page */ },
                            modifier = Modifier
                                .size(40.dp) // MD3 min touch target
                                .semantics { contentDescription = "More info" }
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(min = 120.dp, max = 200.dp) // Dynamic height
                            .padding(horizontal = 8.dp) // MD3 inner padding
                    ) {
                        items(participants) { participant ->
                            val index = participants.indexOf(participant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp), // MD3 row padding
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(colors[index % colors.size], shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    participant.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${(payments[participant] ?: 0.0).format(0)}$",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button (bottom right, round, payment icon)
        FloatingActionButton(
            onClick = { /* TODO: Implement add payment */ },
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(6.dp) // MD3 elevation
        ) {
            Icon(
                Icons.Default.Payment,
                contentDescription = "Record additional payment",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}