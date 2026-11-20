package com.example.smartfinance

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.smartfinance.database.AppDatabase
import com.example.smartfinance.models.User
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Calendar

class ProfileActivity : AppCompatActivity() {

    private var currentUserId = -1
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<ImageView>(R.id.ivBackProfile).setOnClickListener { finish() }

        currentUserId = intent.getIntExtra("USER_ID", -1)

        val prefs = getSharedPreferences("SmartFinancePrefs", Context.MODE_PRIVATE)

        // Theme Mode
        val switchThemeMode = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchThemeMode)
        val isDark = prefs.getBoolean("THEME_DARK", false)
        switchThemeMode.setOnCheckedChangeListener(null)
        switchThemeMode.isChecked = isDark
        switchThemeMode.text = if (isDark) "Giao diện Tối " else "Giao diện Sáng "
        switchThemeMode.setOnCheckedChangeListener { _, isChecked ->
            if (prefs.getBoolean("THEME_DARK", false) == isChecked) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("THEME_DARK", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            switchThemeMode.text = if (isChecked) "Giao diện Tối " else "Giao diện Sáng "
        }

        // Currency
        val tvCurrency = findViewById<TextView>(R.id.tvCurrency)
        var isVND = prefs.getBoolean("CURRENCY_VND", true)
        tvCurrency.text = if (isVND) "VNĐ (đ)" else "USD ($)"
        tvCurrency.setOnClickListener {
            isVND = !isVND
            prefs.edit().putBoolean("CURRENCY_VND", isVND).apply()
            tvCurrency.text = if (isVND) "VNĐ (đ)" else "USD ($)"
            Toast.makeText(this, "Đã đổi đơn vị tiền tệ thành ${tvCurrency.text}", Toast.LENGTH_SHORT).show()
        }

        // Reminder Time
        val btnSetReminderTime = findViewById<LinearLayout>(R.id.btnSetReminderTime)
        val tvReminderTime = findViewById<TextView>(R.id.tvReminderTime)
        val switchReminder = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchReminder)
        
        var reminderHour = prefs.getInt("REMINDER_HOUR", 20)
        var reminderMin = prefs.getInt("REMINDER_MIN", 30)
        val isReminderOn = prefs.getBoolean("REMINDER_ON", true)
        
        switchReminder.isChecked = isReminderOn
        tvReminderTime.text = String.format("%02d:%02d mỗi tối", reminderHour, reminderMin)

        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("REMINDER_ON", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Đã bật nhắc nhở" else "Đã tắt nhắc nhở", Toast.LENGTH_SHORT).show()
        }

        btnSetReminderTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                reminderHour = h
                reminderMin = m
                prefs.edit().putInt("REMINDER_HOUR", h).putInt("REMINDER_MIN", m).apply()
                tvReminderTime.text = String.format("%02d:%02d mỗi tối", h, m)
                switchReminder.isChecked = true
            }, reminderHour, reminderMin, true).show()
        }

        // Security
        val tvSecurityLock = findViewById<TextView>(R.id.tvSecurityLock)
        var isSecurityOn = prefs.getBoolean("SECURITY_ON", false)
        val savedPin = prefs.getString("SECURITY_PIN", "")
        
        tvSecurityLock.text = if (isSecurityOn && !savedPin.isNullOrEmpty()) "Đang bật >" else "Đã tắt >"
        
        tvSecurityLock.setOnClickListener {
            if (isSecurityOn) {
                // Tắt bảo mật
                isSecurityOn = false
                prefs.edit().putBoolean("SECURITY_ON", false).remove("SECURITY_PIN").apply()
                tvSecurityLock.text = "Đã tắt >"
                Toast.makeText(this, "Đã tắt Khóa bảo mật", Toast.LENGTH_SHORT).show()
            } else {
                // Bật bảo mật -> Yêu cầu thiết lập mã PIN
                val pinEditText = EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    hint = "Nhập mã PIN 4 số"
                    filters = arrayOf(android.text.InputFilter.LengthFilter(4))
                }
                
                AlertDialog.Builder(this)
                    .setTitle("Thiết lập mã PIN")
                    .setMessage("Vui lòng nhập mã PIN 4 số để bảo vệ ứng dụng:")
                    .setView(pinEditText)
                    .setPositiveButton("Lưu") { _, _ ->
                        val pin = pinEditText.text.toString()
                        if (pin.length == 4) {
                            isSecurityOn = true
                            prefs.edit().putBoolean("SECURITY_ON", true).putString("SECURITY_PIN", pin).apply()
                            tvSecurityLock.text = "Đang bật >"
                            Toast.makeText(this, "Đã bật Khóa bảo mật với mã PIN", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Mã PIN phải gồm đúng 4 số!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        }

        // Export Report
        findViewById<TextView>(R.id.btnExportReport).setOnClickListener {
            exportTransactionsToCSV()
        }

        // Edit Profile
        findViewById<ImageView>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileBottomSheet()
        }

        // Logout
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi SmartFinance?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    // Xóa session
                    prefs.edit().remove("USER_ID").apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        if (currentUserId == -1) return
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@ProfileActivity).smartFinanceDao()
            currentUser = dao.getUserById(currentUserId)
            withContext(Dispatchers.Main) {
                currentUser?.let { user ->
                    findViewById<TextView>(R.id.tvProfileName).text = user.name
                    findViewById<TextView>(R.id.tvProfileEmail).text = user.email
                }
            }
        }
    }

    private fun showEditProfileBottomSheet() {
        val user = currentUser ?: return
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_profile, null)
        bottomSheetDialog.setContentView(view)

        val edtName = view.findViewById<EditText>(R.id.edtProfileName)
        val edtPassword = view.findViewById<EditText>(R.id.edtProfilePassword)
        
        edtName.setText(user.name)

        view.findViewById<Button>(R.id.btnCancelProfile).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            val newName = edtName.text.toString().trim()
            val newPass = edtPassword.text.toString().trim()
            
            if (newName.isEmpty()) {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val finalPass = if (newPass.isNotEmpty()) newPass else user.passwordHash

            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getInstance(this@ProfileActivity).smartFinanceDao()
                // Do update. (Lưu ý: trong Room, hàm Update trên Entity cần Primary Key khớp)
                dao.updateUser(user.copy(name = newName, passwordHash = finalPass))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    bottomSheetDialog.dismiss()
                    loadUserProfile() // Reload data
                }
            }
        }
        
        bottomSheetDialog.show()
    }

    private fun exportTransactionsToCSV() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(this@ProfileActivity).smartFinanceDao()
                // Cần thêm where userId vào truy vấn này trong môi trường production, 
                // nhưng tạm thời dùng getAllTransactionsHistory để lấy dữ liệu chung của account
                val transactions = dao.getAllTransactionsHistory()
                
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                
                val timestamp = Calendar.getInstance().timeInMillis
                val file = File(downloadsDir, "SmartFinance_SaoKe_$timestamp.csv")
                
                FileOutputStream(file).use { fos ->
                    OutputStreamWriter(fos, "UTF-8").use { writer ->
                        // Add BOM for Excel UTF-8 compatibility
                        writer.write("\uFEFF")
                        writer.write("Mã GD,Ngày,Giờ,Loại,Danh mục,Ví,Số tiền,Ghi chú\n")
                        
                        transactions.forEach { t ->
                            val typeStr = if (t.type == "EXPENSE") "Chi tiêu" else "Thu tiền"
                            // Bao quanh note và category bằng dấu nháy kép để tránh lỗi dấu phẩy trong nội dung
                            writer.write("${t.transactionId},${t.transactionDate},${t.transactionTime},$typeStr,\"${t.categoryName}\",\"${t.walletName}\",${t.amount},\"${t.note}\"\n")
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Đã xuất báo cáo tại:\n${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Lỗi khi xuất báo cáo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}