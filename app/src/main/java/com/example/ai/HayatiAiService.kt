package com.example.ai

import android.util.Log
import com.example.data.model.*
import com.example.data.repository.HayatiRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "HayatiAiService"

class HayatiAiService(private val repository: HayatiRepository) {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.3f
            },
            systemInstruction = content {
                text(
                    """
                    أنت المساعد الذكي لتطبيق «حياتي AI» (Hayati AI).
                    مهمتك مساعدة المستخدم في تنظيم حياته اليومية (العمل، المنزل والعائلة، الإدارة المالية، وتعليم الأبناء).
                    تتحدث باللغة العربية بأسلوب راقٍ وودود، وتفهم تماماً اللهجة المصرية العامية والعربية الفصحى.

                    قواعد صارمة جداً:
                    1. لا تخترع أبداً أرقاماً مالية أو معاملات أو مواعيد غير موجودة في البيانات المزودة إليك.
                    2. عند السؤال عن المصروفات أو الدخل أو المهام، استخدم الأرقام الحقيقية المذكورة في سياق التطبيق المرفق أدناه بدقة.
                    3. إذا طلب المستخدم تسجيل عملية مالية (مصروف أو دخل) أو مهمة أو فاتورة أو امتحان:
                       - استخرج: نوع الإجراء، المبلغ، التصنيف، التاريخ، الوصف، اسم الابن إن وجد.
                       - إذا كانت هناك بيانات أساسية ناقصة (مثل المبلغ في المصروف)، اسأل المستخدم عنها بلطف ولا تقم باختلاقها.
                    4. يجب دائماً في ردك توفير توضيح باللغة العربية وما إذا كان هناك إجراء يحتاج لتأكيد وحفظ.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun processMessage(userPrompt: String): ChatMessage {
        val today = repository.todayDateString
        val totalIncome = repository.calculateTotalIncome().toInt()
        val totalExpense = repository.calculateTotalExpenses().toInt()
        val balance = repository.calculateBalance().toInt()
        val categoryExpenses = repository.calculateExpensesByCategory()
        val overdueTasks = repository.tasks.value.filter { !it.isCompleted && it.dueDate < today }
        val children = repository.children.value.map { "${it.name} (${it.grade})" }
        val pendingBills = repository.bills.value.filter { !it.isPaid }.map { "${it.name}: ${it.amount.toInt()} ج.م (استحقاق ${it.dueDate})" }

        val contextInfo = """
            [بيانات التطبيق الفعلية المسجلة حالياً]
            - تاريخ اليوم: $today (توقيت القاهرة)
            - إجمالي الدخل: $totalIncome ج.م
            - إجمالي المصروفات: $totalExpense ج.م
            - الرصيد المالي المتبقي: $balance ج.م
            - تفصيل المصروفات حسب التصنيف: ${categoryExpenses.map { "${it.key}: ${it.value.toInt()} ج.م" }}
            - عدد المهام المتأخرة: ${overdueTasks.size} ${if (overdueTasks.isNotEmpty()) "(أهمها: ${overdueTasks.joinToString { it.title }})" else "(لا توجد مهام متأخرة)"}
            - الأبناء: $children
            - الفواتير المستحقة غير المدفوعة: $pendingBills
        """.trimIndent()

        val promptWithContext = """
            $contextInfo

            رسالة المستخدم: "$userPrompt"

            قم بتحليل الرسالة والرد بأسلوب طبيعي ومفيد.
            إذا كان الطلب يتطلب إضافة سجل جديد (مصروف، مهمة، فاتورة، امتحان)، حدد ذلك بوضوح في نهاية ردك بتنسيق JSON التالي داخل كود وسم <ACTION_JSON> و </ACTION_JSON>:
            {
              "type": "ADD_EXPENSE" | "ADD_TASK" | "ADD_BILL" | "ADD_EXAM" | "PLAN_DAY",
              "title": "عنوان الإجراء",
              "amount": "المبلغ إن وجد",
              "category": "التصنيف",
              "description": "الوصف",
              "date": "التاريخ بصيغة YYYY-MM-DD",
              "priority": "HIGH" | "MEDIUM" | "LOW",
              "child": "اسم الابن إن وجد"
            }
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(promptWithContext)
            }

            val text = response.text
            if (text.isNullOrBlank()) {
                ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "عذراً، لم أتمكن من الحصول على رد واضح من نموذج Gemini. يرجى المحاولة مرة أخرى."
                )
            } else {
                parseResponseWithAction(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini generation timed out", e)
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "استغرقت معالجة الطلب عبر Gemini وقتاً أطول من المتوقع (انتهت المهلة بعد 45 ثانية). يرجى المحاولة مرة أخرى."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed", e)
            val errorMsg = e.localizedMessage ?: "خطأ في الاتصال بنموذج الذكاء الاصطناعي"
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "تعذر الاتصال بخدمة Gemini AI: $errorMsg\n(تأكد من تفعيل الاتصال بالإنترنت وتسجيل App Check)"
            )
        }
    }

    private fun parseResponseWithAction(rawText: String): ChatMessage {
        var cleanText = rawText
        var extractedAction: AssistantAction? = null

        val startTag = "<ACTION_JSON>"
        val endTag = "</ACTION_JSON>"

        val startIndex = rawText.indexOf(startTag)
        val endIndex = rawText.indexOf(endTag)

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            val jsonPart = rawText.substring(startIndex + startTag.length, endIndex).trim()
            cleanText = (rawText.substring(0, startIndex) + rawText.substring(endIndex + endTag.length)).trim()

            try {
                val json = JSONObject(jsonPart)
                val type = json.optString("type")
                val title = json.optString("title", "تأكيد الإجراء")
                val details = mutableMapOf<String, String>()

                if (json.has("amount")) details["amount"] = json.getString("amount")
                if (json.has("category")) details["category"] = json.getString("category")
                if (json.has("description")) details["description"] = json.getString("description")
                if (json.has("date")) details["date"] = json.getString("date")
                if (json.has("priority")) details["priority"] = json.getString("priority")
                if (json.has("child")) details["child"] = json.getString("child")
                if (json.has("name")) details["name"] = json.getString("name")

                extractedAction = AssistantAction(
                    actionType = type,
                    title = title,
                    details = details
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse action json: $jsonPart", e)
            }
        }

        return ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = cleanText,
            pendingAction = extractedAction
        )
    }
}
