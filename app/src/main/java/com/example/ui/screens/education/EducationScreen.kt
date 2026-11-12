package com.example.ui.screens.education

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.ui.HayatiViewModel
import com.example.ui.components.HayatiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun EducationScreen(
    viewModel: HayatiViewModel,
    modifier: Modifier = Modifier
) {
    val children by viewModel.repository.children.collectAsState()
    val homework by viewModel.repository.homework.collectAsState()
    val exams by viewModel.repository.exams.collectAsState()

    var selectedChildId by remember { mutableStateOf(children.firstOrNull()?.id) }
    val currentChild = remember(children, selectedChildId) {
        children.find { it.id == selectedChildId } ?: children.firstOrNull()
    }

    val childHomework = remember(homework, currentChild) {
        if (currentChild == null) emptyList() else homework.filter { it.childId == currentChild.id }
    }
    val childExams = remember(exams, currentChild) {
        if (currentChild == null) emptyList() else exams.filter { it.childId == currentChild.id }
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
                        text = "تعليم الأبناء والمذاكرة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "متابعة الواجبات، جداول الامتحانات، واختبارات تفاعلية ذكية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.editingChild.value = null
                            viewModel.showAddChildDialog.value = true
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_child_profile_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طفل جديد", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { viewModel.showAddHomeworkDialog.value = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_homework_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("واجب جديد", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Child Switcher Tabs
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(children) { child ->
                    val isSelected = child.id == currentChild?.id
                    Card(
                        onClick = { selectedChildId = child.id },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSelected) TurquoisePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ),
                        modifier = Modifier.width(160.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(child.avatarEmoji, fontSize = 28.sp)
                            Column {
                                Text(
                                    text = child.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = child.grade,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Child Info & AI Quiz Generator Hero
        if (currentChild != null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ملف الطالب: ${currentChild.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "🏫 ${currentChild.school} • ${currentChild.grade}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.editingChild.value = currentChild
                                        viewModel.showAddChildDialog.value = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل الملف", tint = TurquoisePrimary)
                                }

                                if (children.size > 1) {
                                    IconButton(
                                        onClick = { viewModel.repository.deleteChild(currentChild.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الطفل", tint = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }

                        // Subjects chips
                        if (currentChild.subjects.isNotEmpty()) {
                            Text("المواد الدراسية:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(currentChild.subjects) { sub ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(sub, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // AI Quiz Launch Buttons
                        Text(
                            text = "💡 مراجعة واختبارات تفاعلية مناسبة للمرحلة الدراسية:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startSubjectQuiz("علوم", currentChild.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("start_science_quiz_button")
                            ) {
                                Text("اختبار علوم 🔬", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = { viewModel.startSubjectQuiz("رياضيات", currentChild.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("start_math_quiz_button")
                            ) {
                                Text("اختبار رياضيات 📐", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Homework Section
        item {
            SectionHeader(
                title = "الواجبات المدرسية (${childHomework.size})",
                subtitle = "ضع علامة صح عند انتهاء الطفل من أداء الواجب",
                actionText = "+ واجب",
                onActionClick = { viewModel.showAddHomeworkDialog.value = true },
                icon = Icons.Default.MenuBook
            )
        }

        if (childHomework.isEmpty()) {
            item {
                HayatiCard {
                    Text(
                        text = "لا توجد واجبات مسجلة حالياً لهذا الطالب.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(childHomework) { hw ->
                HayatiCard(
                    onClick = { viewModel.repository.toggleHomework(hw.id) },
                    borderColor = if (hw.isCompleted) SuccessGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
                                text = "مادة ${hw.subject} • ${hw.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (hw.isCompleted) FontWeight.Normal else FontWeight.Bold,
                                color = if (hw.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                            if (hw.description.isNotBlank()) {
                                Text(
                                    text = hw.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "📅 موعد التسليم: ${hw.dueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hw.dueDate <= viewModel.repository.todayDateString && !hw.isCompleted) WarningOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.repository.deleteHomework(hw.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الواجب", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }

        // Exams Section
        item {
            SectionHeader(
                title = "مواعيد الامتحانات والاختبارات (${childExams.size})",
                subtitle = "الاستعداد المبكر والمراجعة المركزة",
                actionText = "+ امتحان جديد",
                onActionClick = { viewModel.showAddExamDialog.value = true },
                icon = Icons.Default.EventNote
            )
        }

        if (childExams.isEmpty()) {
            item {
                HayatiCard {
                    Text(
                        text = "لا توجد امتحانات مجدولة حالياً لهذا الطالب.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(childExams) { exam ->
                HayatiCard(
                    borderColor = GoldAccent.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${exam.title} (${exam.subject})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "موعد الاختبار: ${exam.examDate} • الدرجة العظمى: ${exam.maxScore}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (exam.notes.isNotBlank()) {
                                Text(
                                    text = "📌 ${exam.notes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TurquoisePrimary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = exam.examDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            IconButton(
                                onClick = { viewModel.repository.deleteExam(exam.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الامتحان", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}
