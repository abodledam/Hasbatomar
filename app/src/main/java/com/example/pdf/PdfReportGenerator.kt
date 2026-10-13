package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateAndShareReport(
        context: Context,
        transactions: List<TransactionEntity>,
        totalIncome: Double,
        totalExpense: Double,
        netBalance: Double,
        cashBalance: Double,
        bankBalance: Double,
        walletBalance: Double,
        filterDescription: String = "كافة العمليات المسجلة"
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            isFakeBoldText = true
            color = Color.rgb(27, 77, 62) // Hasad Emerald
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
        }

        val headerBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(228, 243, 237)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = Color.BLACK
            textAlign = Paint.Align.RIGHT
        }

        val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            isFakeBoldText = true
            color = Color.BLACK
            textAlign = Paint.Align.RIGHT
        }

        val currencyFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val txDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        // Header Background
        canvas.drawRoundRect(RectF(20f, 20f, 575f, 95f), 12f, 12f, headerBoxPaint)

        // Title and Subtitle
        canvas.drawText("حصاد (Hasad) — كشف الحساب والتقرير المالي", 297f, 48f, titlePaint)
        canvas.drawText(
            "تاريخ الإصدار: ${dateFormat.format(Date())} | النطاق: $filterDescription",
            297f,
            68f,
            subtitlePaint
        )
        canvas.drawText(
            "عدد العمليات: ${transactions.size} عملية | تقرير محاسبي صادر محلياً 100%",
            297f,
            84f,
            subtitlePaint
        )

        // Summary Cards Section
        var currentY = 115f
        val summaryBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 3 Cards: Income, Expense, Net Balance
        // Card 1: Income
        summaryBoxPaint.color = Color.rgb(232, 245, 233)
        canvas.drawRoundRect(RectF(405f, currentY, 575f, currentY + 50f), 8f, 8f, summaryBoxPaint)
        paint.color = Color.rgb(46, 125, 50)
        paint.textSize = 10f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("إجمالي الدخل", 490f, currentY + 18f, paint)
        paint.textSize = 12f
        canvas.drawText("+${currencyFormat.format(totalIncome)} ج.م", 490f, currentY + 38f, paint)

        // Card 2: Expense
        summaryBoxPaint.color = Color.rgb(255, 235, 238)
        canvas.drawRoundRect(RectF(215f, currentY, 385f, currentY + 50f), 8f, 8f, summaryBoxPaint)
        paint.color = Color.rgb(211, 47, 47)
        paint.textSize = 10f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("إجمالي المصروفات", 300f, currentY + 18f, paint)
        paint.textSize = 12f
        canvas.drawText("-${currencyFormat.format(totalExpense)} ج.م", 300f, currentY + 38f, paint)

        // Card 3: Net Balance
        summaryBoxPaint.color = Color.rgb(227, 242, 253)
        canvas.drawRoundRect(RectF(25f, currentY, 195f, currentY + 50f), 8f, 8f, summaryBoxPaint)
        paint.color = if (netBalance >= 0) Color.rgb(27, 77, 62) else Color.rgb(211, 47, 47)
        paint.textSize = 10f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("صافي الرصيد", 110f, currentY + 18f, paint)
        paint.textSize = 12f
        canvas.drawText("${currencyFormat.format(netBalance)} ج.م", 110f, currentY + 38f, paint)

        // Payment Method breakdown sub-row
        currentY += 60f
        val channelBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 247, 245)
        }
        canvas.drawRoundRect(RectF(25f, currentY, 575f, currentY + 28f), 6f, 6f, channelBoxPaint)
        val channelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = Color.rgb(40, 50, 45)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "أرصدة القنوات:  كاش: ${currencyFormat.format(cashBalance)} ج.م   |   حساب بنكي: ${currencyFormat.format(bankBalance)} ج.م   |   محفظة: ${currencyFormat.format(walletBalance)} ج.م",
            300f,
            currentY + 18f,
            channelTextPaint
        )

        // Transactions Table Header
        currentY += 40f
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(27, 77, 62)
        }
        canvas.drawRect(25f, currentY, 575f, currentY + 22f, tableHeaderPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = Color.WHITE
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        // Columns RTL:
        // المبلغ (45) | وسيلة الدفع (110) | التصنيف (195) | النوع (270) | البيان (410) | التاريخ (510) | إيصال (560)
        canvas.drawText("إيصال", 565f, currentY + 15f, headerTextPaint)
        canvas.drawText("التاريخ", 520f, currentY + 15f, headerTextPaint)
        canvas.drawText("البيان والوصف", 430f, currentY + 15f, headerTextPaint)
        canvas.drawText("النوع", 290f, currentY + 15f, headerTextPaint)
        canvas.drawText("التصنيف", 225f, currentY + 15f, headerTextPaint)
        canvas.drawText("وسيلة الدفع", 145f, currentY + 15f, headerTextPaint)
        canvas.drawText("المبلغ", 65f, currentY + 15f, headerTextPaint)

        currentY += 24f

        val rowBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(230, 230, 230)
            strokeWidth = 0.5f
        }

        // Max rows on single page to prevent overflow (or up to ~22 rows)
        val displayTx = transactions.take(24)

        displayTx.forEachIndexed { index, tx ->
            rowBgPaint.color = if (index % 2 == 0) Color.WHITE else Color.rgb(250, 252, 250)
            canvas.drawRect(25f, currentY, 575f, currentY + 20f, rowBgPaint)
            canvas.drawLine(25f, currentY + 20f, 575f, currentY + 20f, linePaint)

            // Has receipt icon indicator
            val receiptIndicator = if (tx.receiptImagePath != null) "✓ نعم" else "—"
            textPaint.color = if (tx.receiptImagePath != null) Color.rgb(27, 77, 62) else Color.GRAY
            canvas.drawText(receiptIndicator, 565f, currentY + 14f, textPaint)

            // Date
            textPaint.color = Color.DKGRAY
            canvas.drawText(txDateFormat.format(Date(tx.dateMillis)), 520f, currentY + 14f, textPaint)

            // Title (truncated)
            textPaint.color = Color.BLACK
            val titleClean = if (tx.title.length > 20) tx.title.take(18) + ".." else tx.title
            canvas.drawText(titleClean, 430f, currentY + 14f, textPaint)

            // Type
            val (typeText, typeColor) = when (tx.transactionType) {
                TransactionType.INCOME -> "دخل" to Color.rgb(46, 125, 50)
                TransactionType.EXPENSE -> "مصروف" to Color.rgb(211, 47, 47)
                TransactionType.TRANSFER -> "تحويل" to Color.rgb(25, 118, 210)
            }
            boldTextPaint.color = typeColor
            canvas.drawText(typeText, 290f, currentY + 14f, boldTextPaint)

            // Category
            textPaint.color = Color.rgb(60, 60, 60)
            val catClean = if (tx.category.length > 12) tx.category.take(10) + ".." else tx.category
            canvas.drawText(catClean, 225f, currentY + 14f, textPaint)

            // Payment method
            val methodText = when (tx.method) {
                PaymentMethod.CASH -> "كاش"
                PaymentMethod.BANK -> "بنك"
                PaymentMethod.WALLET -> "محفظة"
            }
            textPaint.color = Color.DKGRAY
            canvas.drawText(methodText, 145f, currentY + 14f, textPaint)

            // Amount
            val amountFormatted = currencyFormat.format(tx.amount) + " ج.م"
            boldTextPaint.color = typeColor
            canvas.drawText(amountFormatted, 65f, currentY + 14f, boldTextPaint)

            currentY += 21f
        }

        if (transactions.size > displayTx.size) {
            val moreTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                color = Color.GRAY
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "... ويوجد ${transactions.size - displayTx.size} عملية إضافية في السجل",
                300f,
                currentY + 16f,
                moreTextPaint
            )
            currentY += 20f
        }

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            color = Color.GRAY
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "تم استخراج هذا التقرير المحاسبي الذكي عبر تطبيق حصاد (Hasad) — يعمل محلياً بالكامل بدون إنترنت",
            297f,
            820f,
            footerPaint
        )

        pdfDocument.finishPage(page)

        // Save PDF file
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val file = File(reportsDir, "Hasad_Report_${System.currentTimeMillis()}.pdf")

        try {
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        } finally {
            pdfDocument.close()
        }

        return file
    }

    fun sharePdfFile(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "تقرير حسابات حصاد (Hasad)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "مشاركة أو فتح تقرير حصاد PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
