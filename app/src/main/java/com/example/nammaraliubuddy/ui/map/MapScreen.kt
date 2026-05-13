package com.example.nammaraliubuddy.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.*

data class StationStop(
    val name: String,
    val arrivalTime: String,
    val departureTime: String,
    val latLng: LatLng
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasPermission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
            }
        }
    )

    // Route: Bengaluru to Mysuru
    val route = listOf(
        StationStop("KSR Bengaluru", "START", "10:30 AM", LatLng(12.9784, 77.5694)),
        StationStop("Kengeri", "10:50 AM", "10:52 AM", LatLng(12.9080, 77.4741)),
        StationStop("Bidadi", "11:10 AM", "11:12 AM", LatLng(12.7938, 77.3857)),
        StationStop("Ramanagara", "11:30 AM", "11:32 AM", LatLng(12.7233, 77.2762)),
        StationStop("Channapatna", "11:45 AM", "11:47 AM", LatLng(12.6521, 77.2023)),
        StationStop("Maddur", "12:10 PM", "12:12 PM", LatLng(12.5841, 77.0452)),
        StationStop("Mandya", "12:35 PM", "12:37 PM", LatLng(12.5222, 76.8974)),
        StationStop("Mysuru Jn", "01:30 PM", "END", LatLng(12.3164, 76.6416))
    )

    var currentStationIndex by remember { mutableIntStateOf(0) }
    var distanceToDestination by remember { mutableDoubleStateOf(-1.0) }
    var destinationStation by remember { mutableStateOf(route.last()) }
    var notificationSentForStation by remember { mutableStateOf<String?>(null) }

    val platformMap = remember { mutableStateMapOf<String, String>() }
    val database = FirebaseDatabase.getInstance().getReference("pings")

    LaunchedEffect(Unit) {
        route.forEach { station ->
            database.child(station.name).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pf = snapshot.child("platform").getValue(String::class.java) ?: "TBD"
                    platformMap[station.name] = pf
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    fun sendArrivalNotification(stationName: String, platform: String) {
        if (notificationSentForStation == stationName) return

        val builder = NotificationCompat.Builder(context, "TRAIN_ALERTS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Arriving Soon!")
            .setContentText("$stationName is within 5km. Expected Platform: $platform")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                notify(stationName.hashCode(), builder.build())
                notificationSentForStation = stationName
            }
        }
    }

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        val pos = LatLng(it.latitude, it.longitude)
                        userLocation = pos
                        
                        var closestIdx = 0
                        var minDistance = Double.MAX_VALUE
                        route.forEachIndexed { index, station ->
                            val d = calculateDistance(pos, station.latLng)
                            if (d < minDistance) {
                                minDistance = d
                                closestIdx = index
                            }
                        }
                        currentStationIndex = closestIdx
                        val dist = calculateDistance(pos, destinationStation.latLng)
                        distanceToDestination = dist

                        if (dist in 0.0..5000.0) {
                            val pf = platformMap[destinationStation.name] ?: "TBD"
                            sendArrivalNotification(destinationStation.name, pf)
                        }
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
        } else {
            onDispose {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Where is my Train", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("LIVE PLATFORM TRACKING", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (!hasPermission) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Button(onClick = { 
                    val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                }) {
                    Text("Start Live Tracking")
                }
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Train, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("NEXT STATION", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                val nextStation = if (currentStationIndex < route.size - 1) route[currentStationIndex + 1] else route.last()
                                Text(nextStation.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.2f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("ARRIVAL PLATFORM", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                val nextStationName = if (currentStationIndex < route.size - 1) route[currentStationIndex + 1].name else route.last().name
                                val pf = platformMap[nextStationName] ?: "TBD"
                                Text("PF: $pf", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.Yellow)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TRAIN STATUS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("RUNNING ON TIME", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                            }
                        }
                    }
                }

                if (distanceToDestination in 0.0..5000.0) {
                    Surface(
                        color = Color(0xFFE53935),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(12.dp))
                            val destPF = platformMap[destinationStation.name] ?: "TBD"
                            Column {
                                Text("DESTINATION ALERT", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
                                Text("${destinationStation.name} on Platform $destPF", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Text(
                    "Live Route Journey", 
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(route) { index, station ->
                        StationItem(
                            station = station,
                            platform = platformMap[station.name] ?: "TBD",
                            isLast = index == route.size - 1,
                            isPassed = index < currentStationIndex,
                            isCurrent = index == currentStationIndex,
                            isSelectedDestination = station == destinationStation,
                            onSelect = { 
                                destinationStation = station 
                                if (notificationSentForStation != station.name) {
                                    notificationSentForStation = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StationItem(
    station: StationStop,
    platform: String,
    isLast: Boolean,
    isPassed: Boolean,
    isCurrent: Boolean,
    isSelectedDestination: Boolean,
    onSelect: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 22.dp else 12.dp)
                    .background(
                        color = if (isPassed || isCurrent) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) Icon(Icons.Default.Train, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            if (!isLast) {
                Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(if (isPassed) MaterialTheme.colorScheme.primary else Color.LightGray))
            }
        }

        Card(
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else Color.White),
            border = BorderStroke(width = if (isSelectedDestination) 2.dp else 1.dp, color = if (isSelectedDestination) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f)),
            onClick = onSelect
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, fontWeight = FontWeight.Bold, color = if (isPassed) Color.Gray else Color.Black)
                    Text("Expected Platform: $platform", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(station.arrivalTime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (isPassed) Color.Gray else Color.Black)
            }
        }
    }
}

fun calculateDistance(p1: LatLng, p2: LatLng): Double {
    val r = 6371e3
    val lat1 = p1.latitude * PI / 180
    val lat2 = p2.latitude * PI / 180
    val dLat = (p2.latitude - p1.latitude) * PI / 180
    val dLon = (p2.longitude - p1.longitude) * PI / 180
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
