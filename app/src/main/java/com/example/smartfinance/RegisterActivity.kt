package com.example.smartfinance

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartfinance.database.AppDatabase
import com.example.smartfinance.models.User
import com.example.smartfinance.models.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtFullName = findViewById<EditText>(R.id.edtFullName)
        val edtEmail = findViewById<EditText>(R.id.edtRegEmail)
        val edtPassword = findViewById<EditText>(R.id.edtRegPassword)
        val edtConfirm = findViewById<EditText>(R.id.edtConfirmPassword)
        val ivToggleRegPass = findViewById<ImageView>(R.id.ivToggleRegPass)
        val ivToggleConfirmPass = findViewById<ImageView>(R.id.ivToggleConfirmPass)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        setupPasswordToggle(edtPassword, ivToggleRegPass)
        setupPasswordToggle(edtConfirm, ivToggleConfirmPass)

        val dao = AppDatabase.getInstance(this).smartFinanceDao()

        btnRegister.setOnClickListener {
            val name = edtFullName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirm = edtConfirm.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirm) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val existingUser = dao.getUserByEmail(email)
                if (existingUser != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterActivity, "Email này đã được đăng ký!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val newUserId = dao.insertUser(
                    User(name = name, email = email, passwordHash = password, createdAt = currentDate)
                ).toInt()

                dao.insertWallet(
                    Wallet(userId = newUserId, name = "Tiền mặt", iconName = "ic_cash", balance = 0.0)
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        tvBackToLogin.setOnClickListener { finish() }
    }

    private fun setupPasswordToggle(editText: EditText, toggleIcon: ImageView) {
        var isVisible = false
        toggleIcon.setOnClickListener {
            isVisible = !isVisible
            if (isVisible) {
                editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                toggleIcon.setImageResource(R.drawable.ic_eye_open)
            } else {
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleIcon.setImageResource(R.drawable.ic_eye_off)
            }
            editText.setSelection(editText.text.length)
        }
    }
}