package com.example.smartfinance

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartfinance.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {

    private var verifiedEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val btnBack = findViewById<ImageButton>(R.id.btnBackForgot)
        val edtEmail = findViewById<EditText>(R.id.edtForgotEmail)
        val btnVerifyEmail = findViewById<Button>(R.id.btnVerifyEmail)
        val layoutReset = findViewById<LinearLayout>(R.id.layoutResetPassword)
        val edtNewPassword = findViewById<EditText>(R.id.edtNewPassword)
        val edtConfirmNew = findViewById<EditText>(R.id.edtConfirmNewPassword)
        val ivToggleNewPass = findViewById<ImageView>(R.id.ivToggleNewPass)
        val ivToggleConfirmNewPass = findViewById<ImageView>(R.id.ivToggleConfirmNewPass)
        val btnConfirmReset = findViewById<Button>(R.id.btnConfirmReset)

        setupPasswordToggle(edtNewPassword, ivToggleNewPass)
        setupPasswordToggle(edtConfirmNew, ivToggleConfirmNewPass)

        val dao = AppDatabase.getInstance(this).smartFinanceDao()

        btnBack.setOnClickListener { finish() }

        btnVerifyEmail.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email đã đăng ký", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val user = dao.getUserByEmail(email)
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        verifiedEmail = email
                        edtEmail.isEnabled = false
                        btnVerifyEmail.visibility = View.GONE
                        layoutReset.visibility = View.VISIBLE
                        Toast.makeText(this@ForgotPasswordActivity, "Đã tìm thấy tài khoản: ${user.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "Email không tồn tại trong hệ thống!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnConfirmReset.setOnClickListener {
            val newPass = edtNewPassword.text.toString().trim()
            val confirmPass = edtConfirmNew.text.toString().trim()
            val email = verifiedEmail ?: return@setOnClickListener

            if (newPass.length < 6) {
                Toast.makeText(this, "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                dao.updatePassword(email, newPass)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
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