package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val amount: Double,
    val title: String,
    val category: String,
    val paymentMethod: String, // "CASH", "BANK", "WALLET"
    val targetPaymentMethod: String? = null,
    val dateMillis: Long,
    val receiptImagePath: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val transactionType: TransactionType
        get() = try {
            TransactionType.valueOf(type)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }

    val method: PaymentMethod
        get() = try {
            PaymentMethod.valueOf(paymentMethod)
        } catch (e: Exception) {
            PaymentMethod.CASH
        }

    val targetMethod: PaymentMethod?
        get() = targetPaymentMethod?.let {
            try {
                PaymentMethod.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
}
