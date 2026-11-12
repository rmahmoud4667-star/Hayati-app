package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.HayatiViewModel
import com.example.ui.theme.*

@Composable
fun AddOrEditTaskDialog(
    viewModel: HayatiViewModel,
    taskToEdit: WorkTask? = null,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var category by remember { mutableStateOf(taskToEdit?.category ?: "مشاريع") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: Priority.MEDIUM) }
    var status by remember { mutableStateOf(taskToEdit?.status ?: TaskStatus.PENDING) }
    var dueDate by remember { mutableStateOf(taskToEdit?.dueDate ?: viewModel.repository.todayDateString) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("مشاريع", "اجتماعات", "مبيعات", "إدارة", "أخرى")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (taskToEdit == null) "إضافة مهمة عمل جديدة" else "تعديل مهمة العمل",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("عنوان المهمة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("التفاصيل والملاحظات") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("task_desc_input")
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("تاريخ الاستحقاق (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_date_input")
                )

                // Category chips
                Text(
                    text = "التصنيف:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Priority
                Text(
                    text = "الأولوية:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.values().forEach { p ->
                        OutlinedButton(
                            onClick = { priority = p },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (priority == p) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(p.titleAr, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "يرجى كتابة عنوان المهمة"
                                return@Button
                            }
                            if (taskToEdit == null) {
                                viewModel.repository.addTask(
                                    WorkTask(
                                        title = title.trim(),
                                        description = description.trim(),
                                        category = category,
                                        priority = priority,
                                        dueDate = dueDate.trim(),
                                        status = status
                                    )
                                )
                            } else {
                                viewModel.repository.updateTask(
                                    taskToEdit.copy(
                                        title = title.trim(),
                                        description = description.trim(),
                                        category = category,
                                        priority = priority,
                                        dueDate = dueDate.trim(),
                                        status = status
                                    )
                                )
                            }
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_task_button")
                    ) {
                        Text(if (taskToEdit == null) "حفظ المهمة" else "تحديث المهمة")
                    }
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    viewModel: HayatiViewModel,
    onDismiss: () -> Unit
) {
    var isExpense by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("طعام وبقالة") }
    var paymentMethod by remember { mutableStateOf("نقدي") }
    var dateText by remember { mutableStateOf(viewModel.repository.todayDateString) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val expenseCategories = listOf("طعام وبقالة", "المنزل", "تعليم ومدارس", "مواصلات وبنزين", "فواتير", "صحة وأدوية", "تسوق", "ترفيه", "أخرى")
    val incomeCategories = listOf("راتب شهري", "عمل حر وتجارة", "استثمارات", "دخل إضافي")
    val paymentMethods = listOf("نقدي", "بطاقة بنكية", "فودافون كاش / محفظة", "تحويل بنكي")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (isExpense) "تسجيل مصروف جديد" else "تسجيل دخل جديد",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage!!,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Type Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isExpense = true
                                category = "طعام وبقالة"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isExpense) ErrorRed else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("مصروف (-)")
                        }
                        Button(
                            onClick = {
                                isExpense = false
                                category = "عمل حر وتجارة"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isExpense) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("دخل (+)")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            if (it.toDoubleOrNull() != null && it.toDouble() > 0) errorMessage = null
                        },
                        label = { Text("المبلغ (ج.م) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            if (it.isNotBlank()) errorMessage = null
                        },
                        label = { Text("الوصف / البيان *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("transaction_desc_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("تاريخ المعاملة (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "التصنيف:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    val currentList = if (isExpense) expenseCategories else incomeCategories
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentList.take(3).forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "طريقة الدفع:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentMethods.take(3).forEach { pm ->
                            FilterChip(
                                selected = paymentMethod == pm,
                                onClick = { paymentMethod = pm },
                                label = { Text(pm, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull()
                                if (amount == null || amount <= 0) {
                                    errorMessage = "يرجى إدخال مبلغ صحيح أكبر من الصفر"
                                    return@Button
                                }
                                if (description.isBlank()) {
                                    errorMessage = "يرجى كتابة وصف المعاملة"
                                    return@Button
                                }
                                viewModel.repository.addTransaction(
                                    FinancialTransaction(
                                        type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                                        amount = amount,
                                        date = dateText.trim(),
                                        category = category,
                                        description = description.trim(),
                                        paymentMethod = paymentMethod
                                    )
                                )
                                onDismiss()
                            },
                            modifier = Modifier.testTag("save_transaction_button")
                        ) {
                            Text("حفظ المعاملة")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddBillDialog(
    viewModel: HayatiViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("2026-11-20") }
    var billNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickBills = listOf("كهرباء", "مياه", "غاز", "إنترنت منزلي", "صيانة عمارة", "اشتراك موبايل")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إضافة فاتورة / التزام دوري",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Quick selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickBills.take(3).forEach { qb ->
                        FilterChip(
                            selected = name.contains(qb),
                            onClick = { name = "فاتورة $qb" },
                            label = { Text(qb, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("اسم الفاتورة أو الخدمة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bill_name_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (it.toDoubleOrNull() != null && it.toDouble() > 0) errorMessage = null
                    },
                    label = { Text("المبلغ التقديري (ج.م) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bill_amount_input")
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("تاريخ الاستحقاق (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (name.isBlank()) {
                                errorMessage = "يرجى إدخال اسم الفاتورة"
                                return@Button
                            }
                            if (amount == null || amount <= 0) {
                                errorMessage = "يرجى إدخال مبلغ صحيح أكبر من الصفر"
                                return@Button
                            }
                            viewModel.repository.addBill(
                                UtilityBill(
                                    name = name.trim(),
                                    amount = amount,
                                    dueDate = dueDate.trim(),
                                    billNumber = billNumber.trim()
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_bill_button")
                    ) {
                        Text("حفظ الفاتورة")
                    }
                }
            }
        }
    }
}

@Composable
fun AddHomeworkDialog(
    viewModel: HayatiViewModel,
    onDismiss: () -> Unit
) {
    val children by viewModel.repository.children.collectAsState()
    var selectedChild by remember { mutableStateOf(children.firstOrNull()) }
    var subject by remember { mutableStateOf("رياضيات") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(viewModel.repository.todayDateString) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إضافة واجب مدرسي للأبناء",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Select child
                Text("اختر الابن:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    children.forEach { child ->
                        FilterChip(
                            selected = selectedChild?.id == child.id,
                            onClick = { selectedChild = child },
                            label = { Text("${child.avatarEmoji} ${child.name}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("المادة الدراسية *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("hw_subject_input")
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("عنوان الواجب أو الصفحات *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("hw_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل أو إرشادات") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val child = selectedChild ?: children.firstOrNull()
                            if (child == null) {
                                errorMessage = "يرجى إضافة طفل أولاً"
                                return@Button
                            }
                            if (title.isBlank()) {
                                errorMessage = "يرجى كتابة عنوان الواجب"
                                return@Button
                            }
                            viewModel.repository.addHomework(
                                Homework(
                                    childId = child.id,
                                    childName = child.name,
                                    subject = subject.trim(),
                                    title = title.trim(),
                                    description = description.trim(),
                                    dueDate = dueDate.trim()
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_hw_button")
                    ) {
                        Text("حفظ الواجب")
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrEditChildDialog(
    viewModel: HayatiViewModel,
    childToEdit: ChildProfile? = null,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(childToEdit?.name ?: "") }
    var grade by remember { mutableStateOf(childToEdit?.grade ?: "الصف الرابع الابتدائي") }
    var school by remember { mutableStateOf(childToEdit?.school ?: "مدرسة النيل المصرية") }
    var avatarEmoji by remember { mutableStateOf(childToEdit?.avatarEmoji ?: "👦") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emojis = listOf("👦", "👧", "🧒", "🎒", "⭐")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (childToEdit == null) "إضافة ملف ابن جديد" else "تعديل ملف الابن",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Avatar selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (avatarEmoji == emoji) TurquoisePrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { avatarEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("اسم الطفل *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("child_name_input")
                )

                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("المرحلة الدراسية / الصف *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("child_grade_input")
                )

                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it },
                    label = { Text("اسم المدرسة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "يرجى كتابة اسم الطفل"
                                return@Button
                            }
                            if (childToEdit == null) {
                                viewModel.repository.addChild(
                                    ChildProfile(
                                        name = name.trim(),
                                        grade = grade.trim(),
                                        school = school.trim(),
                                        avatarEmoji = avatarEmoji,
                                        subjects = listOf("رياضيات", "علوم", "لغة عربية", "لغة إنجليزية")
                                    )
                                )
                            } else {
                                viewModel.repository.updateChild(
                                    childToEdit.copy(
                                        name = name.trim(),
                                        grade = grade.trim(),
                                        school = school.trim(),
                                        avatarEmoji = avatarEmoji
                                    )
                                )
                            }
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_child_button")
                    ) {
                        Text(if (childToEdit == null) "إضافة الملف" else "حفظ التعديل")
                    }
                }
            }
        }
    }
}

@Composable
fun AddExamDialog(
    viewModel: HayatiViewModel,
    onDismiss: () -> Unit
) {
    val children by viewModel.repository.children.collectAsState()
    var selectedChild by remember { mutableStateOf(children.firstOrNull()) }
    var subject by remember { mutableStateOf("علوم") }
    var title by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("2026-11-20") }
    var maxScoreText by remember { mutableStateOf("30") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إضافة موعد امتحان جديد",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Select child
                Text("اختر الابن:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    children.forEach { child ->
                        FilterChip(
                            selected = selectedChild?.id == child.id,
                            onClick = { selectedChild = child },
                            label = { Text("${child.avatarEmoji} ${child.name}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("المادة الدراسية *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("exam_subject_input")
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("عنوان الامتحان أو نطاق الوحدات *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("exam_title_input")
                )

                OutlinedTextField(
                    value = examDate,
                    onValueChange = { examDate = it },
                    label = { Text("تاريخ الامتحان (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val child = selectedChild ?: children.firstOrNull()
                            if (child == null) {
                                errorMessage = "يرجى اختيار الابن"
                                return@Button
                            }
                            if (title.isBlank()) {
                                errorMessage = "يرجى كتابة عنوان الامتحان"
                                return@Button
                            }
                            viewModel.repository.addExam(
                                Exam(
                                    childId = child.id,
                                    childName = child.name,
                                    subject = subject.trim(),
                                    title = title.trim(),
                                    examDate = examDate.trim(),
                                    maxScore = maxScoreText.toIntOrNull() ?: 30,
                                    notes = notes.trim()
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_exam_button")
                    ) {
                        Text("حفظ الامتحان")
                    }
                }
            }
        }
    }
}

@Composable
fun AddHouseholdTaskDialog(
    viewModel: HayatiViewModel,
    onDismiss: () -> Unit
) {
    val members by viewModel.repository.familyMembers.collectAsState()
    var title by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf("الجميع") }
    var dueDate by remember { mutableStateOf(viewModel.repository.todayDateString) }
    var recurring by remember { mutableStateOf("أسبوعي") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إضافة مهمة منزلية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("وصف المهمة المنزلية *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("household_task_title_input")
                )

                Text("المكلف بالمهمة:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.take(3).forEach { m ->
                        FilterChip(
                            selected = assignedTo == m.name,
                            onClick = { assignedTo = m.name },
                            label = { Text(m.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "يرجى كتابة وصف المهمة"
                                return@Button
                            }
                            viewModel.repository.addHouseholdTask(
                                HouseholdTask(
                                    title = title.trim(),
                                    assignedToName = assignedTo,
                                    dueDate = dueDate,
                                    recurring = recurring
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_household_task_button")
                    ) {
                        Text("حفظ المهمة")
                    }
                }
            }
        }
    }
}

@Composable
fun AddOccasionDialog(
    viewModel: HayatiViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-12-01") }
    var type by remember { mutableStateOf("عيد ميلاد") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val types = listOf("عيد ميلاد", "ذكرى زواج", "مناسبة عائلية", "حفل مدرسي")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إضافة مناسبة عائلية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("اسم المناسبة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("occasion_title_input")
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("التاريخ (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "يرجى كتابة اسم المناسبة"
                                return@Button
                            }
                            viewModel.repository.addOccasion(
                                FamilyOccasion(
                                    title = title.trim(),
                                    date = date.trim(),
                                    type = type,
                                    notes = notes.trim()
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_occasion_button")
                    ) {
                        Text("حفظ المناسبة")
                    }
                }
            }
        }
    }
}

@Composable
fun DailyPlanDialog(
    plan: DailyPlan?,
    onDismiss: () -> Unit
) {
    if (plan == null) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .heightIn(max = 650.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "خطة اليوم الذكية",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = plan.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = plan.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(plan.timeBlocks) { index, block ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TurquoisePrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = block.timeSlot,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoisePrimary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = block.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = block.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تم، اعتماد خطة اليوم")
                }
            }
        }
    }
}

@Composable
fun InteractiveQuizDialog(
    quiz: StudyQuiz?,
    onDismiss: () -> Unit
) {
    if (quiz == null) return

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var isQuizCompleted by remember { mutableStateOf(false) }

    val currentQ = quiz.questions.getOrNull(currentQuestionIndex)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "اختبار ذكي: ${quiz.subject}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = quiz.topic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isQuizCompleted && currentQ != null) {
                    // Lesson brief
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 ملخص سريع: ${quiz.lessonSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "السؤال ${currentQuestionIndex + 1} من ${quiz.questions.size}:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = currentQ.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Options
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentQ.options.forEachIndexed { idx, opt ->
                            val isSelected = selectedOptionIndex == idx
                            val isCorrect = idx == currentQ.correctIndex

                            val borderColor = when {
                                isSubmitted && isCorrect -> SuccessGreen
                                isSubmitted && isSelected && !isCorrect -> ErrorRed
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }

                            val bgColor = when {
                                isSubmitted && isCorrect -> SuccessGreen.copy(alpha = 0.12f)
                                isSubmitted && isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.12f)
                                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.surface
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                    .background(bgColor)
                                    .clickable(enabled = !isSubmitted) { selectedOptionIndex = idx }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (isSubmitted) {
                                    if (isCorrect) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                    } else if (isSelected) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorRed)
                                    }
                                }
                            }
                        }

                        // Explanation after submit
                        if (isSubmitted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedOptionIndex == currentQ.correctIndex) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = currentQ.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الدرجة الحالية: $score / ${quiz.questions.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!isSubmitted) {
                            Button(
                                onClick = {
                                    if (selectedOptionIndex != null) {
                                        isSubmitted = true
                                        if (selectedOptionIndex == currentQ.correctIndex) {
                                            score += 1
                                        }
                                    }
                                },
                                enabled = selectedOptionIndex != null
                            ) {
                                Text("تحقق من الإجابة")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (currentQuestionIndex + 1 < quiz.questions.size) {
                                        currentQuestionIndex += 1
                                        selectedOptionIndex = null
                                        isSubmitted = false
                                    } else {
                                        isQuizCompleted = true
                                    }
                                }
                            ) {
                                Text(if (currentQuestionIndex + 1 < quiz.questions.size) "السؤال التالي" else "عرض النتيجة النهائية")
                            }
                        }
                    }
                } else {
                    // Result Screen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (score == quiz.questions.size) "🎉 ممتاز جداً!" else "👏 محاولة رائعة!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "حصلت على $score من أصل ${quiz.questions.size} أسئلة",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "استمر في المراجعة اليومية لتحقيق أعلى الدرجات في امتحانات الشهر!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إغلاق الاختبار")
                    }
                }
            }
        }
    }
}

@Composable
fun EditBudgetLimitDialog(
    categoryBudget: CategoryBudget,
    currentSpent: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var limitText by remember { mutableStateOf(categoryBudget.limitAmount.toInt().toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickPresets = listOf(1000, 2000, 3000, 5000, 8000, 10000)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(TurquoisePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "سقف الإنفاق الشهري",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "فئة: ${categoryBudget.categoryName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TurquoisePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Current Spent Status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المصروف الفعلي هذا الشهر:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currentSpent.toInt()} ج.م",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = it.filter { ch -> ch.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("الحد الأقصى للإنفاق (ج.م) *") },
                    singleLine = true,
                    supportingText = { Text("المبلغ المسموح بصرفه شهرياً قبل إطلاق التنبيهات") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Quick Presets
                Text(
                    text = "تعيين سريع للمبلغ:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.take(4).forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                limitText = preset.toString()
                                errorMessage = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$preset", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            val parsed = limitText.toDoubleOrNull()
                            if (parsed == null || parsed <= 0) {
                                errorMessage = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                            } else {
                                onSave(parsed)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_budget_limit_button")
                    ) {
                        Text("حفظ السقف")
                    }
                }
            }
        }
    }
}

@Composable
fun EditFamilyMemberDialog(
    member: FamilyMember,
    onSave: (FamilyMember) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(member.name) }
    var role by remember { mutableStateOf(member.role) }
    var notes by remember { mutableStateOf(member.notes) }
    var avatarEmoji by remember { mutableStateOf(member.avatarEmoji) }
    var phone by remember { mutableStateOf(member.phone) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emojiOptions = listOf("👨‍💼", "👩‍🏫", "👧", "👦", "👶", "👵", "🧓", "👤")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "تعديل بيانات فرد الأسرة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Emoji Picker
                Text(
                    text = "الرمز التعبيري:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emojiOptions.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (avatarEmoji == emoji) TurquoisePrimary.copy(alpha = 0.25f) else Color.Transparent)
                                .border(
                                    width = if (avatarEmoji == emoji) 2.dp else 0.5.dp,
                                    color = if (avatarEmoji == emoji) TurquoisePrimary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable { avatarEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("الاسم *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("family_name_input")
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("الدور في الأسرة (مثل: الأب، الأم، الابنة)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("family_role_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات أو الوصف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "يرجى إدخال اسم فرد الأسرة"
                            } else {
                                onSave(
                                    member.copy(
                                        name = name.trim(),
                                        role = role.trim(),
                                        notes = notes.trim(),
                                        avatarEmoji = avatarEmoji,
                                        phone = phone.trim()
                                    )
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_family_member_button")
                    ) {
                        Text("حفظ التعديل")
                    }
                }
            }
        }
    }
}

