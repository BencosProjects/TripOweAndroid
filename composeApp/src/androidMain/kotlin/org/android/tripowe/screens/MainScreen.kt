package org.android.tripowe.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.charts.pie.PieChart
import org.jetbrains.compose.charts.pie.chartPie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repo: AppRepository = remember { AppRepository() }) {
    val trips by repo.trips.collectAsState()
    val currentTrip by repo.currentTrip.collectAsState()
    val participants by repo.participants.collectAsState()
    val payments by remember { derivedStateOf { repo.getPaymentsByParticipant() } }
    val totalAmount by remember { derivedStateOf { repo.totalAmount } }

    // מצב פתיחת הדרופדאון
    var expanded by remember { mutableStateOf(false) }
    var newTripName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // שורה עליונה
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // רכיב שמאל - ריק (אופציונלי)
            Spacer(modifier = Modifier.weight(1f))

            // דרופדאון טיולים במרכז
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
                        // אופציה להוספת טיול
                        DropdownMenuItem(
                            text = { Text("הוסף טיול חדש") },
                            onClick = {
                                // פתח דיאלוג להוספה (פשוט, ניתן להרחיב)
                                newTripName = "" // כאן תוכל להוסיף Dialog
                                expanded = false
                            }
                        )
                    }
                }
            }

            // כפתור הגדרות בימין
            IconButton(onClick = { /* נווט להגדרות */ }) {
                Icon(Icons.Default.Settings, contentDescription = "הגדרות")
            }
        }

        // דיאגרמת עיגול במרכז
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                pieChartData = chartPie {
                    participants.forEach { participant ->
                        slice(
                            label = participant.name,
                            value = payments[participant] ?: 0.0,
                            color = when (participant.id) {
                                1 -> Color.Blue
                                2 -> Color.Green
                                else -> Color.Red
                            }
                        )
                    }
                },
                modifier = Modifier.size(200.dp)
            )
        }

        // טקסט סה"כ מתחת לדיאגרמה
        Text(
            text = "Total ${String.format("%.0f", totalAmount)}$",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        // טבלה קטנה של תשלומים
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                // כותרת טבלה
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp)
                ) {
                    Text("משתתף", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("שילם", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
                // שורות טבלה
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
                                "${payments[participant] ?: 0.0}$",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }

    // דיאלוג פשוט להוספת טיול (אופציונלי)
    if (newTripName.isNotEmpty()) {
        // הוסף AlertDialog כאן אם רוצה
    }
}