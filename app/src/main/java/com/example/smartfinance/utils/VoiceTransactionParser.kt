package com.example.smartfinance.utils

import java.util.Locale

data class ParsedVoiceTransaction(
    val amount: Double,
    val isIncome: Boolean,
    val categoryKeyword: String?,
    val note: String
)

object VoiceTransactionParser {

    fun parse(rawText: String): ParsedVoiceTransaction {
        val text = rawText.lowercase(Locale("vi", "VN")).trim()

        // 1. Phân loại Thu nhập hay Chi tiêu
        val incomeKeywords = listOf("nhận lương", "thưởng", "thu nhập", "được cho", "hoàn tiền", "bán")
        val isIncome = incomeKeywords.any { text.contains(it) }

        // 2. Bóc tách số tiền (hỗ trợ: 45k, 50 nghìn, 2 triệu, 1.5 củ...)
        val amount = extractAmount(text)

        // 3. Nhận diện từ khóa danh mục
        val categoryMap = mapOf(
            "Ăn uống" to listOf("ăn", "uống", "cơm", "phở", "bún", "cà phê", "trà sữa", "nhậu", "siêu thị"),
            "Di chuyển" to listOf("xăng", "đổ xăng", "grab", "xe", "gửi xe", "vé xe", "taxi"),
            "Mua sắm" to listOf("mua", "quần", "áo", "giày", "shopee", "lazada", "mỹ phẩm"),
            "Hóa đơn" to listOf("điện", "nước", "mạng", "internet", "4g", "tiền trọ", "tiền nhà"),
            "Giải trí" to listOf("phim", "game", "nạp thẻ", "du lịch", "chơi"),
            "Lương" to listOf("lương", "thưởng", "trợ cấp")
        )

        val matchedCategory = categoryMap.entries.firstOrNull { (_, keywords) ->
            keywords.any { text.contains(it) }
        }?.key

        return ParsedVoiceTransaction(
            amount = amount,
            isIncome = isIncome,
            categoryKeyword = matchedCategory,
            note = rawText.replaceFirstChar { it.uppercase() }
        )
    }

    private fun extractAmount(text: String): Double {
        val regex = Regex("""(\d+(?:[.,]\d+)?)\s*(k|nghìn|ngàn|triệu|tr|củ)?""")
        val match = regex.find(text) ?: return 0.0

        val number = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
        val unit = match.groupValues[2]

        return when (unit) {
            "k", "nghìn", "ngàn" -> number * 1_000
            "triệu", "tr", "củ" -> number * 1_000_000
            else -> if (number < 1000) number * 1_000 else number
        }
    }
}