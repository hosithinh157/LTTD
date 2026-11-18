package com.example.smartfinance

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartfinance.database.AppDatabase
import com.example.smartfinance.models.Category
import com.example.smartfinance.models.Transaction
import com.example.smartfinance.models.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
class AddTransactionActivity : AppCompatActivity() {

    private var transactionType = "EXPENSE"
    private var selectedCategoryName = "Ăn uống"
    private var selectedWalletName = "Tiền mặt"
    private var selectedDate = ""
    private var selectedTime = ""
    private var expenseAmountDraft = ""
    private var incomeAmountDraft = ""

    // Bộ gợi ý chi tiết cho từng danh mục
    private val subNotesMap = mapOf(
        // Chi tiêu
        "Ăn uống" to listOf("Ăn sáng", "Ăn trưa", "Ăn xế", "Ăn tối", "Cà phê", "Trà sữa", "Đi chợ"),
        "Đi lại" to listOf("Đổ xăng", "Grab / Taxi", "Gửi xe", "Vé xe buýt", "Bảo dưỡng xe"),
        "Tiền trọ" to listOf("Tiền thuê phòng", "Tiền điện", "Tiền nước", "Internet / Wifi", "Phí dịch vụ"),
        "Mua sắm" to listOf("Quần áo", "Mỹ phẩm", "Đồ gia dụng", "Đặt Shopee / Lazada", "Phụ kiện"),
        "Giải trí" to listOf("Xem phim", "Du lịch", "Game / Nạp thẻ", "Tụ tập bạn bè", "Spotify / Netflix"),
        "Học tập" to listOf("Mua sách", "Khóa học online", "Học phí", "In ấn tài liệu", "Văn phòng phẩm"),
        "Hóa đơn" to listOf("Cước điện thoại", "Nạp data 4G", "Thanh toán thẻ", "Bảo hiểm"),
        "Sức khỏe" to listOf("Mua thuốc", "Khám bệnh", "Tập Gym / Thể thao", "Vitamin"),
        // Thu tiền
        "Tiền lương" to listOf("Lương chính thức", "Lương part-time", "Phụ cấp"),
        "Thưởng" to listOf("Thưởng dự án", "Thưởng KPI", "Thưởng lễ / Tết"),
        "Đầu tư" to listOf("Cổ phiếu", "Chứng chỉ quỹ", "Tiền số", "Vàng"),
        "Làm thêm" to listOf("Freelance", "Gia sư", "Bán hàng online"),
        "Kinh doanh" to listOf("Doanh thu ngày", "Lợi nhuận đơn hàng"),
        "Quà tặng" to listOf("Gia đình cho", "Được lì xì", "Quà sinh nhật"),
        "Lãi tiết kiệm" to listOf("Lãi sổ tiết kiệm", "Hoàn tiền ngân hàng"),
        "Thu khác" to listOf("Thu nợ", "Bán đồ cũ", "Khoản thu khác")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        val ivBack = findViewById<ImageView>(R.id.ivBackAdd)
        val tabExpense = findViewById<TextView>(R.id.tabExpense)
        val tabIncome = findViewById<TextView>(R.id.tabIncome)
        val tvAmountSign = findViewById<TextView>(R.id.tvAmountSign)
        val edtAmount = findViewById<EditText>(R.id.edtAmount)
        val ivBackspace = findViewById<ImageView>(R.id.ivBackspace)
        val tvDateTimePicker = findViewById<TextView>(R.id.tvDateTimePicker)
        val edtNote = findViewById<EditText>(R.id.edtTransactionNote)
        val chipGroupSubNotes = findViewById<ChipGroup>(R.id.chipGroupSubNotes)
        val btnSave = findViewById<Button>(R.id.btnSaveTransaction)

        // Mặc định ô số tiền rỗng (hiển thị hint 0)
        edtAmount.setText("")

        val now = Calendar.getInstance()
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        selectedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
        tvDateTimePicker.text = "📅 Hôm nay, $selectedTime"

        ivBack.setOnClickListener { finish() }

        // Hàm cập nhật dải thẻ chi tiết (Chips) bên dưới ô ghi chú
        fun renderSubNoteChips(categoryName: String) {
            chipGroupSubNotes.removeAllViews()
            val suggestions = subNotesMap[categoryName] ?: emptyList()

            for (subText in suggestions) {
                val chip = Chip(this).apply {
                    text = subText
                    textSize = 12f
                    isCheckable = true
                    isCheckedIconVisible = false
                    chipStrokeWidth = 0f
                    chipCornerRadius = 50f
                    setEnsureMinTouchTargetSize(false)
                    chipMinHeight = 34f * resources.displayMetrics.density

                    // Màu nền & chữ thay đổi tự động khi được chọn / chưa chọn
                    val states = arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    )
                    chipBackgroundColor = ColorStateList(
                        states,
                        intArrayOf(Color.parseColor("#066843"), Color.parseColor("#EFF3F8"))
                    )
                    setTextColor(
                        ColorStateList(
                            states,
                            intArrayOf(Color.WHITE, Color.parseColor("#334155"))
                        )
                    )

                    setOnClickListener {
                        edtNote.setText(subText)
                        edtNote.setSelection(edtNote.text.length)
                    }
                }
                chipGroupSubNotes.addView(chip)
            }
        }

