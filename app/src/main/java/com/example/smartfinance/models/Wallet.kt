package com.example.smartfinance.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Wallets",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Wallet(
    @PrimaryKey(autoGenerate = true) val walletId: Int = 0,
    @ColumnInfo(index = true) val userId: Int,
    val name: String,
    val iconName: String,
    val balance: Double
)