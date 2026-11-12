package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.screens.assistant.AssistantScreen
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.education.EducationScreen
import com.example.ui.screens.family.FamilyScreen
import com.example.ui.screens.finance.FinanceScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.work.WorkScreen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HayatiApp(viewModel: HayatiViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isArabic by viewModel.repository.isArabic.collectAsState()
    val isDarkMode by viewModel.repository.isDarkMode.collectAsState()

    val showAddTask by viewModel.showAddTaskDialog.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()

    val showAddExpense by viewModel.showAddExpenseDialog.collectAsState()
    val showAddBill by viewModel.showAddBillDialog.collectAsState()
    val showAddHomework by viewModel.showAddHomeworkDialog.collectAsState()
    val showEditBudget by viewModel.showEditBudgetDialog.collectAsState()
    val editingBudget by viewModel.editingBudget.collectAsState()

    val showAddChild by viewModel.showAddChildDialog.collectAsState()
    val editingChild by viewModel.editingChild.collectAsState()

    val showAddExam by viewModel.showAddExamDialog.collectAsState()
    val showAddHouseholdTask by viewModel.showAddHouseholdTaskDialog.collectAsState()
    val showAddOccasion by viewModel.showAddOccasionDialog.collectAsState()

    val showDailyPlan by viewModel.showDailyPlanDialog.collectAsState()
    val showQuiz by viewModel.showQuizDialog.collectAsState()
    val activeQuiz by viewModel.activeQuiz.collectAsState()
    val dailyPlan by viewModel.repository.dailyPlan.collectAsState()

    // Handle Android system back press
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        viewModel.navigateBack()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val lastActionMessage by viewModel.repository.lastActionMessage.collectAsState()

    LaunchedEffect(lastActionMessage) {
        lastActionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.repository.clearActionMessage()
        }
    }

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        HayatiTheme(darkTheme = isDarkMode) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TurquoisePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isArabic) "حياتي AI" else "HAYATI AI",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isArabic) "مساعدك اليومي الذكي" else "Your Daily Life Assistant",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TurquoisePrimary
                                    )
                                }
                            }
                        },
                        actions = {
                            // Quick links to Calendar, Reports, and Settings
                            IconButton(
                                onClick = { viewModel.navigateTo(Screen.CALENDAR) },
                                modifier = Modifier.testTag("nav_calendar_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "التقويم",
                                    tint = if (currentScreen == Screen.CALENDAR) TurquoisePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(Screen.REPORTS) },
                                modifier = Modifier.testTag("nav_reports_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "التقارير",
                                    tint = if (currentScreen == Screen.REPORTS) TurquoisePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                                modifier = Modifier.testTag("nav_settings_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "الإعدادات",
                                    tint = if (currentScreen == Screen.SETTINGS) TurquoisePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        val navItems = listOf(
                            Triple(Screen.DASHBOARD, Icons.Default.Dashboard, if (isArabic) "الرئيسية" else "Home"),
                            Triple(Screen.WORK, Icons.Default.Work, if (isArabic) "العمل" else "Work"),
                            Triple(Screen.FAMILY, Icons.Default.FamilyRestroom, if (isArabic) "الأسرة" else "Family"),
                            Triple(Screen.FINANCE, Icons.Default.AccountBalanceWallet, if (isArabic) "المالية" else "Finance"),
                            Triple(Screen.EDUCATION, Icons.Default.School, if (isArabic) "التعليم" else "Kids"),
                            Triple(Screen.ASSISTANT, Icons.Default.SmartToy, if (isArabic) "المساعد" else "AI Chat")
                        )

                        navItems.forEach { (screen, icon, label) ->
                            val isSelected = currentScreen == screen
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(screen) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TurquoisePrimary,
                                    selectedTextColor = TurquoisePrimary,
                                    indicatorColor = TurquoisePrimary.copy(alpha = 0.15f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (currentScreen != Screen.ASSISTANT) {
                        FloatingActionButton(
                            onClick = { viewModel.navigateTo(Screen.ASSISTANT) },
                            containerColor = TurquoisePrimary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.testTag("fab_ai_assistant")
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = "المساعد الذكي")
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentScreen) {
                        Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                        Screen.WORK -> WorkScreen(viewModel = viewModel)
                        Screen.FAMILY -> FamilyScreen(viewModel = viewModel)
                        Screen.FINANCE -> FinanceScreen(viewModel = viewModel)
                        Screen.EDUCATION -> EducationScreen(viewModel = viewModel)
                        Screen.ASSISTANT -> AssistantScreen(viewModel = viewModel)
                        Screen.CALENDAR -> CalendarScreen(viewModel = viewModel)
                        Screen.REPORTS -> ReportsScreen(viewModel = viewModel)
                        Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }

            // Interactive Dialogs
            if (showAddTask) {
                AddOrEditTaskDialog(
                    viewModel = viewModel,
                    taskToEdit = editingTask,
                    onDismiss = {
                        viewModel.editingTask.value = null
                        viewModel.showAddTaskDialog.value = false
                    }
                )
            }
            if (showAddExpense) {
                AddExpenseDialog(viewModel = viewModel, onDismiss = { viewModel.showAddExpenseDialog.value = false })
            }
            if (showAddBill) {
                AddBillDialog(viewModel = viewModel, onDismiss = { viewModel.showAddBillDialog.value = false })
            }
            if (showAddHomework) {
                AddHomeworkDialog(viewModel = viewModel, onDismiss = { viewModel.showAddHomeworkDialog.value = false })
            }
            if (showAddChild) {
                AddOrEditChildDialog(
                    viewModel = viewModel,
                    childToEdit = editingChild,
                    onDismiss = {
                        viewModel.editingChild.value = null
                        viewModel.showAddChildDialog.value = false
                    }
                )
            }
            if (showAddExam) {
                AddExamDialog(viewModel = viewModel, onDismiss = { viewModel.showAddExamDialog.value = false })
            }
            if (showAddHouseholdTask) {
                AddHouseholdTaskDialog(viewModel = viewModel, onDismiss = { viewModel.showAddHouseholdTaskDialog.value = false })
            }
            if (showAddOccasion) {
                AddOccasionDialog(viewModel = viewModel, onDismiss = { viewModel.showAddOccasionDialog.value = false })
            }
            if (showDailyPlan) {
                DailyPlanDialog(plan = dailyPlan, onDismiss = { viewModel.showDailyPlanDialog.value = false })
            }
            if (showQuiz) {
                InteractiveQuizDialog(quiz = activeQuiz, onDismiss = { viewModel.showQuizDialog.value = false })
            }
            if (showEditBudget && editingBudget != null) {
                val categoryExpenses = remember(viewModel.repository.transactions) { viewModel.repository.calculateExpensesByCategory() }
                val currentSpent = categoryExpenses[editingBudget!!.categoryName] ?: 0.0
                EditBudgetLimitDialog(
                    categoryBudget = editingBudget!!,
                    currentSpent = currentSpent,
                    onSave = { newLimit ->
                        viewModel.repository.updateBudgetLimit(editingBudget!!.categoryName, newLimit)
                    },
                    onDismiss = {
                        viewModel.editingBudget.value = null
                        viewModel.showEditBudgetDialog.value = false
                    }
                )
            }
        }
    }
}
