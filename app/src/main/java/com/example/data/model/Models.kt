package com.example.data.model

import java.util.UUID

// ==========================================
// WORK & TASKS
// ==========================================

enum class Priority(val titleAr: String, val titleEn: String) {
    HIGH("عالية", "High"),
    MEDIUM("متوسطة", "Medium"),
    LOW("منخفضة", "Low")
}

enum class TaskStatus(val titleAr: String, val titleEn: String) {
    PENDING("قيد الانتظار", "Pending"),
    IN_PROGRESS("جاري التنفيذ", "In Progress"),
    COMPLETED("مكتملة", "Completed")
}

data class WorkTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: String, // YYYY-MM-DD
    val category: String = "مشاريع", // مشاريع, مبيعات, اجتماعات, متابعة, أخرى
    val status: TaskStatus = TaskStatus.PENDING,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkMeeting(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val attendees: String = "",
    val location: String = "عبر الإنترنت (Google Meet)",
    val notes: String = "",
    val actionItems: List<String> = emptyList()
)

// ==========================================
// HOME & FAMILY
// ==========================================

data class FamilyMember(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String, // أب, أم, ابن, ابنة
    val avatarEmoji: String = "👤",
    val birthDate: String = "",
    val notes: String = "",
    val phone: String = ""
)

data class HouseholdTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val assignedToName: String = "الجميع",
    val dueDate: String = "",
    val isCompleted: Boolean = false,
    val recurring: String = "أسبوعي" // يومي, أسبوعي, شهري
)

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: String = "1",
    val estimatedPrice: Double = 0.0, // EGP
    val isPurchased: Boolean = false,
    val category: String = "سوبرماركت" // خضار وفواكه, لحوم وألبان, أدوات منزلية, منظفات
)

data class UtilityBill(
    val id: String = UUID.randomUUID().toString(),
    val name: String, // كهرباء, مياه, غاز, إنترنت, هاتف, صيانة
    val billNumber: String = "",
    val amount: Double, // EGP
    val dueDate: String, // YYYY-MM-DD
    val isPaid: Boolean = false,
    val paidDate: String? = null,
    val iconName: String = "bill"
)

data class HomeMaintenance(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val area: String, // تكييف, سباكة, فلتر مياه, كهرباء
    val lastDate: String = "",
    val nextDate: String = "",
    val estimatedCost: Double = 0.0,
    val notes: String = ""
)

data class FamilyOccasion(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val type: String = "عيد ميلاد", // ذكرى زواج, مناسبة عائلية, نجاح
    val notes: String = ""
)

// ==========================================
// FINANCIAL MANAGER
// ==========================================

enum class TransactionType {
    INCOME, EXPENSE
}

enum class ExpenseCategory(val titleAr: String, val titleEn: String, val icon: String) {
    HOME("المنزل", "Home", "🏠"),
    FOOD("طعام وبقالة", "Food", "🛒"),
    EDUCATION("تعليم ومدارس", "Education", "🎓"),
    TRANSPORTATION("مواصلات وبنزين", "Transportation", "🚗"),
    HEALTH("صحة وأدوية", "Health", "💊"),
    BILLS("فواتير", "Bills", "⚡"),
    SHOPPING("تسوق", "Shopping", "🛍️"),
    ENTERTAINMENT("ترفيه", "Entertainment", "🎬"),
    OTHER("أخرى", "Other", "📦")
}

enum class IncomeCategory(val titleAr: String, val titleEn: String, val icon: String) {
    SALARY("راتب شهري", "Salary", "💼"),
    BUSINESS("عمل حر وتجارة", "Business", "📈"),
    INVESTMENT("استثمارات", "Investment", "💰"),
    OTHER("دخل إضافي", "Other", "✨")
}

data class FinancialTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amount: Double, // EGP
    val date: String, // YYYY-MM-DD
    val category: String, // ExpenseCategory or IncomeCategory title
    val description: String,
    val paymentMethod: String = "نقدي" // نقدي, بطاقة بنكية, محفظة إلكترونية / فودافون كاش, تحويل بنكي
)

data class CategoryBudget(
    val categoryName: String,
    val limitAmount: Double // EGP
)

data class InstallmentDebt(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val monthlyPayment: Double,
    val dueDayOfMonth: Int,
    val isFinished: Boolean = false,
    val notes: String = ""
)

// ==========================================
// CHILDREN EDUCATION
// ==========================================

data class ChildProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val grade: String, // الصف الرابع الابتدائي, الثاني الإعدادي...
    val school: String,
    val avatarEmoji: String = "👦",
    val subjects: List<String> = emptyList()
)

data class Homework(
    val id: String = UUID.randomUUID().toString(),
    val childId: String,
    val childName: String,
    val subject: String,
    val title: String,
    val description: String = "",
    val dueDate: String, // YYYY-MM-DD
    val isCompleted: Boolean = false
)

data class Exam(
    val id: String = UUID.randomUUID().toString(),
    val childId: String,
    val childName: String,
    val subject: String,
    val title: String,
    val examDate: String, // YYYY-MM-DD
    val maxScore: Int = 100,
    val scoreAchieved: Int? = null,
    val notes: String = ""
)

data class QuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class StudyQuiz(
    val id: String = UUID.randomUUID().toString(),
    val childId: String,
    val subject: String,
    val topic: String,
    val lessonSummary: String,
    val questions: List<QuizQuestion>
)

// ==========================================
// AI ASSISTANT & DAILY PLANNER
// ==========================================

data class PlannedTimeBlock(
    val id: String = UUID.randomUUID().toString(),
    val timeSlot: String, // e.g. "08:00 - 09:30"
    val title: String,
    val category: String, // عمل, عائلة, دراسة, راحة
    val description: String,
    val isDone: Boolean = false
)

data class DailyPlan(
    val date: String,
    val summary: String,
    val timeBlocks: List<PlannedTimeBlock>
)

enum class MessageSender {
    USER, ASSISTANT, SYSTEM
}

data class AssistantAction(
    val actionType: String, // ADD_EXPENSE, ADD_TASK, ADD_EXAM, ADD_BILL, PLAN_DAY
    val title: String,
    val details: Map<String, String>,
    val isExecuted: Boolean = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pendingAction: AssistantAction? = null
)

// ==========================================
// CALENDAR & REMINDERS
// ==========================================

enum class CalendarEventType(val labelAr: String, val colorHex: Long) {
    WORK("عمل", 0xFF0D9488),
    FAMILY("عائلة", 0xFFD97706),
    BILL("فاتورة", 0xFFEF4444),
    HOMEWORK("واجب", 0xFF3B82F6),
    EXAM("امتحان", 0xFF8B5CF6)
}

data class CalendarItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String, // YYYY-MM-DD
    val time: String = "",
    val type: CalendarEventType,
    val subtitle: String = "",
    val isCompleted: Boolean = false
)
