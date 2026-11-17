package com.example.smartfinance.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.smartfinance.models.*

@Dao
interface SmartFinanceDao {
    @Insert
    suspend fun insertUser(user: User): Long

    @Insert
    suspend fun insertWallet(wallet: Wallet): Long

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Insert
    suspend fun insertBudget(budget: Budget)

    @Query("SELECT * FROM Transactions ORDER BY transactionDate DESC, transactionTime DESC")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("SELECT IFNULL(SUM(amount), 0.0) FROM Transactions WHERE type = :type AND strftime('%Y-%m', transactionDate) = :month")
    suspend fun getTotalByMonth(type: String, month: String): Double
}