        // Khởi tạo gợi ý cho mục mặc định ("Ăn uống")
        edtNote.setText(selectedCategoryName)
        renderSubNoteChips(selectedCategoryName)

        val categoryNames = listOf(
            findViewById<TextView>(R.id.tvCatName1), findViewById<TextView>(R.id.tvCatName2),
            findViewById<TextView>(R.id.tvCatName3), findViewById<TextView>(R.id.tvCatName4),
            findViewById<TextView>(R.id.tvCatName5), findViewById<TextView>(R.id.tvCatName6),
            findViewById<TextView>(R.id.tvCatName7), findViewById<TextView>(R.id.tvCatName8)
        )
        val categoryIcons = listOf(
            findViewById<TextView>(R.id.tvCatIcon1), findViewById<TextView>(R.id.tvCatIcon2),
            findViewById<TextView>(R.id.tvCatIcon3), findViewById<TextView>(R.id.tvCatIcon4),
            findViewById<TextView>(R.id.tvCatIcon5), findViewById<TextView>(R.id.tvCatIcon6),
            findViewById<TextView>(R.id.tvCatIcon7), findViewById<TextView>(R.id.tvCatIcon8)
        )
        val catCards = listOf(
            findViewById<LinearLayout>(R.id.cat1), findViewById<LinearLayout>(R.id.cat2),
            findViewById<LinearLayout>(R.id.cat3), findViewById<LinearLayout>(R.id.cat4),
            findViewById<LinearLayout>(R.id.cat5), findViewById<LinearLayout>(R.id.cat6),
            findViewById<LinearLayout>(R.id.cat7), findViewById<LinearLayout>(R.id.cat8)
        )

        val expenseCats = listOf("🍴" to "Ăn uống", "🚆" to "Đi lại", "🏠" to "Tiền trọ", "🛍️" to "Mua sắm", "🎮" to "Giải trí", "🎓" to "Học tập", "🧾" to "Hóa đơn", "🩺" to "Sức khỏe")
        val incomeCats = listOf("💰" to "Tiền lương", "🎁" to "Thưởng", "📈" to "Đầu tư", "💼" to "Làm thêm", "🏪" to "Kinh doanh", "🧧" to "Quà tặng", "🏦" to "Lãi tiết kiệm", "✨" to "Thu khác")

        fun selectCategoryAt(index: Int) {
            catCards.forEachIndexed { j, c ->
                if (j == index) {
                    c.setBackgroundResource(R.drawable.bg_category_selected)
                    categoryIcons[j].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#066843"))
                    categoryNames[j].setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    c.setBackgroundResource(R.drawable.bg_card_white)
                    categoryIcons[j].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                    categoryNames[j].setTypeface(null, android.graphics.Typeface.NORMAL)
                }
            }
            selectedCategoryName = categoryNames[index].text.toString()
            // Tự động điền tên danh mục vào ô ghi chú và hiện dải gợi ý chi tiết
            edtNote.setText(selectedCategoryName)
            edtNote.setSelection(edtNote.text.length)
            renderSubNoteChips(selectedCategoryName)
        }

