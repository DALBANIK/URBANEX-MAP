package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Marker
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MarkerRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth
    private val markersCollection = db.collection("markers")
    private val adminsCollection = db.collection("admins")

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // 1. Observe public, approved markers for displaying on the map
    fun observeApprovedMarkers(): Flow<List<Marker>> {
        return markersCollection
            .whereEqualTo("status", "approved")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Marker::class.java)
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "markers")
                }
                throw error
            }
    }

    // 2. Observe the current user's markers (all statuses: pending, approved, rejected)
    fun observeUserMarkers(userId: String): Flow<List<Marker>> {
        return markersCollection
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Marker::class.java)
                    .sortedByDescending { it.createdAt }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "markers")
                }
                throw error
            }
    }

    // 3. Observe markers needing moderation (for administrators/moderators)
    fun observePendingMarkers(): Flow<List<Marker>> {
        return markersCollection
            .whereEqualTo("status", "pending")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Marker::class.java)
                    .sortedByDescending { it.createdAt }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "markers")
                }
                throw error
            }
    }

    // 4. Create new marker (submitted for moderation)
    suspend fun createMarker(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        authorName: String,
        userEmail: String,
        imageUrl: String? = null,
        markerSkin: String = "default"
    ): Result<String> {
        val uid = requireUserId()
        val docRef = markersCollection.document()
        val payload = mutableMapOf<String, Any>(
            "title" to title.trim(),
            "description" to description.trim(),
            "category" to category,
            "latitude" to latitude,
            "longitude" to longitude,
            "userId" to uid,
            "userEmail" to userEmail,
            "authorName" to if (authorName.isNotBlank()) authorName else "Користувач",
            "status" to "pending",
            "markerSkin" to markerSkin,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!imageUrl.isNullOrBlank()) {
            payload["imageUrl"] = imageUrl
        }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // 5. Update marker by author (only if still pending)
    suspend fun updatePendingMarker(
        markerId: String,
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        imageUrl: String? = null,
        markerSkin: String? = null
    ): Result<Unit> {
        requireUserId()
        val docRef = markersCollection.document(markerId)
        val payload = mutableMapOf<String, Any>(
            "title" to title.trim(),
            "description" to description.trim(),
            "category" to category,
            "latitude" to latitude,
            "longitude" to longitude,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (imageUrl != null) {
            payload["imageUrl"] = imageUrl
        }
        if (markerSkin != null) {
            payload["markerSkin"] = markerSkin
        }

        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // 6. Moderate marker (Approve or Reject by Moderator/Admin)
    suspend fun moderateMarker(
        markerId: String,
        newStatus: String,
        note: String?,
        moderatorEmail: String
    ): Result<Unit> {
        requireUserId()
        val docRef = markersCollection.document(markerId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "moderatedBy" to moderatorEmail,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!note.isNullOrBlank()) {
            updates["moderatorNote"] = note.trim()
        }

        return try {
            docRef.update(updates).await()

            // If approved, reward author with +50 points!
            if (newStatus == "approved") {
                try {
                    val snapshot = docRef.get().await()
                    val authorUid = snapshot.getString("userId")
                    if (!authorUid.isNullOrBlank()) {
                        val userDoc = db.collection("users").document(authorUid)
                        userDoc.set(
                            mapOf(
                                "points" to FieldValue.increment(50),
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        ).await()
                    }
                } catch (_: Exception) {
                    // Non-fatal if user doc update fails
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // 7. Delete marker (Author or Admin)
    suspend fun deleteMarker(markerId: String): Result<Unit> {
        requireUserId()
        val docRef = markersCollection.document(markerId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // 8. Check if user has administrative / moderator rights
    suspend fun isUserAdmin(email: String?, uid: String): Boolean {
        val moderatorEmails = setOf(
            "slonecdaniil1922@gmail.com",
            "daniilslonec2000@gmail.com",
            "tancikozenden@gmail.com"
        )
        if (email != null && moderatorEmails.any { it.equals(email.trim(), ignoreCase = true) }) {
            return true
        }
        return try {
            val doc = adminsCollection.document(uid).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
}
