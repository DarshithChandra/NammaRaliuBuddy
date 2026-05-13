package com.example.nammaraliubuddy.ui.station

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*

data class StationInfo(
    val name: String,
    val coachPosition: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationScreen() {
    var selectedStation by remember { mutableStateOf<StationInfo?>(null) }
    val stations = listOf(
        StationInfo("Bagalkot", "General Coach: Front"),
        StationInfo("Ballari", "General Coach: Rear"),
        StationInfo("Belagavi", "General Coach: Middle"),
        StationInfo("Bengaluru Rural", "General Coach: Front"),
        StationInfo("Bengaluru Urban", "General Coach: Front"),
        StationInfo("Bidar", "General Coach: Rear"),
        StationInfo("Chamarajanagar", "General Coach: Front"),
        StationInfo("Chikballapur", "General Coach: Middle"),
        StationInfo("Chikkamagaluru", "General Coach: Front"),
        StationInfo("Chitradurga", "General Coach: Middle"),
        StationInfo("Dakshina Kannada", "General Coach: Rear"),
        StationInfo("Davanagere", "General Coach: Middle"),
        StationInfo("Dharwad", "General Coach: Front"),
        StationInfo("Gadag", "General Coach: Middle"),
        StationInfo("Hassan", "General Coach: Rear"),
        StationInfo("Haveri", "General Coach: Middle"),
        StationInfo("Kalaburagi", "General Coach: Front"),
        StationInfo("Kodagu", "General Coach: Rear"),
        StationInfo("Kolar", "General Coach: Middle"),
        StationInfo("Koppal", "General Coach: Front"),
        StationInfo("Mandya", "General Coach: Front (Engine side)"),
        StationInfo("Mysuru", "General Coach: Rear"),
        StationInfo("Raichur", "General Coach: Middle"),
        StationInfo("Ramanagara", "General Coach: Front"),
        StationInfo("Shivamogga", "General Coach: Middle"),
        StationInfo("Tumakuru", "General Coach: Rear"),
        StationInfo("Udupi", "General Coach: Front"),
        StationInfo("Uttara Kannada", "General Coach: Middle"),
        StationInfo("Vijayapura", "General Coach: Front"),
        StationInfo("Yadgir", "General Coach: Rear"),
        StationInfo("Vijayanagara", "General Coach: Middle")
    ).sortedBy { it.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Station Info", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Select Station",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(selectedStation?.name ?: "Choose a station")
                }
                DropdownMenu(
                    expanded = expanded, 
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 400.dp)
                ) {
                    stations.forEach { station ->
                        DropdownMenuItem(
                            text = { Text(station.name) },
                            onClick = {
                                selectedStation = station
                                expanded = false
                            }
                        )
                    }
                }
            }

            selectedStation?.let { station ->
                CoachInfoCard(station)
                Spacer(modifier = Modifier.height(16.dp))
                AutomatedPlatformCard(station.name)
            }
        }
    }
}

@Composable
fun CoachInfoCard(station: StationInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Train,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Coach Position",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                station.coachPosition,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CoachBox("Engine", isEngine = true)
                CoachBox("Gen", isTarget = station.coachPosition.contains("Front"))
                CoachBox("S1", false)
                CoachBox("S2", false)
                CoachBox("Gen", isTarget = station.coachPosition.contains("Rear"))
            }
        }
    }
}

@Composable
fun CoachBox(label: String, isTarget: Boolean = false, isEngine: Boolean = false) {
    Box(
        modifier = Modifier
            .size(55.dp, 45.dp)
            .background(
                color = when {
                    isEngine -> Color.DarkGray
                    isTarget -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondary
                },
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AutomatedPlatformCard(stationName: String) {
    var platform by remember { mutableStateOf("--") }
    val database = FirebaseDatabase.getInstance().getReference("pings").child(stationName)

    LaunchedEffect(stationName) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                platform = snapshot.child("platform").getValue(String::class.java) ?: "TBD"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Live Automated Update",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "PLATFORM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = platform,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "System generated live status",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
