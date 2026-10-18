package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.data.model.Marker
import com.example.data.model.MarkerCategory
import com.example.data.model.SkinCatalog
import com.example.data.util.AppLanguage
import com.example.data.util.Strings
import com.example.ui.auth.AuthManager
import com.example.ui.components.MarkerImageView
import com.example.ui.dialogs.AddEditMarkerDialog
import com.example.ui.dialogs.MarkerDetailBottomSheet
import com.example.ui.dialogs.TaxiCallDialog
import com.example.ui.map.OsmMapView
import com.example.ui.viewmodel.MapViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MapViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentTab by remember { mutableIntStateOf(0) } // 0: Map, 1: My Markers, 2: Shop, 3: Moderation, 4: Profile

    val approvedMarkers by viewModel.approvedMarkers.collectAsState()
    val userMarkers by viewModel.userMarkers.collectAsState()
    val pendingMarkers by viewModel.pendingMarkers.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMarker by viewModel.selectedMarker.collectAsState()
    val isAddingMarker by viewModel.isAddingMarker.collectAsState()
    val markerDraftLocation by viewModel.markerDraftLocation.collectAsState()
    val editingMarker by viewModel.editingMarker.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val activeRoute by viewModel.activeRoute.collectAsState()
    val isCalculatingRoute by viewModel.isCalculatingRoute.collectAsState()
    val isTaxiDialogOpen by viewModel.isTaxiDialogOpen.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val shopState by viewModel.shopState.collectAsState()

    var tappedMapLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    // Location permission handling
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    var mapTargetLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var tileSourceType by remember { mutableStateOf("standard") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // Tab 0: Map
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = Strings.tabMap) },
                    label = { Text(Strings.tabMap) }
                )

                // Tab 1: My Markers
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (userMarkers.isNotEmpty()) {
                                    Badge { Text("${userMarkers.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = Strings.tabMyMarkers)
                        }
                    },
                    label = { Text(Strings.tabMyMarkers) }
                )

                // Tab 2: Shop & Skins
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(Icons.Default.ShoppingBag, contentDescription = Strings.tabShop)
                    },
                    label = { Text(Strings.tabShop) }
                )

                // Tab 3: Moderation (for admins/moderators)
                if (isAdmin) {
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingMarkers.isNotEmpty()) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("${pendingMarkers.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = Strings.tabModeration)
                            }
                        },
                        label = { Text(Strings.tabModeration) }
                    )
                }

                // Tab 4 (or 3 if not admin): Profile
                val profileTabIndex = if (isAdmin) 4 else 3
                NavigationBarItem(
                    selected = currentTab == profileTabIndex,
                    onClick = { currentTab = profileTabIndex },
                    icon = { Icon(Icons.Default.Person, contentDescription = Strings.tabProfile) },
                    label = { Text(Strings.tabProfile) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> {
                    // TAB 0: MAP VIEW
                    Box(modifier = Modifier.fillMaxSize()) {
                        OsmMapView(
                            markers = approvedMarkers,
                            onMarkerClick = { marker ->
                                viewModel.selectMarker(marker)
                            },
                            onMapTap = { lat, lon ->
                                tappedMapLocation = Pair(lat, lon)
                            },
                            userLocationEnabled = hasLocationPermission,
                            tileSourceType = tileSourceType,
                            targetLocation = mapTargetLocation,
                            userLocation = userLocation,
                            avatarSkin = shopState.selectedAvatarSkin,
                            route = activeRoute,
                            onUserLocationChanged = { lat, lon ->
                                viewModel.updateUserLocation(lat, lon)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Controls Overlay: Search Bar, Language switcher & Action Chips
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Search bar row with Language toggle
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.setSearchQuery(it) },
                                        placeholder = { Text(Strings.searchPlaceholder, maxLines = 1) },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }

                                    // Language quick switch
                                    IconButton(
                                        onClick = {
                                            Strings.currentLanguage = if (Strings.isUk) AppLanguage.ENGLISH else AppLanguage.UKRAINIAN
                                        }
                                    ) {
                                        Text(
                                            text = Strings.currentLanguage.flag,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal Category Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == null,
                                        onClick = { viewModel.setCategoryFilter(null) },
                                        label = { Text("${Strings.allCategories} (${approvedMarkers.size})") },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                                items(MarkerCategory.entries) { cat ->
                                    val isSelected = selectedCategoryFilter == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setCategoryFilter(if (isSelected) null else cat)
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(cat.color)
                                            )
                                        },
                                        label = { Text(cat.localizedTitle(Strings.isUk)) },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }

                            // Quick Action Navigation Chips: Route to Kyiv & Call Taxi
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = { viewModel.routeToKyiv() },
                                    label = { Text(Strings.routeToKyiv, fontWeight = FontWeight.SemiBold) },
                                    icon = {
                                        Icon(
                                            Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                    )
                                )

                                if (activeRoute != null) {
                                    SuggestionChip(
                                        onClick = { viewModel.setTaxiDialogOpen(true) },
                                        label = { Text(Strings.callTaxiButton, fontWeight = FontWeight.Bold) },
                                        icon = {
                                            Icon(
                                                Icons.Default.LocalTaxi,
                                                contentDescription = null,
                                                tint = Color(0xFF854D0E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0xFFFEF08A)
                                        )
                                    )
                                }
                            }
                        }

                        // Right-side floating map controls: My Location, Map Style
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Map style switcher
                            SmallFloatingActionButton(
                                onClick = {
                                    tileSourceType = when (tileSourceType) {
                                        "standard" -> "topo"
                                        "topo" -> "satellite"
                                        else -> "standard"
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = Strings.changeMapLayer
                                )
                            }

                            // My location button
                            SmallFloatingActionButton(
                                onClick = {
                                    if (hasLocationPermission) {
                                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                        val loc = try {
                                            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                        } catch (_: SecurityException) {
                                            null
                                        }
                                        if (loc != null) {
                                            mapTargetLocation = Pair(loc.latitude, loc.longitude)
                                        } else {
                                            mapTargetLocation = Pair(50.4501, 30.5234)
                                        }
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = Strings.myLocation
                                )
                            }
                        }

                        // Bottom Help Hint banner (hidden when route active)
                        if (activeRoute == null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 80.dp, start = 20.dp, end = 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Strings.tapHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Floating Route Details Card when route is active
                        AnimatedVisibility(
                            visible = activeRoute != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 75.dp, start = 14.dp, end = 14.dp)
                        ) {
                            activeRoute?.let { route ->
                                Card(
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Navigation,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = route.destinationName,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = Strings.routeTitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.clearRoute() },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = Strings.clearRouteButton,
                                                    tint = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Detailed Stats Row: Distance, Walking ("щоб дійти"), Driving, Taxi
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Distance
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = Strings.distanceLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (Strings.isUk) route.formattedDistance else route.formattedDistanceEn,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            VerticalDivider(modifier = Modifier.height(26.dp))

                                            // Walking time ("скільки хвилин щоб дійти")
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = Strings.walkingTimeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (Strings.isUk) route.formattedWalkingDuration else route.formattedWalkingDurationEn,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color(0xFF0D9488) // teal
                                                )
                                            }

                                            VerticalDivider(modifier = Modifier.height(26.dp))

                                            // Driving time
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = Strings.drivingTimeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (Strings.isUk) route.formattedDuration else route.formattedDurationEn,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Call Taxi Button
                                        Button(
                                            onClick = { viewModel.setTaxiDialogOpen(true) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFACC15),
                                                contentColor = Color(0xFF713F12)
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalTaxi,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${Strings.callTaxiButton} (~${route.estimatedTaxiFareUah} грн)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Route calculation in progress modal
                        if (isCalculatingRoute) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = Strings.routeCalculating,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = Strings.routeCalculatingSub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Add Marker FAB
                        ExtendedFloatingActionButton(
                            onClick = {
                                val loc = mapTargetLocation ?: Pair(50.4501, 30.5234)
                                viewModel.startAddingMarkerAt(loc.first, loc.second)
                            },
                            icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null) },
                            text = { Text(Strings.addMarker) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 16.dp, end = 16.dp)
                                .testTag("add_marker_fab"),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                1 -> {
                    // TAB 1: MY MARKERS
                    MyMarkersScreen(
                        markers = userMarkers,
                        onMarkerClick = { marker ->
                            viewModel.selectMarker(marker)
                        },
                        onNavigateToMap = { marker ->
                            mapTargetLocation = Pair(marker.latitude, marker.longitude)
                            currentTab = 0
                        }
                    )
                }

                2 -> {
                    // TAB 2: SHOP SCREEN
                    ShopScreen(
                        shopState = shopState,
                        onBuyAvatarSkin = { skinId, price -> viewModel.buyAvatarSkin(skinId, price) },
                        onSelectAvatarSkin = { skinId -> viewModel.selectAvatarSkin(skinId) },
                        onBuyMarkerSkin = { skinId, price -> viewModel.buyMarkerSkin(skinId, price) },
                        onSelectMarkerSkin = { skinId -> viewModel.selectMarkerSkin(skinId) }
                    )
                }

                3 -> {
                    if (isAdmin) {
                        // TAB 3: MODERATION SCREEN
                        ModerationScreen(
                            pendingMarkers = pendingMarkers,
                            onApprove = { marker ->
                                viewModel.approveMarker(marker.id)
                            },
                            onReject = { marker, reason ->
                                viewModel.rejectMarker(marker.id, reason)
                            },
                            onMarkerClick = { marker ->
                                viewModel.selectMarker(marker)
                            }
                        )
                    } else {
                        // Profile Screen for non-admin
                        ProfileScreen(
                            userId = viewModel.currentUserId,
                            userName = viewModel.currentUserName,
                            userEmail = viewModel.currentUserEmail,
                            isAdmin = isAdmin,
                            points = shopState.points,
                            avatarSkin = shopState.selectedAvatarSkin,
                            userMarkersCount = userMarkers.size,
                            approvedMarkersCount = userMarkers.count { it.status == "approved" },
                            onToggleAdminMode = { viewModel.toggleAdminMode() },
                            onSignOut = {
                                AuthManager.signOut(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onSignOut,
                                    scope = coroutineScope
                                )
                            }
                        )
                    }
                }

                4 -> {
                    // TAB 4: PROFILE SCREEN (when admin is present)
                    ProfileScreen(
                        userId = viewModel.currentUserId,
                        userName = viewModel.currentUserName,
                        userEmail = viewModel.currentUserEmail,
                        isAdmin = isAdmin,
                        points = shopState.points,
                        avatarSkin = shopState.selectedAvatarSkin,
                        userMarkersCount = userMarkers.size,
                        approvedMarkersCount = userMarkers.count { it.status == "approved" },
                        onToggleAdminMode = { viewModel.toggleAdminMode() },
                        onSignOut = {
                            AuthManager.signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOut,
                                scope = coroutineScope
                            )
                        }
                    )
                }
            }
        }
    }

    // Tapped Map Point Bottom Dialog (Navigate here OR Add Marker here)
    tappedMapLocation?.let { (lat, lon) ->
        AlertDialog(
            onDismissRequest = { tappedMapLocation = null },
            icon = {
                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text(
                    text = Strings.pointOnMap,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = String.format(Locale.US, "%.5f, %.5f", lat, lon),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Button 1: Route here
                    Button(
                        onClick = {
                            val name = if (Strings.isUk) {
                                "Місце (${String.format(Locale.US, "%.4f, %.4f", lat, lon)})"
                            } else {
                                "Location (${String.format(Locale.US, "%.4f, %.4f", lat, lon)})"
                            }
                            viewModel.calculateRouteTo(lat, lon, name)
                            tappedMapLocation = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.routeHere)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 2: Add Marker here
                    OutlinedButton(
                        onClick = {
                            viewModel.startAddingMarkerAt(lat, lon)
                            tappedMapLocation = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.addMarker)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { tappedMapLocation = null }) {
                    Text(Strings.cancel)
                }
            }
        )
    }

    // Add / Edit Marker Dialog
    if (isAddingMarker && markerDraftLocation != null) {
        AddEditMarkerDialog(
            initialLocation = markerDraftLocation!!,
            editingMarker = editingMarker,
            unlockedMarkerSkins = shopState.unlockedMarkerSkins,
            defaultMarkerSkin = shopState.activeMarkerSkin,
            isSubmitting = isSubmitting,
            onDismiss = { viewModel.cancelAddingMarker() },
            onSubmit = { title, desc, cat, lat, lon, img, skin ->
                viewModel.submitMarker(title, desc, cat, lat, lon, img, skin)
            }
        )
    }

    // Marker Details Bottom Sheet
    selectedMarker?.let { marker ->
        MarkerDetailBottomSheet(
            marker = marker,
            currentUserId = viewModel.currentUserId,
            isAdmin = isAdmin,
            onDismiss = { viewModel.selectMarker(null) },
            onEditClicked = { m -> viewModel.startEditingMarker(m) },
            onDeleteClicked = { m -> viewModel.deleteMarker(m.id) },
            onApproveClicked = { m -> viewModel.approveMarker(m.id) },
            onRejectClicked = { m, reason -> viewModel.rejectMarker(m.id, reason) },
            onRouteClicked = { m ->
                viewModel.calculateRouteTo(m.latitude, m.longitude, m.title)
                viewModel.selectMarker(null)
            }
        )
    }

    // Taxi Call Dialog
    if (isTaxiDialogOpen && activeRoute != null) {
        TaxiCallDialog(
            routeInfo = activeRoute!!,
            onDismiss = { viewModel.setTaxiDialogOpen(false) }
        )
    }
}

@Composable
fun MyMarkersScreen(
    markers: List<Marker>,
    onMarkerClick: (Marker) -> Unit,
    onNavigateToMap: (Marker) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val filteredMarkers = remember(markers, selectedFilter) {
        if (selectedFilter == null) markers else markers.filter { it.status == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = Strings.tabMyMarkers,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (Strings.isUk) "Всі додані вами локації та їхній статус перевірки" else "All your added locations and review status",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("${Strings.allCategories} (${markers.size})") }
            )
            FilterChip(
                selected = selectedFilter == "pending",
                onClick = { selectedFilter = if (selectedFilter == "pending") null else "pending" },
                label = { Text("${if (Strings.isUk) "На перевірці" else "Pending"} (${markers.count { it.status == "pending" }})") }
            )
            FilterChip(
                selected = selectedFilter == "approved",
                onClick = { selectedFilter = if (selectedFilter == "approved") null else "approved" },
                label = { Text("${if (Strings.isUk) "Схвалені" else "Approved"} (${markers.count { it.status == "approved" }})") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredMarkers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (Strings.isUk) "Міток у цьому розділі поки немає" else "No markers found in this category",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMarkers, key = { it.id }) { marker ->
                    MarkerCard(
                        marker = marker,
                        onClick = { onMarkerClick(marker) },
                        onMapClick = { onNavigateToMap(marker) }
                    )
                }
            }
        }
    }
}

