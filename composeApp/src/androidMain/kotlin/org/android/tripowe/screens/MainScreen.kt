package org.android.tripowe.screens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import org.android.tripowe.models.AppRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repo: AppRepository = remember { AppRepository() }) {
    val trips by repo.trips.collectAsState()
    val currentTrip by repo.currentTrip.collectAsState()
    val participants by repo.participants.collectAsState()
    val payments by remember { derivedStateOf { repo.getPaymentsByParticipant() } }
    val totalAmount by remember { derivedStateOf { repo.totalAmount } }

    // Dropdown state
    var expanded by remember { mutableStateOf(false) }
    var showAddTrip by remember { mutableStateOf(false) }
    var newTripName by remember { mutableStateOf("") }

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

            // Dropdown for trips
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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
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

        // Add trip input (simple row for now)
        if (showAddTrip) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTripName,
                    onValueChange = { newTripName = it },
                    label = { Text("שם טיול חדש") },
                    modifier = Modifier.weight(1f)
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

        // Pie Chart (Custom with Canvas - fixed without external libs)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(200.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2
                val centerY = canvasHeight / 2
                val radius = (canvasWidth / 2) - 20  // Padding

                val total = totalAmount
                var startAngle = 0f
                participants.forEach { participant ->
                    val amount = payments[participant] ?: 0.0
                    if (total > 0) {
                        val sweepAngle = (amount / total * 360f).toFloat()
                        val color = when (participant.id) {
                            1 -> Color.Blue
                            2 -> Color.Green
                            else -> Color.Red
                        }
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
            text = "Total ${String.format("%.0f", totalAmount)}$",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        // Payments table
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp)
                ) {
                    Text("משתתף", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("שילם", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                }
                LazyColumn {
                    items(participants) { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                participant.name,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${String.format("%.0f", payments[participant] ?: 0.0)}$",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}