package com.example.smartfinance.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfinance.R
import com.example.smartfinance.dao.TransactionWithCategory
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class TransactionAdapter(
    private var list: List<TransactionWithCategory>
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bgCategoryIcon: FrameLayout = view.findViewById(R.id.bgCategoryIcon)
        val ivCategoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val tvTransNote: TextView = view.findViewById(R.id.tvTransNote)
        val tvTransSub: TextView = view.findViewById(R.id.tvTransSub)
        val tvTransAmount: TextView = view.findViewById(R.id.tvTransAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val formattedMoney = formatCurrency(item.amount)

        holder.tvTransNote.text = item.note.ifEmpty { item.categoryName }
        holder.tvTransSub.text = "${item.categoryName} • ${item.transactionTime}"

        if (item.type == "INCOME") {
            holder.tvTransAmount.text = "+$formattedMoney đ"
            holder.tvTransAmount.setTextColor(Color.parseColor("#066843"))
            holder.bgCategoryIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
            holder.ivCategoryIcon.setImageResource(android.R.drawable.arrow_down_float)
            holder.ivCategoryIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#066843"))
        } else {
            holder.tvTransAmount.text = "-$formattedMoney đ"
            holder.tvTransAmount.setTextColor(Color.parseColor("#0F172A"))
            holder.bgCategoryIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
            holder.ivCategoryIcon.setImageResource(android.R.drawable.arrow_up_float)
            holder.ivCategoryIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<TransactionWithCategory>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun formatCurrency(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
        }
        return DecimalFormat("#,###", symbols).format(amount)
    }
}