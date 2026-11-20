package com.example.smartfinance

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfinance.adapters.BudgetAdapter
import com.example.smartfinance.adapters.BudgetItem
import com.example.smartfinance.adapters.WalletAdapter
import com.example.smartfinance.database.AppDatabase
import com.example.smartfinance.models.Budget
import com.example.smartfinance.models.Category
import com.example.smartfinance.models.Wallet
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.example.smartfinance.utils.NavigationUtils

class BudgetActivity : AppCompatActivity() {

    private lateinit var walletAdapter: WalletAdapter
    private lateinit var budgetAdapter: BudgetAdapter
    private var currentCalendar = Calendar.getInstance()
    private var currentUserId = -1

    private val categoryOptions = listOf("Ăn uống", "Đi lại", "Tiền trọ", "Mua sắm", "Giải trí", "Học tập", "Hóa đơn", "Sức khỏe")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        currentUserId = intent.getIntExtra("USER_ID", -1)
        NavigationUtils.setupNavigation(this, currentUserId, "BUDGET")

        findViewById<ImageView>(R.id.ivBackBudget).setOnClickListener { finish() }

        // Setup Wallets
        val rvWallets = findViewById<RecyclerView>(R.id.rvWallets)
        walletAdapter = WalletAdapter(emptyList()) { wallet -> showWalletBottomSheet(wallet) }
        rvWallets.layoutManager = LinearLayoutManager(this)
        rvWallets.adapter = walletAdapter
        
        findViewById<TextView>(R.id.btnAddWallet).setOnClickListener { showWalletBottomSheet(null) }

        // Setup Budgets
        val rvBudgets = findViewById<RecyclerView>(R.id.rvBudgets)
        budgetAdapter = BudgetAdapter(emptyList()) { budget -> showBudgetBottomSheet(budget) }
        rvBudgets.layoutManager = LinearLayoutManager(this)
        rvBudgets.adapter = budgetAdapter

        findViewById<TextView>(R.id.btnAddBudget).setOnClickListener { showBudgetBottomSheet(null) }

