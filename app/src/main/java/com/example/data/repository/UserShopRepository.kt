package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserShopState(
    val points: Int = 100,
    val selectedAvatarSkin: String = "human",
    val unlockedAvatarSkins: Set<String> = setOf("human"),
    val activeMarkerSkin: String = "default",
    val unlockedMarkerSkins: Set<String> = setOf("default")
)

class UserShopRepository(
    private val context: Context,
    private val userId: String
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("uexmap_user_$userId", Context.MODE_PRIVATE)
    private val db: FirebaseFirestore = try {
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    } catch (_: Exception) {
        FirebaseFirestore.getInstance()
    }

    private val _shopState = MutableStateFlow(loadFromPrefs())
    val shopState: StateFlow<UserShopState> = _shopState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Sync with Firestore in background
        scope.launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    val points = doc.getLong("points")?.toInt() ?: _shopState.value.points
                    val avatar = doc.getString("selectedAvatarSkin") ?: _shopState.value.selectedAvatarSkin
                    @Suppress("UNCHECKED_CAST")
                    val unlockedAvatars = (doc.get("unlockedAvatarSkins") as? List<String>)?.toSet()
                        ?: _shopState.value.unlockedAvatarSkins
                    val markerSkin = doc.getString("activeMarkerSkin") ?: _shopState.value.activeMarkerSkin
                    @Suppress("UNCHECKED_CAST")
                    val unlockedMarkers = (doc.get("unlockedMarkerSkins") as? List<String>)?.toSet()
                        ?: _shopState.value.unlockedMarkerSkins

                    val newState = UserShopState(
                        points = points,
                        selectedAvatarSkin = avatar,
                        unlockedAvatarSkins = unlockedAvatars + "human",
                        activeMarkerSkin = markerSkin,
                        unlockedMarkerSkins = unlockedMarkers + "default"
                    )
                    _shopState.value = newState
                    saveToPrefs(newState)
                } else {
                    // Initialize doc
                    saveToFirestore(_shopState.value)
                }
            } catch (_: Exception) {
                // Ignore network error and keep local prefs
            }
        }
    }

    private fun loadFromPrefs(): UserShopState {
        val points = prefs.getInt("points", 150) // welcome bonus 150 points
        val selectedAvatar = prefs.getString("selectedAvatarSkin", "human") ?: "human"
        val unlockedAvatars = prefs.getStringSet("unlockedAvatarSkins", setOf("human")) ?: setOf("human")
        val activeMarker = prefs.getString("activeMarkerSkin", "default") ?: "default"
        val unlockedMarkers = prefs.getStringSet("unlockedMarkerSkins", setOf("default")) ?: setOf("default")

        return UserShopState(
            points = points,
            selectedAvatarSkin = selectedAvatar,
            unlockedAvatarSkins = unlockedAvatars + "human",
            activeMarkerSkin = activeMarker,
            unlockedMarkerSkins = unlockedMarkers + "default"
        )
    }

    private fun saveToPrefs(state: UserShopState) {
        prefs.edit()
            .putInt("points", state.points)
            .putString("selectedAvatarSkin", state.selectedAvatarSkin)
            .putStringSet("unlockedAvatarSkins", state.unlockedAvatarSkins)
            .putString("activeMarkerSkin", state.activeMarkerSkin)
            .putStringSet("unlockedMarkerSkins", state.unlockedMarkerSkins)
            .apply()
    }

    private fun saveToFirestore(state: UserShopState) {
        scope.launch {
            try {
                db.collection("users").document(userId).set(
                    mapOf(
                        "points" to state.points,
                        "selectedAvatarSkin" to state.selectedAvatarSkin,
                        "unlockedAvatarSkins" to state.unlockedAvatarSkins.toList(),
                        "activeMarkerSkin" to state.activeMarkerSkin,
                        "unlockedMarkerSkins" to state.unlockedMarkerSkins.toList(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {
            }
        }
    }

    fun addPoints(amount: Int) {
        val current = _shopState.value
        val newState = current.copy(points = current.points + amount)
        _shopState.value = newState
        saveToPrefs(newState)
        saveToFirestore(newState)
    }

    fun buyAvatarSkin(skinId: String, price: Int): Boolean {
        val current = _shopState.value
        if (current.unlockedAvatarSkins.contains(skinId)) {
            // Already owned, just select it
            selectAvatarSkin(skinId)
            return true
        }
        if (current.points >= price) {
            val newState = current.copy(
                points = current.points - price,
                unlockedAvatarSkins = current.unlockedAvatarSkins + skinId,
                selectedAvatarSkin = skinId
            )
            _shopState.value = newState
            saveToPrefs(newState)
            saveToFirestore(newState)
            return true
        }
        return false
    }

    fun selectAvatarSkin(skinId: String) {
        val current = _shopState.value
        if (current.unlockedAvatarSkins.contains(skinId)) {
            val newState = current.copy(selectedAvatarSkin = skinId)
            _shopState.value = newState
            saveToPrefs(newState)
            saveToFirestore(newState)
        }
    }

    fun buyMarkerSkin(skinId: String, price: Int): Boolean {
        val current = _shopState.value
        if (current.unlockedMarkerSkins.contains(skinId)) {
            selectMarkerSkin(skinId)
            return true
        }
        if (current.points >= price) {
            val newState = current.copy(
                points = current.points - price,
                unlockedMarkerSkins = current.unlockedMarkerSkins + skinId,
                activeMarkerSkin = skinId
            )
            _shopState.value = newState
            saveToPrefs(newState)
            saveToFirestore(newState)
            return true
        }
        return false
    }

    fun selectMarkerSkin(skinId: String) {
        val current = _shopState.value
        if (current.unlockedMarkerSkins.contains(skinId)) {
            val newState = current.copy(activeMarkerSkin = skinId)
            _shopState.value = newState
            saveToPrefs(newState)
            saveToFirestore(newState)
        }
    }
}
