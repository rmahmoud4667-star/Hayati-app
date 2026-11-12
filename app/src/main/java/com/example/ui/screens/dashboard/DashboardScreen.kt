package com.example.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.ui.HayatiViewModel
import com.example.ui.Screen
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: HayatiViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.repository.tasks.collectAsState()
    val meetings by viewModel.repository.meetings.collectAsState()
    val bills by viewModel.repository.bills.collectAsState()
    val homework by viewModel.repository.homework.collectAsState()
    val exams by viewModel.repository.exams.collectAsState()
    val transactions by viewModel.repository.transactions.collectAsState()
    val dailyPlan by viewModel.repository.dailyPlan.collectAsState()

    val totalIncome = remember(transactions) { viewModel.repository.calculateTotalIncome() }
    val totalExpenses = remember(transactions) { viewModel.repository.calculateTotalExpenses() }
    val balance = remember(transactions) { viewModel.repository.calculateBalance() }

    val todayTasks = remember(tasks) { tasks.filter { it.dueDate <= viewModel.repository.todayDateString } }
    val completedTasksCount = remember(todayTasks) { todayTasks.count { it.isCompleted } }
    val pendingBills = remember(bills) { bills.filter { !it.isPaid } }
    val pendingHw = remember(homework) { homework.filter { !it.isCompleted } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Date Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "مرحباً يا أحمد 👋",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "الأربعاء، 11 نوفمبر 2026 • بتوقيت القاهرة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val storageStatus by viewModel.repository.storageStatus.collectAsState()
                val currentUser by viewModel.authManager.currentUser.collectAsState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (currentUser != null) TurquoisePrimary else GoldAccent)
                    )
                    Text(
                        text = if (currentUser != null) "مزامنة سحابية نشطة (Cloud Firestore)" else "تخزين محلي دائم وآمن على هذا الجهاز",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (currentUser != null) TurquoisePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Prominent Hero Card: "خطط لي يومي بالذكاء الاصطناعي"
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(TurquoisePrimary, GoldAccent)
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_planner_hero_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    NavySurfaceVariant.copy(alpha = 0.95f),
                                    NavyCard.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "مساعد التخطيط اليومي الذكي",
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "تنظيم متكامل ليومك: العمل، الأسرة، والتعليم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "يقوم الذكاء الاصطناعي بجدولة مواعيد اجتماعاتك، مهامك العاجلة، ومذاكرة الأبناء لتفادي التعارض وتحقيق التوازن.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { viewModel.planMyDay() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurquoisePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("plan_my_day_button")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "خطط لي يومي بالذكاء الاصطناعي",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "مهام اليوم",
                    value = "$completedTasksCount / ${todayTasks.size}",
                    subtitle = if (todayTasks.isNotEmpty() && completedTasksCount == todayTasks.size) "أحسنت! اكتملت" else "${todayTasks.size - completedTasksCount} متبقية",
                    icon = Icons.Default.CheckCircleOutline,
                    iconColor = TurquoisePrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.WORK) }
                )

                StatCard(
                    title = "الرصيد المالي",
                    value = "${balance.toInt()} ج.م",
                    subtitle = "دخل ${totalIncome.toInt()} ج.م",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.FINANCE) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "فواتير مستحقة",
                    value = "${pendingBills.size}",
                    subtitle = "${pendingBills.sumOf { it.amount }.toInt()} ج.م",
                    icon = Icons.Default.ReceiptLong,
                    iconColor = if (pendingBills.isNotEmpty()) WarningOrange else SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.FAMILY) }
                )

                StatCard(
                    title = "واجبات وامتحانات",
                    value = "${pendingHw.size + exams.size}",
                    subtitle = "${pendingHw.size} واجب • ${exams.size} امتحان",
                    icon = Icons.Default.School,
                    iconColor = GoldPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.EDUCATION) }
                )
            }
        }

        // Quick Action Buttons
        item {
            HayatiCard {
                Text(
                    text = "إجراءات سريعة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        icon = Icons.Default.AddCircleOutline,
                        label = "مهمة عمل",
                        color = TurquoisePrimary,
                        onClick = { viewModel.showAddTaskDialog.value = true }
                    )
                    QuickActionButton(
                        icon = Icons.Default.AttachMoney,
                        label = "مصروف",
                        color = ErrorRed,
                        onClick = { viewModel.showAddExpenseDialog.value = true }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Receipt,
                        label = "فاتورة",
                        color = WarningOrange,
                        onClick = { viewModel.showAddBillDialog.value = true }
                    )
                    QuickActionButton(
                        icon = Icons.Default.MenuBook,
                        label = "واجب طفل",
                        color = GoldPrimary,
                        onClick = { viewModel.showAddHomeworkDialog.value = true }
                    )
                }
            }
        }

        // Today's Tasks Checklist
        item {
            SectionHeader(
                title = "مهام اليوم وأولويات العمل",
                subtitle = "${todayTasks.count { !it.isCompleted }} مهام بانتظار الإنجاز",
                actionText = "عرض الكل",
                onActionClick = { viewModel.navigateTo(Screen.WORK) },
                icon = Icons.Default.TaskAlt
            )
        }

        if (todayTasks.isEmpty()) {
            item {
                HayatiCard {
                    Text(
                        text = "لا توجد مهام عمل مجدولة لليوم. استمتع بيومك!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(todayTasks.take(4)) { task ->
                HayatiCard(
                    onClick = { viewModel.repository.toggleTaskCompletion(task.id) },
                    borderColor = if (task.isCompleted) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { viewModel.repository.toggleTaskCompletion(task.id) }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Bold,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        PriorityBadge(task.priority)
                    }
                }
            }
        }

        // Upcoming Meetings
        if (meetings.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "اجتماعات ومواعيد اليوم",
                    subtitle = "${meetings.size} مواعيد مجدولة",
                    icon = Icons.Default.Videocam
                )
            }

            items(meetings) { meeting ->
                HayatiCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meeting.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "📍 ${meeting.location} • مع ${meeting.attendees}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = meeting.time,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Education & Children Alert
        item {
            SectionHeader(
                title = "متابعة دراسة الأبناء",
                subtitle = "واجبات وامتحانات الأسبوع الحالي",
                actionText = "قسم التعليم",
                onActionClick = { viewModel.navigateTo(Screen.EDUCATION) },
                icon = Icons.Default.CastForEducation
            )
        }

        items(homework.take(2)) { hw ->
            HayatiCard(
                onClick = { viewModel.repository.toggleHomework(hw.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = hw.isCompleted,
                        onCheckedChange = { viewModel.repository.toggleHomework(hw.id) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${hw.childName} • ${hw.subject}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoisePrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = hw.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (hw.isCompleted) "منجز" else "مستحق اليوم",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hw.isCompleted) SuccessGreen else WarningOrange
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
