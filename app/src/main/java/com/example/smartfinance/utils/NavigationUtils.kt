package com.example.smartfinance.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.TextView
import com.example.smartfinance.AddTransactionActivity
import com.example.smartfinance.BudgetActivity
import com.example.smartfinance.HistoryActivity
import com.example.smartfinance.MainActivity
import com.example.smartfinance.ProfileActivity
import com.example.smartfinance.R
import com.example.smartfinance.ReportActivity

object NavigationUtils {
    fun setupNavigation(activity: Activity, currentUserId: Int, currentTab: String) {
        val navHome = activity.findViewById<TextView>(R.id.navHome)
        val navHistory = activity.findViewById<TextView>(R.id.navHistory)
        val navBudget = activity.findViewById<TextView>(R.id.navBudget)
        val navReport = activity.findViewById<TextView>(R.id.navReport)
        val fabAdd = activity.findViewById<View>(R.id.fabAddTransaction)
        val ivProfile = activity.findViewById<View>(R.id.ivProfile)

        // Reset colors
        val defaultColor = Color.parseColor("#64748B")
        val activeColor = Color.parseColor("#066843")

        navHome?.setTextColor(if (currentTab == "HOME") activeColor else defaultColor)
        navHistory?.setTextColor(if (currentTab == "HISTORY") activeColor else defaultColor)
        navBudget?.setTextColor(if (currentTab == "BUDGET") activeColor else defaultColor)
        navReport?.setTextColor(if (currentTab == "REPORT") activeColor else defaultColor)

        navHome?.setOnClickListener {
            if (currentTab != "HOME") navigateTo(activity, MainActivity::class.java, currentUserId)
        }
        navHistory?.setOnClickListener {
            if (currentTab != "HISTORY") navigateTo(activity, HistoryActivity::class.java, currentUserId)
        }
        navBudget?.setOnClickListener {
            if (currentTab != "BUDGET") navigateTo(activity, BudgetActivity::class.java, currentUserId)
        }
        navReport?.setOnClickListener {
            if (currentTab != "REPORT") navigateTo(activity, ReportActivity::class.java, currentUserId)
        }
        fabAdd?.setOnClickListener {
            navigateTo(activity, AddTransactionActivity::class.java, currentUserId, false)
        }
        ivProfile?.setOnClickListener {
            navigateTo(activity, ProfileActivity::class.java, currentUserId, false)
        }
    }

    private fun navigateTo(activity: Activity, targetClass: Class<*>, userId: Int, reorderToFront: Boolean = true) {
        val intent = Intent(activity, targetClass)
        intent.putExtra("USER_ID", userId)
        if (reorderToFront) {
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        activity.startActivity(intent)
        // Không gọi finish() để tránh phải load lại nếu người dùng ấn Back. 
        // Thay vì thế FLAG_ACTIVITY_REORDER_TO_FRONT sẽ giúp đảo Stack mà không đẻ ra Activity mới.
    }
}