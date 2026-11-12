package com.example.ui.screens.work

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.TaskStatus
import com.example.data.model.WorkTask
import com.example.ui.HayatiViewModel
import com.example.ui.components.CategoryTag
import com.example.ui.components.HayatiCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun WorkScreen(
    viewModel: HayatiViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.repository.tasks.collectAsState()
    val meetings by viewModel.repository.meetings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedPriority by remember { mutableStateOf<Priority?>(null) }
    var selectedStatus by remember { mutableStateOf<TaskStatus?>(null) }
    var showAiSummary by remember { mutableStateOf(false) }

    val categories = listOf("الكل", "مشاريع", "اجتماعات", "مبيعات", "إدارة")

    val overdueTasks = remember(tasks) {
        tasks.filter { !it.isCompleted && it.dueDate < viewModel.repository.todayDateString }
    }

    val filteredTasks = remember(tasks, searchQuery, selectedCategory, selectedPriority, selectedStatus) {
        tasks.filter { task ->
            val matchQuery = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true) || task.description.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == null || selectedCategory == "الكل" || task.category == selectedCategory
            val matchPriority = selectedPriority == null || task.priority == selectedPriority
            val matchStatus = selectedStatus == null || task.status == selectedStatus
            matchQuery && matchCategory && matchPriority && matchStatus
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مساعد العمل والمهام",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "تنظيم المشاريع، اجتماعات الفريق، والأولويات اليومية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        viewModel.editingTask.value = null
                        viewModel.showAddTaskDialog.value = true
                    },
                    modifier = Modifier.testTag("add_work_task_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مهمة جديدة")
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث في مهام العمل...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("work_search_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Categories & Priority & Status Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = (selectedCategory == null && cat == "الكل") || selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = if (cat == "الكل") null else cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedPriority == Priority.HIGH,
                        onClick = { selectedPriority = if (selectedPriority == Priority.HIGH) null else Priority.HIGH },
                        label = { Text("أولوية عالية 🔥", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedPriority == Priority.MEDIUM,
                        onClick = { selectedPriority = if (selectedPriority == Priority.MEDIUM) null else Priority.MEDIUM },
                        label = { Text("أولوية متوسطة", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedStatus == TaskStatus.COMPLETED,
                        onClick = { selectedStatus = if (selectedStatus == TaskStatus.COMPLETED) null else TaskStatus.COMPLETED },
                        label = { Text("المكتملة ✓", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Overdue Alert Banner if any
        if (overdueTasks.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ErrorRed.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تنبيه: لديك ${overdueTasks.size} مهام تجاوزت موعد الاستحقاق!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                            Text(
                                text = overdueTasks.joinToString("، ") { "${it.title} (${it.dueDate})" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // AI Summary & Prioritization Card
        item {
            HayatiCard(
                borderColor = TurquoisePrimary.copy(alpha = 0.4f),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TurquoisePrimary)
                        Text(
                            text = "تحليل الأولويات بالذكاء الاصطناعي",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = { showAiSummary = !showAiSummary }) {
                        Text(if (showAiSummary) "إخفاء" else "عرض التوصيات")
                    }
                }

                AnimatedVisibility(visible = showAiSummary) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡 توصيات تنظيم يوم العمل:\n1. ابدأ فوراً بإنجاز «تسليم التقرير المالي» لأنه ذو أولوية قصوى ومستحق اليوم.\n2. خصص 30 دقيقة لمكالمة العميل لمناقشة التصاميم قبل اجتماع الظهر.\n3. أعد جدولة كشوف الرواتب الشهرية المتأخرة لتفادي تراكم المستحقات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Tasks List
        item {
            SectionHeader(
                title = "قائمة المهام (${filteredTasks.size})",
                subtitle = "ضع علامة للاكتمال أو اضغط على أيقونة التعديل"
            )
        }

        items(filteredTasks) { task ->
            val isOverdue = !task.isCompleted && task.dueDate < viewModel.repository.todayDateString

            HayatiCard(
                borderColor = if (isOverdue) ErrorRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { viewModel.repository.toggleTaskCompletion(task.id) }
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Bold,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (task.description.isNotBlank()) {
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CategoryTag(task.category)
                            Text(
                                text = if (isOverdue) "⚠️ متأخرة: ${task.dueDate}" else "📅 ${task.dueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                                color = if (isOverdue) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PriorityBadge(task.priority)

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    viewModel.editingTask.value = task
                                    viewModel.showAddTaskDialog.value = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "تعديل المهمة",
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.repository.deleteTask(task.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "حذف المهمة",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Meetings section
        item {
            SectionHeader(
                title = "مواعيد واجتماعات العمل",
                subtitle = "مذكرات الاجتماع وبنود العمل المتابعة",
                icon = Icons.Default.Groups
            )
        }

        items(meetings) { meeting ->
            HayatiCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = meeting.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TurquoisePrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${meeting.date} • ${meeting.time}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurquoisePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "👥 الحاضرون: ${meeting.attendees}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "📝 ملاحظات: ${meeting.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (meeting.actionItems.isNotEmpty()) {
                        Text(
                            text = "بنود المتابعة:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        meeting.actionItems.forEach { item ->
                            Text(
                                text = "• $item",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
