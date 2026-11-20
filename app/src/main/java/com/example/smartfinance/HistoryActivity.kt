package com.example.smartfinance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfinance.adapters.HistoryGroupAdapter
import com.example.smartfinance.dao.TransactionHistoryItem
import com.example.smartfinance.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import android.content.res.ColorStateList
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import com.example.smartfinance.utils.NavigationUtils
class HistoryActivity : AppCompatActivity() {

    private lateinit var groupAdapter: HistoryGroupAdapter
    private var allTransactions: List<TransactionHistoryItem> = emptyList()
    private var currentFilterType = "ALL" // "ALL", "EXPENSE", "INCOME"
    private var currentKeyword = ""
    private var currentUserId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        currentUserId = intent.getIntExtra("USER_ID", -1)
        NavigationUtils.setupNavigation(this, currentUserId, "HISTORY")

        val rvHistoryGroups = findViewById<RecyclerView>(R.id.rvHistoryGroups)
        val edtSearch = findViewById<EditText>(R.id.edtSearchHistory)
        val filterAll = findViewById<TextView>(R.id.filterAll)
        val filterExpense = findViewById<TextView>(R.id.filterExpense)
        val filterIncome = findViewById<TextView>(R.id.filterIncome)
        val btnBackHistory = findViewById<View>(R.id.ivBackHistory)

        groupAdapter = HistoryGroupAdapter(emptyList()) { selectedItem ->
            showTransactionDetailBottomSheet(selectedItem)
        }
        rvHistoryGroups.layoutManager = LinearLayoutManager(this)
        rvHistoryGroups.adapter = groupAdapter

        btnBackHistory.setOnClickListener {
            finish()
        }

        // Tìm kiếm theo tên quán, danh mục hoặc số tiền
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentKeyword = s?.toString()?.trim() ?: ""
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Chuyển đổi bộ lọc Tất cả / Chi / Thu
        fun updateFilterUI(selected: TextView) {
            listOf(filterAll, filterExpense, filterIncome).forEach {
                it.setBackgroundResource(R.drawable.bg_pill_soft)
                it.setTextColor(Color.parseColor("#334155"))
            }
            selected.setBackgroundResource(R.drawable.bg_pill_income)
            selected.setTextColor(Color.WHITE)
        }

