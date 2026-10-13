package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionType(val labelArabic: String) {
    EXPENSE("مصروف"),
    INCOME("دخل"),
    TRANSFER("تحويل");

    fun getIcon(): ImageVector = when (this) {
        EXPENSE -> Icons.Default.ArrowDownward
        INCOME -> Icons.Default.ArrowUpward
        TRANSFER -> Icons.Default.SwapHoriz
    }
}

enum class PaymentMethod(val labelArabic: String, val shortLabel: String) {
    CASH("كاش / نقدي", "كاش"),
    BANK("تحويل بنكي", "بنك"),
    WALLET("محفظة إلكترونية", "محفظة");

    fun getIcon(): ImageVector = when (this) {
        CASH -> Icons.Default.Payments
        BANK -> Icons.Default.AccountBalance
        WALLET -> Icons.Default.AccountBalanceWallet
    }
}

object CategoryDefaults {
    val expenseCategories = listOf(
        "فواتير وخدمات",
        "طعام ومشتريات",
        "مواصلات ووقود",
        "إيجار وسكن",
        "تسوق وملابس",
        "صحة وعلاج",
        "صيانة وأعطال",
        "أخرى"
    )

    val incomeCategories = listOf(
        "راتب شهري",
        "مبيعات وأرباح",
        "عمل حر (فريلانس)",
        "استثمارات",
        "مكافأة",
        "أخرى"
    )

    val transferCategories = listOf(
        "تحويل بين الحسابات",
        "سحب نقدي",
        "إيداع بنكي",
        "تغذية محفظة"
    )

    fun getCategoryIcon(category: String): ImageVector {
        return when (category) {
            "فواتير وخدمات" -> Icons.Default.Receipt
            "طعام ومشتريات" -> Icons.Default.Fastfood
            "مواصلات ووقود" -> Icons.Default.DirectionsCar
            "إيجار وسكن" -> Icons.Default.Home
            "تسوق وملابس" -> Icons.Default.ShoppingBag
            "صحة وعلاج" -> Icons.Default.LocalHospital
            "صيانة وأعطال" -> Icons.Default.Build
            "راتب شهري" -> Icons.Default.Work
            "مبيعات وأرباح" -> Icons.Default.BusinessCenter
            "عمل حر (فريلانس)" -> Icons.Default.AttachMoney
            "استثمارات" -> Icons.Default.Star
            "تحويل بين الحسابات", "سحب نقدي", "إيداع بنكي", "تغذية محفظة" -> Icons.Default.SwapHoriz
            else -> Icons.Default.MoreHoriz
        }
    }
}
