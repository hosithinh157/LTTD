package com.example.smartfinance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfinance.adapters.TransactionAdapter
import com.example.smartfinance.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var isBalanceVisible = true
    private var currentBalanceText = "0 đ"
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvAvailableBalance = findViewById<TextView>(R.id.tvAvailableBalance)
        val ivToggleBalance = findViewById<ImageView>(R.id.ivToggleBalance)
        val rvRecentTransactions = findViewById<RecyclerView>(R.id.rvRecentTransactions)

        // Cấu hình RecyclerView
        adapter = TransactionAdapter(emptyList())
        rvRecentTransactions.layoutManager = LinearLayoutManager(this)
        rvRecentTransactions.adapter = adapter

        // Nút ẩn/hiện số dư khả dụng
        ivToggleBalance.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            if (isBalanceVisible) {
                tvAvailableBalance.text = currentBalanceText
                ivToggleBalance.setImageResource(R.drawable.ic_eye_open)
            } else {
                tvAvailableBalance.text = "•••••••• đ"
                ivToggleBalance.setImageResource(R.drawable.ic_eye_off)
            }
        }

        // Mở màn hình Ghi chép giao dịch khi bấm nút +
        findViewById<View>(R.id.fabAddTransaction).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvAvailableBalance = findViewById<TextView>(R.id.tvAvailableBalance)
        val tvTotalIncome = findViewById<TextView>(R.id.tvTotalIncome)
        val tvTotalExpense = findViewById<TextView>(R.id.tvTotalExpense)
        val tvBudgetPercent = findViewById<TextView>(R.id.tvBudgetPercent)
        val progressBudget = findViewById<ProgressBar>(R.id.progressBudget)
        val tvBudgetSpent = findViewById<TextView>(R.id.tvBudgetSpent)
        val tvBudgetRemaining = findViewById<TextView>(R.id.tvBudgetRemaining)

        val dao = AppDatabase.getInstance(this).smartFinanceDao()
        val loggedInUserId = intent.getIntExtra("USER_ID", -1)
        val targetMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        lifecycleScope.launch(Dispatchers.IO) {
            val user = if (loggedInUserId != -1) dao.getUserById(loggedInUserId) else dao.getFirstUser()
            val totalIncome = dao.getTotalByMonth("INCOME", targetMonth)
            val totalExpense = dao.getTotalByMonth("EXPENSE", targetMonth)
            val totalBudget = dao.getTotalBudgetByMonth(targetMonth).let { if (it > 0) it else 12000000.0 }
            val recentList = dao.getRecentTransactionsWithCategory()

            val availableBalance = totalIncome - totalExpense
            val remainingBudget = (totalBudget - totalExpense).coerceAtLeast(0.0)
            val budgetPercent = ((totalExpense / totalBudget) * 100).coerceIn(0.0, 100.0)

            withContext(Dispatchers.Main) {
                tvGreeting.text = "Chào ${user?.name ?: "Mai Anh"} 👋"
                currentBalanceText = "${formatCurrency(availableBalance)} đ"
                if (isBalanceVisible) {
                    tvAvailableBalance.text = currentBalanceText
                }

                tvTotalIncome.text = "↓  +${formatCurrency(totalIncome)} đ"
                tvTotalExpense.text = "↑  -${formatCurrency(totalExpense)} đ"

                tvBudgetPercent.text = String.format(Locale.US, "%.1f%%", budgetPercent)
                progressBudget.progress = budgetPercent.toInt()
                tvBudgetSpent.text = "Đã chi: ${formatCurrency(totalExpense)} đ"
                tvBudgetRemaining.text = "Còn lại: ${formatCurrency(remainingBudget)} đ"

                adapter.updateData(recentList)
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
        }
        return DecimalFormat("#,###", symbols).format(amount)
    }
}