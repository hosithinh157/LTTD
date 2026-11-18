package com.example.smartfinance.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfinance.R
import com.example.smartfinance.dao.TransactionHistoryItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

data class DailyHistoryGroup(
    val dateHeader: String,
    val dailyNetTotal: Double,
    val items: List<TransactionHistoryItem>
)

class HistoryGroupAdapter(
    private var groups: List<DailyHistoryGroup>,
    private val onItemClick: (TransactionHistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryGroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGroupDate: TextView = view.findViewById(R.id.tvGroupDate)
        val tvGroupDailyTotal: TextView = view.findViewById(R.id.tvGroupDailyTotal)
        val containerDayTransactions: LinearLayout = view.findViewById(R.id.containerDayTransactions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.tvGroupDate.text = group.dateHeader

        val prefix = if (group.dailyNetTotal > 0) "+" else ""
        holder.tvGroupDailyTotal.text = "$prefix${formatCurrency(group.dailyNetTotal)} đ"

        holder.containerDayTransactions.removeAllViews()
        val inflater = LayoutInflater.from(holder.itemView.context)

        group.items.forEachIndexed { index, item ->
            val rowView = inflater.inflate(R.layout.item_history_row, holder.containerDayTransactions, false)
            val tvIcon = rowView.findViewById<TextView>(R.id.tvHistoryIcon)
            val tvTitle = rowView.findViewById<TextView>(R.id.tvHistoryTitle)
            val tvWallet = rowView.findViewById<TextView>(R.id.tvHistoryWallet)
            val tvAmount = rowView.findViewById<TextView>(R.id.tvHistoryAmount)
            val divider = rowView.findViewById<View>(R.id.dividerRow)

            tvTitle.text = item.note.ifEmpty { item.categoryName }
            tvWallet.text = item.walletName
            tvIcon.text = getEmojiForCategory(item.categoryName, item.type)

            val formatted = formatCurrency(item.amount)
            if (item.type == "INCOME") {
                tvAmount.text = "+$formatted đ"
                tvAmount.setTextColor(Color.parseColor("#066843"))
                tvIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
            } else {
                tvAmount.text = "-$formatted đ"
                tvAmount.setTextColor(Color.parseColor("#0F172A"))
                tvIcon.backgroundTintList = ColorStateList.valueOf(getBgColorForCategory(item.categoryName))
            }

            // Ẩn đường kẻ ngang ở dòng cuối cùng trong thẻ
            if (index == group.items.lastIndex) {
                divider.visibility = View.GONE
            }

            rowView.setOnClickListener {
                onItemClick(item)
            }

            holder.containerDayTransactions.addView(rowView)
        }
    }

    override fun getItemCount(): Int = groups.size

    fun submitTransactions(rawList: List<TransactionHistoryItem>) {
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val cal = Calendar.getInstance()
        val todayStr = sdfDb.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdfDb.format(cal.time)

        val groupedMap = rawList.groupBy { it.transactionDate }
        val newGroups = groupedMap.map { (dateStr, items) ->
            val header = when (dateStr) {
                todayStr -> "HÔM NAY"
                yesterdayStr -> "HÔM QUA"
                else -> try {
                    sdfDisplay.format(sdfDb.parse(dateStr)!!)
                } catch (e: Exception) {
                    dateStr
                }
            }
            val netTotal = items.sumOf { if (it.type == "INCOME") it.amount else -it.amount }
            DailyHistoryGroup(header, netTotal, items)
        }

        groups = newGroups
        notifyDataSetChanged()
    }

    private fun getEmojiForCategory(catName: String, type: String): String {
        if (type == "INCOME") return "💵"
        return when {
            catName.contains("Ăn", true) -> "🍴"
            catName.contains("Đi", true) -> "🛵"
            catName.contains("trọ", true) -> "🏠"
            catName.contains("Mua", true) -> "🛒"
            catName.contains("Học", true) -> "📖"
            catName.contains("Giải", true) -> "🎮"
            catName.contains("Hóa", true) -> "🧾"
            catName.contains("Sức", true) -> "🩺"
            else -> "💸"
        }
    }

    private fun getBgColorForCategory(catName: String): Int {
        return when {
            catName.contains("Ăn", true) -> Color.parseColor("#FFEDD5")
            catName.contains("Đi", true) -> Color.parseColor("#DBEAFE")
            catName.contains("trọ", true) -> Color.parseColor("#FEE2E2")
            catName.contains("Mua", true) -> Color.parseColor("#CCFBF1")
            catName.contains("Học", true) -> Color.parseColor("#E0E7FF")
            else -> Color.parseColor("#FEF3C7")
        }
    }

    private fun formatCurrency(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply { groupingSeparator = '.' }
        return DecimalFormat("#,###", symbols).format(amount)
    }
}