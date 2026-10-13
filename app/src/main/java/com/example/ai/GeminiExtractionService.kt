package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CategoryDefaults
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class GeminiExtractionService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun extractTransactionDetails(
        promptText: String,
        bitmap: Bitmap? = null
    ): Result<AiExtractionResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent local fallback if API key is not configured in Secrets panel
            val fallback = extractHeuristically(promptText)
            return@withContext Result.success(fallback)
        }

        try {
            val systemInstruction = """
                أنت مساعد محاسبي ذكي لتطبيق "حصاد". مهمتك استخراج تفاصيل العملية المالية من النص العربي أو صورة الإيصال وإرجاع النتيجة بتنسيق JSON حصراً بدون أي نصوص إضافية أو علامات markdown.
                الحقول المطلوبة في JSON:
                {
                  "amount": عدد عشري (المبلغ),
                  "type": "EXPENSE" أو "INCOME" أو "TRANSFER",
                  "category": اسم التصنيف (مثال: فواتير وخدمات، طعام ومشتريات، مواصلات ووقود، إيجار وسكن، تسوق وملابس، صحة وعلاج، راتب شهري، مبيعات وأرباح، تحويل بين الحسابات، أخرى),
                  "paymentMethod": "CASH" أو "BANK" أو "WALLET",
                  "targetPaymentMethod": "CASH" أو "BANK" أو "WALLET" في حال كان تحويل وإلا null,
                  "title": عنوان أو بيان واضح ومختصر للعملية,
                  "notes": ملاحظات توضيحية مستخرجة,
                  "date": تاريخ العملية بتنسيق yyyy-MM-dd إن وجد وإلا فارغ
                }
            """.trimIndent()

            val partsArray = JSONArray()

            val promptBuilder = StringBuilder()
            promptBuilder.append("قم بتحليل واستخراج تفاصيل العملية المالية التالية وإرجاع JSON فقط:\n")
            if (promptText.isNotBlank()) {
                promptBuilder.append("النص: $promptText\n")
            } else if (bitmap != null) {
                promptBuilder.append("يرجى قراءة واستخراج البيانات من صورة الفاتورة/الإيصال المرفقة.\n")
            }

            partsArray.put(JSONObject().put("text", promptBuilder.toString()))

            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                val inlineData = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", base64Image)

                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            val contentsArray = JSONArray()
                .put(JSONObject().put("parts", partsArray))

            val systemContent = JSONObject().put(
                "parts",
                JSONArray().put(JSONObject().put("text", systemInstruction))
            )

            val generationConfig = JSONObject()
                .put("temperature", 0.1)
                .put("responseMimeType", "application/json")

            val rootJson = JSONObject()
                .put("contents", contentsArray)
                .put("systemInstruction", systemContent)
                .put("generationConfig", generationConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.w("GeminiExtraction", "API response error: ${response.code} $bodyString")
                    // If error occurs, fallback gracefully to heuristic extraction
                    val fallback = extractHeuristically(promptText)
                    return@withContext Result.success(fallback.copy(notes = "تم الاستخراج المحلي (تعذر الاتصال بخادم AI: ${response.message})"))
                }

                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val textOutput = parts?.optJSONObject(0)?.optString("text") ?: ""

                val parsed = parseJsonResponse(textOutput)
                return@withContext Result.success(parsed)
            }
        } catch (e: Exception) {
            Log.e("GeminiExtraction", "Error calling Gemini", e)
            val fallback = extractHeuristically(promptText)
            return@withContext Result.success(fallback.copy(notes = "تم الاستخراج المحلي (وضع بدون إنترنت)"))
        }
    }

    private fun parseJsonResponse(rawJson: String): AiExtractionResult {
        return try {
            // Clean up possible markdown code fences
            var cleaned = rawJson.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.removePrefix("```json")
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```")
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```")
            }
            cleaned = cleaned.trim()

            val json = JSONObject(cleaned)
            val amount = json.optDouble("amount", 0.0)
            val typeStr = json.optString("type", "EXPENSE").uppercase(Locale.ROOT)
            val type = try {
                TransactionType.valueOf(typeStr)
            } catch (e: Exception) {
                TransactionType.EXPENSE
            }

            val category = json.optString("category", "أخرى")
            val methodStr = json.optString("paymentMethod", "CASH").uppercase(Locale.ROOT)
            val paymentMethod = try {
                PaymentMethod.valueOf(methodStr)
            } catch (e: Exception) {
                PaymentMethod.CASH
            }

            val targetMethodStr = json.optString("targetPaymentMethod", "")
            val targetPaymentMethod = if (targetMethodStr.isNotBlank() && targetMethodStr != "null") {
                try {
                    PaymentMethod.valueOf(targetMethodStr.uppercase(Locale.ROOT))
                } catch (e: Exception) {
                    null
                }
            } else null

            val title = json.optString("title", "عملية ذكية")
            val notes = json.optString("notes", "تم استخراجها بواسطة Gemini AI")
            val dateStr = json.optString("date", "")

            var dateMillis = System.currentTimeMillis()
            if (dateStr.isNotBlank()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val parsedDate = sdf.parse(dateStr)
                    if (parsedDate != null) {
                        dateMillis = parsedDate.time
                    }
                } catch (e: Exception) {
                    // Ignore date parsing failure
                }
            }

            AiExtractionResult(
                amount = amount,
                type = type,
                category = category,
                paymentMethod = paymentMethod,
                targetPaymentMethod = targetPaymentMethod,
                title = title,
                notes = notes,
                dateMillis = dateMillis,
                rawExplanation = cleaned
            )
        } catch (e: Exception) {
            extractHeuristically(rawJson)
        }
    }

    private fun extractHeuristically(text: String): AiExtractionResult {
        var amount = 0.0
        val matcher = Pattern.compile("(\\d+(\\.\\d+)?)").matcher(text)
        if (matcher.find()) {
            amount = matcher.group(1)?.toDoubleOrNull() ?: 0.0
        }

        val type = when {
            text.contains("راتب") || text.contains("قبضت") || text.contains("ربح") || text.contains("دخل") || text.contains("استلمت") -> TransactionType.INCOME
            text.contains("تحويل") || text.contains("حولت") || text.contains("سحب") || text.contains("إيداع") -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }

        val paymentMethod = when {
            text.contains("بنك") || text.contains("حساب بنكي") || text.contains("فيزا") || text.contains("كارت") -> PaymentMethod.BANK
            text.contains("محفظة") || text.contains("كاش") && (text.contains("فودافون") || text.contains("اورنج") || text.contains("اتصالات") || text.contains("إنستاباي") || text.contains("انستاباي")) -> PaymentMethod.WALLET
            text.contains("محفظه") || text.contains("انستاباي") || text.contains("إنستاباي") -> PaymentMethod.WALLET
            else -> PaymentMethod.CASH
        }

        val targetMethod = if (type == TransactionType.TRANSFER) {
            if (paymentMethod == PaymentMethod.BANK) PaymentMethod.CASH else PaymentMethod.BANK
        } else null

        val category = when {
            text.contains("كهرباء") || text.contains("مياه") || text.contains("غاز") || text.contains("نت") || text.contains("فاتورة") -> "فواتير وخدمات"
            text.contains("أكل") || text.contains("طعام") || text.contains("سوبرماركت") || text.contains("غداء") || text.contains("مطعم") -> "طعام ومشتريات"
            text.contains("بنزين") || text.contains("مواصلات") || text.contains("اوبر") || text.contains("تاكسي") -> "مواصلات ووقود"
            text.contains("إيجار") || text.contains("ايجار") || text.contains("سكن") -> "إيجار وسكن"
            text.contains("دواء") || text.contains("صيدلية") || text.contains("دكتور") || text.contains("عيادة") -> "صحة وعلاج"
            text.contains("راتب") || text.contains("مرتب") -> "راتب شهري"
            text.contains("مبيعات") || text.contains("أرباح") -> "مبيعات وأرباح"
            text.contains("شغل") || text.contains("فريلانس") -> "عمل حر (فريلانس)"
            else -> if (type == TransactionType.INCOME) CategoryDefaults.incomeCategories.first() else CategoryDefaults.expenseCategories.first()
        }

        val title = if (text.isNotBlank()) {
            val words = text.trim().split("\\s+".toRegex()).take(6).joinToString(" ")
            words
        } else "عملية مستخرجة بالذكاء الاصطناعي"

        return AiExtractionResult(
            amount = amount,
            type = type,
            category = category,
            paymentMethod = paymentMethod,
            targetPaymentMethod = targetMethod,
            title = title,
            notes = "تم الاستخراج التلقائي بنجاح",
            dateMillis = System.currentTimeMillis()
        )
    }
}
