package com.example.smartfinance

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.smartfinance.database.AppDatabase
import com.example.smartfinance.models.Category
import com.example.smartfinance.models.Transaction
import com.example.smartfinance.models.Wallet
import com.example.smartfinance.utils.VoiceTransactionParser
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private var transactionType = "EXPENSE"
    private var selectedCategoryName = "Ăn uống"
    private var selectedWalletName = "Tiền mặt"
    private var selectedDate = ""
    private var selectedTime = ""
    private var expenseAmountDraft = ""
    private var incomeAmountDraft = ""

    // Callback cập nhật giao diện sau khi bóc tách giọng nói
    private var onVoiceParsedCallback: ((String) -> Unit)? = null

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull() ?: return@registerForActivityResult
            onVoiceParsedCallback?.invoke(spokenText)
        }
    }

    private val requestMicPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSpeechIntent()
        } else {
            showVoiceFallbackDialog()
        }
    }

    private fun startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchSpeechIntent()
        } else {
            requestMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchSpeechIntent() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói ví dụ: Ăn bún đậu 45 nghìn")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            showVoiceFallbackDialog()
        }
    }

    private fun showVoiceFallbackDialog() {
        val input = EditText(this).apply {
            hint = "VD: Ăn bún đậu 45k hoặc Nhận lương 5 triệu"
        }
        AlertDialog.Builder(this)
            .setTitle("Nhập nhanh bằng giọng nói / câu lệnh")
            .setMessage("Bấm Micro trên bàn phím để nói hoặc nhập câu lệnh:")
            .setView(input)
            .setPositiveButton("Xác nhận") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    onVoiceParsedCallback?.invoke(text)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

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
        val btnVoiceInput = findViewById<View>(R.id.btnVoiceInput)

        edtAmount.setText("")

        val now = Calendar.getInstance()
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        selectedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
        tvDateTimePicker.text = "📅 Hôm nay, $selectedTime"

        ivBack.setOnClickListener { finish() }

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
            edtNote.setText(selectedCategoryName)
            edtNote.setSelection(edtNote.text.length)
            renderSubNoteChips(selectedCategoryName)
        }

        fun bindCategoryGrid(items: List<Pair<String, String>>, defaultIndex: Int = 0) {
            for (i in items.indices) {
                categoryIcons[i].text = items[i].first
                categoryNames[i].text = items[i].second
            }
            selectCategoryAt(defaultIndex)
        }

        catCards.forEachIndexed { index, card ->
            card.setOnClickListener { selectCategoryAt(index) }
        }

        fun addQuickAmount(delta: Long) {
            val current = edtAmount.text.toString().toLongOrNull() ?: 0L
            val next = current + delta
            edtAmount.setText(next.toString())
            edtAmount.setSelection(edtAmount.text.length)
        }
        val btnAdd10k = findViewById<TextView>(R.id.btnAdd10k)
        val btnAdd50k = findViewById<TextView>(R.id.btnAdd50k)
        val btnAdd100k = findViewById<TextView>(R.id.btnAdd100k)
        val btnAdd500k = findViewById<TextView>(R.id.btnAdd500k)

        btnAdd10k.setOnClickListener { addQuickAmount(10_000) }
        btnAdd50k.setOnClickListener { addQuickAmount(50_000) }
        btnAdd100k.setOnClickListener { addQuickAmount(100_000) }
        btnAdd500k.setOnClickListener { addQuickAmount(500_000) }

        ivBackspace.setOnClickListener {
            val text = edtAmount.text.toString()
            if (text.isNotEmpty()) {
                edtAmount.setText(text.dropLast(1))
                edtAmount.setSelection(edtAmount.text.length)
            }
        }

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

        updateQuickAmountChips(isExpense = true)

        fun switchToExpenseTab(forceUpdate: Boolean = false) {
            if (!forceUpdate && transactionType == "EXPENSE") return
            if (transactionType == "INCOME") {
                incomeAmountDraft = edtAmount.text.toString()
            }
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

        fun switchToIncomeTab(forceUpdate: Boolean = false) {
            if (!forceUpdate && transactionType == "INCOME") return
            if (transactionType == "EXPENSE") {
                expenseAmountDraft = edtAmount.text.toString()
            }
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

        tabExpense.setOnClickListener { switchToExpenseTab() }
        tabIncome.setOnClickListener { switchToIncomeTab() }

        // Tự động điền toàn bộ giao diện khi nhận diện được giọng nói
        onVoiceParsedCallback = { spokenText ->
            val parsed = VoiceTransactionParser.parse(spokenText)
            val amountStr = if (parsed.amount > 0) parsed.amount.toLong().toString() else ""

            // Chuẩn hóa tên danh mục khớp với danh sách trên màn hình
            val normalizedCategory = when (parsed.categoryKeyword) {
                "Di chuyển" -> "Đi lại"
                "Lương" -> "Tiền lương"
                else -> parsed.categoryKeyword
            }

            if (parsed.isIncome) {
                incomeAmountDraft = amountStr
                switchToIncomeTab(forceUpdate = true)
                val idx = incomeCats.indexOfFirst { it.second.equals(normalizedCategory, ignoreCase = true) }
                if (idx >= 0) selectCategoryAt(idx)
            } else {
                expenseAmountDraft = amountStr
                switchToExpenseTab(forceUpdate = true)
                val idx = expenseCats.indexOfFirst { it.second.equals(normalizedCategory, ignoreCase = true) }
                if (idx >= 0) selectCategoryAt(idx)
            }

            if (amountStr.isNotEmpty()) {
                edtAmount.setText(amountStr)
                edtAmount.setSelection(edtAmount.text.length)
            }
            edtNote.setText(parsed.note)
            edtNote.setSelection(edtNote.text.length)

            Toast.makeText(
                this,
                "Đã điền: ${parsed.note} (${selectedCategoryName})",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Gắn sự kiện cho nút Micro (nằm ngoài btnSave)
        btnVoiceInput?.setOnClickListener {
            startVoiceRecognition()
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