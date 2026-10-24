package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val shopName: String = "বিসমিল্লাহ জেনারেল স্টোর",
    val ownerName: String = "রফিকুল ইসলাম",
    val bkashNumber: String = "01812345678",
    val nagadNumber: String = "01712345678",
    val language: String = "bn", // "bn" or "en"
    val autoReadBack: Boolean = true
)
