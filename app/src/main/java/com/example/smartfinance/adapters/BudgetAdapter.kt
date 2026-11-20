package com.example.smartfinance.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfinance.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class BudgetItem(
    val budgetId: Int,
    val categoryId: Int,
    val categoryName: String,
    val limitAmount: Double,
    val spentAmount: Double
)

class BudgetAdapter(
    private var items: List<BudgetItem>,
    private val onItemClick: (BudgetItem) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvBudgetIcon)
        val tvName: TextView = view.findViewById(R.id.tvBudgetCatName)
        val tvLimit: TextView = view.findViewById(R.id.tvBudgetLimit)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBudgetBar)
        val tvSpent: TextView = view.findViewById(R.id.tvBudgetSpent)
        val tvRemaining: TextView = view.findViewById(R.id.tvBudgetRemaining)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        val iconMap = mapOf(
            "Ăn uống" to "🍴", "Đi lại" to "🚆", "Tiền trọ" to "🏠", 
            "Mua sắm" to "🛍️", "Giải trí" to "🎮", "Học tập" to "🎓", 
            "Hóa đơn" to "🧾", "Sức khỏe" to "🩺"
        )
        holder.tvIcon.text = iconMap[item.categoryName] ?: "📦"
        holder.tvName.text = item.categoryName
        
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply { groupingSeparator = '.' }
        val df = DecimalFormat("#,###", symbols)
        
        holder.tvLimit.text = "${df.format(item.limitAmount)} đ"
        holder.tvSpent.text = "Đã tiêu: ${df.format(item.spentAmount)} đ"
        
        val remaining = item.limitAmount - item.spentAmount
        holder.tvRemaining.text = "Còn lại: ${df.format(remaining.coerceAtLeast(0.0))} đ"
        
        val percent = if (item.limitAmount > 0) (item.spentAmount / item.limitAmount) * 100 else 0.0
        holder.progressBar.progress = percent.toInt().coerceAtMost(100)
        
        // Đổi màu thanh tiến độ & chữ còn lại dựa trên tỷ lệ
        val colorHex = when {
            percent < 80 -> "#066843" // Xanh lá (An toàn)
            percent <= 100 -> "#F59E0B" // Cam (Sắp hết)
            else -> "#B91C1C" // Đỏ (Vượt mức)
        }
        val color = Color.parseColor(colorHex)
        holder.progressBar.progressTintList = ColorStateList.valueOf(color)
        holder.tvRemaining.setTextColor(color)
        
        if (percent > 100) {
            holder.tvRemaining.text = "Vượt mức: ${df.format(-remaining)} đ"
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<BudgetItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}