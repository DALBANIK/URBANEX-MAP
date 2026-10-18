package com.example.ui.map

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Marker
import com.example.data.model.RouteInfo
import com.example.data.model.SkinCatalog
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OsmMapView(
    markers: List<Marker>,
    onMarkerClick: (Marker) -> Unit,
    onMapTap: (lat: Double, lon: Double) -> Unit,
    userLocationEnabled: Boolean,
    tileSourceType: String = "standard",
    targetLocation: Pair<Double, Double>? = null,
    userLocation: Pair<Double, Double>? = null,
    avatarSkin: String = "human",
    route: RouteInfo? = null,
    onUserLocationChanged: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Synchronize Android lifecycle with Osmdroid MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    // React to targetLocation updates
    LaunchedEffect(targetLocation) {
        if (targetLocation != null && mapViewRef != null) {
            mapViewRef?.controller?.animateTo(
                GeoPoint(targetLocation.first, targetLocation.second),
                15.0,
                1000L
            )
        }
    }

    // Fit route in view when route changes
    LaunchedEffect(route) {
        val map = mapViewRef
        if (route != null && map != null && route.waypoints.size >= 2) {
            val minLat = route.waypoints.minOf { it.latitude }
            val maxLat = route.waypoints.maxOf { it.latitude }
            val minLon = route.waypoints.minOf { it.longitude }
            val maxLon = route.waypoints.maxOf { it.longitude }

            val latPadding = max(0.01, (maxLat - minLat) * 0.2)
            val lonPadding = max(0.01, (maxLon - minLon) * 0.2)

            val boundingBox = BoundingBox(
                maxLat + latPadding,
                maxLon + lonPadding,
                minLat - latPadding,
                minLon - lonPadding
            )
            map.zoomToBoundingBox(boundingBox, true, 100)
        }
    }

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName

            MapView(ctx).apply {
                mapViewRef = this
                setMultiTouchControls(true)
                setBuiltInZoomControls(false)
                isTilesScaledToDpi = true

                // Default starting view (Kyiv center)
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(50.4501, 30.5234))

                // Tap on map events
                val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        onMapTap(p.latitude, p.longitude)
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint): Boolean {
                        onMapTap(p.latitude, p.longitude)
                        return true
                    }
                })
                overlays.add(eventsOverlay)

                // User location overlay
                if (userLocationEnabled) {
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                        enableMyLocation()
                        runOnFirstFix {
                            val myLoc = myLocation
                            if (myLoc != null) {
                                onUserLocationChanged?.invoke(myLoc.latitude, myLoc.longitude)
                            }
                        }
                    }
                    locationOverlayRef = locationOverlay
                    overlays.add(locationOverlay)
                }
            }
        },
        update = { map ->
            // Update tile source
            val tileSource = when (tileSourceType) {
                "topo" -> TileSourceFactory.OpenTopo
                "satellite" -> TileSourceFactory.USGS_SAT
                else -> TileSourceFactory.MAPNIK
            }
            if (map.tileProvider.tileSource != tileSource) {
                map.setTileSource(tileSource)
            }

            // Remove existing dynamic overlays (keeping events overlay and location overlay)
            val overlaysToKeep = map.overlays.filter {
                it is MapEventsOverlay || it is MyLocationNewOverlay
            }
            map.overlays.clear()
            map.overlays.addAll(overlaysToKeep)

            // Draw route polyline if present
            if (route != null && route.waypoints.size >= 2) {
                // Background shadow polyline for 3D look
                val shadowPolyline = Polyline(map).apply {
                    outlinePaint.color = Color.argb(120, 15, 23, 42)
                    outlinePaint.strokeWidth = 18f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(route.waypoints.map { GeoPoint(it.latitude, it.longitude) })
                }
                map.overlays.add(shadowPolyline)

                // Main vibrant blue route polyline
                val routePolyline = Polyline(map).apply {
                    outlinePaint.color = Color.parseColor("#2563EB") // Royal blue
                    outlinePaint.strokeWidth = 12f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(route.waypoints.map { GeoPoint(it.latitude, it.longitude) })
                }
                map.overlays.add(routePolyline)

                // Destination flag marker
                val destMarker = org.osmdroid.views.overlay.Marker(map).apply {
                    position = GeoPoint(route.destinationPoint.latitude, route.destinationPoint.longitude)
                    title = route.destinationName
                    snippet = "Фініш: ${route.formattedDistance} (${route.formattedDuration})"
                    setAnchor(
                        org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                        org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM
                    )
                }
                map.overlays.add(destMarker)
            }

            // Draw player's custom avatar marker (Human, Donut, Horse, Cat, Dog)
            userLocation?.let { (uLat, uLon) ->
                val avatarMarker = org.osmdroid.views.overlay.Marker(map).apply {
                    position = GeoPoint(uLat, uLon)
                    title = "Ваш аватар (${SkinCatalog.getAvatar(avatarSkin).nameUa})"
                    icon = MarkerPinHelper.getAvatarDrawable(context, avatarSkin)
                    setAnchor(
                        org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                        org.osmdroid.views.overlay.Marker.ANCHOR_CENTER
                    )
                }
                map.overlays.add(avatarMarker)
            }

            // Add all place markers with custom skins (visible to everyone)
            markers.forEach { marker ->
                val osmMk = org.osmdroid.views.overlay.Marker(map).apply {
                    position = GeoPoint(marker.latitude, marker.longitude)
                    title = marker.title
                    snippet = marker.description
                    icon = MarkerPinHelper.getPinDrawable(context, marker.categoryEnum, marker.markerSkin)
                    setAnchor(
                        org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                        org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM
                    )
                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(marker)
                        true
                    }
                }
                map.overlays.add(osmMk)
            }

            map.invalidate()
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun max(a: Double, b: Double): Double = if (a > b) a else b
