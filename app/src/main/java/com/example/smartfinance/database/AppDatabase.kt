package com.example.smartfinance.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartfinance.dao.SmartFinanceDao
import com.example.smartfinance.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Wallet::class, Category::class, Transaction::class, Budget::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smartFinanceDao(): SmartFinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "SmartFinance_Room.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback()) // Tự động nạp dữ liệu gốc ngay khi DB khởi tạo
                    .build()
                INSTANCE = instance
                instance
            }
        }

        class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.smartFinanceDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: SmartFinanceDao) {
                // 1. Tạo User mặc định
                val userId = dao.insertUser(
                    User(name = "Mai Anh", email = "maianh@email.com", passwordHash = "123456", createdAt = "2026-11-01")
                ).toInt()

                // 2. Tạo Ví
                val walletId = dao.insertWallet(
                    Wallet(userId = userId, name = "Vietcombank", iconName = "ic_bank", balance = 10850000.0)
                ).toInt()

                // 3. Tạo Danh mục
                val catFoodId = dao.insertCategory(Category(name = "Ăn uống", type = "EXPENSE", iconName = "ic_food")).toInt()
                val catIncomeId = dao.insertCategory(Category(name = "Thu nhập", type = "INCOME", iconName = "ic_salary")).toInt()
                val catMoveId = dao.insertCategory(Category(name = "Đi lại", type = "EXPENSE", iconName = "ic_car")).toInt()

                // 4. Tạo Ngân sách tháng 11
                dao.insertBudget(
                    Budget(userId = userId, categoryId = catFoodId, month = "2026-11", limitAmount = 12000000.0)
                )

                // 5. Tạo Giao dịch mẫu
                dao.insertTransaction(Transaction(userId = userId, walletId = walletId, categoryId = catFoodId, amount = 45000.0, type = "EXPENSE", note = "Cà phê Highlands", transactionDate = "2026-11-18", transactionTime = "08:30"))
                dao.insertTransaction(Transaction(userId = userId, walletId = walletId, categoryId = catIncomeId, amount = 18000000.0, type = "INCOME", note = "Tiền lương", transactionDate = "2026-11-17", transactionTime = "09:00"))
                dao.insertTransaction(Transaction(userId = userId, walletId = walletId, categoryId = catMoveId, amount = 78000.0, type = "EXPENSE", note = "Grab Car", transactionDate = "2026-11-17", transactionTime = "18:15"))
            }
        }
    }
}