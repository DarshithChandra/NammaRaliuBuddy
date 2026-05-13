package com.example.nammaraliubuddy.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nammaraliubuddy.ui.home.HomeScreen
import com.example.nammaraliubuddy.ui.map.MapScreen
import com.example.nammaraliubuddy.ui.station.StationScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Station : Screen("station")
    object Map : Screen("map")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Station.route) {
            StationScreen()
        }
        composable(Screen.Map.route) {
            MapScreen()
        }
    }
}
