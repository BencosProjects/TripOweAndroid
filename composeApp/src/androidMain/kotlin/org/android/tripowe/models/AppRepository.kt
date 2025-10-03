package org.android.tripowe.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        Participant(4, "דני")

    ))
    val participants = _participants.asStateFlow()

    private val _expenses = MutableStateFlow(listOf(
        Expense(1, "בנזין", 1000.0, 1), // אליס
        Expense(2, "אוכל", 800.0, 2),   // בוב
        Expense(3, "לינה", 600.0, 3),    // צ'ארלי
        Expense(4, "סמים", 200.0, 4) //דני
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
}