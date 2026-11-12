package com.example.ui.screens.reports

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HayatiViewModel
import com.example.ui.components.HayatiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.theme.*

@Composable
fun ReportsScreen(
    viewModel: HayatiViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.repository.tasks.collectAsState()
    val homework by viewModel.repository.homework.collectAsState()
    val bills by viewModel.repository.bills.collectAsState()
    val transactions by viewModel.repository.transactions.collectAsState()

    val completedTasks = remember(tasks) { tasks.count { it.isCompleted } }
    val taskCompletionRate = remember(tasks) {
        if (tasks.isNotEmpty()) (completedTasks.toFloat() / tasks.size * 100).toInt() else 100
    }

    val totalIncome = remember(transactions) { viewModel.repository.calculateTotalIncome() }
    val totalExpenses = remember(transactions) { viewModel.repository.calculateTotalExpenses() }
    val balance = remember(transactions) { viewModel.repository.calculateBalance() }
    val categoryExpenses = remember(transactions) { viewModel.repository.calculateExpensesByCategory() }

    val completedHw = remember(homework) { homework.count { it.isCompleted } }
    val paidBills = remember(bills) { bills.count { it.isPaid } }

    var showExportSnackbar by remember { mutableStateOf(false) }

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
                        text = "التقارير والإحصائيات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "مؤشرات الإنتاجية، الأداء المالي، ومتابعة الأبناء",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showExportSnackbar = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير التقرير")
                }
            }
        }

        if (showExportSnackbar) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        Text(
                            text = "تم تجهيز التقرير الشامل لشهر نوفمبر بصيغة PDF وجاهز للطباعة والمشاركة!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Productivity Card
        item {
            HayatiCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مؤشر الإنتاجية وإنجاز المهام",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$taskCompletionRate%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TurquoisePrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { taskCompletionRate / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = TurquoisePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Text(
                        text = "تم إنجاز $completedTasks من أصل ${tasks.size} مهام عمل مسجلة في التطبيق.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Key Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "الفواتير المسددة",
                    value = "$paidBills / ${bills.size}",
                    subtitle = "${(if (bills.isNotEmpty()) paidBills.toFloat() / bills.size * 100 else 100).toInt()}% مسدد",
                    icon = Icons.Default.Receipt,
                    iconColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "واجبات منجزة للأبناء",
                    value = "$completedHw / ${homework.size}",
                    subtitle = "متابعة دراسية ممتازة",
                    icon = Icons.Default.School,
                    iconColor = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Financial Spending Distribution
        item {
            SectionHeader(
                title = "توزيع الإنفاق حسب التصنيفات",
                subtitle = "تحليل المصروفات الفعلية المسجلة",
                icon = Icons.Default.PieChart
            )
        }

        items(categoryExpenses.toList()) { (category, amount) ->
            val percentage = if (totalExpenses > 0) (amount / totalExpenses * 100).toInt() else 0

            HayatiCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${amount.toInt()} ج.م ($percentage%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TurquoisePrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = TurquoisePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
