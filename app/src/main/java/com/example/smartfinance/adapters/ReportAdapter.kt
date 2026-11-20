package com.example.smartfinance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfinance.R
import com.example.smartfinance.dao.CategoryExpenseResult
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ReportAdapter(
    private var items: List<CategoryExpenseResult>,
    private var totalExpense: Double
) : RecyclerView.Adapter<ReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvReportIcon)
        val tvName: TextView = view.findViewById(R.id.tvReportCatName)
        val tvAmount: TextView = view.findViewById(R.id.tvReportAmount)
        val tvPercent: TextView = view.findViewById(R.id.tvReportPercent)
        val progressBar: ProgressBar = view.findViewById(R.id.progressReportCat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // Cố gắng map tên danh mục ra emoji tương ứng (dựa theo data đã có)
        val iconMap = mapOf(
            "Ăn uống" to "🍴", "Đi lại" to "🚆", "Tiền trọ" to "🏠", 
            "Mua sắm" to "🛍️", "Giải trí" to "🎮", "Học tập" to "🎓", 
            "Hóa đơn" to "🧾", "Sức khỏe" to "🩺"
        )
        holder.tvIcon.text = iconMap[item.categoryName] ?: "📦"
        
        holder.tvName.text = item.categoryName
        
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply { groupingSeparator = '.' }
        val df = DecimalFormat("#,###", symbols)
        holder.tvAmount.text = "${df.format(item.totalAmount)} đ"
        
        val percent = if (totalExpense > 0) (item.totalAmount / totalExpense) * 100 else 0.0
        holder.tvPercent.text = String.format(Locale.US, "%.1f%%", percent)
        holder.progressBar.progress = percent.toInt()
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<CategoryExpenseResult>, newTotal: Double) {
        items = newItems
        totalExpense = newTotal
        notifyDataSetChanged()
    }
}