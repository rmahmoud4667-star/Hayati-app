package com.example.ui.screens.finance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.TransactionType
import com.example.ui.HayatiViewModel
import com.example.ui.components.HayatiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    viewModel: HayatiViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.repository.transactions.collectAsState()
    val budgets by viewModel.repository.budgets.collectAsState()
    val installments by viewModel.repository.installments.collectAsState()

    val totalIncome = remember(transactions) { viewModel.repository.calculateTotalIncome() }
    val totalExpenses = remember(transactions) { viewModel.repository.calculateTotalExpenses() }
    val balance = remember(transactions) { viewModel.repository.calculateBalance() }
    val categoryExpenses = remember(transactions) { viewModel.repository.calculateExpensesByCategory() }

    var selectedTab by remember { mutableStateOf(0) } // 0: المعاملات, 1: سقوف وتنبيهات الإنفاق, 2: الأقساط
    var txSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    // Categories in warning or exceeded state
    val exceededBudgets = remember(budgets, categoryExpenses) {
        budgets.filter { b ->
            val spent = categoryExpenses[b.categoryName] ?: 0.0
            spent > b.limitAmount
        }
    }

    val warningBudgets = remember(budgets, categoryExpenses) {
        budgets.filter { b ->
            val spent = categoryExpenses[b.categoryName] ?: 0.0
            spent <= b.limitAmount && b.limitAmount > 0 && (spent / b.limitAmount) >= 0.8
        }
    }

    // Days remaining in current month (November has 30 days, today is 11 or 12)
    val daysRemainingInMonth = 19

    fun getCategoryIcon(name: String): String = when (name) {
        "طعام وبقالة" -> "🛒"
        "تعليم ومدارس" -> "🎓"
        "المنزل" -> "🏠"
        "مواصلات وبنزين" -> "🚗"
        "فواتير" -> "⚡"
        "صحة وأدوية" -> "💊"
        "ترفيه" -> "🎬"
        "تسوق" -> "🛍️"
        else -> "📦"
    }

    val filteredTransactions = remember(transactions, txSearchQuery, selectedCategoryFilter) {
        transactions.filter { tx ->
            val matchesSearch = txSearchQuery.isBlank() ||
                    tx.description.contains(txSearchQuery, ignoreCase = true) ||
                    tx.category.contains(txSearchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == null || tx.category == selectedCategoryFilter
            matchesSearch && matchesCategory
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الإدارة المالية والميزانية",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "حسابات دقيقة 100% بالجنيه المصري (EGP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.showAddExpenseDialog.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    modifier = Modifier.testTag("add_finance_transaction_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاملة جديدة")
                }
            }
        }

        // Financial Overview Summary Card
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ملخص شهر نوفمبر 2026",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الرصيد المالي المتبقي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${balance.toInt()} ج.م",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) SuccessGreen else ErrorRed
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(TurquoisePrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (totalIncome > 0) "معدل الصرف: ${(totalExpenses / totalIncome * 100).toInt()}%" else "0%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TurquoisePrimary
                            )
                        }
                    }

                    // Income & Expense details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SuccessGreen.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("إجمالي الدخل (+)", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                Text("${totalIncome.toInt()} ج.م", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ErrorRed.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("إجمالي المصاريف (-)", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                                Text("${totalExpenses.toInt()} ج.م", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ErrorRed)
                            }
                        }
                    }
                }
            }
        }

        // Sub Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TurquoisePrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("المعاملات (${transactions.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("سقوف الإنفاق")
                            if (exceededBudgets.isNotEmpty() || warningBudgets.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (exceededBudgets.isNotEmpty()) ErrorRed else WarningOrange)
                                )
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("الأقساط (${installments.size})") }
                )
            }
        }

        // ==========================================
        // TAB 0: TRANSACTIONS LIST
        // ==========================================
        if (selectedTab == 0) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = txSearchQuery,
                        onValueChange = { txSearchQuery = it },
                        placeholder = { Text("بحث في المعاملات أو المتاجر...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (txSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { txSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("transaction_search_input")
                    )

                    // Category Filter Chips
                    val categories = listOf("الكل") + budgets.map { it.categoryName }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = (cat == "الكل" && selectedCategoryFilter == null) || (cat == selectedCategoryFilter)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryFilter = if (cat == "الكل") null else cat
                                },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurquoisePrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = TurquoisePrimary
                                )
                            )
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    HayatiCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🔍", fontSize = 32.sp)
                            Text(
                                text = "لا توجد معاملات مطابقة للبحث",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "جرب تغيير كلمات البحث أو اختر فئة أخرى",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    HayatiCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (tx.type == TransactionType.INCOME) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.type == TransactionType.INCOME) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (tx.type == TransactionType.INCOME) SuccessGreen else ErrorRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = tx.description,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${tx.category} • ${tx.paymentMethod} • ${tx.date}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${if (tx.type == TransactionType.INCOME) "+" else "-"}${tx.amount.toInt()} ج.م",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == TransactionType.INCOME) SuccessGreen else ErrorRed
                                )
                                IconButton(
                                    onClick = { viewModel.repository.deleteTransaction(tx.id) },
                                    modifier = Modifier.size(28.dp).testTag("delete_tx_${tx.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // TAB 1: SPENDING CAPS & BUDGET ALERTS
        // ==========================================
        if (selectedTab == 1) {
            // Priority Alert Banner
            if (exceededBudgets.isNotEmpty() || warningBudgets.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (exceededBudgets.isNotEmpty()) ErrorRed.copy(alpha = 0.12f) else WarningOrange.copy(alpha = 0.12f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (exceededBudgets.isNotEmpty()) ErrorRed else WarningOrange
                            )
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("budget_alert_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (exceededBudgets.isNotEmpty()) Icons.Default.Warning else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (exceededBudgets.isNotEmpty()) ErrorRed else WarningOrange
                                )
                                Text(
                                    text = if (exceededBudgets.isNotEmpty()) "تنبيه هام: تم تجاوز سقف الإنفاق في ${exceededBudgets.size} فئات!" else "تنبيه: اقتراب شديد من الحد الأقصى في ${warningBudgets.size} فئات",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (exceededBudgets.isNotEmpty()) ErrorRed else WarningOrange
                                )
                            }
                            Text(
                                text = if (exceededBudgets.isNotEmpty())
                                    "الفئات المتجاوزة: ${exceededBudgets.joinToString("، ") { it.categoryName }}. يرجى ترشيد الاستهلاك أو رفع سقف الإنفاق."
                                else
                                    "الفئات التي قاربت على النفاد (أكثر من 80%): ${warningBudgets.joinToString("، ") { it.categoryName }}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "سقوف الإنفاق الشهري لكل فئة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "اضغط على أي فئة لتعديل الحد الأقصى والحساب اليومي",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(budgets, key = { it.categoryName }) { budget ->
                val spent = categoryExpenses[budget.categoryName] ?: 0.0
                val ratio = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                val isExceeded = spent > budget.limitAmount
                val isWarning = !isExceeded && ratio >= 0.8f
                val remainingAmount = budget.limitAmount - spent
                val dailyAllowance = if (remainingAmount > 0) remainingAmount / daysRemainingInMonth else 0.0

                HayatiCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isExceeded) ErrorRed.copy(alpha = 0.15f)
                                            else if (isWarning) WarningOrange.copy(alpha = 0.15f)
                                            else TurquoisePrimary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(getCategoryIcon(budget.categoryName), fontSize = 18.sp)
                                }

                                Column {
                                    Text(
                                        text = budget.categoryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "السقف: ${budget.limitAmount.toInt()} ج.م / شهر",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Alert Badge
                            val badgeColor = when {
                                isExceeded -> ErrorRed
                                isWarning -> WarningOrange
                                else -> SuccessGreen
                            }
                            val badgeText = when {
                                isExceeded -> "🚨 متجاوز"
                                isWarning -> "⚠️ ${(ratio * 100).toInt()}%"
                                else -> "✅ ${(ratio * 100).toInt()}%"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = when {
                                isExceeded -> ErrorRed
                                isWarning -> WarningOrange
                                else -> TurquoisePrimary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Numerical details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المصروف: ${spent.toInt()} ج.م",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (isExceeded)
                                    "تجاوز بـ ${(spent - budget.limitAmount).toInt()} ج.م"
                                else
                                    "المتبقي: ${remainingAmount.toInt()} ج.م",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeded) ErrorRed else SuccessGreen
                            )
                        }

                        // Safe daily spending advice
                        if (!isExceeded && remainingAmount > 0) {
                            Text(
                                text = "💡 المتاح للإنفاق الآمن: قرابة ${dailyAllowance.toInt()} ج.م يومياً حتى نهاية الشهر",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isExceeded) {
                            Text(
                                text = "🚨 تم استنفاد السقف بالكامل لهذا الشهر. يرجى توقيف المصاريف غير الضرورية.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorRed
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Edit Limit Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.editingBudget.value = budget
                                    viewModel.showEditBudgetDialog.value = true
                                },
                                modifier = Modifier.testTag("edit_budget_${budget.categoryName}")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل سقف الإنفاق")
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // TAB 2: INSTALLMENTS & DEBTS
        // ==========================================
        if (selectedTab == 2) {
            item {
                SectionHeader(
                    title = "الأقساط والالتزامات المالية الدورية",
                    subtitle = "مواعيد استحقاق الأقساط البنكية والتمويلية"
                )
            }

            items(installments) { ins ->
                val remaining = ins.totalAmount - ins.paidAmount
                val progress = if (ins.totalAmount > 0) (ins.paidAmount / ins.totalAmount).toFloat() else 0f

                HayatiCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = ins.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "يوم ${ins.dueDayOfMonth} من كل شهر • ${ins.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${ins.monthlyPayment.toInt()} ج.م/شهر",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TurquoisePrimary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = TurquoisePrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تم سداد: ${ins.paidAmount.toInt()} ج.م",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen
                            )
                            Text(
                                text = "المتبقي: ${remaining.toInt()} ج.م",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
