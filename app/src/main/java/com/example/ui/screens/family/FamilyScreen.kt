package com.example.ui.screens.family

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
import com.example.data.model.FamilyMember
import com.example.data.model.ShoppingItem
import com.example.data.model.UtilityBill
import com.example.ui.HayatiViewModel
import com.example.ui.components.HayatiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun FamilyScreen(
    viewModel: HayatiViewModel,
    modifier: Modifier = Modifier
) {
    val familyMembers by viewModel.repository.familyMembers.collectAsState()
    val householdTasks by viewModel.repository.householdTasks.collectAsState()
    val shoppingList by viewModel.repository.shoppingList.collectAsState()
    val bills by viewModel.repository.bills.collectAsState()
    val maintenance by viewModel.repository.maintenance.collectAsState()
    val occasions by viewModel.repository.occasions.collectAsState()

    var newShoppingName by remember { mutableStateOf("") }
    var newShoppingPrice by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: الكل, 1: المشتريات, 2: الفواتير, 3: مهام المنزل والمناسبات
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }

    val totalShoppingCost = remember(shoppingList) {
        shoppingList.sumOf { it.estimatedPrice }
    }
    val unpaidBillsTotal = remember(bills) {
        bills.filter { !it.isPaid }.sumOf { it.amount }
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
            Column {
                Text(
                    text = "المنزل والعائلة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ملفات أفراد الأسرة، قائمة المشتريات، وفواتير الخدمات المنزلية",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Family Members Carousel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "ملف تعريف العائلة وأفراد الأسرة (${familyMembers.size})",
                    subtitle = "المسؤوليات المشتركة والتواصل العائلي"
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(familyMembers) { member ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .width(135.dp)
                            .clickable { editingMember = member }
                            .testTag("family_member_${member.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(TurquoisePrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(member.avatarEmoji, fontSize = 24.sp)
                            }
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = member.role,
                                style = MaterialTheme.typography.labelSmall,
                                color = TurquoisePrimary
                            )
                            Text(
                                text = "تعديل ✎",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Segment Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TurquoisePrimary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("نظرة عامة") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("المشتريات") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("الفواتير") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("المهام والمناسبات") })
            }
        }

        // SHOPPING LIST SECTION
        if (selectedTab == 0 || selectedTab == 1) {
            item {
                SectionHeader(
                    title = "قائمة المشتريات والسوبرماركت (${shoppingList.size})",
                    subtitle = "الإجمالي التقديري: ${totalShoppingCost.toInt()} ج.م",
                    icon = Icons.Default.ShoppingCart
                )
            }

            // Quick add item field
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newShoppingName,
                        onValueChange = { newShoppingName = it },
                        placeholder = { Text("اسم السلعة (مثلاً: جبنة، عصير...)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("shopping_item_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newShoppingPrice,
                        onValueChange = { newShoppingPrice = it },
                        placeholder = { Text("السعر") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("shopping_item_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(
                        onClick = {
                            if (newShoppingName.isNotBlank()) {
                                viewModel.repository.addShoppingItem(
                                    ShoppingItem(
                                        name = newShoppingName.trim(),
                                        estimatedPrice = newShoppingPrice.toDoubleOrNull() ?: 0.0
                                    )
                                )
                                newShoppingName = ""
                                newShoppingPrice = ""
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(TurquoisePrimary)
                            .testTag("add_shopping_item_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة", tint = Color.White)
                    }
                }
            }

            items(shoppingList) { item ->
                HayatiCard(
                    onClick = { viewModel.repository.toggleShoppingItem(item.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = item.isPurchased,
                            onCheckedChange = { viewModel.repository.toggleShoppingItem(item.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (item.isPurchased) FontWeight.Normal else FontWeight.SemiBold,
                                color = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                            if (item.category.isNotBlank()) {
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (item.estimatedPrice > 0) {
                            Text(
                                text = "${item.estimatedPrice.toInt()} ج.م",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TurquoisePrimary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.repository.deleteShoppingItem(item.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "حذف", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }

        // BILLS SECTION
        if (selectedTab == 0 || selectedTab == 2) {
            item {
                SectionHeader(
                    title = "فواتير الخدمات المنزلية",
                    subtitle = "المستحق دفعه: ${unpaidBillsTotal.toInt()} ج.م",
                    actionText = "+ فاتورة جديدة",
                    onActionClick = { viewModel.showAddBillDialog.value = true },
                    icon = Icons.Default.ReceiptLong
                )
            }

            items(bills) { bill ->
                HayatiCard(
                    onClick = { viewModel.repository.toggleBillPaid(bill.id) },
                    borderColor = if (bill.isPaid) SuccessGreen.copy(alpha = 0.3f) else WarningOrange.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = bill.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (bill.billNumber.isNotBlank()) {
                                    Text(
                                        text = "#${bill.billNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "تاريخ الاستحقاق: ${bill.dueDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${bill.amount.toInt()} ج.م",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (bill.isPaid) SuccessGreen else MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.repository.toggleBillPaid(bill.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (bill.isPaid) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary,
                                        contentColor = if (bill.isPaid) SuccessGreen else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = if (bill.isPaid) "تم السداد ✓" else "تسجيل كمدفوع",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.repository.deleteBill(bill.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الفاتورة", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }

        // HOUSEHOLD TASKS & OCCASIONS SECTION
        if (selectedTab == 0 || selectedTab == 3) {
            item {
                SectionHeader(
                    title = "المهام المنزلية المشتركة",
                    subtitle = "توزيع المسؤوليات بين أفراد الأسرة",
                    actionText = "+ مهمة منزلية",
                    onActionClick = { viewModel.showAddHouseholdTaskDialog.value = true },
                    icon = Icons.Default.HomeWork
                )
            }

            items(householdTasks) { ht ->
                HayatiCard(
                    onClick = { viewModel.repository.toggleHouseholdTask(ht.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = ht.isCompleted,
                            onCheckedChange = { viewModel.repository.toggleHouseholdTask(ht.id) }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ht.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (ht.isCompleted) FontWeight.Normal else FontWeight.Bold,
                                color = if (ht.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "المكلف: ${ht.assignedToName} • التكرار: ${ht.recurring}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.repository.deleteHouseholdTask(ht.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "المناسبات العائلية وأعياد الميلاد",
                    subtitle = "تنبيهات مسبقة لمناسبات الأسرة السعيدة",
                    actionText = "+ مناسبة جديدة",
                    onActionClick = { viewModel.showAddOccasionDialog.value = true },
                    icon = Icons.Default.Celebration
                )
            }

            items(occasions) { occasion ->
                HayatiCard(
                    backgroundColor = GoldAccent.copy(alpha = 0.1f),
                    borderColor = GoldAccent.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🎂", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = occasion.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${occasion.type} • ${occasion.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = occasion.date,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            IconButton(
                                onClick = { viewModel.repository.deleteOccasion(occasion.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "صيانة الأجهزة والمرافق المنزلية",
                    subtitle = "مواعيد فحص التكييفات والفلاتر",
                    icon = Icons.Default.Handyman
                )
            }

            items(maintenance) { item ->
                HayatiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الموعد القادم: ${item.nextDate} • ${item.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${item.estimatedCost.toInt()} ج.م",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                }
            }
        }
    }

    if (editingMember != null) {
        com.example.ui.components.EditFamilyMemberDialog(
            member = editingMember!!,
            onSave = { updated ->
                viewModel.repository.updateFamilyMember(updated)
            },
            onDismiss = { editingMember = null }
        )
    }
}
