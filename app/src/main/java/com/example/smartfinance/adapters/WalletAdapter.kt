package com.example.smartfinance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfinance.R
import com.example.smartfinance.models.Wallet
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class WalletAdapter(
    private var wallets: List<Wallet>,
    private val onItemClick: (Wallet) -> Unit
) : RecyclerView.Adapter<WalletAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvWalletIcon)
        val tvName: TextView = view.findViewById(R.id.tvWalletName)
        val tvBalance: TextView = view.findViewById(R.id.tvWalletBalance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wallet = wallets[position]
        
        holder.tvIcon.text = if (wallet.name.contains("tiền mặt", ignoreCase = true)) "💵" 
                             else if (wallet.name.contains("thẻ", ignoreCase = true) || wallet.name.contains("bank", ignoreCase = true)) "💳"
                             else "👛"
        
        holder.tvName.text = wallet.name
        
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply { groupingSeparator = '.' }
        val df = DecimalFormat("#,###", symbols)
        holder.tvBalance.text = "${df.format(wallet.balance)} đ"
        
        holder.itemView.setOnClickListener { onItemClick(wallet) }
    }

    override fun getItemCount() = wallets.size

    fun updateData(newWallets: List<Wallet>) {
        wallets = newWallets
        notifyDataSetChanged()
    }
}