        fun bindCategoryGrid(items: List<Pair<String, String>>) {
            for (i in items.indices) {
                categoryIcons[i].text = items[i].first
                categoryNames[i].text = items[i].second
            }
            selectCategoryAt(0)
        }

        tabExpense.setOnClickListener {
            transactionType = "EXPENSE"
            tabExpense.setBackgroundResource(R.drawable.bg_pill_expense)
            tabExpense.setTextColor(Color.WHITE)
            tabIncome.background = null
            tabIncome.setTextColor(Color.parseColor("#475569"))
            tvAmountSign.text = "- "
            tvAmountSign.setTextColor(Color.parseColor("#B91C1C"))
            bindCategoryGrid(expenseCats)
        }

        tabIncome.setOnClickListener {
            transactionType = "INCOME"
            tabIncome.setBackgroundResource(R.drawable.bg_pill_income)
            tabIncome.setTextColor(Color.WHITE)
            tabExpense.background = null
            tabExpense.setTextColor(Color.parseColor("#475569"))
            tvAmountSign.text = "+ "
            tvAmountSign.setTextColor(Color.parseColor("#066843"))
            bindCategoryGrid(incomeCats)
        }

        catCards.forEachIndexed { index, card ->
            card.setOnClickListener { selectCategoryAt(index) }
        }

        // Nút cộng nhanh (+10k, +50k, +100k, +500k)
        fun addQuickAmount(delta: Long) {
            val current = edtAmount.text.toString().toLongOrNull() ?: 0L
            val next = current + delta
            edtAmount.setText(next.toString())
            edtAmount.setSelection(edtAmount.text.length)
        }
        findViewById<TextView>(R.id.btnAdd10k).setOnClickListener { addQuickAmount(10_000) }
        findViewById<TextView>(R.id.btnAdd50k).setOnClickListener { addQuickAmount(50_000) }
        findViewById<TextView>(R.id.btnAdd100k).setOnClickListener { addQuickAmount(100_000) }
        findViewById<TextView>(R.id.btnAdd500k).setOnClickListener { addQuickAmount(500_000) }

        ivBackspace.setOnClickListener {
            val text = edtAmount.text.toString()
            if (text.isNotEmpty()) {
                edtAmount.setText(text.dropLast(1))
                edtAmount.setSelection(edtAmount.text.length)
            }
        }

        // 1. Ánh xạ 4 nút nhanh
        val btnAdd10k = findViewById<TextView>(R.id.btnAdd10k)
        val btnAdd50k = findViewById<TextView>(R.id.btnAdd50k)
        val btnAdd100k = findViewById<TextView>(R.id.btnAdd100k)
        val btnAdd500k = findViewById<TextView>(R.id.btnAdd500k)

        // 2. Hàm đổi dấu & màu cho 4 nút nhanh theo loại giao dịch
        fun updateQuickAmountChips(isExpense: Boolean) {
            val sign = if (isExpense) "-" else "+"
            val textColor = Color.parseColor(if (isExpense) "#B91C1C" else "#066843")

            val buttons = listOf(
                btnAdd10k to "10k",
                btnAdd50k to "50k",
                btnAdd100k to "100k",
                btnAdd500k to "500k"
            )
            for ((btn, label) in buttons) {
                btn.text = "$sign$label"
                btn.setTextColor(textColor)
            }
        }

// Khởi tạo trạng thái mặc định (Chi tiêu -> dấu -)
        updateQuickAmountChips(isExpense = true)

// 3. Gọi lại trong sự kiện chuyển tab Chi tiêu / Thu tiền
        tabExpense.setOnClickListener {
            if (transactionType == "EXPENSE") return@setOnClickListener

            // Lưu lại số tiền đang nhập dở bên Thu tiền trước khi rời đi
            incomeAmountDraft = edtAmount.text.toString()

            // Chuyển sang Chi tiêu và nạp số tiền riêng của Chi tiêu (mặc định rỗng = 0)
            transactionType = "EXPENSE"
            edtAmount.setText(expenseAmountDraft)
            edtAmount.setSelection(edtAmount.text.length)

            tabExpense.setBackgroundResource(R.drawable.bg_pill_expense)
            tabExpense.setTextColor(Color.WHITE)
            tabIncome.background = null
            tabIncome.setTextColor(Color.parseColor("#475569"))
            tvAmountSign.text = "- "
            tvAmountSign.setTextColor(Color.parseColor("#B91C1C"))
            updateQuickAmountChips(isExpense = true)
            bindCategoryGrid(expenseCats)
        }

