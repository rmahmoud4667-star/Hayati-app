package com.example.ui.screens.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarEventType
import com.example.data.model.CalendarItem
import com.example.data.repository.StorageEngine
import com.example.ui.CalendarViewMode
import com.example.ui.HayatiViewModel
import com.example.ui.components.HayatiCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: HayatiViewModel,
    modifier: Modifier = Modifier
) {
    // Collect data integrated with Firebase Firestore and local persistence
    val tasks by viewModel.repository.tasks.collectAsState()
    val meetings by viewModel.repository.meetings.collectAsState()
    val bills by viewModel.repository.bills.collectAsState()
    val homework by viewModel.repository.homework.collectAsState()
    val exams by viewModel.repository.exams.collectAsState()
    val occasions by viewModel.repository.occasions.collectAsState()
    val storageStatus by viewModel.repository.storageStatus.collectAsState()
    val viewMode by viewModel.calendarViewMode.collectAsState()

    var selectedEventType by remember { mutableStateOf<CalendarEventType?>(null) }

    // Date formatting & Calendar navigation
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    val today = viewModel.repository.todayDateString

    // Month & Year state
    val calendarInstance = remember {
        Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo")).apply {
            try {
                time = dateFormatter.parse(today) ?: Date()
            } catch (e: Exception) {
                // fallback to current system time
            }
        }
    }
    var currentYear by remember { mutableStateOf(calendarInstance.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(calendarInstance.get(Calendar.MONTH)) } // 0-based: 0 = Jan, 10 = Nov
    var selectedDateString by remember { mutableStateOf(today) }

    // Combine all domain data from Firestore into unified CalendarItem models
    val allCalendarItems = remember(tasks, meetings, bills, homework, exams, occasions) {
        val items = mutableListOf<CalendarItem>()

        tasks.forEach {
            items.add(
                CalendarItem(
                    id = it.id,
                    title = it.title,
                    date = it.dueDate,
                    type = CalendarEventType.WORK,
                    subtitle = "مهمة عمل [أولوية ${it.priority.titleAr}]",
                    isCompleted = it.isCompleted
                )
            )
        }

        meetings.forEach {
            items.add(
                CalendarItem(
                    id = it.id,
                    title = it.title,
                    date = it.date,
                    time = it.time,
                    type = CalendarEventType.WORK,
                    subtitle = if (it.attendees.isNotBlank()) "اجتماع عمل مع ${it.attendees}" else "اجتماع عمل"
                )
            )
        }

        bills.forEach {
            items.add(
                CalendarItem(
                    id = it.id,
                    title = it.name,
                    date = it.dueDate,
                    type = CalendarEventType.BILL,
                    subtitle = "فاتورة دورية بقيمة ${it.amount.toInt()} ج.م",
                    isCompleted = it.isPaid
                )
            )
        }

        homework.forEach {
            items.add(
                CalendarItem(
                    id = it.id,
                    title = "${it.childName}: ${it.title} (${it.subject})",
                    date = it.dueDate,
                    type = CalendarEventType.HOMEWORK,
                    subtitle = "واجب مدرسي مطلوب",
                    isCompleted = it.isCompleted
                )
            )
        }

        exams.forEach {
            items.add(
                CalendarItem(
                    id = it.id,
                    title = "امتحان ${it.subject} - ${it.childName}",
                    date = it.examDate,
                    type = CalendarEventType.EXAM,
                    subtitle = if (it.notes.isNotBlank()) it.notes else it.title
                )
            )
        }

        occasions.forEach {
            items.add(
                CalendarItem(
                    id = it.id,
                    title = it.title,
                    date = it.date,
                    type = CalendarEventType.FAMILY,
                    subtitle = it.type
                )
            )
        }

        items.sortedBy { it.date }
    }

    // Map events by date (YYYY-MM-DD) for fast grid lookup
    val itemsByDate = remember(allCalendarItems) {
        allCalendarItems.groupBy { it.date }
    }

    // Month details calculation
    val arabicMonthNames = remember {
        listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
    }

    val daysInMonth = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo"))
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo"))
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        // Calendar.SUNDAY = 1, SATURDAY = 7
        cal.get(Calendar.DAY_OF_WEEK)
    }

    // Days grid cells (accounting for Saturday start in Arabic calendar: السبت = 0)
    // Calendar.SATURDAY = 7 -> offset 0
    // Calendar.SUNDAY = 1 -> offset 1
    // Calendar.MONDAY = 2 -> offset 2
    // Calendar.TUESDAY = 3 -> offset 3
    // Calendar.WEDNESDAY = 4 -> offset 4
    // Calendar.THURSDAY = 5 -> offset 5
    // Calendar.FRIDAY = 6 -> offset 6
    val leadingOffset = remember(firstDayOfWeek) {
        when (firstDayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    val weekDaysArabic = remember {
        listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
    }

    // Events for selected day
    val selectedDayEvents = remember(selectedDateString, allCalendarItems, selectedEventType) {
        val dayList = allCalendarItems.filter { it.date == selectedDateString }
        if (selectedEventType == null) dayList else dayList.filter { it.type == selectedEventType }
    }

    // Mode-specific list for DAY / WEEK / MONTH tabs
    val currentModeItems = remember(viewMode, allCalendarItems, selectedEventType, currentYear, currentMonth, today) {
        val filtered = when (viewMode) {
            CalendarViewMode.DAY -> allCalendarItems.filter { it.date == selectedDateString }
            CalendarViewMode.WEEK -> {
                // Next 7 days from selected or today
                val start = selectedDateString
                val cal = Calendar.getInstance()
                cal.time = try { dateFormatter.parse(start) ?: Date() } catch (e: Exception) { Date() }
                cal.add(Calendar.DAY_OF_YEAR, 7)
                val end = dateFormatter.format(cal.time)
                allCalendarItems.filter { it.date in start..end }
            }
            CalendarViewMode.MONTH -> {
                val prefix = String.format(Locale.ENGLISH, "%04d-%02d", currentYear, currentMonth + 1)
                allCalendarItems.filter { it.date.startsWith(prefix) }
            }
        }
        if (selectedEventType == null) filtered else filtered.filter { it.type == selectedEventType }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header with Sync Status
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التقويم الشهري والمواعيد",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Cloud Sync Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (storageStatus.engine == StorageEngine.CLOUD_FIRESTORE)
                            TurquoisePrimary.copy(alpha = 0.15f)
                        else
                            GoldAccent.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (storageStatus.engine == StorageEngine.CLOUD_FIRESTORE) TurquoisePrimary else GoldAccent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (storageStatus.engine == StorageEngine.CLOUD_FIRESTORE)
                                    Icons.Default.CloudDone
                                else
                                    Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (storageStatus.engine == StorageEngine.CLOUD_FIRESTORE)
                                    TurquoisePrimary
                                else
                                    GoldAccent
                            )
                            Text(
                                text = if (storageStatus.engine == StorageEngine.CLOUD_FIRESTORE) "سحابي Firestore" else "محلي دائم",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (storageStatus.engine == StorageEngine.CLOUD_FIRESTORE)
                                    TurquoisePrimary
                                else
                                    GoldAccent
                            )
                        }
                    }
                }

                Text(
                    text = "عرض شبكي شهري متكامل يدمج مهام العمل، امتحانات الأبناء، وتواريخ استحقاق الفواتير",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // View Mode Switcher: شهري (شبكي), أسبوعي, يومي
        item {
            TabRow(
                selectedTabIndex = viewMode.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TurquoisePrimary
            ) {
                CalendarViewMode.values().forEach { mode ->
                    Tab(
                        selected = viewMode == mode,
                        onClick = { viewModel.calendarViewMode.value = mode },
                        text = {
                            Text(
                                text = mode.titleAr,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (viewMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("calendar_view_${mode.name.lowercase()}")
                    )
                }
            }
        }

        // MONTHLY GRID COMPONENT (Shown prominently when in MONTH mode or available as overview)
        item {
            HayatiCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_calendar_grid")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Month & Year Selector Header with Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentMonth == 11) {
                                    currentMonth = 0
                                    currentYear += 1
                                } else {
                                    currentMonth += 1
                                }
                            },
                            modifier = Modifier.testTag("btn_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "الشهر التالي",
                                tint = TurquoisePrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${arabicMonthNames[currentMonth]} $currentYear",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val monthEventCount = remember(currentYear, currentMonth, allCalendarItems) {
                                val prefix = String.format(Locale.ENGLISH, "%04d-%02d", currentYear, currentMonth + 1)
                                allCalendarItems.count { it.date.startsWith(prefix) }
                            }
                            Text(
                                text = "$monthEventCount موعد واستحقاق",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurquoisePrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentMonth == 0) {
                                    currentMonth = 11
                                    currentYear -= 1
                                } else {
                                    currentMonth -= 1
                                }
                            },
                            modifier = Modifier.testTag("btn_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "الشهر السابق",
                                tint = TurquoisePrimary
                            )
                        }
                    }

                    // Days of week header row (Saturday to Friday)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        weekDaysArabic.forEach { dayName ->
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Calendar Grid Matrix (5 to 6 rows)
                    val totalCells = leadingOffset + daysInMonth
                    val rows = (totalCells + 6) / 7

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNumber = cellIndex - leadingOffset + 1

                                    if (dayNumber in 1..daysInMonth) {
                                        val dateString = String.format(
                                            Locale.ENGLISH,
                                            "%04d-%02d-%02d",
                                            currentYear,
                                            currentMonth + 1,
                                            dayNumber
                                        )
                                        val dayEvents = itemsByDate[dateString] ?: emptyList()
                                        val isToday = (dateString == today)
                                        val isSelected = (dateString == selectedDateString)

                                        // Day Cell
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        isSelected -> TurquoisePrimary.copy(alpha = 0.25f)
                                                        isToday -> GoldAccent.copy(alpha = 0.18f)
                                                        dayEvents.isNotEmpty() -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = when {
                                                        isSelected -> 2.dp
                                                        isToday -> 1.5.dp
                                                        else -> 0.5.dp
                                                    },
                                                    color = when {
                                                        isSelected -> TurquoisePrimary
                                                        isToday -> GoldAccent
                                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    selectedDateString = dateString
                                                }
                                                .padding(2.dp)
                                                .testTag("calendar_day_$dateString"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxSize().padding(vertical = 2.dp)
                                            ) {
                                                // Day number
                                                Text(
                                                    text = dayNumber.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> TurquoisePrimary
                                                        isToday -> GoldAccent
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )

                                                // Event Category Indicator Dots
                                                if (dayEvents.isNotEmpty()) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    ) {
                                                        // Show dots for distinct types on this day (Tasks, Bills, Exams, etc.)
                                                        val distinctTypes = dayEvents.map { it.type }.distinct().take(3)
                                                        distinctTypes.forEach { type ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(5.dp)
                                                                    .padding(horizontal = 0.5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(type.colorHex))
                                                            )
                                                        }
                                                        if (dayEvents.size > 3) {
                                                            Text(
                                                                text = "+",
                                                                fontSize = 7.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TurquoisePrimary
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.height(5.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        // Empty cell outside current month
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Legend of event types
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalendarEventType.values().forEach { type ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(type.colorHex))
                                )
                                Text(
                                    text = type.labelAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedEventType == null,
                    onClick = { selectedEventType = null },
                    label = { Text("الكل (${allCalendarItems.size})", style = MaterialTheme.typography.labelSmall) }
                )
                CalendarEventType.values().forEach { type ->
                    val isSelected = selectedEventType == type
                    val count = allCalendarItems.count { it.type == type }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedEventType = if (isSelected) null else type },
                        label = { Text("${type.labelAr} ($count)", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(type.colorHex))
                            )
                        }
                    )
                }
            }
        }

        // Selected Day Details Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (viewMode == CalendarViewMode.DAY || selectedDateString == today)
                            "أحداث اليوم المحدد ($selectedDateString)"
                        else
                            "مواعيد وأحداث: $selectedDateString",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${selectedDayEvents.size} ارتباط مسجل في هذا اليوم",
                        style = MaterialTheme.typography.bodySmall,
                        color = TurquoisePrimary
                    )
                }

                if (selectedDateString != today) {
                    OutlinedButton(
                        onClick = {
                            selectedDateString = today
                            try {
                                val cal = Calendar.getInstance()
                                cal.time = dateFormatter.parse(today) ?: Date()
                                currentYear = cal.get(Calendar.YEAR)
                                currentMonth = cal.get(Calendar.MONTH)
                            } catch (e: Exception) {}
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_back_to_today")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TurquoisePrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اليوم", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // List of events for the selected day or current view mode
        val itemsToDisplay = if (viewMode == CalendarViewMode.MONTH) selectedDayEvents else currentModeItems

        if (itemsToDisplay.isEmpty()) {
            item {
                HayatiCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = TurquoisePrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "لا توجد مواعيد أو استحقاقات مسجلة في هذا اليوم ($selectedDateString)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "يمكنك إضافة مهمة عمل جديدة، أو تسجيل فاتورة، أو إضافة امتحان أطفال ليظهر تلقائياً هنا وفي سحابة Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(itemsToDisplay, key = { it.id }) { item ->
                val color = Color(item.type.colorHex)
                HayatiCard(
                    borderColor = color.copy(alpha = 0.4f),
                    modifier = Modifier.testTag("calendar_item_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Category colored pillar
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )

                            // Type Icon Badge
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.type) {
                                        CalendarEventType.WORK -> Icons.Default.Work
                                        CalendarEventType.BILL -> Icons.Default.Receipt
                                        CalendarEventType.HOMEWORK -> Icons.Default.MenuBook
                                        CalendarEventType.EXAM -> Icons.Default.School
                                        CalendarEventType.FAMILY -> Icons.Default.FamilyRestroom
                                    },
                                    contentDescription = item.type.labelAr,
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Date, Time & Status Pill
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = color.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (item.time.isNotBlank()) "${item.date} • ${item.time}" else item.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (item.isCompleted) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "مكتمل",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
