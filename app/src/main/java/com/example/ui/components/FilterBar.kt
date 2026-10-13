package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.ui.theme.HasadEmerald
import com.example.ui.viewmodel.AmountBracketFilter
import com.example.ui.viewmodel.DateFilterPeriod
import com.example.ui.viewmodel.FilterState

@Composable
fun FilterBar(
    filterState: FilterState,
    onSearchChange: (String) -> Unit,
    onTypeChange: (TransactionType?) -> Unit,
    onMethodChange: (PaymentMethod?) -> Unit,
    onAmountBracketChange: (AmountBracketFilter) -> Unit,
    onCustomAmountChange: (Double?, Double?) -> Unit,
    onDatePeriodChange: (DateFilterPeriod) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomAmountInputs by remember { mutableStateOf(false) }
    var minAmountText by remember { mutableStateOf(filterState.customMinAmount?.toString() ?: "") }
    var maxAmountText by remember { mutableStateOf(filterState.customMaxAmount?.toString() ?: "") }

    val hasActiveFilters = filterState.searchQuery.isNotBlank() ||
            filterState.typeFilter != null ||
            filterState.paymentMethodFilter != null ||
            filterState.amountBracket != AmountBracketFilter.ALL ||
            filterState.datePeriod != DateFilterPeriod.ALL

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_field"),
            placeholder = { Text("بحث في السجل بالبيان أو التصنيف أو الملاحظات...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (filterState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "مسح",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HasadEmerald,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // Reset Filter row if active
        if (hasActiveFilters) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = HasadEmerald
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "فلاتر نشطة مُطبقة",
                        fontSize = 11.sp,
                        color = HasadEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = {
                        showCustomAmountInputs = false
                        minAmountText = ""
                        maxAmountText = ""
                        onResetFilters()
                    },
                    modifier = Modifier.testTag("reset_filters_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "إعادة ضبط الفلاتر",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Horizontal filter chips - Row 1: Types & Date periods
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Type Chips
            FilterChip(
                selected = filterState.typeFilter == null,
                onClick = { onTypeChange(null) },
                label = { Text("الكل", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HasadEmerald, selectedLabelColor = Color.White)
            )

            FilterChip(
                selected = filterState.typeFilter == TransactionType.EXPENSE,
                onClick = { onTypeChange(TransactionType.EXPENSE) },
                label = { Text("مصروفات", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.error, selectedLabelColor = Color.White)
            )

            FilterChip(
                selected = filterState.typeFilter == TransactionType.INCOME,
                onClick = { onTypeChange(TransactionType.INCOME) },
                label = { Text("مدخولات", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HasadEmerald, selectedLabelColor = Color.White)
            )

            FilterChip(
                selected = filterState.typeFilter == TransactionType.TRANSFER,
                onClick = { onTypeChange(TransactionType.TRANSFER) },
                label = { Text("تحويلات", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
            )

            // Date period divider & chips
            Spacer(modifier = Modifier.width(4.dp))
            DateFilterPeriod.entries.filter { it != DateFilterPeriod.ALL }.forEach { period ->
                FilterChip(
                    selected = filterState.datePeriod == period,
                    onClick = {
                        onDatePeriodChange(if (filterState.datePeriod == period) DateFilterPeriod.ALL else period)
                    },
                    label = { Text(period.labelArabic, fontSize = 11.sp) }
                )
            }
        }

        // Horizontal filter chips - Row 2: Amount Brackets & Payment Methods
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "المبلغ:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            AmountBracketFilter.entries.forEach { bracket ->
                FilterChip(
                    selected = filterState.amountBracket == bracket,
                    onClick = {
                        if (bracket == AmountBracketFilter.CUSTOM) {
                            showCustomAmountInputs = !showCustomAmountInputs
                            onAmountBracketChange(AmountBracketFilter.CUSTOM)
                        } else {
                            showCustomAmountInputs = false
                            onAmountBracketChange(bracket)
                        }
                    },
                    label = { Text(bracket.labelArabic, fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_bracket_${bracket.name}")
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "الوسيلة:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            PaymentMethod.entries.forEach { method ->
                FilterChip(
                    selected = filterState.paymentMethodFilter == method,
                    onClick = {
                        onMethodChange(if (filterState.paymentMethodFilter == method) null else method)
                    },
                    label = { Text(method.shortLabel, fontSize = 11.sp) }
                )
            }
        }

        // Animated custom amount inputs
        AnimatedVisibility(visible = showCustomAmountInputs || filterState.amountBracket == AmountBracketFilter.CUSTOM) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = minAmountText,
                        onValueChange = {
                            minAmountText = it
                            val min = it.toDoubleOrNull()
                            val max = maxAmountText.toDoubleOrNull()
                            onCustomAmountChange(min, max)
                        },
                        label = { Text("الحد الأدنى (ج.م)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_min_amount_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = {
                            maxAmountText = it
                            val min = minAmountText.toDoubleOrNull()
                            val max = it.toDoubleOrNull()
                            onCustomAmountChange(min, max)
                        },
                        label = { Text("الحد الأقصى (ج.م)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_max_amount_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}
