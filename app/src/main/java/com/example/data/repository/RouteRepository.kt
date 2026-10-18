package com.example.data.repository

import com.example.data.model.RouteInfo
import com.example.data.model.RoutePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

class RouteRepository {

    suspend fun calculateRoute(
        startLat: Double,
        startLon: Double,
        destLat: Double,
        destLon: Double,
        destinationName: String
    ): RouteInfo = withContext(Dispatchers.IO) {
        val startPoint = RoutePoint(startLat, startLon)
        val destPoint = RoutePoint(destLat, destLon)

        try {
            // OSRM Public Driving Routing API
            val urlString = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$destLon,$destLat?overview=full&geometries=geojson"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "uexmap/1.0 (Android)")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (json.optString("code") == "Ok") {
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val firstRoute = routes.getJSONObject(0)
                        val distanceMeters = firstRoute.getDouble("distance")
                        val durationSeconds = firstRoute.getDouble("duration")

                        val geometry = firstRoute.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val waypoints = mutableListOf<RoutePoint>()

                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            // GeoJSON coordinates format is [lon, lat]
                            val lon = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            waypoints.add(RoutePoint(lat, lon))
                        }

                        val distanceKm = distanceMeters / 1000.0
                        val durationMinutes = max(1, (durationSeconds / 60.0).roundToInt())
                        val fare = calculateTaxiFare(distanceKm)

                        return@withContext RouteInfo(
                            startPoint = startPoint,
                            destinationPoint = destPoint,
                            destinationName = destinationName,
                            distanceKm = distanceKm,
                            durationMinutes = durationMinutes,
                            waypoints = if (waypoints.isNotEmpty()) waypoints else listOf(startPoint, destPoint),
                            estimatedTaxiFareUah = fare
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback calculation in case of connection failure or offline
        }

        // Fallback calculation using straight-line + road curvature factor (1.25)
        val distanceKm = calculateHaversineDistance(startLat, startLon, destLat, destLon) * 1.25
        val durationMinutes = max(1, ((distanceKm / 50.0) * 60.0).roundToInt())
        val fare = calculateTaxiFare(distanceKm)

        val waypoints = listOf(
            startPoint,
            RoutePoint((startLat + destLat) / 2.0, (startLon + destLon) / 2.0),
            destPoint
        )

        RouteInfo(
            startPoint = startPoint,
            destinationPoint = destPoint,
            destinationName = destinationName,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            waypoints = waypoints,
            estimatedTaxiFareUah = fare
        )
    }

    private fun calculateTaxiFare(distanceKm: Double): Int {
        val baseFee = 70.0
        val perKm = 15.0
        return max(80, (baseFee + distanceKm * perKm).roundToInt())
    }

    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