        findViewById<ImageView>(R.id.btnPrevBudgetMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            loadBudgetData()
        }
        findViewById<ImageView>(R.id.btnNextBudgetMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            loadBudgetData()
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentUserId == -1) {
            val dao = AppDatabase.getInstance(this).smartFinanceDao()
            lifecycleScope.launch(Dispatchers.IO) {
                val user = dao.getFirstUser()
                if (user != null) {
                    currentUserId = user.userId
                    withContext(Dispatchers.Main) { 
                        loadWalletData()
                        loadBudgetData() 
                    }
                }
            }
        } else {
            loadWalletData()
            loadBudgetData()
        }
    }

    private fun loadWalletData() {
        if (currentUserId == -1) return
        val dao = AppDatabase.getInstance(this).smartFinanceDao()
        lifecycleScope.launch(Dispatchers.IO) {
            val wallets = dao.getWalletsByUser(currentUserId)
            withContext(Dispatchers.Main) {
                walletAdapter.updateData(wallets)
            }
        }
    }

    private fun loadBudgetData() {
        if (currentUserId == -1) return

        val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val dbMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        
        findViewById<TextView>(R.id.tvBudgetMonth).text = "Tháng ${monthFormat.format(currentCalendar.time)}"
        val targetMonthDb = dbMonthFormat.format(currentCalendar.time)
        
        val dao = AppDatabase.getInstance(this).smartFinanceDao()
        lifecycleScope.launch(Dispatchers.IO) {
            val budgets = dao.getBudgetsByUserAndMonth(currentUserId, targetMonthDb)
            val expenses = dao.getExpenseGroupedByCategory(currentUserId, targetMonthDb)
            
            val budgetItems = budgets.mapNotNull { b ->
                val cat = dao.getCategoryById(b.categoryId) ?: return@mapNotNull null
                val spent = expenses.find { it.categoryName == cat.name }?.totalAmount ?: 0.0
                BudgetItem(b.budgetId, cat.categoryId, cat.name, b.limitAmount, spent)
            }
            
            withContext(Dispatchers.Main) {
                budgetAdapter.updateData(budgetItems)
                
                if (budgetItems.isEmpty()) {
                    findViewById<TextView>(R.id.tvEmptyBudget).visibility = View.VISIBLE
                    findViewById<RecyclerView>(R.id.rvBudgets).visibility = View.GONE
                } else {
                    findViewById<TextView>(R.id.tvEmptyBudget).visibility = View.GONE
                    findViewById<RecyclerView>(R.id.rvBudgets).visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showWalletBottomSheet(existingWallet: Wallet?) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_wallet, null)
        bottomSheetDialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.tvWalletSheetTitle)
        val edtName = view.findViewById<EditText>(R.id.edtWalletName)
        val edtBalance = view.findViewById<EditText>(R.id.edtWalletBalance)
        val btnSave = view.findViewById<Button>(R.id.btnSaveWallet)
        val btnDelete = view.findViewById<Button>(R.id.btnDeleteWallet)

        if (existingWallet != null) {
            tvTitle.text = "Sửa ví"
            edtName.setText(existingWallet.name)
            edtBalance.setText(existingWallet.balance.toLong().toString())
            btnDelete.visibility = View.VISIBLE
        }

        btnDelete.setOnClickListener {
            existingWallet?.let { wallet ->
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getInstance(this@BudgetActivity).smartFinanceDao().deleteWallet(wallet.walletId)
                    withContext(Dispatchers.Main) {
                        bottomSheetDialog.dismiss()
                        loadWalletData()
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            val name = edtName.text.toString().trim()
            val balanceStr = edtBalance.text.toString().trim()
            if (name.isEmpty() || balanceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val balance = balanceStr.toDoubleOrNull() ?: 0.0

            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getInstance(this@BudgetActivity).smartFinanceDao()
                if (existingWallet != null) {
                    dao.updateWallet(existingWallet.copy(name = name, balance = balance))
                } else {
                    dao.insertWallet(Wallet(userId = currentUserId, name = name, iconName = "ic_wallet", balance = balance))
                }
                withContext(Dispatchers.Main) {
                    bottomSheetDialog.dismiss()
                    loadWalletData()
                }
            }
        }
        bottomSheetDialog.show()
    }

    private fun showBudgetBottomSheet(existingBudget: BudgetItem?) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_budget, null)
        bottomSheetDialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.tvBudgetSheetTitle)
        val spinnerCat = view.findViewById<Spinner>(R.id.spinnerBudgetCategory)
        val edtLimit = view.findViewById<EditText>(R.id.edtBudgetLimit)
        val btnSave = view.findViewById<Button>(R.id.btnSaveBudget)
        val btnDelete = view.findViewById<Button>(R.id.btnDeleteBudget)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCat.adapter = adapter

        if (existingBudget != null) {
            tvTitle.text = "Sửa ngân sách"
            val index = categoryOptions.indexOf(existingBudget.categoryName)
            if (index >= 0) spinnerCat.setSelection(index)
            edtLimit.setText(existingBudget.limitAmount.toLong().toString())
            btnDelete.visibility = View.VISIBLE
        }

        val targetMonthDb = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.time)

        btnDelete.setOnClickListener {
            existingBudget?.let { bItem ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val dao = AppDatabase.getInstance(this@BudgetActivity).smartFinanceDao()
                    val b = dao.getBudgetsByUserAndMonth(currentUserId, targetMonthDb).find { it.budgetId == bItem.budgetId }
                    if (b != null) dao.deleteBudget(b)
                    withContext(Dispatchers.Main) {
                        bottomSheetDialog.dismiss()
                        loadBudgetData()
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            val limitStr = edtLimit.text.toString().trim()
            val selectedCatName = spinnerCat.selectedItem.toString()
            if (limitStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập hạn mức", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val limit = limitStr.toDoubleOrNull() ?: 0.0

            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getInstance(this@BudgetActivity).smartFinanceDao()
                // Find or create category
                var cat = dao.getCategoryByNameAndType(selectedCatName, "EXPENSE")
                if (cat == null) {
                    val newId = dao.insertCategory(Category(name = selectedCatName, type = "EXPENSE", iconName = "ic_cat")).toInt()
                    cat = dao.getCategoryById(newId)
                }
                if (cat == null) return@launch // Fallback safe

                if (existingBudget != null) {
                    val b = dao.getBudgetsByUserAndMonth(currentUserId, targetMonthDb).find { it.budgetId == existingBudget.budgetId }
                    if (b != null) dao.updateBudget(b.copy(categoryId = cat.categoryId, limitAmount = limit))
                } else {
                    dao.insertBudget(Budget(userId = currentUserId, categoryId = cat.categoryId, month = targetMonthDb, limitAmount = limit))
                }
                withContext(Dispatchers.Main) {
                    bottomSheetDialog.dismiss()
                    loadBudgetData()
                }
            }
        }
        bottomSheetDialog.show()
    }
}