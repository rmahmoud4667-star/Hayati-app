package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.HayatiAiService
import com.example.auth.AuthManager
import com.example.data.model.*
import com.example.data.repository.HayatiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen(val titleAr: String, val titleEn: String) {
    DASHBOARD("الرئيسية", "Dashboard"),
    WORK("العمل والمهام", "Work & Tasks"),
    FAMILY("المنزل والعائلة", "Home & Family"),
    FINANCE("المالية", "Finance"),
    EDUCATION("تعليم الأبناء", "Education"),
    ASSISTANT("المساعد الذكي", "AI Assistant"),
    CALENDAR("التقويم", "Calendar"),
    REPORTS("التقارير", "Reports"),
    SETTINGS("الإعدادات", "Settings")
}

enum class CalendarViewMode(val titleAr: String, val titleEn: String) {
    DAY("يومي", "Day"),
    WEEK("أسبوعي", "Week"),
    MONTH("شهري", "Month")
}

class HayatiViewModel(
    application: Application
) : AndroidViewModel(application) {

    val repository: HayatiRepository = HayatiRepository(application.applicationContext)
    val aiService: HayatiAiService = HayatiAiService(repository)
    val authManager: AuthManager = AuthManager(application.applicationContext)

    init {
        authManager.attemptSilentAutoSignIn(viewModelScope)
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                repository.refreshStorageStatus()
                if (user != null) {
                    repository.syncAllLocalToCloud()
                }
            }
        }
    }

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<Screen>()

    // Dialog state toggles
    val showAddTaskDialog = MutableStateFlow(false)
    val editingTask = MutableStateFlow<WorkTask?>(null)

    val showAddExpenseDialog = MutableStateFlow(false)
    val showAddBillDialog = MutableStateFlow(false)
    val showAddHomeworkDialog = MutableStateFlow(false)

    val showEditBudgetDialog = MutableStateFlow(false)
    val editingBudget = MutableStateFlow<CategoryBudget?>(null)

    val showAddChildDialog = MutableStateFlow(false)
    val editingChild = MutableStateFlow<ChildProfile?>(null)

    val showAddExamDialog = MutableStateFlow(false)
    val showAddHouseholdTaskDialog = MutableStateFlow(false)
    val showAddOccasionDialog = MutableStateFlow(false)

    val showDailyPlanDialog = MutableStateFlow(false)
    val showQuizDialog = MutableStateFlow(false)
    val activeQuiz = MutableStateFlow<StudyQuiz?>(null)

    val isAiThinking = MutableStateFlow(false)

    val calendarViewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val searchWorkQuery = MutableStateFlow("")
    val filterWorkPriority = MutableStateFlow<Priority?>(null)

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_screenHistory.isNotEmpty()) {
            _currentScreen.value = _screenHistory.removeAt(_screenHistory.size - 1)
            return true
        }
        if (_currentScreen.value != Screen.DASHBOARD) {
            _currentScreen.value = Screen.DASHBOARD
            return true
        }
        return false
    }

    fun planMyDay() {
        viewModelScope.launch {
            repository.generateDailyPlan()
            showDailyPlanDialog.value = true
        }
    }

    fun sendChatMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed
        )
        repository._chatMessages.update { it + userMsg }

        viewModelScope.launch {
            isAiThinking.value = true
            try {
                val assistantResponse = aiService.processMessage(trimmed)
                repository._chatMessages.update { it + assistantResponse }
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "تعذر إتمام الطلب عبر Gemini: ${e.localizedMessage ?: "خطأ غير متوقع"}"
                )
                repository._chatMessages.update { it + errorMsg }
            } finally {
                isAiThinking.value = false
            }
        }
    }

    fun startSubjectQuiz(subject: String, childName: String) {
        val quiz = repository.getSampleQuizForSubject(subject, childName)
        activeQuiz.value = quiz
        showQuizDialog.value = true
    }

    fun toggleLanguage() {
        repository.isArabic.value = !repository.isArabic.value
    }

    fun toggleTheme() {
        repository.isDarkMode.value = !repository.isDarkMode.value
    }

    fun signInWithGoogle(activity: Activity) {
        authManager.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = {
                repository.refreshStorageStatus()
                repository.syncAllLocalToCloud()
            }
        )
    }

    fun signOut() {
        authManager.signOut(viewModelScope) {
            repository.refreshStorageStatus()
        }
    }
}
