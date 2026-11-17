package com.example.smartfinance

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartfinance.database.AppDatabase
import com.example.smartfinance.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = AppDatabase.getInstance(this)
        val dao = db.smartFinanceDao()

        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Tạo dữ liệu mẫu nếu bảng Transactions đang trống
            if (dao.getAllTransactions().isEmpty()) {
                val userId = dao.insertUser(User(name = "Mai Anh", email = "maianh@email.com", passwordHash = "123456", createdAt = "2026-11-01")).toInt()
                val walletId = dao.insertWallet(Wallet(userId = userId, name = "Vietcombank", iconName = "ic_bank", balance = 18000000.0)).toInt()
                val catIncomeId = dao.insertCategory(Category(name = "Lương", type = "INCOME", iconName = "ic_salary")).toInt()
                val catFoodId = dao.insertCategory(Category(name = "Ăn uống", type = "EXPENSE", iconName = "ic_food")).toInt()

                dao.insertTransaction(Transaction(userId = userId, walletId = walletId, categoryId = catIncomeId, amount = 15000000.0, type = "INCOME", note = "Lương tháng 11", transactionDate = "2026-11-05", transactionTime = "09:00"))
                dao.insertTransaction(Transaction(userId = userId, walletId = walletId, categoryId = catFoodId, amount = 12750000.0, type = "EXPENSE", note = "Ăn uống & Mua sắm", transactionDate = "2026-11-15", transactionTime = "12:30"))
            }

            // 2. Tính tỷ lệ cảnh báo mất cân bằng thu chi tháng 11/2026
            val totalIncome = dao.getTotalByMonth("INCOME", "2026-11")
            val totalExpense = dao.getTotalByMonth("EXPENSE", "2026-11")

            if (totalIncome > 0) {
                val ratio = (totalExpense / totalIncome) * 100
                Log.d("SmartFinanceDB", "Tỷ lệ chi/thu: $ratio%") // Kết quả: 85.0%
            }
        }
    }
}