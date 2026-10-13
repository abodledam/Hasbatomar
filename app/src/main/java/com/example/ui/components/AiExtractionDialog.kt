package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.AiExtractionResult
import com.example.ui.theme.HasadEmerald
import com.example.ui.theme.HasadGold
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AiExtractionDialog(
    isLoading: Boolean,
    aiResult: AiExtractionResult?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onExtract: (promptText: String, imageUri: Uri?) -> Unit,
    onApplyToForm: (receiptUri: Uri?) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val samplePrompts = listOf(
        "دفعت 350 جنيه كاش فاتورة كهرباء عن شهر سبتمبر",
        "استلمت 12500 تحويل بنكي راتب شهر أكتوبر",
        "دفعت 1450 جنيه بمحفظة فودافون كاش سوبرماركت",
        "حولت 2000 جنيه من الحساب البنكي إلى الكاش"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HasadEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = HasadGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "مساعد استخراج العمليات (Gemini AI)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "نموذج gemini-3.5-flash لاستخراج المبالغ والبيانات",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "أدخل وصف العملية بحرية أو ارفع صورة الفاتورة/الإيصال للاستخراج الفوري:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick sample prompt chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    samplePrompts.forEach { sample ->
                        FilterChip(
                            selected = promptInput == sample,
                            onClick = { promptInput = sample },
                            label = { Text(sample.take(28) + "..", fontSize = 10.sp) }
                        )
                    }
                }

                // Text input
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("مثال: دفعت 350 جنيه كاش فاتورة كهرباء عن شهر سبتمبر", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input"),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                // Image Attachment section (Zero-permission Photo Picker)
                if (selectedImageUri != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "صورة الإيصال المحددة",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تم إرفاق صورة الإيصال",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "سيقوم AI بقراءة المبلغ والتفاصيل منها",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { selectedImageUri = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إزالة الصورة",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
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
                            .testTag("pick_receipt_ai_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = HasadEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفع أو تصوير صورة الإيصال / الفاتورة", fontSize = 12.sp)
                    }
                }

                // Extract Button
                Button(
                    onClick = {
                        onExtract(promptInput, selectedImageUri)
                    },
                    enabled = !isLoading && (promptInput.isNotBlank() || selectedImageUri != null),
                    colors = ButtonDefaults.buttonColors(containerColor = HasadEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("run_ai_extraction_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري استخراج البيانات بذكاء...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = HasadGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بدء الاستخراج الذكي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Error indicator
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                // AI Result Preview
                AnimatedVisibility(visible = aiResult != null) {
                    if (aiResult != null) {
                        val currencyFormat = NumberFormat.getNumberInstance(Locale.getDefault())
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HasadEmerald.copy(alpha = 0.08f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = HasadEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "البيانات المستخرجة بنجاح:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HasadEmerald
                                    )
                                }

                                Text(
                                    text = "• البيان: ${aiResult.title}",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "• المبلغ: ${currencyFormat.format(aiResult.amount)} ج.م",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• النوع: ${aiResult.type.labelArabic} | التصنيف: ${aiResult.category}",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "• وسيلة الدفع: ${aiResult.paymentMethod.labelArabic}" +
                                            if (aiResult.targetPaymentMethod != null) " ← ${aiResult.targetPaymentMethod.labelArabic}" else "",
                                    fontSize = 12.sp
                                )
                                if (aiResult.notes.isNotBlank()) {
                                    Text(
                                        text = "• ملاحظات: ${aiResult.notes}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (aiResult != null) {
                Button(
                    onClick = { onApplyToForm(selectedImageUri) },
                    colors = ButtonDefaults.buttonColors(containerColor = HasadEmerald),
                    modifier = Modifier.testTag("apply_ai_to_form_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تطبيق وتعبئة النموذج")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
