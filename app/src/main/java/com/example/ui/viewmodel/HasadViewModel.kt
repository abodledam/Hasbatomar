package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiExtractionResult
import com.example.ai.GeminiExtractionService
import com.example.auth.GoogleAuthManager
import com.example.auth.UserProfile
import com.example.data.db.HasadDatabase
import com.example.data.model.CategoryDefaults
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.TransactionRepository
import com.example.pdf.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

enum class AmountBracketFilter(val labelArabic: String) {
    ALL("الكل"),
    UNDER_500("أقل من 500 ج.م"),
    FROM_500_TO_2000("500 إلى 2,000 ج.م"),
    FROM_2000_TO_5000("2,000 إلى 5,000 ج.م"),
    OVER_5000("أكثر من 5,000 ج.م"),
    CUSTOM("مبلغ مخصص")
}

enum class DateFilterPeriod(val labelArabic: String) {
    ALL("كل الفترات"),
    TODAY("اليوم"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    THIS_YEAR("هذا العام")
}

data class FilterState(
    val searchQuery: String = "",
    val typeFilter: TransactionType? = null,
    val paymentMethodFilter: PaymentMethod? = null,
    val categoryFilter: String? = null,
    val amountBracket: AmountBracketFilter = AmountBracketFilter.ALL,
    val customMinAmount: Double? = null,
    val customMaxAmount: Double? = null,
    val datePeriod: DateFilterPeriod = DateFilterPeriod.ALL
)

data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val walletBalance: Double = 0.0
)

class HasadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val authManager: GoogleAuthManager = GoogleAuthManager(application)
    private val geminiService = GeminiExtractionService()

    val currentUser: StateFlow<UserProfile> = authManager.currentUser

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiResult = MutableStateFlow<AiExtractionResult?>(null)
    val aiResult: StateFlow<AiExtractionResult?> = _aiResult.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // For editing or pre-filling transaction form
    private val _activeEditingTransaction = MutableStateFlow<TransactionEntity?>(null)
    val activeEditingTransaction: StateFlow<TransactionEntity?> = _activeEditingTransaction.asStateFlow()

    private val _isFormOpen = MutableStateFlow(false)
    val isFormOpen: StateFlow<Boolean> = _isFormOpen.asStateFlow()

    private val _isAiDialogOpen = MutableStateFlow(false)
    val isAiDialogOpen: StateFlow<Boolean> = _isAiDialogOpen.asStateFlow()

    private val _isAuthDialogOpen = MutableStateFlow(false)
    val isAuthDialogOpen: StateFlow<Boolean> = _isAuthDialogOpen.asStateFlow()

    private val _activeReceiptToView = MutableStateFlow<String?>(null)
    val activeReceiptToView: StateFlow<String?> = _activeReceiptToView.asStateFlow()

    private val _reportMessage = MutableStateFlow<String?>(null)
    val reportMessage: StateFlow<String?> = _reportMessage.asStateFlow()

    init {
        val database = HasadDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())
    }

    private val allTransactions: StateFlow<List<TransactionEntity>> =
        repository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Filtered transactions for the ledger view
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _filterState
    ) { transactions, filters ->
        transactions.filter { tx ->
            // Search query filter
            val matchesQuery = filters.searchQuery.isBlank() ||
                    tx.title.contains(filters.searchQuery, ignoreCase = true) ||
                    (tx.notes?.contains(filters.searchQuery, ignoreCase = true) == true) ||
                    tx.category.contains(filters.searchQuery, ignoreCase = true)

            // Type filter
            val matchesType = filters.typeFilter == null || tx.transactionType == filters.typeFilter

            // Payment method filter
            val matchesMethod = filters.paymentMethodFilter == null ||
                    tx.method == filters.paymentMethodFilter ||
                    (tx.transactionType == TransactionType.TRANSFER && tx.targetMethod == filters.paymentMethodFilter)

            // Category filter
            val matchesCategory = filters.categoryFilter == null || tx.category == filters.categoryFilter

            // Amount bracket filter
            val matchesAmount = when (filters.amountBracket) {
                AmountBracketFilter.ALL -> true
                AmountBracketFilter.UNDER_500 -> tx.amount < 500.0
                AmountBracketFilter.FROM_500_TO_2000 -> tx.amount in 500.0..2000.0
                AmountBracketFilter.FROM_2000_TO_5000 -> tx.amount in 2000.0..5000.0
                AmountBracketFilter.OVER_5000 -> tx.amount > 5000.0
                AmountBracketFilter.CUSTOM -> {
                    val min = filters.customMinAmount ?: 0.0
                    val max = filters.customMaxAmount ?: Double.MAX_VALUE
                    tx.amount in min..max
                }
            }

            // Date period filter
            val matchesDate = when (filters.datePeriod) {
                DateFilterPeriod.ALL -> true
                DateFilterPeriod.TODAY -> isSameDay(tx.dateMillis, System.currentTimeMillis())
                DateFilterPeriod.THIS_WEEK -> isSameWeek(tx.dateMillis, System.currentTimeMillis())
                DateFilterPeriod.THIS_MONTH -> isSameMonth(tx.dateMillis, System.currentTimeMillis())
                DateFilterPeriod.THIS_YEAR -> isSameYear(tx.dateMillis, System.currentTimeMillis())
            }

            matchesQuery && matchesType && matchesMethod && matchesCategory && matchesAmount && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cumulative financial summary calculated from all transactions
    val financialSummary: StateFlow<FinancialSummary> = allTransactions.combine(_filterState) { transactions, _ ->
        var income = 0.0
        var expense = 0.0
        var cash = 0.0
        var bank = 0.0
        var wallet = 0.0

        for (tx in transactions) {
            when (tx.transactionType) {
                TransactionType.INCOME -> {
                    income += tx.amount
                    when (tx.method) {
                        PaymentMethod.CASH -> cash += tx.amount
                        PaymentMethod.BANK -> bank += tx.amount
                        PaymentMethod.WALLET -> wallet += tx.amount
                    }
                }
                TransactionType.EXPENSE -> {
                    expense += tx.amount
                    when (tx.method) {
                        PaymentMethod.CASH -> cash -= tx.amount
                        PaymentMethod.BANK -> bank -= tx.amount
                        PaymentMethod.WALLET -> wallet -= tx.amount
                    }
                }
                TransactionType.TRANSFER -> {
                    // Deduct from source
                    when (tx.method) {
                        PaymentMethod.CASH -> cash -= tx.amount
                        PaymentMethod.BANK -> bank -= tx.amount
                        PaymentMethod.WALLET -> wallet -= tx.amount
                    }
                    // Add to target
                    when (tx.targetMethod) {
                        PaymentMethod.CASH -> cash += tx.amount
                        PaymentMethod.BANK -> bank += tx.amount
                        PaymentMethod.WALLET -> wallet += tx.amount
                        null -> {}
                    }
                }
            }
        }

        FinancialSummary(
            totalIncome = income,
            totalExpense = expense,
            netBalance = income - expense,
            cashBalance = cash,
            bankBalance = bank,
            walletBalance = wallet
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    // Filter mutations
    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateTypeFilter(type: TransactionType?) {
        _filterState.value = _filterState.value.copy(typeFilter = type)
    }

    fun updatePaymentMethodFilter(method: PaymentMethod?) {
        _filterState.value = _filterState.value.copy(paymentMethodFilter = method)
    }

    fun updateCategoryFilter(category: String?) {
        _filterState.value = _filterState.value.copy(categoryFilter = category)
    }

    fun updateAmountBracket(bracket: AmountBracketFilter) {
        _filterState.value = _filterState.value.copy(amountBracket = bracket)
    }

    fun updateCustomAmountRange(min: Double?, max: Double?) {
        _filterState.value = _filterState.value.copy(
            customMinAmount = min,
            customMaxAmount = max,
            amountBracket = AmountBracketFilter.CUSTOM
        )
    }

    fun updateDatePeriod(period: DateFilterPeriod) {
        _filterState.value = _filterState.value.copy(datePeriod = period)
    }

    fun resetFilters() {
        _filterState.value = FilterState()
    }

    // Transaction CRUD
    fun openNewTransactionForm(initialAiResult: AiExtractionResult? = null) {
        if (initialAiResult != null) {
            _activeEditingTransaction.value = TransactionEntity(
                type = initialAiResult.type.name,
                amount = initialAiResult.amount,
                title = initialAiResult.title,
                category = initialAiResult.category,
                paymentMethod = initialAiResult.paymentMethod.name,
                targetPaymentMethod = initialAiResult.targetPaymentMethod?.name,
                dateMillis = initialAiResult.dateMillis,
                notes = initialAiResult.notes
            )
        } else {
            _activeEditingTransaction.value = null
        }
        _isFormOpen.value = true
    }

    fun openEditTransactionForm(transaction: TransactionEntity) {
        _activeEditingTransaction.value = transaction
        _isFormOpen.value = true
    }

    fun closeTransactionForm() {
        _isFormOpen.value = false
        _activeEditingTransaction.value = null
    }

    fun saveTransaction(
        id: Long,
        type: TransactionType,
        amount: Double,
        title: String,
        category: String,
        paymentMethod: PaymentMethod,
        targetPaymentMethod: PaymentMethod?,
        dateMillis: Long,
        receiptImagePath: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = id,
                type = type.name,
                amount = amount,
                title = title.ifBlank { "عملية بدون عنوان" },
                category = category.ifBlank { "أخرى" },
                paymentMethod = paymentMethod.name,
                targetPaymentMethod = if (type == TransactionType.TRANSFER) targetPaymentMethod?.name else null,
                dateMillis = dateMillis,
                receiptImagePath = receiptImagePath,
                notes = notes
            )

            if (id == 0L) {
                repository.insertTransaction(entity)
            } else {
                repository.updateTransaction(entity)
            }
            closeTransactionForm()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            // Delete local receipt image file if exists
            transaction.receiptImagePath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            repository.deleteTransaction(transaction)
        }
    }

    // Receipt image handling
    suspend fun saveReceiptImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val receiptsDir = File(context.filesDir, "receipts")
            if (!receiptsDir.exists()) {
                receiptsDir.mkdirs()
            }
            val fileName = "receipt_${System.currentTimeMillis()}.jpg"
            val destFile = File(receiptsDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // AI Gateway & Google Account Gate
    fun onAiToolRequested() {
        if (!currentUser.value.isSignedIn) {
            _isAuthDialogOpen.value = true
        } else {
            _isAiDialogOpen.value = true
        }
    }

    fun closeAuthDialog() {
        _isAuthDialogOpen.value = false
    }

    fun closeAiDialog() {
        _isAiDialogOpen.value = false
        _aiResult.value = null
        _aiError.value = null
    }

    fun performGoogleSignIn(name: String, email: String) {
        authManager.signIn(name, email)
        _isAuthDialogOpen.value = false
        // Proceed to AI dialog directly!
        _isAiDialogOpen.value = true
    }

    fun signOutGoogle() {
        authManager.signOut()
    }

    fun extractWithAi(promptText: String, imageUri: Uri? = null) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiError.value = null
            _aiResult.value = null

            val context = getApplication<Application>()
            var bitmap: Bitmap? = null
            if (imageUri != null) {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(imageUri)?.use { stream ->
                            bitmap = BitmapFactory.decodeStream(stream)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val result = geminiService.extractTransactionDetails(promptText, bitmap)
            result.onSuccess { extracted ->
                _aiResult.value = extracted
                _isAiLoading.value = false
            }.onFailure { error ->
                _aiError.value = error.message ?: "تعذر استخراج البيانات بالذكاء الاصطناعي"
                _isAiLoading.value = false
            }
        }
    }

    fun applyAiResultToForm(receiptUri: Uri? = null) {
        val result = _aiResult.value ?: return
        viewModelScope.launch {
            var receiptPath: String? = null
            if (receiptUri != null) {
                receiptPath = saveReceiptImage(receiptUri)
            }
            openNewTransactionForm(result)
            if (receiptPath != null) {
                _activeEditingTransaction.value = _activeEditingTransaction.value?.copy(
                    receiptImagePath = receiptPath
                )
            }
            closeAiDialog()
        }
    }

    // PDF Export
    fun exportPdfReport() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val transactions = filteredTransactions.value
            val summary = financialSummary.value
            val filterDesc = when {
                _filterState.value.datePeriod != DateFilterPeriod.ALL -> _filterState.value.datePeriod.labelArabic
                _filterState.value.typeFilter != null -> _filterState.value.typeFilter!!.labelArabic
                else -> "كافة العمليات (${transactions.size} عملية)"
            }

            withContext(Dispatchers.IO) {
                val file = PdfReportGenerator.generateAndShareReport(
                    context = context,
                    transactions = transactions,
                    totalIncome = summary.totalIncome,
                    totalExpense = summary.totalExpense,
                    netBalance = summary.netBalance,
                    cashBalance = summary.cashBalance,
                    bankBalance = summary.bankBalance,
                    walletBalance = summary.walletBalance,
                    filterDescription = filterDesc
                )

                if (file != null) {
                    PdfReportGenerator.sharePdfFile(context, file)
                    _reportMessage.value = "تم إنشاء التقرير بنجاح"
                } else {
                    _reportMessage.value = "حدث خطأ أثناء إنشاء التقرير"
                }
            }
        }
    }

    fun dismissReportMessage() {
        _reportMessage.value = null
    }

    fun viewReceipt(imagePath: String?) {
        _activeReceiptToView.value = imagePath
    }

    fun closeReceiptViewer() {
        _activeReceiptToView.value = null
    }

    // Date helper utilities
    private fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(millis1: Long, millis2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(millis1: Long, millis2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
    }

    private fun isSameYear(millis1: Long, millis2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
    }
}
