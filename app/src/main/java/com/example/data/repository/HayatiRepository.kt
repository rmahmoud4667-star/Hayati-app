package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

enum class StorageEngine(val labelAr: String, val labelEn: String) {
    LOCAL_PERSISTENT("تخزين محلي دائم ومحفوظ على هذا الجهاز", "Local Persistent Storage"),
    CLOUD_FIRESTORE("سحابة Google Cloud Firestore المأمنة", "Cloud Firestore")
}

data class StorageStatus(
    val engine: StorageEngine = StorageEngine.LOCAL_PERSISTENT,
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "",
    val userEmail: String? = null,
    val userId: String? = null,
    val lastError: String? = null,
    val cloudDatabaseId: String = ""
)

class HayatiRepository(private val context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences("hayati_local_storage", Context.MODE_PRIVATE)

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val todayDateString: String = dateFormatter.format(Date())

    // ----------------------------------------------------
    // Initial Seed Data (Realistic Egyptian Family context)
    // ----------------------------------------------------
    private val defaultTasks = listOf(
        WorkTask(
            id = "w1",
            title = "تسليم التقرير المالي للربع الرابع",
            description = "مراجعة جداول الإيرادات والمصروفات مع المحاسب القانوني",
            priority = Priority.HIGH,
            dueDate = todayDateString,
            category = "مشاريع",
            status = TaskStatus.IN_PROGRESS,
            isCompleted = false
        ),
        WorkTask(
            id = "w2",
            title = "اجتماع فريق التطوير والمنتج",
            description = "مناقشة إطلاق النسخة الجديدة ومراجعة ملاحظات العملاء",
            priority = Priority.HIGH,
            dueDate = todayDateString,
            category = "اجتماعات",
            status = TaskStatus.PENDING,
            isCompleted = false
        ),
        WorkTask(
            id = "w3",
            title = "إرسال عرض الأسعار لشركة الأهرام",
            description = "تجهيز العقد المبدئي وحساب تكاليف التوريد",
            priority = Priority.MEDIUM,
            dueDate = todayDateString,
            category = "مبيعات",
            status = TaskStatus.PENDING,
            isCompleted = false
        ),
        WorkTask(
            id = "w4",
            title = "تجديد اشتراك السيرفرات السنوي",
            description = "متابعة قسم المشتريات لدفع فواتير AWS و Google Cloud",
            priority = Priority.LOW,
            dueDate = "2026-11-20",
            category = "إدارة",
            status = TaskStatus.PENDING,
            isCompleted = false
        ),
        WorkTask(
            id = "w5",
            title = "مراجعة كشوف الرواتب الشهرية",
            description = "اعتماد مكافآت الأداء والخصومات لقسم المبيعات",
            priority = Priority.MEDIUM,
            dueDate = "2026-11-09", // Overdue task
            category = "إدارة",
            status = TaskStatus.PENDING,
            isCompleted = false
        )
    )

    private val defaultMeetings = listOf(
        WorkMeeting(
            id = "m1",
            title = "اجتماع خطة الأسبوع وتوزيع المهام",
            date = todayDateString,
            time = "10:30 ص",
            attendees = "م. كريم، أ. سارة، د. تامر",
            location = "قاعة الاجتماعات 2 / Google Meet",
            notes = "التركيز على سرعة الإنجاز وضمان جودة الكود",
            actionItems = listOf("تحديث لوحة المهام", "إرسال ملخص الاجتماع بالبريد")
        ),
        WorkMeeting(
            id = "m2",
            title = "مكالمة مع العميل لمناقشة التصاميم",
            date = todayDateString,
            time = "02:00 م",
            attendees = "أ. محمود فوزي",
            location = "مكالمة هاتفية",
            notes = "مراجعة ألوان الهوية والشعار الجديد",
            actionItems = listOf("تعديل درجة لون التركواز", "إرسال النموذج النهائي")
        )
    )

    private val defaultFamilyMembers = listOf(
        FamilyMember(id = "f1", name = "محمود رضا", role = "الأب", avatarEmoji = "👨‍💼", notes = "رب الأسرة"),
        FamilyMember(id = "f2", name = "هاجر", role = "الأم", avatarEmoji = "👩‍🏫", notes = "ربة المنزل"),
        FamilyMember(id = "f3", name = "كادي", role = "الابنة", avatarEmoji = "👧", birthDate = "2016-05-14", notes = "المرحلة الابتدائية"),
        FamilyMember(id = "f4", name = "هنا", role = "الابنة الصغرى", avatarEmoji = "👧", birthDate = "2019-10-18", notes = "المرحلة التمهيدية")
    )

    private val defaultHouseholdTasks = listOf(
        HouseholdTask(id = "ht1", title = "شراء مستلزمات البقالة والخضار للأسبوع", assignedToName = "محمود رضا", dueDate = todayDateString, isCompleted = false),
        HouseholdTask(id = "ht2", title = "تغيير شمعات فلتر المياه المنزلي", assignedToName = "محمود رضا", dueDate = todayDateString, isCompleted = false),
        HouseholdTask(id = "ht3", title = "متابعة مذاكرة الرياضيات مع كادي", assignedToName = "هاجر", dueDate = todayDateString, isCompleted = true)
    )

    private val defaultShoppingList = listOf(
        ShoppingItem(id = "s1", name = "حليب كامل الدسم (4 عبوات)", quantity = "4 لتر", estimatedPrice = 160.0, isPurchased = true, category = "لحوم وألبان"),
        ShoppingItem(id = "s2", name = "خضار وفاكهة أسبوعية (طماطم، خيار، موز)", quantity = "5 كجم", estimatedPrice = 180.0, isPurchased = false, category = "خضار وفواكه"),
        ShoppingItem(id = "s3", name = "أرز مصري وزيت زيتون", quantity = "عبوة", estimatedPrice = 220.0, isPurchased = false, category = "سوبرماركت"),
        ShoppingItem(id = "s4", name = "منظف أرضيات ومسحوق غسيل", quantity = "1", estimatedPrice = 190.0, isPurchased = false, category = "منظفات")
    )

    private val defaultBills = listOf(
        UtilityBill(id = "b1", name = "فاتورة الإنترنت المنزلي (WE)", billNumber = "023588910", amount = 320.0, dueDate = "2026-11-15", isPaid = false, iconName = "wifi"),
        UtilityBill(id = "b2", name = "فاتورة الكهرباء (شحن كارت)", billNumber = "884129", amount = 450.0, dueDate = "2026-11-18", isPaid = false, iconName = "electric"),
        UtilityBill(id = "b3", name = "فاتورة الغاز الطبيعي (بتروتريد)", billNumber = "109432", amount = 110.0, dueDate = "2026-11-20", isPaid = false, iconName = "gas"),
        UtilityBill(id = "b4", name = "فاتورة المياه", billNumber = "432190", amount = 95.0, dueDate = "2026-11-25", isPaid = true, paidDate = "2026-11-02", iconName = "water"),
        UtilityBill(id = "b5", name = "صيانة العمارة والأسانسير", billNumber = "شقة 6", amount = 300.0, dueDate = "2026-11-05", isPaid = true, paidDate = "2026-11-04", iconName = "building")
    )

    private val defaultMaintenance = listOf(
        HomeMaintenance(id = "hm1", title = "صيانة وتنظيف أجهزة التكييف", area = "تكييفات", lastDate = "2026-05-10", nextDate = "2026-11-25", estimatedCost = 350.0, notes = "تنظيف فلاتر وفحص الفريون"),
        HomeMaintenance(id = "hm2", title = "صيانة سخان الغاز المنزلي", area = "سباكة", lastDate = "2026-08-15", nextDate = "2026-12-01", estimatedCost = 150.0, notes = "تغيير جلدة الرداخ")
    )

    private val defaultOccasions = listOf(
        FamilyOccasion(id = "fo1", title = "عيد ميلاد نور (7 سنوات)", date = "2026-11-25", type = "عيد ميلاد", notes = "تحضير كعكة وشراء الهدية"),
        FamilyOccasion(id = "fo2", title = "ذكرى الزواج العائلي السعيد", date = "2026-12-14", type = "ذكرى زواج", notes = "حجز عشاء هادئ")
    )

    private val defaultTransactions = listOf(
        FinancialTransaction(id = "t1", type = TransactionType.INCOME, amount = 24000.0, date = "2026-11-01", category = "راتب شهري", description = "راتب شهر نوفمبر - الشركة", paymentMethod = "تحويل بنكي"),
        FinancialTransaction(id = "t2", type = TransactionType.INCOME, amount = 6500.0, date = "2026-11-06", category = "عمل حر وتجارة", description = "مستحقات استشارة تقنية لعميل", paymentMethod = "محفظة إلكترونية"),
        FinancialTransaction(id = "t3", type = TransactionType.EXPENSE, amount = 1200.0, date = "2026-11-02", category = "طعام وبقالة", description = "مشتريات كارفور وسوبرماركت الأسبوع", paymentMethod = "بطاقة بنكية"),
        FinancialTransaction(id = "t4", type = TransactionType.EXPENSE, amount = 1500.0, date = "2026-11-03", category = "تعليم ومدارس", description = "قسط باص المدرسة وملازم لغة عربية", paymentMethod = "نقدي"),
        FinancialTransaction(id = "t5", type = TransactionType.EXPENSE, amount = 300.0, date = "2026-11-04", category = "المنزل", description = "صيانة العمارة والأسانسير", paymentMethod = "نقدي"),
        FinancialTransaction(id = "t6", type = TransactionType.EXPENSE, amount = 650.0, date = "2026-11-07", category = "مواصلات وبنزين", description = "بنزين سيارة 95 وكارتة السفر", paymentMethod = "بطاقة بنكية"),
        FinancialTransaction(id = "t7", type = TransactionType.EXPENSE, amount = 420.0, date = "2026-11-09", category = "صحة وأدوية", description = "أدوية وفيتامينات من الصيدلية", paymentMethod = "نقدي"),
        FinancialTransaction(id = "t8", type = TransactionType.EXPENSE, amount = 550.0, date = todayDateString, category = "طعام وبقالة", description = "خضار ولحوم طازجة للبيت", paymentMethod = "نقدي")
    )

    private val defaultBudgets = listOf(
        CategoryBudget("طعام وبقالة", 6000.0),
        CategoryBudget("تعليم ومدارس", 5000.0),
        CategoryBudget("المنزل", 2500.0),
        CategoryBudget("مواصلات وبنزين", 2000.0),
        CategoryBudget("فواتير", 1500.0),
        CategoryBudget("صحة وأدوية", 1500.0),
        CategoryBudget("ترفيه", 1500.0),
        CategoryBudget("تسوق", 2000.0)
    )

    private val defaultInstallments = listOf(
        InstallmentDebt(id = "ins1", title = "قسط سيارة الأسرة", totalAmount = 180000.0, paidAmount = 120000.0, monthlyPayment = 4500.0, dueDayOfMonth = 10, isFinished = false, notes = "البنك الأهلي المصري"),
        InstallmentDebt(id = "ins2", title = "تقسيط أجهزة منزلية (غسالة)", totalAmount = 14000.0, paidAmount = 10500.0, monthlyPayment = 1160.0, dueDayOfMonth = 20, isFinished = false, notes = "فاليو ValU")
    )

    private val defaultChildren = listOf(
        ChildProfile(
            id = "c1",
            name = "كادي",
            grade = "الصف الرابع الابتدائي",
            school = "مدرسة النيل المصرية الدولية",
            avatarEmoji = "👧",
            subjects = listOf("رياضيات", "علوم", "لغة عربية", "لغة إنجليزية", "دراسات اجتماعية", "تكنولوجيا المعلومات")
        ),
        ChildProfile(
            id = "c2",
            name = "هنا",
            grade = "الصف الأول الابتدائي",
            school = "مدرسة النيل المصرية الدولية",
            avatarEmoji = "👧",
            subjects = listOf("لغة عربية", "حساب", "لغة إنجليزية", "تربية دينية", "أنشطة ورسم")
        )
    )

    private val defaultHomework = listOf(
        Homework(id = "hw1", childId = "c1", childName = "كادي", subject = "رياضيات", title = "حل تمارين القسمة المطولة ص 42", description = "حل المسائل من 1 إلى 8 في كشكول الواجب", dueDate = todayDateString, isCompleted = false),
        Homework(id = "hw2", childId = "c1", childName = "كادي", subject = "علوم", title = "رسم مخطط الجهاز الهضمي وكتابة وظيفة المعدة", description = "تحضير درس التكيف في الكائنات الحية", dueDate = "2026-11-13", isCompleted = false),
        Homework(id = "hw3", childId = "c2", childName = "هنا", subject = "لغة عربية", title = "كتابة حرف الصاد والضاد مع الحركات", description = "كتابة سطرين بخط جميل", dueDate = todayDateString, isCompleted = true)
    )

    private val defaultExams = listOf(
        Exam(id = "ex1", childId = "c1", childName = "كادي", subject = "علوم", title = "اختبار شهر نوفمبر - الجهاز التنفسي والهضمي", examDate = "2026-11-16", maxScore = 30, notes = "مراجعة أسئلة كتاب الوزارة"),
        Exam(id = "ex2", childId = "c1", childName = "كادي", subject = "لغة إنجليزية", title = "امتحان قصير Unit 3 & 4 Vocabulary", examDate = "2026-11-19", maxScore = 20, notes = "التركيز على تصريف الأفعال"),
        Exam(id = "ex3", childId = "c2", childName = "هنا", subject = "حساب", title = "اختبار الجمع والطرح حتى 20", examDate = "2026-11-22", maxScore = 20, notes = "تشجيع واستخدام المكعبات")
    )

    private val defaultMessages = listOf(
        ChatMessage(
            id = "msg1",
            sender = MessageSender.ASSISTANT,
            text = "أهلاً بك يا أستاذ محمود رضا في حياتي AI! أنا مساعدك الذكي لتنظيم عملك، منزلك، مصاريفك، ومتابعة دراسة كادي وهنا. يمكنك سؤالي أو إعطائي أوامر مباشرة مثل:\n• «سجل مصروف 150 جنيه خضار»\n• «فكرني بفاتورة النت يوم 15»\n• «أضف امتحان علوم لكادي الأحد»\n• «خطط لي يومي بالذكاء الاصطناعي»\nكيف يمكنني مساعدتك الآن؟"
        )
    )

    // ----------------------------------------------------
    // StateFlows
    // ----------------------------------------------------
    val _tasks = MutableStateFlow(defaultTasks)
    val tasks: StateFlow<List<WorkTask>> = _tasks.asStateFlow()

    val _meetings = MutableStateFlow(defaultMeetings)
    val meetings: StateFlow<List<WorkMeeting>> = _meetings.asStateFlow()

    val _familyMembers = MutableStateFlow(defaultFamilyMembers)
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    val _householdTasks = MutableStateFlow(defaultHouseholdTasks)
    val householdTasks: StateFlow<List<HouseholdTask>> = _householdTasks.asStateFlow()

    val _shoppingList = MutableStateFlow(defaultShoppingList)
    val shoppingList: StateFlow<List<ShoppingItem>> = _shoppingList.asStateFlow()

    val _bills = MutableStateFlow(defaultBills)
    val bills: StateFlow<List<UtilityBill>> = _bills.asStateFlow()

    val _maintenance = MutableStateFlow(defaultMaintenance)
    val maintenance: StateFlow<List<HomeMaintenance>> = _maintenance.asStateFlow()

    val _occasions = MutableStateFlow(defaultOccasions)
    val occasions: StateFlow<List<FamilyOccasion>> = _occasions.asStateFlow()

    val _transactions = MutableStateFlow(defaultTransactions)
    val transactions: StateFlow<List<FinancialTransaction>> = _transactions.asStateFlow()

    val _budgets = MutableStateFlow(defaultBudgets)
    val budgets: StateFlow<List<CategoryBudget>> = _budgets.asStateFlow()

    val _installments = MutableStateFlow(defaultInstallments)
    val installments: StateFlow<List<InstallmentDebt>> = _installments.asStateFlow()

    val _children = MutableStateFlow(defaultChildren)
    val children: StateFlow<List<ChildProfile>> = _children.asStateFlow()

    val _homework = MutableStateFlow(defaultHomework)
    val homework: StateFlow<List<Homework>> = _homework.asStateFlow()

    val _exams = MutableStateFlow(defaultExams)
    val exams: StateFlow<List<Exam>> = _exams.asStateFlow()

    val _chatMessages = MutableStateFlow(defaultMessages)
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    val _dailyPlan = MutableStateFlow<DailyPlan?>(null)
    val dailyPlan: StateFlow<DailyPlan?> = _dailyPlan.asStateFlow()

    val isArabic = MutableStateFlow(true)
    val isDarkMode = MutableStateFlow(true)
    val currencySymbol = MutableStateFlow("ج.م") // EGP

    val _storageStatus = MutableStateFlow(
        StorageStatus(
            engine = StorageEngine.LOCAL_PERSISTENT,
            cloudDatabaseId = try { context?.getString(R.string.firestore_database_id) ?: "" } catch (_: Exception) { "" }
        )
    )
    val storageStatus: StateFlow<StorageStatus> = _storageStatus.asStateFlow()

    val _lastActionMessage = MutableStateFlow<String?>(null)
    val lastActionMessage: StateFlow<String?> = _lastActionMessage.asStateFlow()

    fun clearActionMessage() {
        _lastActionMessage.value = null
    }

    private val recentSubmissions = ConcurrentHashMap<String, Long>()

    fun isDuplicateSubmission(fingerprint: String, windowMs: Long = 2000L): Boolean {
        val now = System.currentTimeMillis()
        val last = recentSubmissions[fingerprint]
        if (last != null && (now - last) < windowMs) {
            return true
        }
        recentSubmissions[fingerprint] = now
        if (recentSubmissions.size > 200) {
            val cutoff = now - 60_000L
            recentSubmissions.entries.removeIf { it.value < cutoff }
        }
        return false
    }

    // Validation Methods
    fun validateTask(task: WorkTask): String? {
        if (task.title.trim().isEmpty()) return "عنوان المهمة مطلوب"
        if (task.title.length > 200) return "عنوان المهمة طويل جداً (الحد الأقصى 200 حرف)"
        if (task.dueDate.trim().isEmpty()) return "تاريخ الاستحقاق مطلوب"
        return null
    }

    fun validateTransaction(tx: FinancialTransaction): String? {
        if (tx.amount <= 0) return "مبلغ المعاملة المالية يجب أن يكون أكبر من صفر"
        if (tx.category.trim().isEmpty()) return "يرجى تحديد تصنيف المعاملة المالية"
        if (tx.date.trim().isEmpty()) return "تاريخ المعاملة مطلوب"
        return null
    }

    fun validateBill(bill: UtilityBill): String? {
        if (bill.name.trim().isEmpty()) return "اسم الفاتورة مطلوب"
        if (bill.amount < 0) return "مبلغ الفاتورة لا يمكن أن يكون سالباً"
        if (bill.dueDate.trim().isEmpty()) return "تاريخ استحقاق الفاتورة مطلوب"
        return null
    }

    fun validateShoppingItem(item: ShoppingItem): String? {
        if (item.name.trim().isEmpty()) return "اسم الصنف مطلوب"
        if (item.estimatedPrice < 0) return "السعر التقديري لا يمكن أن يكون سالباً"
        return null
    }

    fun validateChild(child: ChildProfile): String? {
        if (child.name.trim().isEmpty()) return "اسم الابن/الابنة مطلوب"
        if (child.grade.trim().isEmpty()) return "المرحلة الدراسية مطلوبة"
        return null
    }

    fun validateHomework(hw: Homework): String? {
        if (hw.title.trim().isEmpty()) return "عنوان الواجب مطلوب"
        if (hw.subject.trim().isEmpty()) return "المادة الدراسية مطلوبة"
        if (hw.childId.trim().isEmpty()) return "يرجى اختيار الابن/الابنة"
        if (hw.dueDate.trim().isEmpty()) return "موعد تسليم الواجب مطلوب"
        return null
    }

    fun validateExam(exam: Exam): String? {
        if (exam.title.trim().isEmpty()) return "عنوان الامتحان مطلوب"
        if (exam.subject.trim().isEmpty()) return "المادة الدراسية مطلوبة"
        if (exam.childId.trim().isEmpty()) return "يرجى اختيار الابن/الابنة"
        if (exam.examDate.trim().isEmpty()) return "تاريخ الامتحان مطلوب"
        if (exam.maxScore <= 0) return "الدرجة العظمى يجب أن تكون أكبر من صفر"
        return null
    }

    fun validateHouseholdTask(task: HouseholdTask): String? {
        if (task.title.trim().isEmpty()) return "اسم المهمة المنزلية مطلوب"
        return null
    }

    fun validateOccasion(occ: FamilyOccasion): String? {
        if (occ.title.trim().isEmpty()) return "اسم المناسبة مطلوب"
        if (occ.date.trim().isEmpty()) return "تاريخ المناسبة مطلوب"
        return null
    }

    // Cloud Firestore Sync Helpers
    private val firestoreDb: FirebaseFirestore? by lazy {
        try {
            val dbId = context?.getString(R.string.firestore_database_id)
            if (!dbId.isNullOrBlank()) {
                FirebaseFirestore.getInstance(dbId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("HayatiRepo", "Firestore custom instance init deferred: ${e.message}")
            null
        }
    }

    fun getAuthenticatedUser(): com.google.firebase.auth.FirebaseUser? {
        if (context == null) return null
        return try {
            Firebase.auth.currentUser
        } catch (_: Throwable) {
            null
        }
    }

    private fun syncDocToFirestore(collectionPath: String, docId: String, data: Map<String, Any?>) {
        val user = getAuthenticatedUser() ?: return
        val db = firestoreDb ?: return
        val uid = user.uid

        val enriched = data.toMutableMap().apply {
            put("userId", uid)
            put("updatedAt", FieldValue.serverTimestamp())
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                _storageStatus.update { it.copy(isSyncing = true, lastError = null) }
                db.collection("users").document(uid).collection(collectionPath).document(docId)
                    .set(enriched, SetOptions.merge())
                    .await()
                val nowStr = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Date())
                _storageStatus.update {
                    it.copy(
                        engine = StorageEngine.CLOUD_FIRESTORE,
                        isSyncing = false,
                        lastSyncTime = nowStr,
                        userEmail = user.email,
                        userId = uid,
                        lastError = null
                    )
                }
            } catch (e: Exception) {
                Log.e("HayatiFirestore", "Sync failed for $collectionPath/$docId", e)
                _storageStatus.update {
                    it.copy(
                        isSyncing = false,
                        lastError = "فشل المزامنة مع Cloud Firestore: ${e.localizedMessage ?: "خطأ في الشبكة"}"
                    )
                }
            }
        }
    }

    private fun deleteDocFromFirestore(collectionPath: String, docId: String) {
        val user = getAuthenticatedUser() ?: return
        val db = firestoreDb ?: return
        val uid = user.uid
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(uid).collection(collectionPath).document(docId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("HayatiFirestore", "Delete failed for $collectionPath/$docId", e)
            }
        }
    }

    fun refreshStorageStatus() {
        val user = getAuthenticatedUser()
        if (user != null) {
            _storageStatus.update {
                it.copy(
                    engine = StorageEngine.CLOUD_FIRESTORE,
                    userId = user.uid,
                    userEmail = user.email,
                    cloudDatabaseId = try { context?.getString(R.string.firestore_database_id) ?: "" } catch (_: Exception) { "" }
                )
            }
        } else {
            _storageStatus.update {
                it.copy(
                    engine = StorageEngine.LOCAL_PERSISTENT,
                    userId = null,
                    userEmail = null
                )
            }
        }
    }

    init {
        loadFromStorage()
        refreshStorageStatus()
    }

    // ----------------------------------------------------
    // Local Persistence Engine (JSON + SharedPreferences)
    // ----------------------------------------------------
    private fun saveToStorage() {
        if (prefs == null) return
        try {
            val root = JSONObject()

            // Tasks
            val tasksArr = JSONArray()
            _tasks.value.forEach { t ->
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("title", t.title)
                obj.put("description", t.description)
                obj.put("priority", t.priority.name)
                obj.put("dueDate", t.dueDate)
                obj.put("category", t.category)
                obj.put("status", t.status.name)
                obj.put("isCompleted", t.isCompleted)
                obj.put("createdAt", t.createdAt)
                tasksArr.put(obj)
            }
            root.put("tasks", tasksArr)

            // Transactions
            val txArr = JSONArray()
            _transactions.value.forEach { tx ->
                val obj = JSONObject()
                obj.put("id", tx.id)
                obj.put("type", tx.type.name)
                obj.put("amount", tx.amount)
                obj.put("date", tx.date)
                obj.put("category", tx.category)
                obj.put("description", tx.description)
                obj.put("paymentMethod", tx.paymentMethod)
                txArr.put(obj)
            }
            root.put("transactions", txArr)

            // Bills
            val billsArr = JSONArray()
            _bills.value.forEach { b ->
                val obj = JSONObject()
                obj.put("id", b.id)
                obj.put("name", b.name)
                obj.put("billNumber", b.billNumber)
                obj.put("amount", b.amount)
                obj.put("dueDate", b.dueDate)
                obj.put("isPaid", b.isPaid)
                obj.put("paidDate", b.paidDate ?: "")
                obj.put("iconName", b.iconName)
                billsArr.put(obj)
            }
            root.put("bills", billsArr)

            // Shopping
            val shopArr = JSONArray()
            _shoppingList.value.forEach { s ->
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("quantity", s.quantity)
                obj.put("estimatedPrice", s.estimatedPrice)
                obj.put("isPurchased", s.isPurchased)
                obj.put("category", s.category)
                shopArr.put(obj)
            }
            root.put("shopping", shopArr)

            // Children
            val childArr = JSONArray()
            _children.value.forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("grade", c.grade)
                obj.put("school", c.school)
                obj.put("avatarEmoji", c.avatarEmoji)
                val subArr = JSONArray()
                c.subjects.forEach { subArr.put(it) }
                obj.put("subjects", subArr)
                childArr.put(obj)
            }
            root.put("children", childArr)

            // Homework
            val hwArr = JSONArray()
            _homework.value.forEach { h ->
                val obj = JSONObject()
                obj.put("id", h.id)
                obj.put("childId", h.childId)
                obj.put("childName", h.childName)
                obj.put("subject", h.subject)
                obj.put("title", h.title)
                obj.put("description", h.description)
                obj.put("dueDate", h.dueDate)
                obj.put("isCompleted", h.isCompleted)
                hwArr.put(obj)
            }
            root.put("homework", hwArr)

            // Exams
            val examArr = JSONArray()
            _exams.value.forEach { e ->
                val obj = JSONObject()
                obj.put("id", e.id)
                obj.put("childId", e.childId)
                obj.put("childName", e.childName)
                obj.put("subject", e.subject)
                obj.put("title", e.title)
                obj.put("examDate", e.examDate)
                obj.put("maxScore", e.maxScore)
                obj.put("scoreAchieved", e.scoreAchieved ?: -1)
                obj.put("notes", e.notes)
                examArr.put(obj)
            }
            root.put("exams", examArr)

            // Household Tasks
            val htArr = JSONArray()
            _householdTasks.value.forEach { ht ->
                val obj = JSONObject()
                obj.put("id", ht.id)
                obj.put("title", ht.title)
                obj.put("assignedToName", ht.assignedToName)
                obj.put("dueDate", ht.dueDate)
                obj.put("isCompleted", ht.isCompleted)
                obj.put("recurring", ht.recurring)
                htArr.put(obj)
            }
            root.put("householdTasks", htArr)

            // Family Members
            val fmArr = JSONArray()
            _familyMembers.value.forEach { fm ->
                val obj = JSONObject()
                obj.put("id", fm.id)
                obj.put("name", fm.name)
                obj.put("role", fm.role)
                obj.put("avatarEmoji", fm.avatarEmoji)
                obj.put("birthDate", fm.birthDate)
                obj.put("notes", fm.notes)
                obj.put("phone", fm.phone)
                fmArr.put(obj)
            }
            root.put("familyMembers", fmArr)

            // Occasions
            val occArr = JSONArray()
            _occasions.value.forEach { o ->
                val obj = JSONObject()
                obj.put("id", o.id)
                obj.put("title", o.title)
                obj.put("date", o.date)
                obj.put("type", o.type)
                obj.put("notes", o.notes)
                occArr.put(obj)
            }
            root.put("occasions", occArr)

            prefs.edit().putString("hayati_state_v1", root.toString()).apply()
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    private fun loadFromStorage() {
        if (prefs == null) return
        val jsonStr = prefs.getString("hayati_state_v1", null) ?: return
        try {
            val root = JSONObject(jsonStr)

            // Load Tasks
            if (root.has("tasks")) {
                val arr = root.getJSONArray("tasks")
                val list = mutableListOf<WorkTask>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        WorkTask(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            priority = try { Priority.valueOf(obj.getString("priority")) } catch (_: Exception) { Priority.MEDIUM },
                            dueDate = obj.getString("dueDate"),
                            category = obj.optString("category", "مشاريع"),
                            status = try { TaskStatus.valueOf(obj.getString("status")) } catch (_: Exception) { TaskStatus.PENDING },
                            isCompleted = obj.optBoolean("isCompleted", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) _tasks.value = list
            }

            // Load Transactions
            if (root.has("transactions")) {
                val arr = root.getJSONArray("transactions")
                val list = mutableListOf<FinancialTransaction>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        FinancialTransaction(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            type = try { TransactionType.valueOf(obj.getString("type")) } catch (_: Exception) { TransactionType.EXPENSE },
                            amount = obj.getDouble("amount"),
                            date = obj.getString("date"),
                            category = obj.getString("category"),
                            description = obj.getString("description"),
                            paymentMethod = obj.optString("paymentMethod", "نقدي")
                        )
                    )
                }
                if (list.isNotEmpty()) _transactions.value = list
            }

            // Load Bills
            if (root.has("bills")) {
                val arr = root.getJSONArray("bills")
                val list = mutableListOf<UtilityBill>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        UtilityBill(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.getString("name"),
                            billNumber = obj.optString("billNumber", ""),
                            amount = obj.getDouble("amount"),
                            dueDate = obj.getString("dueDate"),
                            isPaid = obj.optBoolean("isPaid", false),
                            paidDate = obj.optString("paidDate").takeIf { it.isNotBlank() },
                            iconName = obj.optString("iconName", "bill")
                        )
                    )
                }
                if (list.isNotEmpty()) _bills.value = list
            }

            // Load Shopping
            if (root.has("shopping")) {
                val arr = root.getJSONArray("shopping")
                val list = mutableListOf<ShoppingItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ShoppingItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.getString("name"),
                            quantity = obj.optString("quantity", "1"),
                            estimatedPrice = obj.optDouble("estimatedPrice", 0.0),
                            isPurchased = obj.optBoolean("isPurchased", false),
                            category = obj.optString("category", "سوبرماركت")
                        )
                    )
                }
                if (list.isNotEmpty()) _shoppingList.value = list
            }

            // Load Children
            if (root.has("children")) {
                val arr = root.getJSONArray("children")
                val list = mutableListOf<ChildProfile>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val subArr = obj.optJSONArray("subjects")
                    val subs = mutableListOf<String>()
                    if (subArr != null) {
                        for (s in 0 until subArr.length()) subs.add(subArr.getString(s))
                    }
                    list.add(
                        ChildProfile(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.getString("name"),
                            grade = obj.optString("grade", "ابتدائي"),
                            school = obj.optString("school", ""),
                            avatarEmoji = obj.optString("avatarEmoji", "👦"),
                            subjects = subs
                        )
                    )
                }
                if (list.isNotEmpty()) _children.value = list
            }

            // Load Homework
            if (root.has("homework")) {
                val arr = root.getJSONArray("homework")
                val list = mutableListOf<Homework>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Homework(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            childId = obj.getString("childId"),
                            childName = obj.getString("childName"),
                            subject = obj.getString("subject"),
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            dueDate = obj.getString("dueDate"),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
                if (list.isNotEmpty()) _homework.value = list
            }

            // Load Exams
            if (root.has("exams")) {
                val arr = root.getJSONArray("exams")
                val list = mutableListOf<Exam>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val ach = obj.optInt("scoreAchieved", -1)
                    list.add(
                        Exam(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            childId = obj.getString("childId"),
                            childName = obj.getString("childName"),
                            subject = obj.getString("subject"),
                            title = obj.getString("title"),
                            examDate = obj.getString("examDate"),
                            maxScore = obj.optInt("maxScore", 100),
                            scoreAchieved = if (ach >= 0) ach else null,
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                if (list.isNotEmpty()) _exams.value = list
            }

            // Load Household Tasks
            if (root.has("householdTasks")) {
                val arr = root.getJSONArray("householdTasks")
                val list = mutableListOf<HouseholdTask>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        HouseholdTask(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.getString("title"),
                            assignedToName = obj.optString("assignedToName", "الجميع"),
                            dueDate = obj.optString("dueDate", ""),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            recurring = obj.optString("recurring", "أسبوعي")
                        )
                    )
                }
                if (list.isNotEmpty()) _householdTasks.value = list
            }

            // Load Occasions
            if (root.has("occasions")) {
                val arr = root.getJSONArray("occasions")
                val list = mutableListOf<FamilyOccasion>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        FamilyOccasion(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.getString("title"),
                            date = obj.getString("date"),
                            type = obj.optString("type", "عيد ميلاد"),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                if (list.isNotEmpty()) _occasions.value = list
            }

            // Load Family Members
            if (root.has("familyMembers")) {
                val arr = root.getJSONArray("familyMembers")
                val list = mutableListOf<FamilyMember>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        FamilyMember(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.getString("name"),
                            role = obj.getString("role"),
                            avatarEmoji = obj.optString("avatarEmoji", "👤"),
                            birthDate = obj.optString("birthDate", ""),
                            notes = obj.optString("notes", ""),
                            phone = obj.optString("phone", "")
                        )
                    )
                }
                if (list.isNotEmpty()) _familyMembers.value = list
            }
        } catch (_: Exception) {
            // Keep defaults
        }
    }

    // ----------------------------------------------------
    // Actions - Work
    // ----------------------------------------------------
    fun addTask(task: WorkTask): Boolean {
        if (isDuplicateSubmission("add_task_${task.title}_${task.dueDate}")) {
            _lastActionMessage.value = "تم إرسال المهمة مسبقاً، يرجى الانتظار"
            return false
        }
        val validationError = validateTask(task)
        if (validationError != null) {
            _lastActionMessage.value = validationError
            return false
        }

        _tasks.update { listOf(task) + it }
        saveToStorage()
        syncDocToFirestore("tasks", task.id, mapOf(
            "title" to task.title,
            "description" to task.description,
            "priority" to task.priority.name,
            "dueDate" to task.dueDate,
            "category" to task.category,
            "status" to task.status.name,
            "isCompleted" to task.isCompleted,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        _lastActionMessage.value = "تمت إضافة المهمة بنجاح"
        return true
    }

    fun updateTask(task: WorkTask): Boolean {
        val validationError = validateTask(task)
        if (validationError != null) {
            _lastActionMessage.value = validationError
            return false
        }
        _tasks.update { it.map { t -> if (t.id == task.id) task else t } }
        saveToStorage()
        syncDocToFirestore("tasks", task.id, mapOf(
            "title" to task.title,
            "description" to task.description,
            "priority" to task.priority.name,
            "dueDate" to task.dueDate,
            "category" to task.category,
            "status" to task.status.name,
            "isCompleted" to task.isCompleted
        ))
        _lastActionMessage.value = "تم تحديث المهمة"
        return true
    }

    fun deleteTask(taskId: String) {
        _tasks.update { it.filter { t -> t.id != taskId } }
        saveToStorage()
        deleteDocFromFirestore("tasks", taskId)
        _lastActionMessage.value = "تم حذف المهمة"
    }

    fun toggleTaskCompletion(taskId: String) {
        var updatedTask: WorkTask? = null
        _tasks.update {
            it.map { t ->
                if (t.id == taskId) {
                    val newCompleted = !t.isCompleted
                    val modified = t.copy(
                        isCompleted = newCompleted,
                        status = if (newCompleted) TaskStatus.COMPLETED else TaskStatus.PENDING
                    )
                    updatedTask = modified
                    modified
                } else t
            }
        }
        saveToStorage()
        updatedTask?.let {
            syncDocToFirestore("tasks", it.id, mapOf(
                "isCompleted" to it.isCompleted,
                "status" to it.status.name
            ))
        }
    }

    fun addMeeting(meeting: WorkMeeting) {
        _meetings.update { listOf(meeting) + it }
        saveToStorage()
    }

    // ----------------------------------------------------
    // Actions - Family & Home
    // ----------------------------------------------------
    fun addFamilyMember(member: FamilyMember) {
        _familyMembers.update { it + member }
        saveToStorage()
        syncDocToFirestore("family_members", member.id, mapOf(
            "name" to member.name,
            "role" to member.role,
            "avatarEmoji" to member.avatarEmoji,
            "birthDate" to member.birthDate,
            "notes" to member.notes,
            "phone" to member.phone,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        _lastActionMessage.value = "تمت إضافة فرد الأسرة"
    }

    fun updateFamilyMember(member: FamilyMember) {
        _familyMembers.update { it.map { m -> if (m.id == member.id) member else m } }
        saveToStorage()
        syncDocToFirestore("family_members", member.id, mapOf(
            "name" to member.name,
            "role" to member.role,
            "avatarEmoji" to member.avatarEmoji,
            "birthDate" to member.birthDate,
            "notes" to member.notes,
            "phone" to member.phone
        ))
        _lastActionMessage.value = "تم تحديث بيانات ${member.name}"
    }

    fun deleteFamilyMember(memberId: String) {
        _familyMembers.update { it.filter { m -> m.id != memberId } }
        saveToStorage()
        deleteDocFromFirestore("family_members", memberId)
        _lastActionMessage.value = "تم حذف فرد الأسرة"
    }

    fun addHouseholdTask(task: HouseholdTask): Boolean {
        if (isDuplicateSubmission("add_household_${task.title}")) return false
        val err = validateHouseholdTask(task)
        if (err != null) {
            _lastActionMessage.value = err
            return false
        }
        _householdTasks.update { listOf(task) + it }
        saveToStorage()
        syncDocToFirestore("household_tasks", task.id, mapOf(
            "title" to task.title,
            "assignedToName" to task.assignedToName,
            "dueDate" to task.dueDate,
            "isCompleted" to task.isCompleted,
            "recurring" to task.recurring,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        _lastActionMessage.value = "تمت إضافة المهمة المنزلية"
        return true
    }

    fun toggleHouseholdTask(id: String) {
        var updated: HouseholdTask? = null
        _householdTasks.update {
            it.map { t ->
                if (t.id == id) {
                    val mod = t.copy(isCompleted = !t.isCompleted)
                    updated = mod
                    mod
                } else t
            }
        }
        saveToStorage()
        updated?.let {
            syncDocToFirestore("household_tasks", it.id, mapOf("isCompleted" to it.isCompleted))
        }
    }

    fun deleteHouseholdTask(id: String) {
        _householdTasks.update { it.filter { t -> t.id != id } }
        saveToStorage()
        deleteDocFromFirestore("household_tasks", id)
    }

    fun addShoppingItem(item: ShoppingItem): Boolean {
        if (isDuplicateSubmission("add_shop_${item.name}")) return false
        val err = validateShoppingItem(item)
        if (err != null) {
            _lastActionMessage.value = err
            return false
        }
        _shoppingList.update { listOf(item) + it }
        saveToStorage()
        syncDocToFirestore("shopping_items", item.id, mapOf(
            "name" to item.name,
            "quantity" to item.quantity,
            "estimatedPrice" to item.estimatedPrice,
            "isPurchased" to item.isPurchased,
            "category" to item.category,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        _lastActionMessage.value = "تمت إضافة الصنف لقائمة المشتريات"
        return true
    }

    fun toggleShoppingItem(id: String) {
        var updated: ShoppingItem? = null
        _shoppingList.update {
            it.map { s ->
                if (s.id == id) {
                    val mod = s.copy(isPurchased = !s.isPurchased)
                    updated = mod
                    mod
                } else s
            }
        }
        saveToStorage()
        updated?.let {
            syncDocToFirestore("shopping_items", it.id, mapOf("isPurchased" to it.isPurchased))
        }
    }

    fun deleteShoppingItem(id: String) {
        _shoppingList.update { it.filter { s -> s.id != id } }
        saveToStorage()
        deleteDocFromFirestore("shopping_items", id)
    }

    fun addBill(bill: UtilityBill): Boolean {
        if (isDuplicateSubmission("add_bill_${bill.name}_${bill.dueDate}")) return false
        val err = validateBill(bill)
        if (err != null) {
            _lastActionMessage.value = err
            return false
        }
        _bills.update { listOf(bill) + it }
        saveToStorage()
        syncDocToFirestore("bills", bill.id, mapOf(
            "name" to bill.name,
            "billNumber" to bill.billNumber,
            "amount" to bill.amount,
            "dueDate" to bill.dueDate,
            "isPaid" to bill.isPaid,
            "paidDate" to bill.paidDate,
            "iconName" to bill.iconName,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        _lastActionMessage.value = "تمت إضافة الفاتورة بنجاح"
        return true
    }

    fun deleteBill(billId: String) {
        _bills.update { it.filter { b -> b.id != billId } }
        saveToStorage()
        deleteDocFromFirestore("bills", billId)
        _lastActionMessage.value = "تم حذف الفاتورة"
    }

    fun toggleBillPaid(id: String) {
        var updated: UtilityBill? = null
        _bills.update {
            it.map { b ->
                if (b.id == id) {
                    val paid = !b.isPaid
                    val mod = b.copy(isPaid = paid, paidDate = if (paid) todayDateString else null)
                    updated = mod
                    mod
                } else b
            }
        }
        saveToStorage()
        updated?.let {
            syncDocToFirestore("bills", it.id, mapOf("isPaid" to it.isPaid, "paidDate" to it.paidDate))
        }
    }

    fun addMaintenance(item: HomeMaintenance) {
        _maintenance.update { listOf(item) + it }
        saveToStorage()
    }

    fun addOccasion(occasion: FamilyOccasion): Boolean {
        if (isDuplicateSubmission("add_occ_${occasion.title}_${occasion.date}")) return false
        val err = validateOccasion(occasion)
        if (err != null) {
            _lastActionMessage.value = err
            return false
        }
        _occasions.update { listOf(occasion) + it }
        saveToStorage()
        syncDocToFirestore("occasions", occasion.id, mapOf(
            "title" to occasion.title,
            "date" to occasion.date,
            "type" to occasion.type,
            "notes" to occasion.notes,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        _lastActionMessage.value = "تمت إضافة المناسبة العائلية"
        return true
    }

    fun deleteOccasion(id: String) {
        _occasions.update { it.filter { o -> o.id != id } }
        saveToStorage()
        deleteDocFromFirestore("occasions", id)
    }

    // ----------------------------------------------------
    // Actions - Financial
    // ----------------------------------------------------
    fun addTransaction(transaction: FinancialTransaction): Boolean {
        if (isDuplicateSubmission("add_tx_${transaction.type}_${transaction.amount}_${transaction.date}")) {
            _lastActionMessage.value = "تم تسجيل هذه المعاملة بالفعل"
            return false
        }
        val err = validateTransaction(transaction)
        if (err != null) {
            _lastActionMessage.value = err
            return false
        }
        _transactions.update { listOf(transaction) + it }
        saveToStorage()
        syncDocToFirestore("transactions", transaction.id, mapOf(
            "type" to transaction.type.name,
            "amount" to transaction.amount,
            "date" to transaction.date,
            "category" to transaction.category,
            "description" to transaction.description,
            "paymentMethod" to transaction.paymentMethod,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        val typeLabel = if (transaction.type == TransactionType.INCOME) "الإيراد" else "المصروف"
        _lastActionMessage.value = "تم تسجيل $typeLabel بنجاح"
        return true
    }

    fun deleteTransaction(id: String) {
        _transactions.update { it.filter { t -> t.id != id } }
        saveToStorage()
        deleteDocFromFirestore("transactions", id)
        _lastActionMessage.value = "تم حذف المعاملة المالية"
    }

    fun updateBudgetLimit(category: String, limit: Double) {
        _budgets.update { list ->
            val existing = list.find { it.categoryName == category }
            if (existing != null) {
                list.map { if (it.categoryName == category) it.copy(limitAmount = limit) else it }
            } else {
                list + CategoryBudget(category, limit)
            }
        }
        saveToStorage()
    }

    fun addInstallment(item: InstallmentDebt) {
        _installments.update { listOf(item) + it }
        saveToStorage()
    }

    // Exact financial calculations via application code
    fun calculateTotalIncome(): Double {
        return _transactions.value.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    fun calculateTotalExpenses(): Double {
        return _transactions.value.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    fun calculateBalance(): Double {
        return calculateTotalIncome() - calculateTotalExpenses()
    }

    fun calculateExpensesByCategory(): Map<String, Double> {
        return _transactions.value
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    // ----------------------------------------------------
    // Actions - Children & Education
    // ----------------------------------------------------
    fun addChild(child: ChildProfile): Boolean {
        if (isDuplicateSubmission("add_child_${child.name}")) return false
        val err = validateChild(child)
        if (err != null) {
            _lastActionMessage.value = err
            return false
        }
        _children.update { it + child }
        saveToStorage()
        syncDocToFirestore("children", child.id, mapOf(
            "name" to child.name,
            "grade" to child.grade,
            "school" to child.school,
            "avatarEmoji" to child.avatarEmoji,
            "subjects" to child.subjects,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        _lastActionMessage.value = "تمت إضافة ملف الطفل بنجاح"
        return true
    }

    fun updateChild(child: ChildProfile): Boolean {
        val err = validateChild(child)
        if (err != null) {
            _lastActionMessage.value = err
            return false
        }
        _children.update { it.map { c -> if (c.id == child.id) child else c } }
        saveToStorage()
        syncDocToFirestore("children", child.id, mapOf(
            "name" to child.name,
            "grade" to child.grade,
            "school" to child.school,
            "avatarEmoji" to child.avatarEmoji,
            "subjects" to child.subjects
        ))
        _lastActionMessage.value = "تم تحديث الملف الدراسي"
        return true
    }

    fun deleteChild(childId: String) {
        _children.update { it.filter { c -> c.id != childId } }
        _homework.update { it.filter { h -> h.childId != childId } }
        _exams.update { it.filter { e -> e.childId != childId } }
        saveToStorage()
        deleteDocFromFirestore("children", childId)
        _lastActionMessage.value = "تم حذف ملف الطفل وواجباته"
    }

    fun addHomework(hw: Homework): Boolean {
        if (isDuplicateSubmission("add_hw_${hw.childId}_${hw.title}_${hw.dueDate}")) return false
        val err = validateHomework(hw)
        if (err != null) {
            _lastActionMessage.value = err
            return false
        }
        _homework.update { listOf(hw) + it }
        saveToStorage()
        syncDocToFirestore("homework", hw.id, mapOf(
            "childId" to hw.childId,
            "childName" to hw.childName,
            "subject" to hw.subject,
            "title" to hw.title,
            "description" to hw.description,
            "dueDate" to hw.dueDate,
            "isCompleted" to hw.isCompleted,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        _lastActionMessage.value = "تمت إضافة الواجب المدرسي"
        return true
    }

    fun toggleHomework(id: String) {
        var updated: Homework? = null
        _homework.update {
            it.map { h ->
                if (h.id == id) {
                    val mod = h.copy(isCompleted = !h.isCompleted)
                    updated = mod
                    mod
                } else h
            }
        }
        saveToStorage()
        updated?.let {
            syncDocToFirestore("homework", it.id, mapOf("isCompleted" to it.isCompleted))
        }
    }

    fun deleteHomework(id: String) {
        _homework.update { it.filter { h -> h.id != id } }
        saveToStorage()
        deleteDocFromFirestore("homework", id)
    }

    fun addExam(exam: Exam): Boolean {
        if (isDuplicateSubmission("add_exam_${exam.childId}_${exam.subject}_${exam.examDate}")) return false
        val err = validateExam(exam)
        if (err != null) {
            _lastActionMessage.value = err
            return false
        }
        _exams.update { listOf(exam) + it }
        saveToStorage()
        syncDocToFirestore("exams", exam.id, mapOf(
            "childId" to exam.childId,
            "childName" to exam.childName,
            "subject" to exam.subject,
            "title" to exam.title,
            "examDate" to exam.examDate,
            "maxScore" to exam.maxScore,
            "scoreAchieved" to exam.scoreAchieved,
            "notes" to exam.notes,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        _lastActionMessage.value = "تمت إضافة موعد الامتحان"
        return true
    }

    fun deleteExam(id: String) {
        _exams.update { it.filter { e -> e.id != id } }
        saveToStorage()
        deleteDocFromFirestore("exams", id)
    }

    fun syncAllLocalToCloud() {
        val user = getAuthenticatedUser() ?: return
        _storageStatus.update { it.copy(isSyncing = true) }
        _tasks.value.forEach { t ->
            syncDocToFirestore("tasks", t.id, mapOf(
                "title" to t.title, "description" to t.description, "priority" to t.priority.name,
                "dueDate" to t.dueDate, "category" to t.category, "status" to t.status.name, "isCompleted" to t.isCompleted
            ))
        }
        _transactions.value.forEach { tx ->
            syncDocToFirestore("transactions", tx.id, mapOf(
                "type" to tx.type.name, "amount" to tx.amount, "date" to tx.date, "category" to tx.category,
                "description" to tx.description, "paymentMethod" to tx.paymentMethod
            ))
        }
        _bills.value.forEach { b ->
            syncDocToFirestore("bills", b.id, mapOf(
                "name" to b.name, "billNumber" to b.billNumber, "amount" to b.amount, "dueDate" to b.dueDate,
                "isPaid" to b.isPaid, "paidDate" to b.paidDate, "iconName" to b.iconName
            ))
        }
        _shoppingList.value.forEach { s ->
            syncDocToFirestore("shopping_items", s.id, mapOf(
                "name" to s.name, "quantity" to s.quantity, "estimatedPrice" to s.estimatedPrice,
                "isPurchased" to s.isPurchased, "category" to s.category
            ))
        }
        _children.value.forEach { c ->
            syncDocToFirestore("children", c.id, mapOf(
                "name" to c.name, "grade" to c.grade, "school" to c.school,
                "avatarEmoji" to c.avatarEmoji, "subjects" to c.subjects
            ))
        }
        _homework.value.forEach { h ->
            syncDocToFirestore("homework", h.id, mapOf(
                "childId" to h.childId, "childName" to h.childName, "subject" to h.subject,
                "title" to h.title, "description" to h.description, "dueDate" to h.dueDate, "isCompleted" to h.isCompleted
            ))
        }
        _exams.value.forEach { e ->
            syncDocToFirestore("exams", e.id, mapOf(
                "childId" to e.childId, "childName" to e.childName, "subject" to e.subject,
                "title" to e.title, "examDate" to e.examDate, "maxScore" to e.maxScore,
                "scoreAchieved" to e.scoreAchieved, "notes" to e.notes
            ))
        }
        _householdTasks.value.forEach { ht ->
            syncDocToFirestore("household_tasks", ht.id, mapOf(
                "title" to ht.title, "assignedToName" to ht.assignedToName, "dueDate" to ht.dueDate,
                "isCompleted" to ht.isCompleted, "recurring" to ht.recurring
            ))
        }
        _occasions.value.forEach { o ->
            syncDocToFirestore("occasions", o.id, mapOf(
                "title" to o.title, "date" to o.date, "type" to o.type, "notes" to o.notes
            ))
        }
        _familyMembers.value.forEach { fm ->
            syncDocToFirestore("family_members", fm.id, mapOf(
                "name" to fm.name, "role" to fm.role, "avatarEmoji" to fm.avatarEmoji,
                "birthDate" to fm.birthDate, "notes" to fm.notes, "phone" to fm.phone
            ))
        }
    }

    // ----------------------------------------------------
    // Smart Daily Planner Generator
    // ----------------------------------------------------
    fun generateDailyPlan(): DailyPlan {
        val todayTasks = _tasks.value.filter { !it.isCompleted }
        val todayMeetings = _meetings.value
        val todayHomework = _homework.value.filter { !it.isCompleted }
        val pendingBills = _bills.value.filter { !it.isPaid }

        val timeBlocks = mutableListOf<PlannedTimeBlock>()

        timeBlocks.add(
            PlannedTimeBlock(
                timeSlot = "07:30 - 08:30",
                title = "بداية اليوم وفطور الأسرة وتجهيز الأبناء",
                category = "عائلة",
                description = "تنظيم الحقائب ومراجعة جدول المدرسة ومستلزمات الصباح"
            )
        )

        if (todayMeetings.isNotEmpty()) {
            val firstM = todayMeetings.first()
            timeBlocks.add(
                PlannedTimeBlock(
                    timeSlot = "09:30 - 10:30",
                    title = "اجتماع عمل: ${firstM.title}",
                    category = "عمل",
                    description = "الحضور في الموعد ومناقشة النقاط المحددة: ${firstM.attendees}"
                )
            )
        }

        val highPriorityTask = todayTasks.firstOrNull { it.priority == Priority.HIGH }
        if (highPriorityTask != null) {
            timeBlocks.add(
                PlannedTimeBlock(
                    timeSlot = "11:00 - 01:00",
                    title = "تركيز عميق: ${highPriorityTask.title}",
                    category = "عمل",
                    description = highPriorityTask.description.ifEmpty { "إنجاز العمل المطلوب بأعلى أولوية" }
                )
            )
        }

        timeBlocks.add(
            PlannedTimeBlock(
                timeSlot = "01:30 - 02:30",
                title = "استراحة الصلاة والغداء والتواصل مع الأسرة",
                category = "راحة",
                description = "تجديد النشاط والراحة بعد التركيز الصباحي"
            )
        )

        val nextTask = todayTasks.filter { it != highPriorityTask }.firstOrNull()
        if (nextTask != null) {
            timeBlocks.add(
                PlannedTimeBlock(
                    timeSlot = "02:30 - 04:30",
                    title = "متابعة المهام: ${nextTask.title}",
                    category = "عمل",
                    description = "استكمال باقي متطلبات العمل ومراسلات الفريق"
                )
            )
        }

        if (todayHomework.isNotEmpty()) {
            val hw = todayHomework.first()
            timeBlocks.add(
                PlannedTimeBlock(
                    timeSlot = "05:00 - 06:30",
                    title = "جلسة مذاكرة ودعم: ${hw.childName} (${hw.subject})",
                    category = "دراسة",
                    description = "متابعة حل الواجب: ${hw.title}"
                )
            )
        }

        if (pendingBills.isNotEmpty()) {
            val b = pendingBills.first()
            timeBlocks.add(
                PlannedTimeBlock(
                    timeSlot = "07:00 - 07:30",
                    title = "سداد التزام مالي: ${b.name}",
                    category = "مالي",
                    description = "دفع مبلغ ${b.amount.toInt()} ج.م قبل موعد الاستحقاق (${b.dueDate})"
                )
            )
        }

        timeBlocks.add(
            PlannedTimeBlock(
                timeSlot = "08:30 - 10:30",
                title = "جلسة عائلية مسائية وترفيه",
                category = "عائلة",
                description = "قضاء وقت ممتع ومراجعة خطة الغد قبل النوم"
            )
        )

        val plan = DailyPlan(
            date = todayDateString,
            summary = "تم تنظيم جدولك لليوم بتوازن بين أهداف العمل ذات الأولوية العالية، مذاكرة الأبناء، والالتزامات العائلية.",
            timeBlocks = timeBlocks
        )
        _dailyPlan.value = plan
        return plan
    }

    // ----------------------------------------------------
    // AI Assistant - Natural Language Understanding & Execution
    // ----------------------------------------------------
    fun sendUserMessage(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed
        )
        _chatMessages.update { it + userMsg }

        // Process with AI Engine
        val response = processNaturalLanguageIntent(trimmed)
        _chatMessages.update { it + response }
    }

    private fun processNaturalLanguageIntent(prompt: String): ChatMessage {
        val lower = prompt.lowercase()

        // 1. "كم صرفت على البيت هذا الشهر؟" or questions about spending
        if (lower.contains("كم صرفت") || lower.contains("المصروفات") || lower.contains("إجمالي المصاريف")) {
            val total = calculateTotalExpenses()
            val homeExpense = _transactions.value
                .filter { it.type == TransactionType.EXPENSE && (it.category.contains("المنزل") || it.category.contains("طعام")) }
                .sumOf { it.amount }

            return ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "حسب سجلاتك المالية المسجلة فعلياً:\n• إجمالي مصروفات المنزل والطعام هذا الشهر: ${homeExpense.toInt()} ج.م.\n• إجمالي كافة المصروفات: ${total.toInt()} ج.م.\n• الرصيد المتبقي من الدخل: ${calculateBalance().toInt()} ج.م."
            )
        }

        // 2. "ما المهام المتأخرة عندي؟"
        if (lower.contains("متأخرة") || lower.contains("المهام المتأخرة") || lower.contains("فاتتني")) {
            val overdue = _tasks.value.filter { !it.isCompleted && it.dueDate < todayDateString }
            if (overdue.isEmpty()) {
                return ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "ممتاز يا أحمد! لا توجد لديك أي مهام متأخرة عن موعد استحقاقها حالياً. كل مهامك مجدولة في مواعيدها."
                )
            } else {
                val listStr = overdue.joinToString("\n") { "• ${it.title} (كانت مستحقة: ${it.dueDate}) [أولوية ${it.priority.titleAr}]" }
                return ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "لديك ${overdue.size} مهام متأخرة تحتاج لتدخلك:\n$listStr\nهل ترغب في إعادة جدولتها لليوم؟"
                )
            }
        }

        // 3. "خطط لي يومي" or "خطط لي الغد"
        if (lower.contains("خطط") || lower.contains("خطة اليوم") || lower.contains("جدول اليوم")) {
            val plan = generateDailyPlan()
            return ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "تم إنشاء خطتك الذكية لليوم بناءً على مهامك وأولوياتك:\n\n${plan.summary}\n\n• 09:30 ص: اجتماع خطة الأسبوع وتوزيع المهام\n• 11:00 ص: تركيز عميق على التقرير المالي\n• 05:00 م: متابعة واجب الرياضيات مع عمر\n• 07:00 م: متابعة سداد الفواتير المستحقة\n\nيمكنك استعراض الخطة الزمنية التفاعلية كاملة في لوحة التحكم والتقويم!",
                pendingAction = AssistantAction(
                    actionType = "PLAN_DAY",
                    title = "اعتماد الخطة اليومية",
                    details = mapOf("date" to todayDateString)
                )
            )
        }

        // 4. "سجل مصروف 150 جنيه مشتريات للبيت"
        val numberRegex = Regex("""(\d+(\.\d+)?)""")
        val match = numberRegex.find(lower)
        val amount = match?.value?.toDoubleOrNull()

        if (amount != null && (lower.contains("مصروف") || lower.contains("صرفت") || lower.contains("اشتريت") || lower.contains("دفعت"))) {
            var category = "أخرى"
            if (lower.contains("بيت") || lower.contains("منزل")) category = "المنزل"
            if (lower.contains("أكل") || lower.contains("طعام") || lower.contains("خضار") || lower.contains("بقالة") || lower.contains("سوبرماركت")) category = "طعام وبقالة"
            if (lower.contains("بنزين") || lower.contains("مواصلات") || lower.contains("أوبر") || lower.contains("عربية")) category = "مواصلات وبنزين"
            if (lower.contains("مدرسة") || lower.contains("دروس") || lower.contains("كتب") || lower.contains("تعليم")) category = "تعليم ومدارس"
            if (lower.contains("صيدلية") || lower.contains("دواء") || lower.contains("دكتور")) category = "صحة وأدوية"

            val desc = prompt.replace(Regex("""(سجل|مصروف|صرفت|اشتريت|دفعت|جنيه|\d+)"""), "").trim().ifEmpty { "مصروف $category" }

            val action = AssistantAction(
                actionType = "ADD_EXPENSE",
                title = "تسجيل مصروف جديد",
                details = mapOf(
                    "amount" to amount.toString(),
                    "category" to category,
                    "description" to desc,
                    "date" to todayDateString
                )
            )

            return ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "فهمت طلبك: تريد تسجيل مصروف بمبلغ **${amount.toInt()} ج.م** تحت تصنيف **$category** لوصف: «$desc».\nاضغط أدناه للتأكيد والإضافة الفورية لمحفظتك.",
                pendingAction = action
            )
        }

        // 5. "فكرني بدفع فاتورة الإنترنت يوم 20"
        if (lower.contains("فاتورة") || lower.contains("فكرني بدفع") || lower.contains("كهرباء") || lower.contains("إنترنت") || lower.contains("غاز")) {
            var billName = "فاتورة خدمات"
            if (lower.contains("إنترنت") || lower.contains("نت")) billName = "فاتورة الإنترنت"
            if (lower.contains("كهرباء")) billName = "فاتورة الكهرباء"
            if (lower.contains("غاز")) billName = "فاتورة الغاز"
            if (lower.contains("مياه")) billName = "فاتورة المياه"

            val billAmount = amount ?: 300.0
            val day = if (lower.contains("20")) "20" else if (lower.contains("15")) "15" else "25"
            val dueDate = "2026-11-$day"

            val action = AssistantAction(
                actionType = "ADD_BILL",
                title = "إضافة تذكير فاتورة",
                details = mapOf(
                    "name" to billName,
                    "amount" to billAmount.toString(),
                    "dueDate" to dueDate
                )
            )

            return ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "تم استخراج تذكير الفاتورة: **$billName** بمبلغ **${billAmount.toInt()} ج.م** وموعد استحقاق **$dueDate**.\nهل ترغب في إضافتها إلى قائمة الفواتير والتقويم الموحد؟",
                pendingAction = action
            )
        }

        // 6. "أضف امتحان علوم يوم الأحد"
        if (lower.contains("امتحان") || lower.contains("اختبار")) {
            val child = if (lower.contains("نور")) "نور" else "عمر"
            var subject = "علوم"
            if (lower.contains("رياضيات") || lower.contains("حساب")) subject = "رياضيات"
            if (lower.contains("عربي") || lower.contains("لغة عربية")) subject = "لغة عربية"
            if (lower.contains("إنجليزي") || lower.contains("انجليزي")) subject = "لغة إنجليزية"
            if (lower.contains("دراسات")) subject = "دراسات اجتماعية"

            val action = AssistantAction(
                actionType = "ADD_EXAM",
                title = "إضافة امتحان مدرسي",
                details = mapOf(
                    "child" to child,
                    "subject" to subject,
                    "title" to "امتحان $subject",
                    "date" to "2026-11-18"
                )
            )

            return ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "سأقوم بجدولة **امتحان $subject** للطالب **$child** يوم 18 نوفمبر في جدول الامتحانات والتقويم.\nاضغط لتأكيد الإضافة.",
                pendingAction = action
            )
        }

        // 7. General Assistant Response
        return ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "أهلاً بك! يمكنني تنفيذ الأوامر مباشرة لمساعدتك، على سبيل المثال:\n• تسجيل مصاريف أو إيرادات جديدة في محفظتك.\n• إضافة مهام عمل واجتماعات مع تحديد الأولويات.\n• تنظيم فواتير المنزل وجداول الصيانة ومستلزمات التسوق.\n• جدولة واجبات وامتحانات أبنائك وشرح الدروس وإنشاء اختبارات تفاعلية.\n• تنظيم يومك بضغطة زر عبر «خطط لي يومي»."
        )
    }

    fun executePendingAction(action: AssistantAction) {
        when (action.actionType) {
            "ADD_EXPENSE" -> {
                val amount = action.details["amount"]?.toDoubleOrNull() ?: 100.0
                val category = action.details["category"] ?: "أخرى"
                val desc = action.details["description"] ?: "مصروف"
                val date = action.details["date"] ?: todayDateString
                addTransaction(
                    FinancialTransaction(
                        type = TransactionType.EXPENSE,
                        amount = amount,
                        category = category,
                        description = desc,
                        date = date,
                        paymentMethod = "نقدي"
                    )
                )
            }
            "ADD_BILL" -> {
                val name = action.details["name"] ?: "فاتورة"
                val amount = action.details["amount"]?.toDoubleOrNull() ?: 200.0
                val dueDate = action.details["dueDate"] ?: todayDateString
                addBill(
                    UtilityBill(
                        name = name,
                        amount = amount,
                        dueDate = dueDate,
                        isPaid = false
                    )
                )
            }
            "ADD_EXAM" -> {
                val childName = action.details["child"] ?: "عمر"
                val subject = action.details["subject"] ?: "علوم"
                val title = action.details["title"] ?: "امتحان"
                val date = action.details["date"] ?: todayDateString
                val child = _children.value.find { it.name.contains(childName) } ?: _children.value.first()
                addExam(
                    Exam(
                        childId = child.id,
                        childName = child.name,
                        subject = subject,
                        title = title,
                        examDate = date,
                        maxScore = 30
                    )
                )
            }
            "ADD_TASK" -> {
                val title = action.details["title"] ?: action.details["description"] ?: "مهمة عمل"
                val desc = action.details["description"] ?: ""
                val priorityStr = action.details["priority"] ?: "MEDIUM"
                val priority = try { Priority.valueOf(priorityStr) } catch (_: Exception) { Priority.MEDIUM }
                val date = action.details["date"] ?: todayDateString
                val cat = action.details["category"] ?: "مشاريع"
                addTask(
                    WorkTask(
                        title = title,
                        description = desc,
                        priority = priority,
                        dueDate = date,
                        category = cat
                    )
                )
            }
            "PLAN_DAY" -> {
                generateDailyPlan()
            }
        }

        // Mark action executed in chat
        _chatMessages.update { list ->
            list.map { msg ->
                if (msg.pendingAction == action) {
                    msg.copy(pendingAction = action.copy(isExecuted = true))
                } else msg
            }
        }
    }

    // ----------------------------------------------------
    // Educational Quizzes Generation (Interactive)
    // ----------------------------------------------------
    fun getSampleQuizForSubject(subject: String, childName: String): StudyQuiz {
        return when {
            subject.contains("علوم") -> StudyQuiz(
                childId = "c1",
                subject = "علوم",
                topic = "الجهاز الهضمي والتنفسي",
                lessonSummary = "يقوم الجهاز الهضمي بتحويل الطعام المعقد إلى مواد غذائية بسيطة يمتصها الجسم للحصول على الطاقة، بينما يقوم الجهاز التنفسي بإمداد الجسم بالأكسجين وطرد ثاني أكسيد الكربون.",
                questions = listOf(
                    QuizQuestion(
                        id = "q1",
                        question = "ما العضو الرئيسي المسؤول عن امتصاص العناصر الغذائية في الجهاز الهضمي؟",
                        options = listOf("المعدة", "الأمعاء الدقيقة", "المريء", "الكبد"),
                        correctIndex = 1,
                        explanation = "الأمعاء الدقيقة مسؤولة عن الامتصاص النهائي للعناصر الغذائية المفيدة ونقلها للدم."
                    ),
                    QuizQuestion(
                        id = "q2",
                        question = "تبدأ عملية هضم الطعام في الفم بمساعدة اللعاب والأسنان.",
                        options = listOf("صح", "خطأ"),
                        correctIndex = 0,
                        explanation = "صحيح، يقوم اللعاب بترطيب الطعام وهضم النشويات جزئياً داخل الفم."
                    ),
                    QuizQuestion(
                        id = "q3",
                        question = "أي من الغازات التالية يحتاجه الجسم أثناء عملية التنفس؟",
                        options = listOf("النيتروجين", "ثاني أكسيد الكربون", "الأكسجين", "الهيدروجين"),
                        correctIndex = 2,
                        explanation = "يدخل الأكسجين إلى الرئتين أثناء الشهيق ليصل إلى جميع خلايا الجسم."
                    )
                )
            )
            subject.contains("رياضيات") || subject.contains("حساب") -> StudyQuiz(
                childId = "c1",
                subject = "رياضيات",
                topic = "القسمة والضرب",
                lessonSummary = "القسمة هي عملية عكسية لعملية الضرب، حيث نقوم بتوزيع عدد معين إلى مجموعات متساوية.",
                questions = listOf(
                    QuizQuestion(
                        id = "mq1",
                        question = "إذا كان 6 × 8 = 48، فما ناتج 48 ÷ 6؟",
                        options = listOf("7", "8", "9", "6"),
                        correctIndex = 1,
                        explanation = "القسمة والضرب عمليتان عكسيتان: 48 ÷ 6 = 8 لأن 8 × 6 = 48."
                    ),
                    QuizQuestion(
                        id = "mq2",
                        question = "أي رقم نضربه في أي عدد يعطي الناتج صفراً؟",
                        options = listOf("1", "0", "10", "نفس الرقم"),
                        correctIndex = 1,
                        explanation = "خاصية الضرب في صفر: حاصل ضرب أي رقم في صفر يساوي صفر دائماً."
                    ),
                    QuizQuestion(
                        id = "mq3",
                        question = "ناتج قسمة أي عدد غير الصفر على نفسه يساوي 1.",
                        options = listOf("صح", "خطأ"),
                        correctIndex = 0,
                        explanation = "صحيح، مثل 5 ÷ 5 = 1، 100 ÷ 100 = 1."
                    )
                )
            )
            else -> StudyQuiz(
                childId = "c1",
                subject = subject,
                topic = "درس عام ومراجعة شاملة",
                lessonSummary = "مراجعة أهم المفاهيم الأساسية للدرس واستخلاص النتائج والتمارين.",
                questions = listOf(
                    QuizQuestion(
                        id = "gq1",
                        question = "القراءة والممارسة اليومية تساعد على تثبيت المعلومات الدراسية.",
                        options = listOf("صح", "خطأ"),
                        correctIndex = 0,
                        explanation = "الممارسة والتكرار المنتظم هما أساس التعلم الفعال والتفوق الدراسي."
                    )
                )
            )
        }
    }

    // Reset to default sample data
    fun resetData() {
        _tasks.value = defaultTasks
        _meetings.value = defaultMeetings
        _familyMembers.value = defaultFamilyMembers
        _householdTasks.value = defaultHouseholdTasks
        _shoppingList.value = defaultShoppingList
        _bills.value = defaultBills
        _maintenance.value = defaultMaintenance
        _occasions.value = defaultOccasions
        _transactions.value = defaultTransactions
        _budgets.value = defaultBudgets
        _installments.value = defaultInstallments
        _children.value = defaultChildren
        _homework.value = defaultHomework
        _exams.value = defaultExams
        _chatMessages.value = defaultMessages
        _dailyPlan.value = null

        prefs?.edit()?.clear()?.apply()
        saveToStorage()
    }
}
