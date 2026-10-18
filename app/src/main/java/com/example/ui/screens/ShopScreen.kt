package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AvatarSkinItem
import com.example.data.model.MarkerSkinItem
import com.example.data.model.SkinCatalog
import com.example.data.repository.UserShopState
import com.example.data.util.Strings

@Composable
fun ShopScreen(
    shopState: UserShopState,
    onBuyAvatarSkin: (skinId: String, price: Int) -> Unit,
    onSelectAvatarSkin: (skinId: String) -> Unit,
    onBuyMarkerSkin: (skinId: String, price: Int) -> Unit,
    onSelectMarkerSkin: (skinId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Points Balance Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1E3A8A),
                                    Color(0xFF2563EB),
                                    Color(0xFF0284C7)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFBBF24)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF78350F),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = Strings.pointsBalance,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = "${shopState.points} ${Strings.pointsShort}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 28.sp
                                        ),
                                        color = Color(0xFFFEF08A)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF86EFAC),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.earnPointsRule,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Avatar Skins for user on map
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.avatarSkinsSection,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = if (Strings.isUk) {
                        "Оберіть, як ви будете відображатися на карті для себе"
                    } else {
                        "Choose how you appear as a marker pin on the map"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(SkinCatalog.avatarSkins.size) { index ->
            val skin = SkinCatalog.avatarSkins[index]
            val isUnlocked = shopState.unlockedAvatarSkins.contains(skin.id) || skin.id == "human"
            val isSelected = shopState.selectedAvatarSkin == skin.id

            AvatarSkinCard(
                skin = skin,
                isUnlocked = isUnlocked,
                isSelected = isSelected,
                canAfford = shopState.points >= skin.pricePoints,
                onSelect = { onSelectAvatarSkin(skin.id) },
                onBuy = { onBuyAvatarSkin(skin.id, skin.pricePoints) }
            )
        }

        // Section 2: Marker Skins for custom place pins
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.markerSkinsSection,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = if (Strings.isUk) {
                        "Унікальні мітки для ваших локацій, які бачать всі користувачі"
                    } else {
                        "Unique pins for your locations that all users will see"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(SkinCatalog.markerSkins.size) { index ->
            val skin = SkinCatalog.markerSkins[index]
            val isUnlocked = shopState.unlockedMarkerSkins.contains(skin.id) || skin.id == "default"
            val isSelected = shopState.activeMarkerSkin == skin.id

            MarkerSkinCard(
                skin = skin,
                isUnlocked = isUnlocked,
                isSelected = isSelected,
                canAfford = shopState.points >= skin.pricePoints,
                onSelect = { onSelectMarkerSkin(skin.id) },
                onBuy = { onBuyMarkerSkin(skin.id, skin.pricePoints) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AvatarSkinCard(
    skin: AvatarSkinItem,
    isUnlocked: Boolean,
    isSelected: Boolean,
    canAfford: Boolean,
    onSelect: () -> Unit,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) skin.accentColor else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                skin.accentColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji circle
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(skin.accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skin.emoji,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (Strings.isUk) skin.nameUa else skin.nameEn,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (Strings.isUk) skin.descriptionUa else skin.descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isSelected) {
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = skin.accentColor,
                        disabledContentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.selected)
                }
            } else if (isUnlocked) {
                OutlinedButton(
                    onClick = onSelect,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Strings.select)
                }
            } else {
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(12.dp),
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.accentColor
                    )
                ) {
                    Text("${skin.pricePoints} pts")
                }
            }
        }
    }
}

@Composable
fun MarkerSkinCard(
    skin: MarkerSkinItem,
    isUnlocked: Boolean,
    isSelected: Boolean,
    canAfford: Boolean,
    onSelect: () -> Unit,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) skin.pinColor else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                skin.pinColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(skin.pinColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skin.emoji,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (Strings.isUk) skin.nameUa else skin.nameEn,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (Strings.isUk) skin.descriptionUa else skin.descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isSelected) {
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = skin.pinColor,
                        disabledContentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.selected)
                }
            } else if (isUnlocked) {
                OutlinedButton(
                    onClick = onSelect,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Strings.select)
                }
            } else {
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(12.dp),
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.pinColor
                    )
                ) {
                    Text("${skin.pricePoints} pts")
                }
            }
        }
    }
}
