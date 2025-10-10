package org.android.tripowe.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val participants by repo.participants.collectAsState()
    val payments by remember { derivedStateOf { repo.getPaymentsByParticipant() } }
    val totalAmount by remember { derivedStateOf { repo.totalAmount } }
    val debtSummary by remember { derivedStateOf { repo.getUserDebtSummary() } }

    // Dynamic color list for pie chart and table legends
    val colors = listOf(
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFF44336),
        Color(0xFFFFC107), Color(0xFF9C27B0), Color(0xFF00BCD4),
        Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B),
        Color(0xFFE91E63)
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .semantics { contentDescription = "Trip expense overview" }
        ) {
            // Top App Bar with elevation
            TopAppBar(
                title = { Text("TripOwe", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate to settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "App toolbar" }
            )

            // Pie Chart with dynamic size and legend
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 220.dp)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(200.dp)
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
            }

            // Total text
            Text(
                text = "Total ${totalAmount.format(0)}$",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            // Debt summary text
            val userBalance = debtSummary.contains("owe you")
            Text(
                text = debtSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (userBalance) MaterialTheme.colorScheme.primary else if (debtSummary.contains("owes")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            // Payments table
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            "Participant",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
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
                        IconButton(
                            onClick = { /* TODO: Navigate to info page */ },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "More info" }
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(min = 150.dp, max = 250.dp)
                            .padding(horizontal = 12.dp)
                    ) {
                        items(participants) { participant ->
                            val index = participants.indexOf(participant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(colors[index % colors.size], shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    participant.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${(payments[participant] ?: 0.0).format(0)}$",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button with animation
        val animatedColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.primary,
            animationSpec = tween(durationMillis = 300)
        )
        FloatingActionButton(
            onClick = { /* TODO: Implement add payment */ },
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = animatedColor,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(
                Icons.Default.Payment,
                contentDescription = "Record additional payment",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}