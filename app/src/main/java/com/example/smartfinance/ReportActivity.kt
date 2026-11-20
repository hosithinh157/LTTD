package com.example.smartfinance

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfinance.adapters.ReportAdapter
import com.example.smartfinance.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.example.smartfinance.utils.NavigationUtils

class ReportActivity : AppCompatActivity() {

    private lateinit var adapter: ReportAdapter
    private var currentCalendar = Calendar.getInstance()
    private var currentUserId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        // Lấy userId được truyền từ MainActivity
        currentUserId = intent.getIntExtra("USER_ID", -1)
        NavigationUtils.setupNavigation(this, currentUserId, "REPORT")

        findViewById<ImageView>(R.id.ivBackReport).setOnClickListener { finish() }

        val rvReport = findViewById<RecyclerView>(R.id.rvReportCategories)
        adapter = ReportAdapter(emptyList(), 0.0)
        rvReport.layoutManager = LinearLayoutManager(this)
        rvReport.adapter = adapter

        findViewById<ImageView>(R.id.btnPrevMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            loadReportData()
        }
        findViewById<ImageView>(R.id.btnNextMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            loadReportData()
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentUserId == -1) {
            // Nếu không có userId truyền vào, thử lấy User đầu tiên (fallback)
            val dao = AppDatabase.getInstance(this).smartFinanceDao()
            lifecycleScope.launch(Dispatchers.IO) {
                val user = dao.getFirstUser()
                if (user != null) {
                    currentUserId = user.userId
                    withContext(Dispatchers.Main) { loadReportData() }
                }
            }
        } else {
            loadReportData()
        }
    }

    private fun loadReportData() {
        if (currentUserId == -1) return

        val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val dbMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        
        findViewById<TextView>(R.id.tvReportMonth).text = "Tháng ${monthFormat.format(currentCalendar.time)}"
        
        val targetMonthDb = dbMonthFormat.format(currentCalendar.time)
        
        val dao = AppDatabase.getInstance(this).smartFinanceDao()
        lifecycleScope.launch(Dispatchers.IO) {
            val totalIncome = dao.getTotalByMonthAndUser(currentUserId, "INCOME", targetMonthDb)
            val totalExpense = dao.getTotalByMonthAndUser(currentUserId, "EXPENSE", targetMonthDb)
            // Lấy danh sách tỷ trọng phân bổ chi tiêu
            val expenseList = dao.getExpenseGroupedByCategory(currentUserId, targetMonthDb)
            
            withContext(Dispatchers.Main) {
                val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply { groupingSeparator = '.' }
                val df = DecimalFormat("#,###", symbols)
                
                findViewById<TextView>(R.id.tvReportTotalIncome).text = "+${df.format(totalIncome)} đ"
                findViewById<TextView>(R.id.tvReportTotalExpense).text = "-${df.format(totalExpense)} đ"
                
                adapter.updateData(expenseList, totalExpense)
                
                if (expenseList.isEmpty()) {
                    findViewById<TextView>(R.id.tvEmptyReport).visibility = View.VISIBLE
                    findViewById<RecyclerView>(R.id.rvReportCategories).visibility = View.GONE
                } else {
                    findViewById<TextView>(R.id.tvEmptyReport).visibility = View.GONE
                    findViewById<RecyclerView>(R.id.rvReportCategories).visibility = View.VISIBLE
                }
            }
        }
    }
}