@Composable
fun ModerationScreen(
    pendingMarkers: List<Marker>,
    onApprove: (Marker) -> Unit,
    onReject: (Marker, String) -> Unit,
    onMarkerClick: (Marker) -> Unit
) {
    var rejectingMarker by remember { mutableStateOf<Marker?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = Strings.moderationTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${Strings.pendingReview}: ${pendingMarkers.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pendingMarkers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = Strings.allApprovedMsg,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = Strings.allApprovedSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pendingMarkers, key = { it.id }) { marker ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMarkerClick(marker) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Photo if attached
                            if (!marker.imageUrl.isNullOrBlank()) {
                                MarkerImageView(
                                    imageUrl = marker.imageUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = marker.categoryEnum.color.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = marker.categoryEnum.localizedTitle(Strings.isUk),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = marker.categoryEnum.color,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (marker.markerSkin != "default") {
                                        val skin = marker.markerSkinItem
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = skin.pinColor.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "${skin.emoji} ${if (Strings.isUk) skin.nameUa else skin.nameEn}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = skin.pinColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = String.format("%.4f, %.4f", marker.latitude, marker.longitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = marker.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (marker.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = marker.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "${Strings.author}: ${marker.authorName} (${marker.userEmail})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Approve & Reject buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onApprove(marker) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Strings.approve)
                                }

                                OutlinedButton(
                                    onClick = { rejectingMarker = marker },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Strings.reject)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rejectingMarker?.let { marker ->
        AlertDialog(
            onDismissRequest = { rejectingMarker = null },
            title = { Text(Strings.rejectionReasonTitle) },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    placeholder = { Text(if (Strings.isUk) "Вкажіть причину..." else "Enter reason...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = if (rejectReason.isNotBlank()) rejectReason else (if (Strings.isUk) "Не відповідає вимогам" else "Does not meet requirements")
                        onReject(marker, reason)
                        rejectingMarker = null
                        rejectReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Strings.reject)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingMarker = null }) {
                    Text(Strings.cancel)
                }
            }
        )
    }
}

@Composable
fun ProfileScreen(
    userId: String,
    userName: String,
    userEmail: String,
    isAdmin: Boolean,
    points: Int,
    avatarSkin: String,
    userMarkersCount: Int,
    approvedMarkersCount: Int,
    onToggleAdminMode: () -> Unit,
    onSignOut: () -> Unit
) {
    val currentAvatar = SkinCatalog.getAvatar(avatarSkin)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Avatar Initial / Emoji
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(currentAvatar.accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentAvatar.emoji,
                fontSize = 42.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (userName.isNotBlank()) userName else (if (Strings.isUk) "Користувач" else "Explorer"),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = userEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAdmin) Strings.officialModerator else Strings.userStatus,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Row (Points, Markers, Approved)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$points",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFFD97706)
                    )
                    Text(
                        text = Strings.pointsEarnedStat,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$userMarkersCount",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Strings.markersAddedStat,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$approvedMarkersCount",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = Strings.markersApprovedStat,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Switcher Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Strings.languageSection,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { Strings.currentLanguage = AppLanguage.UKRAINIAN },
                        colors = if (Strings.isUk) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🇺🇦 Українська", fontWeight = if (Strings.isUk) FontWeight.Bold else FontWeight.Normal)
                    }

                    OutlinedButton(
                        onClick = { Strings.currentLanguage = AppLanguage.ENGLISH },
                        colors = if (!Strings.isUk) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🇬🇧 English", fontWeight = if (!Strings.isUk) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Moderator Info & Testing Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.testModeratorToggle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Strings.moderatorAccountsInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAdmin,
                        onCheckedChange = { onToggleAdminMode() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // App Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Strings.aboutApp,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Strings.aboutAppDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sign Out Button
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("sign_out_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.signOut)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MarkerCard(
    marker: Marker,
    onClick: () -> Unit,
    onMapClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Photo thumbnail if present
            if (!marker.imageUrl.isNullOrBlank()) {
                MarkerImageView(
                    imageUrl = marker.imageUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = marker.categoryEnum.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = marker.categoryEnum.localizedTitle(Strings.isUk),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = marker.categoryEnum.color,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (marker.markerSkin != "default") {
                        val skin = marker.markerSkinItem
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = skin.pinColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "${skin.emoji} ${if (Strings.isUk) skin.nameUa else skin.nameEn}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = skin.pinColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = marker.statusEnum.color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = marker.statusEnum.localizedTitle(Strings.isUk),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = marker.statusEnum.color,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = marker.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (marker.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = marker.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.4f, %.4f", marker.latitude, marker.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                TextButton(
                    onClick = onMapClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (Strings.isUk) "Показати на карті" else "Show on map", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
