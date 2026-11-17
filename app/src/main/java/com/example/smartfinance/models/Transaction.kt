package com.example.smartfinance.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Transactions",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Wallet::class, parentColumns = ["walletId"], childColumns = ["walletId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Category::class, parentColumns = ["categoryId"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val transactionId: Int = 0,
    @ColumnInfo(index = true) val userId: Int,
    @ColumnInfo(index = true) val walletId: Int,
    @ColumnInfo(index = true) val categoryId: Int,
    val amount: Double,
    val type: String, // "EXPENSE" hoặc "INCOME"
    val note: String,
    val transactionDate: String, // YYYY-MM-DD
    val transactionTime: String  // HH:MM
)