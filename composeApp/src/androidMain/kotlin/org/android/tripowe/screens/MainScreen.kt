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
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
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
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
                            label = { Text("בחר טיול") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0D47A1),
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color(0xFF0D47A1)
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth().clip(RoundedCornerShape(50.dp))
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
                                text = { Text("הוסף טיול חדש") },
                                onClick = {
                                    showAddTrip = true
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Settings button
                IconButton(onClick = { /* Navigate to settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "הגדרות")
                }
            }

            // Add trip input
            if (showAddTrip) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTripName,
                        onValueChange = { newTripName = it },
                        label = { Text("שם טיול חדש") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0D47A1),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF0D47A1)
                        ),
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(50.dp))
                    )
                    IconButton(
                        onClick = {
                            if (newTripName.isNotBlank()) {
                                repo.addTrip(newTripName)
                                newTripName = ""
                                showAddTrip = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "הוסף")
                    }
                }
            }

            // Pie Chart (reduced padding)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(200.dp)
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

            // Total text (reduced padding)
            Text(
                text = "Total ${totalAmount.format(0)}$",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            // Debt summary text
            val userBalance = repo.getUserDebtSummary().contains("owe you")
            Text(
                text = debtSummary,
                fontSize = 16.sp,
                color = if (userBalance) Color.Green else if (debtSummary.contains("owes")) Color.Red else Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            // Payments table (scrollable, modern, centered, with color legend)
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(8.dp)
                    ) {
                        Text("משתתף", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("שילם", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        items(participants) { participant ->
                            val index = participants.indexOf(participant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
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
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "${(payments[participant] ?: 0.0).format(0)}$",
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
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
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Payment, contentDescription = "תיעוד תשלום נוסף")
        }
    }
}