        filterAll.setOnClickListener {
            currentFilterType = "ALL"
            updateFilterUI(filterAll)
            applyFilterAndSearch()
        }
        filterExpense.setOnClickListener {
            currentFilterType = "EXPENSE"
            updateFilterUI(filterExpense)
            applyFilterAndSearch()
        }
        filterIncome.setOnClickListener {
            currentFilterType = "INCOME"
            updateFilterUI(filterIncome)
            applyFilterAndSearch()
        }
    }

    override fun onResume() {
        super.onResume()
        loadHistoryFromDb()
    }

    private fun loadHistoryFromDb() {
        val dao = AppDatabase.getInstance(this).smartFinanceDao()
        val tvTotalExpense = findViewById<TextView>(R.id.tvHistoryTotalExpense)
        val tvTotalIncome = findViewById<TextView>(R.id.tvHistoryTotalIncome)

        lifecycleScope.launch(Dispatchers.IO) {
            val list = dao.getAllTransactionsHistory()
            val totalExp = list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalInc = list.filter { it.type == "INCOME" }.sumOf { it.amount }

            withContext(Dispatchers.Main) {
                allTransactions = list
                tvTotalExpense.text = "-${formatCurrency(totalExp)} đ"
                tvTotalIncome.text = "+${formatCurrency(totalInc)} đ"
                applyFilterAndSearch()
            }
        }
    }

    private fun applyFilterAndSearch() {
        val filtered = allTransactions.filter { item ->
            val matchesType = (currentFilterType == "ALL") || (item.type == currentFilterType)
            val matchesQuery = currentKeyword.isEmpty() ||
                    item.note.contains(currentKeyword, ignoreCase = true) ||
                    item.categoryName.contains(currentKeyword, ignoreCase = true) ||
                    item.walletName.contains(currentKeyword, ignoreCase = true) ||
                    item.amount.toLong().toString().contains(currentKeyword)
            matchesType && matchesQuery
        }
        groupAdapter.submitTransactions(filtered)
    }

    private fun showTransactionDetailBottomSheet(item: TransactionHistoryItem) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_transaction_detail, null)
        dialog.setContentView(view)

        val btnClose = view.findViewById<TextView>(R.id.btnCloseDetail)
        val tvIcon = view.findViewById<TextView>(R.id.tvDetailIcon)
        val tvBadgeType = view.findViewById<TextView>(R.id.tvDetailBadgeType)
        val tvAmount = view.findViewById<TextView>(R.id.tvDetailAmount)
        val tvCategory = view.findViewById<TextView>(R.id.tvDetailCategory)
        val tvWallet = view.findViewById<TextView>(R.id.tvDetailWallet)
        val tvDateTime = view.findViewById<TextView>(R.id.tvDetailDateTime)
        val tvNote = view.findViewById<TextView>(R.id.tvDetailNote)
        val tvCode = view.findViewById<TextView>(R.id.tvDetailCode)
        val btnDelete = view.findViewById<Button>(R.id.btnDeleteTransaction)

        val formattedMoney = formatCurrency(item.amount)
        tvCategory.text = item.categoryName
        tvWallet.text = item.walletName
        tvNote.text = item.note.ifEmpty { item.categoryName }
        tvCode.text = String.format(Locale.US, "#SF-%04d", item.transactionId)

        // Định dạng ngày hiển thị dd/MM/yyyy • HH:mm
        val displayDate = try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfOut = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdfOut.format(sdfIn.parse(item.transactionDate)!!)
        } catch (e: Exception) {
            item.transactionDate
        }
        tvDateTime.text = "$displayDate • ${item.transactionTime}"

        if (item.type == "INCOME") {
            tvBadgeType.text = "Khoản thu"
            tvBadgeType.setBackgroundResource(R.drawable.bg_pill_income)
            tvAmount.text = "+$formattedMoney đ"
            tvAmount.setTextColor(Color.parseColor("#066843"))
            tvIcon.text = "💵"
            tvIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
        } else {
            tvBadgeType.text = "Khoản chi"
            tvBadgeType.setBackgroundResource(R.drawable.bg_pill_expense)
            tvAmount.text = "-$formattedMoney đ"
            tvAmount.setTextColor(Color.parseColor("#B91C1C"))
            tvIcon.text = "💸"
            tvIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        // Xóa giao dịch + hoàn trả lại số dư ví trong Room DB
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc muốn xóa giao dịch \"${item.note}\" không? Số dư ví sẽ được tự động cập nhật lại.")
                .setPositiveButton("Xóa") { _, _ ->
                    val dao = AppDatabase.getInstance(this).smartFinanceDao()
                    lifecycleScope.launch(Dispatchers.IO) {
                        val rawTrans = dao.getTransactionById(item.transactionId)
                        if (rawTrans != null) {
                            // Hoàn lại tiền cho ví (nếu xóa khoản chi thì cộng lại, xóa khoản thu thì trừ đi)
                            val revertDelta = if (rawTrans.type == "INCOME") -rawTrans.amount else rawTrans.amount
                            dao.updateWalletBalance(rawTrans.walletId, revertDelta)
                            dao.deleteTransactionById(rawTrans.transactionId)
                        }
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                            Toast.makeText(this@HistoryActivity, "Đã xóa giao dịch", Toast.LENGTH_SHORT).show()
                            loadHistoryFromDb()
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        dialog.show()
    }

    private fun formatCurrency(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply { groupingSeparator = '.' }
        return DecimalFormat("#,###", symbols).format(amount)
    }
}