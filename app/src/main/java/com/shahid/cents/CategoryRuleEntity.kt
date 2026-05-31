package com.shahid.cents

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey val pattern: String, // Lowercase merchant name or sender name
    val category: String,
    val isSenderRule: Boolean = false
)
