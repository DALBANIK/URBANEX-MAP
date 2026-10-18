package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MarkerCategory(
    val key: String,
    val titleUa: String,
    val titleEn: String,
    val color: Color
) {
    TOURISM("tourism", "Туризм і пам'ятки", "Tourism & Landmarks", CatTourism),
    FOOD("food", "Їжа та заклади", "Food & Dining", CatFood),
    NATURE("nature", "Природа і парки", "Nature & Parks", CatNature),
    HISTORY("history", "Історія і культура", "History & Culture", CatHistory),
    HAZARD("hazard", "Увага / Попередження", "Caution / Hazard", CatHazard),
    OTHER("other", "Інше", "Other", CatOther);

    fun localizedTitle(isUk: Boolean): String = if (isUk) titleUa else titleEn

    companion object {
        fun fromKey(key: String): MarkerCategory {
            return entries.firstOrNull { it.key == key } ?: OTHER
        }
    }
}

enum class ModerationStatus(
    val key: String,
    val titleUa: String,
    val titleEn: String,
    val color: Color
) {
    PENDING("pending", "На модерації", "Pending Review", StatusPending),
    APPROVED("approved", "Схвалено", "Approved", StatusApproved),
    REJECTED("rejected", "Відхилено", "Rejected", StatusRejected);

    fun localizedTitle(isUk: Boolean): String = if (isUk) titleUa else titleEn

    companion object {
        fun fromKey(key: String): ModerationStatus {
            return entries.firstOrNull { it.key == key } ?: PENDING
        }
    }
}

@IgnoreExtraProperties
data class Marker(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "other",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val userId: String = "",
    val userEmail: String = "",
    val authorName: String = "",
    val status: String = "pending",
    val moderatorNote: String? = null,
    val moderatedBy: String? = null,
    val imageUrl: String? = null,
    val markerSkin: String = "default",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: MarkerCategory
        get() = MarkerCategory.fromKey(category)

    val statusEnum: ModerationStatus
        get() = ModerationStatus.fromKey(status)

    val markerSkinItem: MarkerSkinItem
        get() = SkinCatalog.getMarkerSkin(markerSkin)

    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "title" to title,
            "description" to description,
            "category" to category,
            "latitude" to latitude,
            "longitude" to longitude,
            "userId" to userId,
            "userEmail" to userEmail,
            "authorName" to authorName,
            "status" to "pending",
            "moderatorNote" to moderatorNote,
            "moderatedBy" to moderatedBy,
            "imageUrl" to imageUrl,
            "markerSkin" to markerSkin,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to (updatedAt ?: FieldValue.serverTimestamp())
        )
        return map.filterValues { it != null } as Map<String, Any>
    }
}
