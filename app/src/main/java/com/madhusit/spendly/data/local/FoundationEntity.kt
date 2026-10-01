package com.madhusit.spendly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foundation_state")
data class FoundationEntity(
    @PrimaryKey val id: Int = 1,
    val message: String
)
