package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CategoryDefaults
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.HasadEmerald
import com.example.ui.theme.HasadGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    activeTransaction: TransactionEntity?,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit,
    onSaveReceiptUri: suspend (Uri) -> String?,
    onAiAssistantClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val isEditing = activeTransaction != null && activeTransaction.id != 0L

    var type by remember { mutableStateOf(activeTransaction?.transactionType ?: TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf(activeTransaction?.let { if (it.amount > 0.0) it.amount.toString() else "" } ?: "") }
    var title by remember { mutableStateOf(activeTransaction?.title ?: "") }
    var category by remember {
        mutableStateOf(
            activeTransaction?.category ?: CategoryDefaults.expenseCategories.first()
        )
    }
    var paymentMethod by remember { mutableStateOf(activeTransaction?.method ?: PaymentMethod.CASH) }
    var targetPaymentMethod by remember {
        mutableStateOf(activeTransaction?.targetMethod ?: PaymentMethod.BANK)
    }
    var dateMillis by remember { mutableLongStateOf(activeTransaction?.dateMillis ?: System.currentTimeMillis()) }
    var receiptImagePath by remember { mutableStateOf(activeTransaction?.receiptImagePath) }
    var pendingReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var notes by remember { mutableStateOf(activeTransaction?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingReceiptUri = uri
        }
    }

    val availableCategories = when (type) {
        TransactionType.EXPENSE -> CategoryDefaults.expenseCategories
        TransactionType.INCOME -> CategoryDefaults.incomeCategories
        TransactionType.TRANSFER -> CategoryDefaults.transferCategories
    }

    val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier.testTag("transaction_form_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "تعديل العملية المالية" else "تسجيل عملية مالية جديدة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            // Prominent Top AI Assistant Banner
            Surface(
                onClick = onAiAssistantClick,
                shape = RoundedCornerShape(12.dp),
                color = HasadEmerald.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_gateway_form_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(HasadEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = HasadGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "استخراج ذكي بالذكاء الاصطناعي (AI Gateway)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HasadEmerald
                        )
                        Text(
                            text = "املأ هذا النموذج تلقائياً من نص حر أو صورة إيصال",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Transaction Type Selector (Segmented Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expense
                FilterChip(
                    selected = type == TransactionType.EXPENSE,
                    onClick = {
                        type = TransactionType.EXPENSE
                        if (!CategoryDefaults.expenseCategories.contains(category)) {
                            category = CategoryDefaults.expenseCategories.first()
                        }
                    },
                    label = { Text("مصروف", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRed,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_chip_expense")
                )

                // Income
                FilterChip(
                    selected = type == TransactionType.INCOME,
                    onClick = {
                        type = TransactionType.INCOME
                        if (!CategoryDefaults.incomeCategories.contains(category)) {
                            category = CategoryDefaults.incomeCategories.first()
                        }
                    },
                    label = { Text("دخل", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeGreen,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_chip_income")
                )

                // Transfer
                FilterChip(
                    selected = type == TransactionType.TRANSFER,
                    onClick = {
                        type = TransactionType.TRANSFER
                        category = CategoryDefaults.transferCategories.first()
                    },
                    label = { Text("تحويل", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TransferBlue,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_chip_transfer")
                )
            }

            // Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("المبلغ المالي *") },
                suffix = { Text("ج.م", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            // Title / Description Field
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("البيان والوصف *") },
                placeholder = { Text("مثال: فاتورة كهرباء، راتب، مشتريات...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "التصنيف:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryDefaults.getCategoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HasadEmerald,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Payment Method Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (type == TransactionType.TRANSFER) "من وسيلة الدفع (المصدر):" else "وسيلة الدفع:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method.shortLabel, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = method.getIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // If Transfer: Target Payment Method Selector
            AnimatedVisibility(visible = type == TransactionType.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "إلى وسيلة الدفع (المستلم):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = targetPaymentMethod == method,
                                onClick = { targetPaymentMethod = method },
                                label = { Text(method.shortLabel, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = method.getIcon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Receipt Attachment Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "صورة الإيصال أو الفاتورة:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (pendingReceiptUri != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = pendingReceiptUri,
                                contentDescription = "إيصال جديد محدد",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "تم تحديد إيصال جديد (سيُحفظ محلياً)",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { pendingReceiptUri = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إلغاء",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else if (receiptImagePath != null) {
                    val file = File(receiptImagePath!!)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (file.exists()) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "صورة الإيصال المحفوظة",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "إيصال محفوظ مسبقاً",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { receiptImagePath = null }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الإيصال",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attach_receipt_form_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = HasadEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرفاق صورة الإيصال أو الفاتورة", fontSize = 12.sp)
                    }
                }
            }

            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات إضافية (اختياري)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input_field"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            // Date display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التاريخ المسجل: ${dateFormatter.format(Date(dateMillis))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Error display
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            // Submit Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMessage = "يرجى إدخال مبلغ مالي صحيح أكبر من الصفر"
                        return@Button
                    }
                    if (title.isBlank()) {
                        errorMessage = "يرجى إدخال بيان أو وصف للعملية"
                        return@Button
                    }
                    if (type == TransactionType.TRANSFER && paymentMethod == targetPaymentMethod) {
                        errorMessage = "في التحويلات، يجب اختيار وسيلتي دفع مختلفتين (من / إلى)"
                        return@Button
                    }

                    scope.launch {
                        var finalImagePath = receiptImagePath
                        if (pendingReceiptUri != null) {
                            finalImagePath = onSaveReceiptUri(pendingReceiptUri!!)
                        }

                        onSave(
                            activeTransaction?.id ?: 0L,
                            type,
                            amount,
                            title,
                            category,
                            paymentMethod,
                            if (type == TransactionType.TRANSFER) targetPaymentMethod else null,
                            dateMillis,
                            finalImagePath,
                            notes.ifBlank { null }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HasadEmerald),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_transaction_button")
            ) {
                Text(
                    text = if (isEditing) "حفظ التعديلات" else "تسجيل العملية في السجل",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
