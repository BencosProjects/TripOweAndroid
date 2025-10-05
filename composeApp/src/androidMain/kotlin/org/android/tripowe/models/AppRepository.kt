package org.android.tripowe.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

data class Trip(val id: Int, val name: String)
data class Participant(val id: Int, val name: String)
data class Expense(val id: Int, val description: String, val amount: Double, val payerId: Int)

fun BigDecimal.format(digits: Int) = this.setScale(digits, RoundingMode.HALF_UP).toString()
fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)

class AppRepository {
    private val _currentTrip = MutableStateFlow(Trip(1, "טיול לים המלח"))
    val currentTrip = _currentTrip.asStateFlow()

    private val _trips = MutableStateFlow(listOf(
        Trip(1, "טיול לים המלח"),
        Trip(2, "טיול לצפון")
    ))
    val trips = _trips.asStateFlow()

    private val _participants = MutableStateFlow(listOf(
    Participant(1, "אליס"),
    Participant(2, "בוב"),
    Participant(3, "צ'ארלי"),
    Participant(4, "shon"),
    Participant(5, "a"),
    Participant(6, "b"),
    Participant(7, "c")
))
    val participants = _participants.asStateFlow()

    private val _expenses = MutableStateFlow(listOf(
    Expense(1, "בנזין", 400.0, 1),
    Expense(2, "אוכל", 400.0, 2),
    Expense(3, "לינה", 500.0, 3),
    Expense(4, "drugs", 300.0, 4),
    Expense(5, "a", 100.0, 5),
    Expense(6, "b", 200.0, 6),
    Expense(7, "c", 50.0, 7)
))
    val expenses = _expenses.asStateFlow()

    fun addTrip(name: String) {
        val newId = _trips.value.size + 1
        _trips.value = _trips.value + Trip(newId, name)
    }

    fun setCurrentTrip(tripId: Int) {
        _currentTrip.value = _trips.value.find { it.id == tripId } ?: _currentTrip.value
    }

    fun getPaymentsByParticipant(): Map<Participant, Double> {
        val payments = mutableMapOf<Participant, Double>()
        _participants.value.forEach { participant ->
            payments[participant] = _expenses.value
                .filter { it.payerId == participant.id }
                .sumOf { it.amount }
        }
        return payments
    }

    val totalAmount: Double
        get() = _expenses.value.sumOf { it.amount }

    // פונקציה מתוקנת: חישוב סיכום חובות עם חלוקה פרופורציונלית
    fun getUserDebtSummary(userId: Int = 1): String {
        val participants = _participants.value
        val expenses = _expenses.value
        val numParticipants = participants.size

        if (numParticipants == 0) return "No participants – cannot calculate debts"
        if (expenses.isEmpty()) return "No expenses recorded – everything is settled"

        val balances = mutableMapOf<Int, BigDecimal>()
        val zero = BigDecimal.ZERO
        val sharePrecision = 2

        expenses.forEach { expense ->
            val amount = BigDecimal(expense.amount.toString())
            val share = amount.divide(BigDecimal(numParticipants), sharePrecision, RoundingMode.HALF_UP)
            balances[expense.payerId] = (balances[expense.payerId] ?: zero) + amount - share
            participants.forEach { p ->
                if (p.id != expense.payerId) {
                    balances[p.id] = (balances[p.id] ?: zero) - share
                }
            }
        }

        if (balances.values.all { it == zero }) return "Everything is settled – no debts between participants"

        val userBalance = balances[userId] ?: zero
        val userName = participants.find { it.id == userId }?.name ?: "You"

        if (userBalance > zero) {
            // חייבים למשתמש – חלק את חובות החייבים פרופורציונלית בין הנושים (כולל המשתמש)
            val positiveBalances = balances.filterValues { it > zero }.values.sumOf { it }
            if (positiveBalances == zero) return "No creditors – cannot calculate specific debts"

            val debtsToUser = mutableListOf<String>()
            val debtors = participants.filter { it.id != userId && (balances[it.id] ?: zero) < zero }
            debtors.forEach { debtor ->
                val debtorDebt = -(balances[debtor.id] ?: zero) // חוב חיובי של החייב
                val owedToUser = debtorDebt * (userBalance / positiveBalances) // חלק פרופורציונלי
                if (owedToUser > zero) {
                    debtsToUser.add("${debtor.name} owes you ${owedToUser.format(2)}$")
                }
            }
            return if (debtsToUser.isNotEmpty()) {
                debtsToUser.joinToString(" and ")
            } else {
                "No specific debts to you"
            }
        } else if (userBalance < zero) {
            // המשתמש חייב – חלק את החוב שלו פרופורציונלית בין הנושים
            val negativeBalance = -userBalance // חוב חיובי של המשתמש
            val positiveBalances = balances.filterValues { it > zero }.values.sumOf { it }
            if (positiveBalances == zero) return "No creditors – cannot calculate specific debts"

            val userDebts = mutableListOf<String>()
            val creditors = participants.filter { it.id != userId && (balances[it.id] ?: zero) > zero }
            creditors.forEach { creditor ->
                val creditorShare = balances[creditor.id] ?: zero
                val owedToCreditor = negativeBalance * (creditorShare / positiveBalances) // חלק פרופורציונלי
                if (owedToCreditor > zero) {
                    userDebts.add("You owe ${owedToCreditor.format(2)}$ to ${creditor.name}")
                }
            }
            return if (userDebts.isNotEmpty()) {
                userDebts.joinToString(" and ")
            } else {
                "No specific debts from you"
            }
        } else {
            return "No debts for $userName"
        }
    }
}