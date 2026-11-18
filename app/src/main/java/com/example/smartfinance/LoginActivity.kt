package com.example.smartfinance

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartfinance.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)
        val btnBiometric = findViewById<Button>(R.id.btnBiometric)
        val btnGoogle = findViewById<LinearLayout>(R.id.btnGoogle)
        val btnApple = findViewById<LinearLayout>(R.id.btnApple)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        var isPasswordVisible = false

        val dao = AppDatabase.getInstance(this).smartFinanceDao()

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ Email và Mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val user = dao.login(email, password)
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        Toast.makeText(this@LoginActivity, "Chào mừng ${user.name}!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("USER_ID", user.userId)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Đang kết nối đăng nhập bằng Google...", Toast.LENGTH_SHORT).show()
        }

        btnApple.setOnClickListener {
            Toast.makeText(this, "Đang kết nối đăng nhập bằng Apple ID...", Toast.LENGTH_SHORT).show()
        }

        btnBiometric.setOnClickListener {
            Toast.makeText(this, "Vui lòng chạm cảm biến vân tay hoặc nhìn vào camera", Toast.LENGTH_SHORT).show()
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                // Hiện mật khẩu & đổi icon sang mắt mở
                edtPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_eye_open)
            } else {
                // Ẩn mật khẩu & đổi icon sang mắt gạch chéo
                edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            // Đưa con trỏ về cuối chuỗi
            edtPassword.setSelection(edtPassword.text.length)
        }
    }
}