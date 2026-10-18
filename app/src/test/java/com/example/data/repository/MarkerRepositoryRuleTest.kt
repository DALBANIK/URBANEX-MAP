package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Marker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MarkerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createMarker_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = MarkerRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createMarker(
                title = "Golden Gate Park",
                description = "Beautiful scenic park",
                category = "nature",
                latitude = 50.4501,
                longitude = 30.5234,
                authorName = "Alice",
                userEmail = ALICE_EMAIL
            )
        }

        assertTrue(result.isSuccess)
        val markerId = result.getOrThrow()
        assertTrue(markerId.isNotEmpty())
    }

    @Test
    fun observeUserMarkers_authenticatedOwner_returnsMatchingMarkers() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = MarkerRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createMarker(
                title = "Museum",
                description = "Historical museum",
                category = "history",
                latitude = 50.45,
                longitude = 30.52,
                authorName = "Alice",
                userEmail = ALICE_EMAIL
            )
        }
        val markerId = createResult.getOrThrow()

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserMarkers(aliceUid).first { markers: List<Marker> ->
                markers.any { it.id == markerId }
            }
        }
        assertTrue(list.any { it.id == markerId })
    }

    @Test
    fun observeApprovedMarkers_returnsApprovedMarkers() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = MarkerRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createMarker(
                title = "Approved Spot",
                description = "Verified place",
                category = "tourism",
                latitude = 50.45,
                longitude = 30.52,
                authorName = "Alice",
                userEmail = ALICE_EMAIL
            )
        }
        val markerId = createResult.getOrThrow()

        // Admin signs in and approves marker
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = MarkerRepository(firestore)
        val modResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.moderateMarker(markerId, "approved", "Approved by admin", ADMIN_EMAIL)
        }
        assertTrue(modResult.isSuccess)

        // Any authenticated user can now observe it
        signInTestUser(ALICE_EMAIL)
        val approvedList = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeApprovedMarkers().first { markers: List<Marker> ->
                markers.any { it.id == markerId }
            }
        }
        assertTrue(approvedList.any { it.id == markerId })
    }

    @Test
    fun observeMarkers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = MarkerRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeApprovedMarkers().first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val isPermissionDenied = (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    (e.cause as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    (e.cause?.cause as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            assertTrue("Expected PERMISSION_DENIED but got: $e", isPermissionDenied)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val ADMIN_EMAIL = "slonecdaniil1922@gmail.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
