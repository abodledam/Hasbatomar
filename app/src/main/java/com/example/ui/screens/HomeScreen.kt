package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AiExtractionDialog
import com.example.ui.components.FilterBar
import com.example.ui.components.GoogleSignInDialog
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.SummaryCard
import com.example.ui.components.TransactionCard
import com.example.ui.theme.HasadEmerald
import com.example.ui.theme.HasadGold
import com.example.ui.viewmodel.HasadViewModel

@Composable
fun HomeScreen(
    viewModel: HasadViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.financialSummary.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val userProfile by viewModel.currentUser.collectAsState()

    val isFormOpen by viewModel.isFormOpen.collectAsState()
    val activeTransaction by viewModel.activeEditingTransaction.collectAsState()

    val isAiDialogOpen by viewModel.isAiDialogOpen.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    val isAuthDialogOpen by viewModel.isAuthDialogOpen.collectAsState()
    val activeReceiptToView by viewModel.activeReceiptToView.collectAsState()
    val reportMessage by viewModel.reportMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(reportMessage) {
        reportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissReportMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary AI FAB
                FloatingActionButton(
                    onClick = { viewModel.onAiToolRequested() },
                    containerColor = HasadGold,
                    contentColor = Color(0xFF3E2700),
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_ai_assistant")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "استخراج ذكي",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Primary Add Transaction Extended FAB
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openNewTransactionForm() },
                    containerColor = HasadEmerald,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text(
                            text = "تسجيل عملية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("fab_add_transaction")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary Hero Card
            item {
                SummaryCard(
                    summary = summary,
                    userProfile = userProfile,
                    onAiClick = { viewModel.onAiToolRequested() },
                    onExportPdfClick = { viewModel.exportPdfReport() },
                    onProfileClick = { viewModel.onAiToolRequested() }
                )
            }

            // Advanced Filters & Search Bar
            item {
                FilterBar(
                    filterState = filterState,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onTypeChange = { viewModel.updateTypeFilter(it) },
                    onMethodChange = { viewModel.updatePaymentMethodFilter(it) },
                    onAmountBracketChange = { viewModel.updateAmountBracket(it) },
                    onCustomAmountChange = { min, max -> viewModel.updateCustomAmountRange(min, max) },
                    onDatePeriodChange = { viewModel.updateDatePeriod(it) },
                    onResetFilters = { viewModel.resetFilters() }
                )
            }

            // Transactions Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل العمليات المالية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${filteredTransactions.size} عملية",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty State or List of Transactions
            if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد عمليات مطابقة للفلاتر الحالية",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اضغط على زر (+ تسجيل عملية) أو استعن بمساعد الذكاء الاصطناعي",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = filteredTransactions,
                    key = { it.id }
                ) { transaction ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TransactionCard(
                            transaction = transaction,
                            onEdit = { viewModel.openEditTransactionForm(transaction) },
                            onDelete = { viewModel.deleteTransaction(transaction) },
                            onViewReceipt = { viewModel.viewReceipt(it) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Add/Edit Transaction
    if (isFormOpen) {
        TransactionFormSheet(
            activeTransaction = activeTransaction,
            onDismiss = { viewModel.closeTransactionForm() },
            onSave = { id, type, amount, title, cat, method, targetMethod, dateMillis, receiptPath, notes ->
                viewModel.saveTransaction(
                    id = id,
                    type = type,
                    amount = amount,
                    title = title,
                    category = cat,
                    paymentMethod = method,
                    targetPaymentMethod = targetMethod,
                    dateMillis = dateMillis,
                    receiptImagePath = receiptPath,
                    notes = notes
                )
            },
            onSaveReceiptUri = { uri ->
                viewModel.saveReceiptImage(uri)
            },
            onAiAssistantClick = {
                viewModel.closeTransactionForm()
                viewModel.onAiToolRequested()
            }
        )
    }

    // Google Sign-In gate modal
    if (isAuthDialogOpen) {
        GoogleSignInDialog(
            userProfile = userProfile,
            onDismiss = { viewModel.closeAuthDialog() },
            onSignIn = { name, email ->
                viewModel.performGoogleSignIn(name, email)
            },
            onSignOut = {
                viewModel.signOutGoogle()
            }
        )
    }

    // AI Extraction Dialog
    if (isAiDialogOpen) {
        AiExtractionDialog(
            isLoading = isAiLoading,
            aiResult = aiResult,
            errorMessage = aiError,
            onDismiss = { viewModel.closeAiDialog() },
            onExtract = { prompt, imageUri ->
                viewModel.extractWithAi(prompt, imageUri)
            },
            onApplyToForm = { uri ->
                viewModel.applyAiResultToForm(uri)
            }
        )
    }

    // Receipt Full-Size Viewer Dialog
    if (activeReceiptToView != null) {
        ReceiptViewerDialog(
            imagePath = activeReceiptToView!!,
            onDismiss = { viewModel.closeReceiptViewer() }
        )
    }
}