        tabIncome.setOnClickListener {
            if (transactionType == "INCOME") return@setOnClickListener

            // Lưu lại số tiền đang nhập dở bên Chi tiêu trước khi rời đi
            expenseAmountDraft = edtAmount.text.toString()

            // Chuyển sang Thu tiền và nạp số tiền riêng của Thu tiền (mặc định rỗng = 0)
            transactionType = "INCOME"
            edtAmount.setText(incomeAmountDraft)
            edtAmount.setSelection(edtAmount.text.length)

            tabIncome.setBackgroundResource(R.drawable.bg_pill_income)
            tabIncome.setTextColor(Color.WHITE)
            tabExpense.background = null
            tabExpense.setTextColor(Color.parseColor("#475569"))
            tvAmountSign.text = "+ "
            tvAmountSign.setTextColor(Color.parseColor("#066843"))
            updateQuickAmountChips(isExpense = false)
            bindCategoryGrid(incomeCats)
        }

        // Chọn nguồn ví
        val chipCash = findViewById<TextView>(R.id.chipWalletCash)
        val chipBank = findViewById<TextView>(R.id.chipWalletBank)
        val chipMomo = findViewById<TextView>(R.id.chipWalletMomo)
        val walletChips = listOf(chipCash, chipBank, chipMomo)

        walletChips.forEach { chip ->
            chip.setOnClickListener {
                walletChips.forEach {
                    it.setBackgroundResource(R.drawable.bg_pill_soft)
                    it.setTextColor(Color.parseColor("#1E293B"))
                }
                chip.setBackgroundResource(R.drawable.bg_pill_income)
                chip.setTextColor(Color.WHITE)
                selectedWalletName = chip.text.toString()
            }
        }

        // Chọn ngày & giờ
        tvDateTimePicker.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                TimePickerDialog(this, { _, h, min ->
                    selectedTime = String.format(Locale.US, "%02d:%02d", h, min)
                    tvDateTimePicker.text = "📅 $d/${m + 1}, $selectedTime"
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Lưu giao dịch
        val dao = AppDatabase.getInstance(this).smartFinanceDao()
        btnSave.setOnClickListener {
            val amount = edtAmount.text.toString().toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                Toast.makeText(this, "Vui lòng nhập số tiền lớn hơn 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val note = edtNote.text.toString().trim().ifEmpty { selectedCategoryName }

            lifecycleScope.launch(Dispatchers.IO) {
                val user = dao.getFirstUser() ?: return@launch
                val userId = user.userId

                val wallets = dao.getWalletsByUser(userId)
                val wallet = wallets.find { it.name.equals(selectedWalletName, ignoreCase = true) }
                val walletId = wallet?.walletId ?: dao.insertWallet(
                    Wallet(userId = userId, name = selectedWalletName, iconName = "ic_wallet", balance = 0.0)
                ).toInt()

                val existingCat = dao.getCategoryByNameAndType(selectedCategoryName, transactionType)
                val categoryId = existingCat?.categoryId ?: dao.insertCategory(
                    Category(name = selectedCategoryName, type = transactionType, iconName = "ic_cat")
                ).toInt()

                dao.insertTransaction(
                    Transaction(
                        userId = userId,
                        walletId = walletId,
                        categoryId = categoryId,
                        amount = amount,
                        type = transactionType,
                        note = note,
                        transactionDate = selectedDate,
                        transactionTime = selectedTime
                    )
                )
                val balanceDelta = if (transactionType == "INCOME") amount else -amount
                dao.updateWalletBalance(walletId, balanceDelta)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddTransactionActivity, "Đã lưu giao dịch!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}