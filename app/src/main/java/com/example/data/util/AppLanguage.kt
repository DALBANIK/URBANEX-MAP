package com.example.data.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val title: String, val flag: String) {
    UKRAINIAN("uk", "Українська", "🇺🇦"),
    ENGLISH("en", "English", "🇬🇧");

    val isUk: Boolean get() = this == UKRAINIAN
}

object Strings {
    var currentLanguage by mutableStateOf(AppLanguage.UKRAINIAN)

    val isUk: Boolean get() = currentLanguage.isUk

    // Navigation
    val tabMap get() = if (isUk) "Карта" else "Map"
    val tabMyMarkers get() = if (isUk) "Мої мітки" else "My Markers"
    val tabModeration get() = if (isUk) "Модерація" else "Moderation"
    val tabShop get() = if (isUk) "Магазин" else "Shop"
    val tabProfile get() = if (isUk) "Профіль" else "Profile"

    // Search & Filter
    val searchPlaceholder get() = if (isUk) "Пошук міток за назвою чи описом..." else "Search markers by name or details..."
    val allCategories get() = if (isUk) "Усі" else "All"

    // Actions & Map
    val addMarker get() = if (isUk) "Додати мітку" else "Add Marker"
    val routeToKyiv get() = if (isUk) "Шлях до Києва" else "Route to Kyiv"
    val routeHere get() = if (isUk) "Прокласти маршрут сюди" else "Route to this location"
    val pointOnMap get() = if (isUk) "Точка на карті" else "Point on map"
    val tapHint get() = if (isUk) "Торкніться карти в будь-якому місці для навігації або додавання мітки" else "Tap anywhere on the map to navigate or add a marker"
    val changeMapLayer get() = if (isUk) "Змінити шар карти" else "Switch map style"
    val myLocation get() = if (isUk) "Моє місцезнаходження" else "My location"
    val routeCalculating get() = if (isUk) "Прокладання маршруту..." else "Calculating route..."
    val routeCalculatingSub get() = if (isUk) "Розрахунок кілометрів, часу пішки та на авто" else "Calculating kilometers, walking and driving time"

    // Route Card
    val routeTitle get() = if (isUk) "Маршрут прокладено" else "Route plotted"
    val distanceLabel get() = if (isUk) "Відстань" else "Distance"
    val walkingTimeLabel get() = if (isUk) "Пішки (дійти)" else "Walking time"
    val drivingTimeLabel get() = if (isUk) "На авто" else "By car"
    val taxiLabel get() = if (isUk) "Таксі" else "Taxi"
    val callTaxiButton get() = if (isUk) "Подзвонити до таксі" else "Call Taxi"
    val clearRouteButton get() = if (isUk) "Скинути маршрут" else "Clear Route"

    // Shop & Points
    val shopTitle get() = if (isUk) "Магазин скінів та поінтів" else "Skins & Points Store"
    val shopSubtitle get() = if (isUk) "Заробляйте поінти за схвалені мітки та відкривайте унікальні скіни!" else "Earn points for approved markers and unlock unique skins!"
    val pointsBalance get() = if (isUk) "Ваш баланс поінтів:" else "Your Points Balance:"
    val pointsShort get() = if (isUk) "поінтів" else "points"
    val avatarSkinsSection get() = if (isUk) "Скіни для вашого аватара на карті" else "Avatar skins for map location"
    val markerSkinsSection get() = if (isUk) "Скіни для міток (видно всім користувачам)" else "Marker skins (visible to everyone)"
    val selected get() = if (isUk) "Обрано" else "Selected"
    val select get() = if (isUk) "Обрати" else "Select"
    val buy get() = if (isUk) "Купити" else "Buy"
    val notEnoughPoints get() = if (isUk) "Недостатньо поінтів!" else "Not enough points!"
    val purchaseSuccess get() = if (isUk) "Успішно придбано!" else "Successfully purchased!"
    val skinSelectedMessage get() = if (isUk) "Скін активовано!" else "Skin activated!"
    val earnPointsRule get() = if (isUk) "+50 поінтів за кожну схвалену точку!" else "+50 points for each approved marker!"

    // Photo
    val photoLabel get() = if (isUk) "Фото мітки" else "Marker Photo"
    val addPhotoBtn get() = if (isUk) "Додати фото з галереї" else "Add photo from gallery"
    val changePhotoBtn get() = if (isUk) "Змінити фото" else "Change photo"
    val removePhotoBtn get() = if (isUk) "Видалити фото" else "Remove photo"
    val photoUrlPrompt get() = if (isUk) "або вставте посилання на зображення:" else "or enter image URL:"

    // Moderation
    val moderationTitle get() = if (isUk) "Панель модерації" else "Moderation Panel"
    val pendingReview get() = if (isUk) "Очікують перевірки" else "Pending review"
    val allApprovedMsg get() = if (isUk) "Всі мітки перевірено!" else "All markers reviewed!"
    val allApprovedSub get() = if (isUk) "Немає нових міток, які потребують перевірки" else "No new markers waiting for moderation"
    val approve get() = if (isUk) "Схвалити (+50 поінтів автору)" else "Approve (+50 pts to author)"
    val reject get() = if (isUk) "Відхилити" else "Reject"
    val author get() = if (isUk) "Автор" else "Author"
    val rejectionReasonTitle get() = if (isUk) "Причина відхилення" else "Rejection reason"
    val cancel get() = if (isUk) "Скасувати" else "Cancel"

    // Profile
    val profileTitle get() = if (isUk) "Профіль" else "Profile"
    val officialModerator get() = if (isUk) "Офіційний модератор uexmap" else "Official uexmap Moderator"
    val userStatus get() = if (isUk) "Користувач uexmap" else "uexmap Explorer"
    val moderatorAccountsInfo get() = if (isUk) "Модератори додатку: daniilslonec2000@gmail.com, tancikozenden@gmail.com" else "Official moderators: daniilslonec2000@gmail.com, tancikozenden@gmail.com"
    val testModeratorToggle get() = if (isUk) "Тестовий режим модератора" else "Test Moderator Mode"
    val languageSection get() = if (isUk) "Мова додатку / Language" else "App Language / Мова додатку"
    val markersAddedStat get() = if (isUk) "Додано міток" else "Markers Added"
    val markersApprovedStat get() = if (isUk) "Схвалено" else "Approved"
    val pointsEarnedStat get() = if (isUk) "Поінти" else "Points"
    val signOut get() = if (isUk) "Вийти з акаунту" else "Sign Out"
    val aboutApp get() = if (isUk) "Про додаток uexmap" else "About uexmap"
    val aboutAppDesc get() = if (isUk) "uexmap — інтерактивна карта міток з модерацією, прокладанням маршрутів пішки та авто, викликом таксі та магазином скінів." else "uexmap is an interactive crowd map with moderation, walking & driving navigation, taxi integration, and custom skins store."
}
