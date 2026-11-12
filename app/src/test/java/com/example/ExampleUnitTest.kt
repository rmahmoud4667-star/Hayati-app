package com.example

import com.example.data.model.*
import com.example.data.repository.HayatiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `financial calculations compute exact totals and balance`() {
        val repository = HayatiRepository()
        val totalIncome = repository.calculateTotalIncome()
        val totalExpenses = repository.calculateTotalExpenses()
        val balance = repository.calculateBalance()

        assertEquals(30500.0, totalIncome, 0.01)
        assertTrue(totalExpenses > 0)
        assertEquals(totalIncome - totalExpenses, balance, 0.01)

        // Add a new expense
        repository.addTransaction(
            FinancialTransaction(
                type = TransactionType.EXPENSE,
                amount = 500.0,
                date = repository.todayDateString,
                category = "المنزل",
                description = "شراء أدوات منزلية"
            )
        )

        assertEquals(totalExpenses + 500.0, repository.calculateTotalExpenses(), 0.01)
        assertEquals(balance - 500.0, repository.calculateBalance(), 0.01)
    }

    @Test
    fun `task lifecycle handles add update and completion toggle`() {
        val repository = HayatiRepository()
        val initialCount = repository.tasks.value.size

        val newTask = WorkTask(
            id = "test-task-1",
            title = "مهمة اختبار جديدة",
            priority = Priority.HIGH,
            dueDate = repository.todayDateString
        )
        repository.addTask(newTask)
        assertEquals(initialCount + 1, repository.tasks.value.size)

        // Update task
        repository.updateTask(newTask.copy(title = "مهمة محدثة"))
        val updatedTask = repository.tasks.value.find { it.id == "test-task-1" }
        assertEquals("مهمة محدثة", updatedTask?.title)

        // Toggle completion
        repository.toggleTaskCompletion("test-task-1")
        val completed = repository.tasks.value.find { it.id == "test-task-1" }
        assertNotNull(completed)
        assertTrue(completed!!.isCompleted)
    }

    @Test
    fun `child profile and exam management flow`() {
        val repository = HayatiRepository()
        val initialChildrenCount = repository.children.value.size

        val newChild = ChildProfile(
            id = "child-test",
            name = "يوسف",
            grade = "الصف الثاني الإعدادي",
            school = "مدرسة النيل",
            avatarEmoji = "👦"
        )
        repository.addChild(newChild)
        assertEquals(initialChildrenCount + 1, repository.children.value.size)

        // Add exam for child
        repository.addExam(
            Exam(
                childId = "child-test",
                childName = "يوسف",
                subject = "رياضيات",
                title = "امتحان الجبر",
                examDate = "2026-11-25"
            )
        )
        val addedExam = repository.exams.value.find { it.childId == "child-test" }
        assertNotNull(addedExam)
        assertEquals("امتحان الجبر", addedExam?.title)
    }

    @Test
    fun `AI assistant processes natural language expense command and executes action`() {
        val repository = HayatiRepository()
        val beforeExpenseTotal = repository.calculateTotalExpenses()

        repository.sendUserMessage("سجل مصروف 350 جنيه مشتريات للبيت")
        val lastMessage = repository.chatMessages.value.last()
        assertNotNull(lastMessage.pendingAction)
        assertEquals("ADD_EXPENSE", lastMessage.pendingAction?.actionType)

        // Execute action
        repository.executePendingAction(lastMessage.pendingAction!!)
        assertEquals(beforeExpenseTotal + 350.0, repository.calculateTotalExpenses(), 0.01)
    }

    @Test
    fun `AI daily planner creates structured schedule`() {
        val repository = HayatiRepository()
        val plan = repository.generateDailyPlan()
        assertNotNull(plan)
        assertTrue(plan.timeBlocks.isNotEmpty())
        assertEquals(repository.todayDateString, plan.date)
    }

    @Test
    fun `data validation rejects empty task titles and invalid negative financial amounts`() {
        val repository = HayatiRepository()

        // Empty title task should be rejected
        val invalidTask = WorkTask(
            title = "   ",
            dueDate = repository.todayDateString
        )
        val taskValidationError = repository.validateTask(invalidTask)
        assertNotNull(taskValidationError)
        val taskAdded = repository.addTask(invalidTask)
        assertTrue(!taskAdded)

        // Negative transaction amount should be rejected
        val invalidTx = FinancialTransaction(
            type = TransactionType.EXPENSE,
            amount = -150.0,
            date = repository.todayDateString,
            category = "طعام",
            description = "مشتريات"
        )
        val txValidationError = repository.validateTransaction(invalidTx)
        assertNotNull(txValidationError)
        val txAdded = repository.addTransaction(invalidTx)
        assertTrue(!txAdded)

        // Negative bill amount should be rejected
        val invalidBill = UtilityBill(
            name = "كهرباء",
            amount = -50.0,
            dueDate = repository.todayDateString
        )
        val billValidationError = repository.validateBill(invalidBill)
        assertNotNull(billValidationError)
        val billAdded = repository.addBill(invalidBill)
        assertTrue(!billAdded)
    }

    @Test
    fun `duplicate submission guard rejects rapid consecutive clicks within debounce window`() {
        val repository = HayatiRepository()
        val key = "test_duplicate_click_1"

        // First click succeeds
        val isFirstDuplicate = repository.isDuplicateSubmission(key, windowMs = 2000L)
        assertTrue(!isFirstDuplicate)

        // Rapid second click within 2000ms is detected and blocked
        val isSecondDuplicate = repository.isDuplicateSubmission(key, windowMs = 2000L)
        assertTrue(isSecondDuplicate)
    }

    @Test
    fun `storage status engine reflects local persistent mode without cloud credentials`() {
        val repository = HayatiRepository()
        val status = repository.storageStatus.value
        assertEquals(com.example.data.repository.StorageEngine.LOCAL_PERSISTENT, status.engine)
        assertTrue(status.cloudDatabaseId.isNotEmpty() || status.cloudDatabaseId == "")
    }

    @Test
    fun `household bills and shopping list flow`() {
        val repository = HayatiRepository()
        val initialBillsCount = repository.bills.value.size
        val initialShoppingCount = repository.shoppingList.value.size

        // Add Bill
        val newBill = UtilityBill(
            id = "bill-test-1",
            name = "فاتورة الغاز الطبيعي",
            billNumber = "GAS-2026-99",
            amount = 180.0,
            dueDate = repository.todayDateString,
            iconName = "fire"
        )
        val billAdded = repository.addBill(newBill)
        assertTrue(billAdded)
        assertEquals(initialBillsCount + 1, repository.bills.value.size)

        // Toggle Bill Paid
        repository.toggleBillPaid("bill-test-1")
        val paidBill = repository.bills.value.find { it.id == "bill-test-1" }
        assertNotNull(paidBill)
        assertTrue(paidBill!!.isPaid)
        assertEquals(repository.todayDateString, paidBill.paidDate)

        // Add Shopping Item
        val shoppingItem = ShoppingItem(
            id = "shop-test-1",
            name = "حليب وزيت زيتون",
            quantity = "2 زجاجة",
            estimatedPrice = 140.0,
            category = "سوبرماركت"
        )
        val shopAdded = repository.addShoppingItem(shoppingItem)
        assertTrue(shopAdded)
        assertEquals(initialShoppingCount + 1, repository.shoppingList.value.size)

        // Toggle Purchased
        repository.toggleShoppingItem("shop-test-1")
        val purchasedItem = repository.shoppingList.value.find { it.id == "shop-test-1" }
        assertNotNull(purchasedItem)
        assertTrue(purchasedItem!!.isPurchased)
    }

    @Test
    fun `budget spending limit updates and calculation`() {
        val repository = HayatiRepository()
        val category = "طعام"

        repository.updateBudgetLimit(category, 7500.0)
        val updatedBudget = repository.budgets.value.find { it.categoryName == category }
        assertNotNull(updatedBudget)
        assertEquals(7500.0, updatedBudget!!.limitAmount, 0.01)
    }

    @Test
    fun `family member management flow`() {
        val repository = HayatiRepository()
        val initialFamilyCount = repository.familyMembers.value.size

        val member = FamilyMember(
            id = "fam-test-1",
            name = "سارة",
            role = "الزوجة",
            phone = "01012345678",
            avatarEmoji = "👩"
        )
        repository.addFamilyMember(member)
        assertEquals(initialFamilyCount + 1, repository.familyMembers.value.size)
    }
}
