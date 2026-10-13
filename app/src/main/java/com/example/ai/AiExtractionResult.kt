package com.example.ai

import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType

data class AiExtractionResult(
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String = "أخرى",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val targetPaymentMethod: PaymentMethod? = null,
    val title: String = "",
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val rawExplanation: String = ""
)
