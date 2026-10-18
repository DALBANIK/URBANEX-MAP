package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Marker
import com.example.data.model.MarkerCategory
import com.example.data.model.RouteInfo
import com.example.data.repository.MarkerRepository
import com.example.data.repository.RouteRepository
import com.example.data.repository.UserShopRepository
import com.example.data.repository.UserShopState
import com.example.data.util.Strings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: MarkerRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String,
    private val userShopRepository: UserShopRepository,
    private val routeRepository: RouteRepository = RouteRepository()
) : ViewModel() {

    val shopState: StateFlow<UserShopState> = userShopRepository.shopState

    private val _selectedCategoryFilter = MutableStateFlow<MarkerCategory?>(null)
    val selectedCategoryFilter: StateFlow<MarkerCategory?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMarker = MutableStateFlow<Marker?>(null)
    val selectedMarker: StateFlow<Marker?> = _selectedMarker.asStateFlow()

    private val _markerDraftLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val markerDraftLocation: StateFlow<Pair<Double, Double>?> = _markerDraftLocation.asStateFlow()

    private val _isAddingMarker = MutableStateFlow(false)
    val isAddingMarker: StateFlow<Boolean> = _isAddingMarker.asStateFlow()

    private val _editingMarker = MutableStateFlow<Marker?>(null)
    val editingMarker: StateFlow<Marker?> = _editingMarker.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    // Navigation & Routing state
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _activeRoute = MutableStateFlow<RouteInfo?>(null)
    val activeRoute: StateFlow<RouteInfo?> = _activeRoute.asStateFlow()

    private val _isCalculatingRoute = MutableStateFlow(false)
    val isCalculatingRoute: StateFlow<Boolean> = _isCalculatingRoute.asStateFlow()

    private val _isTaxiDialogOpen = MutableStateFlow(false)
    val isTaxiDialogOpen: StateFlow<Boolean> = _isTaxiDialogOpen.asStateFlow()

    init {
        viewModelScope.launch {
            _isAdmin.value = repository.isUserAdmin(currentUserEmail, currentUserId)
        }
    }

    fun toggleAdminMode() {
        _isAdmin.value = !_isAdmin.value
    }

    fun setAdminMode(enabled: Boolean) {
        _isAdmin.value = enabled
    }

    // Shop actions
    fun buyAvatarSkin(skinId: String, price: Int) {
        val success = userShopRepository.buyAvatarSkin(skinId, price)
        if (success) {
            _actionMessage.value = Strings.purchaseSuccess
        } else {
            _actionMessage.value = Strings.notEnoughPoints
        }
    }

    fun selectAvatarSkin(skinId: String) {
        userShopRepository.selectAvatarSkin(skinId)
        _actionMessage.value = Strings.skinSelectedMessage
    }

    fun buyMarkerSkin(skinId: String, price: Int) {
        val success = userShopRepository.buyMarkerSkin(skinId, price)
        if (success) {
            _actionMessage.value = Strings.purchaseSuccess
        } else {
            _actionMessage.value = Strings.notEnoughPoints
        }
    }

    fun selectMarkerSkin(skinId: String) {
        userShopRepository.selectMarkerSkin(skinId)
        _actionMessage.value = Strings.skinSelectedMessage
    }

    // Raw stream of approved markers from Firestore
    private val rawApprovedMarkers: StateFlow<List<Marker>> = repository.observeApprovedMarkers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Filtered approved markers for map display
    val approvedMarkers: StateFlow<List<Marker>> = combine(
        rawApprovedMarkers,
        _selectedCategoryFilter,
        _searchQuery
    ) { markers, categoryFilter, query ->
        markers.filter { marker ->
            val matchesCategory = categoryFilter == null || marker.category == categoryFilter.key
            val matchesQuery = query.isBlank() ||
                    marker.title.contains(query, ignoreCase = true) ||
                    marker.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Stream of current user's markers
    val userMarkers: StateFlow<List<Marker>> = repository.observeUserMarkers(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Stream of pending markers for moderation (for admin)
    val pendingMarkers: StateFlow<List<Marker>> = repository.observePendingMarkers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun setCategoryFilter(category: MarkerCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectMarker(marker: Marker?) {
        _selectedMarker.value = marker
    }

    fun startAddingMarkerAt(lat: Double, lon: Double) {
        _markerDraftLocation.value = Pair(lat, lon)
        _isAddingMarker.value = true
    }

    fun cancelAddingMarker() {
        _isAddingMarker.value = false
        _markerDraftLocation.value = null
        _editingMarker.value = null
    }

    fun startEditingMarker(marker: Marker) {
        _editingMarker.value = marker
        _markerDraftLocation.value = Pair(marker.latitude, marker.longitude)
        _isAddingMarker.value = true
        _selectedMarker.value = null
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun updateUserLocation(lat: Double, lon: Double) {
        _userLocation.value = Pair(lat, lon)
    }

    // Build route from current user location (or default starting point) to destination
    fun calculateRouteTo(destLat: Double, destLon: Double, destName: String) {
        val start = _userLocation.value ?: Pair(50.4501, 30.5234)
        viewModelScope.launch {
            _isCalculatingRoute.value = true
            _selectedMarker.value = null
            try {
                val route = routeRepository.calculateRoute(
                    startLat = start.first,
                    startLon = start.second,
                    destLat = destLat,
                    destLon = destLon,
                    destinationName = destName
                )
                _activeRoute.value = route
                val durationText = if (Strings.isUk) route.formattedDuration else route.formattedDurationEn
                val distanceText = if (Strings.isUk) route.formattedDistance else route.formattedDistanceEn
                val walkText = if (Strings.isUk) route.formattedWalkingDuration else route.formattedWalkingDurationEn
                _actionMessage.value = if (Strings.isUk) {
                    "Маршрут до «$destName»: $distanceText (на авто: $durationText, пішки: $walkText)"
                } else {
                    "Route to \"$destName\": $distanceText (car: $durationText, walking: $walkText)"
                }
            } catch (e: Exception) {
                _actionMessage.value = "Error calculating route: ${e.localizedMessage}"
            } finally {
                _isCalculatingRoute.value = false
            }
        }
    }

    // Quick shortcut: route to Kyiv city center (Майдан Незалежності)
    fun routeToKyiv() {
        calculateRouteTo(
            destLat = 50.4501,
            destLon = 30.5234,
            destName = if (Strings.isUk) "м. Київ (Центр)" else "Kyiv (City Center)"
        )
    }

    fun clearRoute() {
        _activeRoute.value = null
    }

    fun setTaxiDialogOpen(open: Boolean) {
        _isTaxiDialogOpen.value = open
    }

    fun submitMarker(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        imageUrl: String? = null,
        markerSkin: String = "default"
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val editing = _editingMarker.value
            if (editing != null) {
                val result = repository.updatePendingMarker(
                    markerId = editing.id,
                    title = title,
                    description = description,
                    category = category,
                    latitude = latitude,
                    longitude = longitude,
                    imageUrl = imageUrl,
                    markerSkin = markerSkin
                )
                if (result.isSuccess) {
                    _actionMessage.value = if (Strings.isUk) "Мітку успішно оновлено!" else "Marker updated successfully!"
                    cancelAddingMarker()
                } else {
                    _actionMessage.value = "Помилка оновлення: ${result.exceptionOrNull()?.localizedMessage}"
                }
            } else {
                val result = repository.createMarker(
                    title = title,
                    description = description,
                    category = category,
                    latitude = latitude,
                    longitude = longitude,
                    authorName = currentUserName,
                    userEmail = currentUserEmail,
                    imageUrl = imageUrl,
                    markerSkin = markerSkin
                )
                if (result.isSuccess) {
                    _actionMessage.value = if (Strings.isUk) {
                        "Мітку надіслано на модерацію! Після схвалення ви отримаєте +50 поінтів."
                    } else {
                        "Marker submitted for moderation! You'll get +50 points once approved."
                    }
                    cancelAddingMarker()
                } else {
                    _actionMessage.value = "Помилка додавання: ${result.exceptionOrNull()?.localizedMessage}"
                }
            }
            _isSubmitting.value = false
        }
    }

    fun approveMarker(markerId: String, note: String? = null) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.moderateMarker(
                markerId = markerId,
                newStatus = "approved",
                note = note ?: if (Strings.isUk) "Схвалено модератором" else "Approved by moderator",
                moderatorEmail = currentUserEmail
            )
            if (result.isSuccess) {
                // If moderator is also author or for testing, give current user points too
                userShopRepository.addPoints(50)
                _actionMessage.value = if (Strings.isUk) {
                    "Мітку схвалено! Автору нараховано +50 поінтів."
                } else {
                    "Marker approved! Author received +50 points."
                }
                _selectedMarker.value = null
            } else {
                _actionMessage.value = "Помилка схвалення: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _isSubmitting.value = false
        }
    }

    fun rejectMarker(markerId: String, reason: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.moderateMarker(
                markerId = markerId,
                newStatus = "rejected",
                note = reason,
                moderatorEmail = currentUserEmail
            )
            if (result.isSuccess) {
                _actionMessage.value = if (Strings.isUk) "Мітку відхилено." else "Marker rejected."
                _selectedMarker.value = null
            } else {
                _actionMessage.value = "Помилка відхилення: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _isSubmitting.value = false
        }
    }

    fun deleteMarker(markerId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.deleteMarker(markerId)
            if (result.isSuccess) {
                _actionMessage.value = if (Strings.isUk) "Мітку видалено." else "Marker deleted."
                _selectedMarker.value = null
            } else {
                _actionMessage.value = "Помилка видалення: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _isSubmitting.value = false
        }
    }
}
