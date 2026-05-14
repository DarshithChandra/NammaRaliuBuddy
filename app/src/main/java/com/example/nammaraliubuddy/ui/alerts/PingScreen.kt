package com.example.nammaraliubuddy.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

data class PingAlert(
    val stationName: String = "",
    val platform: String = "",
    val timestamp: Long = 0,
    val message: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen() {
    val alerts = remember { mutableStateListOf<PingAlert>() }
    val database = FirebaseDatabase.getInstance().getReference("alerts")

    LaunchedEffect(Unit) {
        database.limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                alerts.clear()
                for (child in snapshot.children) {
                    val alert = child.getValue(PingAlert::class.java)
                    if (alert != null) alerts.add(0, alert)
                }
                
                // Simulated alerts if the live node is empty
                if (alerts.isEmpty()) {
                    alerts.addAll(listOf(
                        PingAlert("Bengaluru Urban", "1", System.currentTimeMillis(), "Express reaching shortly on Platform 1"),
                        PingAlert("Mandya", "2", System.currentTimeMillis() - 300000, "Intercity standing on Platform 2"),
                        PingAlert("Mysuru Junction", "6", System.currentTimeMillis() - 900000, "Train departure confirmed from PF 6"),
                        PingAlert("Davanagere", "1", System.currentTimeMillis() - 1200000, "Platform 1 is clear for arrival"),
                        PingAlert("Hubballi Jn", "1", System.currentTimeMillis() - 1500000, "Fast Passenger arrived on PF 1"),
                        PingAlert("Belagavi", "1", System.currentTimeMillis() - 2000000, "Automated Status: Platform 1 Active")
                    ))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Pings & Notifications", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Live Activity Feed",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alerts) { alert ->
                    AlertCard(alert)
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: PingAlert) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = sdf.format(Date(alert.timestamp))

    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.stationName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                Text(
                    text = "Platform: ${alert.platform}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}
