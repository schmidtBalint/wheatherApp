package com.example.jetpackcompose.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.jetpackcompose.data.ForecastItem
import com.example.jetpackcompose.storage.Keys
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jetpackcompose.viewmodel.WeatherViewModel
import com.example.jetpackcompose.ui.components.SearchBarSample
import com.example.jetpackcompose.ui.components.WeatherCard

@Composable
fun ForecastWeatherView(forecast: List<ForecastItem>) {
    val context = LocalContext.current
    var hometown by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    val weatherViewModel: WeatherViewModel = viewModel()
    val errorMessage by weatherViewModel.errorMessage.collectAsState()

    // Retrieve hometown and apiKey from DataStore
    LaunchedEffect(Unit) {
        context.dataStore.data.collect { preferences ->
            hometown = preferences[Keys.HOMETOWN_KEY] ?: ""
            apiKey = preferences[Keys.API_TOKEN_KEY] ?: ""

            if (hometown.isNotEmpty() && apiKey.isNotEmpty()) {
                weatherViewModel.fetchForecastData(hometown, apiKey)
            }
        }
    }

    val searchQuery = rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar for entering city name
        SearchBarSample(
            selectedMenu = "Forecast",
            apiKey = apiKey,
            onQueryChanged = { query ->
                searchQuery.value = query
                if (query.isNotEmpty()) {
                    weatherViewModel.fetchForecastData(query, apiKey)
                } else {
                    if (hometown.isNotEmpty() && apiKey.isNotEmpty()) {
                        weatherViewModel.fetchForecastData(hometown, apiKey)
                    }
                }
            }
        )
    }

    // Display error message if any
    errorMessage?.let {
        Text(
            text = it,
            color = Color.Red,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 25.sp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Display message if hometown is not set
        if (searchQuery.value.isEmpty() && hometown.isEmpty()) {
            Text(
                text = "Set your hometown in settings",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 24.sp,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(16.dp)
            )
        } else if (forecast.isNotEmpty()) {
            // Display forecast data
            Text(
                text = "Forecast for ${searchQuery.value.takeIf { it.isNotEmpty() } ?: hometown}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    color = Color.Black
                ),
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .align(Alignment.CenterHorizontally)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(forecast) { forecastItem ->
                    WeatherCard(forecastItem = forecastItem)
                }
            }
        }

        // Display loading message if forecast data is empty and no error
        if (forecast.isEmpty() && errorMessage == null) {
            Text(
                text = "Loading forecast...",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 18.sp,
                    color = Color.Gray
                ),
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}