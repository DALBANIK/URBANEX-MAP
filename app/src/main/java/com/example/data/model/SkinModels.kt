package com.example.data.model

import androidx.compose.ui.graphics.Color

data class AvatarSkinItem(
    val id: String,
    val nameUa: String,
    val nameEn: String,
    val descriptionUa: String,
    val descriptionEn: String,
    val emoji: String,
    val pricePoints: Int,
    val accentColor: Color
)

data class MarkerSkinItem(
    val id: String,
    val nameUa: String,
    val nameEn: String,
    val descriptionUa: String,
    val descriptionEn: String,
    val emoji: String,
    val pricePoints: Int,
    val pinColor: Color
)

object SkinCatalog {
    val avatarSkins = listOf(
        AvatarSkinItem(
            id = "human",
            nameUa = "Человечок",
            nameEn = "Human",
            descriptionUa = "Класичний силует пішохода",
            descriptionEn = "Classic pedestrian walker",
            emoji = "🚶",
            pricePoints = 0,
            accentColor = Color(0xFF2563EB)
        ),
        AvatarSkinItem(
            id = "donut",
            nameUa = "Пончик",
            nameEn = "Donut",
            descriptionUa = "Апетитний солодкий пончик з глазур'ю",
            descriptionEn = "Glazed sweet delicious donut",
            emoji = "🍩",
            pricePoints = 100,
            accentColor = Color(0xFFEC4899)
        ),
        AvatarSkinItem(
            id = "horse",
            nameUa = "Кінь",
            nameEn = "Horse",
            descriptionUa = "Швидкий та шляхетний скакун",
            descriptionEn = "Swift and noble thoroughbred",
            emoji = "🐴",
            pricePoints = 150,
            accentColor = Color(0xFFB45309)
        ),
        AvatarSkinItem(
            id = "cat",
            nameUa = "Кіт",
            nameEn = "Cat",
            descriptionUa = "Граціозний та спритний пухнастик",
            descriptionEn = "Graceful agile purring cat",
            emoji = "🐱",
            pricePoints = 150,
            accentColor = Color(0xFFF97316)
        ),
        AvatarSkinItem(
            id = "dog",
            nameUa = "Собака",
            nameEn = "Dog",
            descriptionUa = "Вірний, веселий та енергійний друг",
            descriptionEn = "Faithful energetic cheerful dog",
            emoji = "🐶",
            pricePoints = 150,
            accentColor = Color(0xFF10B981)
        )
    )

    val markerSkins = listOf(
        MarkerSkinItem(
            id = "default",
            nameUa = "Класичний пін",
            nameEn = "Classic Pin",
            descriptionUa = "Стандартна акуратна мітка",
            descriptionEn = "Standard neat location pin",
            emoji = "📍",
            pricePoints = 0,
            pinColor = Color(0xFF2563EB)
        ),
        MarkerSkinItem(
            id = "gold_crown",
            nameUa = "Золота корона",
            nameEn = "Golden Crown",
            descriptionUa = "Преміальна золота мітка з короною",
            descriptionEn = "Royal gold pin with shining crown",
            emoji = "👑",
            pricePoints = 120,
            pinColor = Color(0xFFEAB308)
        ),
        MarkerSkinItem(
            id = "neon_cyber",
            nameUa = "Неон Кібер",
            nameEn = "Neon Cyber",
            descriptionUa = "Футуристична неонова мітка з блискавкою",
            descriptionEn = "Futuristic neon pin with cyber spark",
            emoji = "⚡",
            pricePoints = 120,
            pinColor = Color(0xFF06B6D4)
        ),
        MarkerSkinItem(
            id = "star_gem",
            nameUa = "Рубінова зірка",
            nameEn = "Ruby Star",
            descriptionUa = "Мітка найвищої якості для топових місць",
            descriptionEn = "Premium star badge for top tier spots",
            emoji = "⭐",
            pricePoints = 150,
            pinColor = Color(0xFF8B5CF6)
        ),
        MarkerSkinItem(
            id = "flame_fire",
            nameUa = "Вогонь / Hotspot",
            nameEn = "Flame Hotspot",
            descriptionUa = "Найгарячіша точка на карті міста",
            descriptionEn = "The hottest trending location on map",
            emoji = "🔥",
            pricePoints = 200,
            pinColor = Color(0xFFEF4444)
        )
    )

    fun getAvatar(id: String): AvatarSkinItem {
        return avatarSkins.firstOrNull { it.id == id } ?: avatarSkins.first()
    }

    fun getMarkerSkin(id: String): MarkerSkinItem {
        return markerSkins.firstOrNull { it.id == id } ?: markerSkins.first()
    }
}
