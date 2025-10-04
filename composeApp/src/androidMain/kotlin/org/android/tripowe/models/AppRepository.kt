package org.android.tripowe.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class Trip(val id: Int, val name: String)
data class Participant(val id: Int, val name: String)
data class Expense(val id: Int, val description: String, val amount: Double, val payerId: Int)

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
        Participant(3, "צ'ארלי")
    ))
    val participants = _participants.asStateFlow()

    private val _expenses = MutableStateFlow(listOf(
        Expense(1, "בנזין", 1000.0, 1), // אליס
        Expense(2, "אוכל", 800.0, 2),   // בוב
        Expense(3, "לינה", 600.0, 3)    // צ'ארלי
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

    // חישוב חובות למשתמש הראשי (אליס, id=1)
    fun getUserDebtSummary(userId: Int = 1): String {
        val balances = mutableMapOf<Int, Double>()
        val numParticipants = _participants.value.size

        _expenses.value.forEach { expense ->
            val share = expense.amount / numParticipants
            balances[expense.payerId] = (balances[expense.payerId] ?: 0.0) + expense.amount - share
            _participants.value.forEach { p ->
                if (p.id != expense.payerId) {
                    balances[p.id] = (balances[p.id] ?: 0.0) - share
                }
            }
        }

        val userBalance = balances[userId] ?: 0.0
        val userName = _participants.value.find { it.id == userId }?.name ?: "You"
        if (userBalance > 0) {
            val each = userBalance / (numParticipants - 1)
            return "The participants owe you ${userBalance.format(2)}\$, ${each.format(2)}\$ each"
        } else if (userBalance < 0) {
            val creditor = _participants.value.firstOrNull { (balances[it.id] ?: 0.0) > 0 }?.name ?: "someone"
            return "$userName owes ${(-userBalance).format(2)}\$ to $creditor"
        } else {
            return "No debts for $userName"
        }
    }
}