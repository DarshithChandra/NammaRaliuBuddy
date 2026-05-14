package com.example.nammaraliubuddy.ui.map

import android.Manifest
import android.annotation.SuppressLint
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
import kotlinx.coroutines.delay
import kotlin.math.*

data class StationStop(
    val name: String,
    val arrivalTime: String,
    val departureTime: String,
    val latLng: LatLng,
    val defaultPlatform: String
)

data class Train(
    val number: String,
    val name: String,
    val route: List<StationStop>
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var isSimulating by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasPermission
        }
    )

    val trains = listOf(
        Train("16232", "BENGALURU - MYSURU EXPRESS", listOf(
            StationStop("KSR Bengaluru", "START", "10:30 AM", LatLng(12.9784, 77.5694), "1"),
            StationStop("Kengeri", "10:50 AM", "10:52 AM", LatLng(12.9080, 77.4741), "2"),
            StationStop("Bidadi", "11:10 AM", "11:12 AM", LatLng(12.7938, 77.3857), "1"),
            StationStop("Ramanagara", "11:30 AM", "11:32 AM", LatLng(12.7233, 77.2762), "3"),
            StationStop("Channapatna", "11:45 AM", "11:47 AM", LatLng(12.6521, 77.2023), "2"),
            StationStop("Maddur", "12:10 PM", "12:12 PM", LatLng(12.5841, 77.0452), "1"),
            StationStop("Mandya", "12:35 PM", "12:37 PM", LatLng(12.5222, 76.8974), "2"),
            StationStop("Mysuru Jn", "01:30 PM", "END", LatLng(12.3164, 76.6416), "6")
        )),
        Train("17301", "DHARWAD EXPRESS", listOf(
            StationStop("KSR Bengaluru", "START", "09:00 PM", LatLng(12.9784, 77.5694), "8"),
            StationStop("Tumakuru", "10:15 PM", "10:17 PM", LatLng(13.3392, 77.1006), "1"),
            StationStop("Arsikere Jn", "11:45 PM", "11:50 PM", LatLng(13.3134, 76.2573), "2"),
            StationStop("Birur Jn", "12:20 AM", "12:22 AM", LatLng(13.6264, 75.9757), "1"),
            StationStop("Davanagere", "01:50 AM", "01:52 AM", LatLng(14.4644, 75.9218), "1"),
            StationStop("Haveri", "02:45 AM", "02:47 AM", LatLng(14.7942, 75.4026), "1"),
            StationStop("Hubballi Jn", "04:15 AM", "END", LatLng(15.3486, 75.1492), "1")
        )),
        Train("16589", "RANI CHENNAMMA EXP", listOf(
            StationStop("KSR Bengaluru", "START", "11:00 PM", LatLng(12.9784, 77.5694), "9"),
            StationStop("Tumakuru", "12:10 AM", "12:12 AM", LatLng(13.3392, 77.1006), "2"),
            StationStop("Arsikere Jn", "01:30 AM", "01:35 AM", LatLng(13.3134, 76.2573), "1"),
            StationStop("Hubballi Jn", "05:40 AM", "06:00 AM", LatLng(15.3486, 75.1492), "1"),
            StationStop("Dharwad", "06:23 AM", "06:25 AM", LatLng(15.4851, 74.9892), "1"),
            StationStop("Belagavi", "08:50 AM", "09:00 AM", LatLng(15.8497, 74.4977), "1"),
            StationStop("Miraj Jn", "12:15 PM", "END", LatLng(16.8242, 74.6468), "1")
        )),
        Train("12725", "SIDDHAGANGA EXP", listOf(
            StationStop("KSR Bengaluru", "START", "01:00 PM", LatLng(12.9784, 77.5694), "10"),
            StationStop("Yesvantpur Jn", "01:10 PM", "01:12 PM", LatLng(13.0236, 77.5503), "3"),
            StationStop("Tumakuru", "02:10 PM", "02:12 PM", LatLng(13.3392, 77.1006), "1"),
            StationStop("Birur Jn", "04:30 PM", "04:32 PM", LatLng(13.6264, 75.9757), "2"),
            StationStop("Davanagere", "06:15 PM", "06:17 PM", LatLng(14.4644, 75.9218), "1"),
            StationStop("Hubballi Jn", "09:00 PM", "END", LatLng(15.3486, 75.1492), "2")
        ))
    )

    var searchQuery by remember { mutableStateOf("") }
    var currentTrain by remember { mutableStateOf(trains[0]) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val filteredTrains = if (searchQuery.isBlank()) trains else trains.filter { 
        it.number.contains(searchQuery) || it.name.contains(searchQuery, ignoreCase = true) 
    }

    var currentStationIndex by remember { mutableIntStateOf(0) }
    var distanceToDestination by remember { mutableDoubleStateOf(-1.0) }
    var destinationStation by remember { mutableStateOf(currentTrain.route.last()) }
    var notificationSentForStation by remember { mutableStateOf<String?>(null) }

    val platformMap = remember { mutableStateMapOf<String, String>() }
    val database = FirebaseDatabase.getInstance().getReference("pings")

    LaunchedEffect(currentTrain) {
        currentTrain.route.forEach { station ->
            database.child(station.name).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pf = snapshot.child("platform").getValue(String::class.java)
                    if (pf != null) platformMap[station.name] = pf
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
        destinationStation = currentTrain.route.last()
        notificationSentForStation = null
    }

    // Simulation logic
    LaunchedEffect(isSimulating, destinationStation) {
        if (isSimulating) {
            var step = 0
            while (isSimulating) {
                val target = destinationStation.latLng
                val start = currentTrain.route[0].latLng
                val progress = (step / 50.0)
                val lerpLat = start.latitude + (target.latitude - start.latitude) * progress
                val lerpLng = start.longitude + (target.longitude - start.longitude) * progress
                userLocation = LatLng(lerpLat, lerpLng)
                
                step = (step + 1) % 51
                delay(1000)
            }
        }
    }

    fun sendArrivalNotification(stationName: String, platform: String) {
        if (notificationSentForStation == stationName) return
        val builder = NotificationCompat.Builder(context, "TRAIN_ALERTS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Arriving Soon!")
            .setContentText("$stationName is within 5km. Stand on Platform: $platform")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        
        try {
            with(NotificationManagerCompat.from(context)) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33) {
                    notify(stationName.hashCode(), builder.build())
                    notificationSentForStation = stationName
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Tracker logic
    LaunchedEffect(userLocation, destinationStation) {
        userLocation?.let { pos ->
            var closestIdx = 0
            var minDistance = Double.MAX_VALUE
            currentTrain.route.forEachIndexed { index, station ->
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
                sendArrivalNotification(destinationStation.name, platformMap[destinationStation.name] ?: destinationStation.defaultPlatform)
            }
        }
    }

    DisposableEffect(hasPermission) {
        if (hasPermission && !isSimulating) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { userLocation = LatLng(it.latitude, it.longitude) }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
        } else onDispose {}
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.primary)) {
                TopAppBar(
                    title = {
                        if (isSearchExpanded) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search Train No/Name", color = Color.White.copy(0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        } else {
                            Column {
                                Text("Train Status", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                Text(currentTrain.name, color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded; searchQuery = "" }) {
                            Icon(if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search, null, tint = Color.White)
                        }
                        IconButton(onClick = { isSimulating = !isSimulating }) {
                            Icon(Icons.Default.PlayArrow, null, tint = if (isSimulating) Color.Yellow else Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
                if (isSearchExpanded && searchQuery.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.heightIn(max = 300.dp)) {
                            filteredTrains.forEach { train ->
                                DropdownMenuItem(
                                    text = { Text("${train.number} - ${train.name}") },
                                    onClick = {
                                        currentTrain = train
                                        isSearchExpanded = false
                                        searchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Train, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            val nextStation = if (currentStationIndex < currentTrain.route.size - 1) currentTrain.route[currentStationIndex + 1] else currentTrain.route.last()
                            Text("APPROACHING", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            Text(nextStation.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.White.copy(0.2f))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column {
                            val nextStation = if (currentStationIndex < currentTrain.route.size - 1) currentTrain.route[currentStationIndex + 1] else currentTrain.route.last()
                            Text("PLATFORM", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            Text("STAND ON PF: ${platformMap[nextStation.name] ?: nextStation.defaultPlatform}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.Yellow)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            Text(if (isSimulating) "SIMULATING" else "ON TIME", color = if (isSimulating) Color.Yellow else Color(0xFF81C784), fontWeight = FontWeight.Bold)
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
                        Icon(Icons.Default.NotificationsActive, null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("${destinationStation.name} on PF ${platformMap[destinationStation.name] ?: destinationStation.defaultPlatform} in 5km!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Journey Progress", Modifier.padding(start = 20.dp, top = 16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                itemsIndexed(currentTrain.route) { index, station ->
                    StationItem(
                        station = station,
                        platform = platformMap[station.name] ?: station.defaultPlatform,
                        isLast = index == currentTrain.route.size - 1,
                        isPassed = index < currentStationIndex,
                        isCurrent = index == currentStationIndex,
                        isSelected = station == destinationStation
                    ) {
                        destinationStation = station
                        notificationSentForStation = null
                    }
                }
            }
        }
    }
}

@Composable
fun StationItem(station: StationStop, platform: String, isLast: Boolean, isPassed: Boolean, isCurrent: Boolean, isSelected: Boolean, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            Box(Modifier.size(if (isCurrent) 22.dp else 12.dp).background(if (isPassed || isCurrent) MaterialTheme.colorScheme.primary else Color.LightGray, CircleShape), Alignment.Center) {
                if (isCurrent) Icon(Icons.Default.Train, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            if (!isLast) Box(Modifier.fillMaxHeight().width(2.dp).background(if (isPassed) MaterialTheme.colorScheme.primary else Color.LightGray))
        }
        Card(
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else Color.White),
            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(0.4f)),
            onClick = onSelect
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, fontWeight = FontWeight.Bold, color = if (isPassed) Color.Gray else Color.Black)
                    Text("PF: $platform", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(station.arrivalTime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (isPassed) Color.Gray else Color.Black)
            }
        }
    }
}

fun calculateDistance(p1: LatLng, p2: LatLng): Double {
    val r = 6371e3
    val lat1 = p1.latitude * PI / 180; val lat2 = p2.latitude * PI / 180
    val dLat = (p2.latitude - p1.latitude) * PI / 180; val dLon = (p2.longitude - p1.longitude) * PI / 180
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
