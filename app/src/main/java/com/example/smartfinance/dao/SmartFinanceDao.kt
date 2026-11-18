package com.example.smartfinance.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.smartfinance.models.*
data class TransactionWithCategory(
    val transactionId: Int,
    val amount: Double,
    val type: String,
    val note: String,
    val transactionDate: String,
    val transactionTime: String,
    val categoryName: String
)
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

    // Thêm vào interface SmartFinanceDao
    @Query("SELECT * FROM Users WHERE email = :email AND passwordHash = :password LIMIT 1")
    suspend fun login(email: String, password: String): User?

    @Query("SELECT * FROM Users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("UPDATE Users SET passwordHash = :newPassword WHERE email = :email")
    suspend fun updatePassword(email: String, newPassword: String): Int

    // Thêm các câu truy vấn này vào interface SmartFinanceDao:
    @Query("SELECT * FROM Users LIMIT 1")
    suspend fun getFirstUser(): User?

    @Query("SELECT * FROM Users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): User?

    @Query("SELECT IFNULL(SUM(limitAmount), 0.0) FROM Budgets WHERE month = :month")
    suspend fun getTotalBudgetByMonth(month: String): Double

    @Query("""
    SELECT t.transactionId, t.amount, t.type, t.note, t.transactionDate, t.transactionTime, c.name AS categoryName 
    FROM Transactions t 
    INNER JOIN Categories c ON t.categoryId = c.categoryId 
    ORDER BY t.transactionDate DESC, t.transactionTime DESC 
    LIMIT 10
    """)
    suspend fun getRecentTransactionsWithCategory(): List<TransactionWithCategory>
}