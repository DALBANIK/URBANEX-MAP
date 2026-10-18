package com.example.data.model

data class RoutePoint(
    val latitude: Double,
    val longitude: Double
)

data class RouteInfo(
    val startPoint: RoutePoint,
    val destinationPoint: RoutePoint,
    val destinationName: String,
    val distanceKm: Double,
    val durationMinutes: Int, // driving duration
    val durationWalkingMinutes: Int = kotlin.math.max(1, (distanceKm / 4.8 * 60).toInt()),
    val waypoints: List<RoutePoint>,
    val estimatedTaxiFareUah: Int
) {
    val formattedDistance: String
        get() = if (distanceKm >= 1.0) {
            String.format(java.util.Locale.US, "%.1f км", distanceKm)
        } else {
            "${(distanceKm * 1000).toInt()} м"
        }

    val formattedDistanceEn: String
        get() = if (distanceKm >= 1.0) {
            String.format(java.util.Locale.US, "%.1f km", distanceKm)
        } else {
            "${(distanceKm * 1000).toInt()} m"
        }

    val formattedDuration: String
        get() = formatDuration(durationMinutes, isUk = true)

    val formattedDurationEn: String
        get() = formatDuration(durationMinutes, isUk = false)

    val formattedWalkingDuration: String
        get() = formatDuration(durationWalkingMinutes, isUk = true)

    val formattedWalkingDurationEn: String
        get() = formatDuration(durationWalkingMinutes, isUk = false)

    private fun formatDuration(mins: Int, isUk: Boolean): String {
        return if (mins >= 60) {
            val hours = mins / 60
            val m = mins % 60
            if (isUk) {
                if (m > 0) "$hours год $m хв" else "$hours год"
            } else {
                if (m > 0) "$hours hr $m min" else "$hours hr"
            }
        } else {
            if (isUk) "$mins хв" else "$mins min"
        }
    }
}
