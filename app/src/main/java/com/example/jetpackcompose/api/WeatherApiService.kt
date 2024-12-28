package com.example.jetpackcompose.api

import android.util.Log
import com.example.jetpackcompose.data.ForecastData
import com.example.jetpackcompose.data.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object WeatherApiService {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    // OkHttpClient instance for making network requests
    private val client = OkHttpClient.Builder().build()

    // Retrofit instance for creating the API service
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API interface for defining the endpoints
    private val api = retrofit.create(WeatherApi::class.java)

    interface WeatherApi {
        // Endpoint for fetching current weather data
        @GET("weather")
        suspend fun fetchWeather(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String = "metric"
        ): retrofit2.Response<WeatherData>

        // Endpoint for fetching weather forecast data
        @GET("forecast")
        suspend fun fetchForecast(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String = "metric"
        ): retrofit2.Response<ForecastData>
    }

    // Function to fetch current weather data
    suspend fun fetchWeather(city: String, apiKey: String): WeatherData? {
        return try {
            // Perform network request on a background thread
            withContext(Dispatchers.Default) {
                val response = api.fetchWeather(city, apiKey)
                if (response.isSuccessful) {
                    response.body() // Return the weather data if successful
                } else {
                    Log.e("WeatherApiService", "Failed to fetch data: ${response.code()}")
                    null // Return null if the request failed
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherApiService", "Error fetching data: ${e.message}")
            null // Return null if an exception occurred
        }
    }

    // Function to fetch weather forecast data
    suspend fun fetchForecast(city: String, apiKey: String): ForecastData? {
        return try {
            // Perform network request on a background thread
            withContext(Dispatchers.IO) {
                val response = api.fetchForecast(city, apiKey)
                if (response.isSuccessful) {
                    response.body() // Return the forecast data if successful
                } else {
                    Log.e("WeatherApiService", "Failed to fetch forecast data: ${response.code()}")
                    null // Return null if the request failed
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherApiService", "Error fetching forecast data: ${e.message}")
            null // Return null if an exception occurred
        }
    }
}