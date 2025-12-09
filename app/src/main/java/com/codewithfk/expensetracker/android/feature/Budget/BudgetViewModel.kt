package com.codewithfk.expensetracker.android.feature.Budget

import androidx.lifecycle.viewModelScope
import com.codewithfk.expensetracker.android.base.BaseViewModel
import com.codewithfk.expensetracker.android.base.HomeNavigationEvent
import com.codewithfk.expensetracker.android.base.UiEvent
import com.codewithfk.expensetracker.android.utils.Utils
import com.codewithfk.expensetracker.android.data.dao.ExpenseDao
import com.codewithfk.expensetracker.android.data.model.ExpenseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.codewithfk.expensetracker.android.feature.home.TransactionItem
import java.text.SimpleDateFormat
import java.util.Date

@HiltViewModel
class BudgetViewModel @Inject constructor(val dao: ExpenseDao) : BaseViewModel() {
    val expenses = dao.getAllExpense()

    fun deleteExpense(expenseEntity: ExpenseEntity) {
        viewModelScope.launch {
            dao.deleteExpense(expenseEntity)
            Log.d("HomeViewModel", "Deleted expense: $expenseEntity")
        }
    }
    override fun onEvent(event: UiEvent) {
        when (event) {
            is BudgetUiEvent.OnSeeExpensesClicked -> {
                viewModelScope.launch {
                    _navigationEvent.emit(HomeNavigationEvent.NavigateToSeeAllExpenses)
                }
            }

            is BudgetUiEvent.OnSeeIncomeClicked -> {
                viewModelScope.launch {
                    _navigationEvent.emit(HomeNavigationEvent.NavigateToSeeAllIncome)
                }
            }

        }
    }

    fun getBalance(list: List<ExpenseEntity>): String {
        var balance = 0.0
        for (expense in list) {
            if (expense.type == "Income") {
                balance += expense.amount
            } else {
                balance -= expense.amount
            }
        }
        return Utils.formatCurrency(balance)
    }

    fun getTotalExpense(list: List<ExpenseEntity>): String {
        var total = 0.0
        for (expense in list) {
            if (expense.type != "Income") {
                total += expense.amount
            }
        }

        return Utils.formatCurrency(total)
    }

    fun getTotalIncome(list: List<ExpenseEntity>): String {
        var totalIncome = 0.0
        for (expense in list) {
            if (expense.type == "Income") {
                totalIncome += expense.amount
            }
        }
        return Utils.formatCurrency(totalIncome)
    }

    fun getBudgetLeft(list: List<ExpenseEntity>): String {
        var totalIncome = 0.0
        for (expense in list) {
            if (expense.type == "Income") {
                totalIncome += expense.amount
            }
        }
        var total = 0.0
        for (expense in list) {
            if (expense.type != "Income") {
                total += expense.amount
            }
        }

        totalIncome -= total
        return Utils.formatCurrency(totalIncome)
    }

    fun getSavingsPercentage(list: List<ExpenseEntity>): String {
        var totalIncome = 0.0
        for (expense in list) {
            if (expense.type == "Income") {
                totalIncome += expense.amount
            }
        }
        var totalExpenses = 0.0
        for (expense in list) {
            if (expense.type != "Income") {
                totalExpenses += expense.amount
            }
        }

        val savings = totalIncome - totalExpenses
        val savingsPercentage = if (totalIncome > 0) (savings / totalIncome) * 100 else 0.0

        return String.format("%.2f", savingsPercentage) + "%"
    }
}

sealed class BudgetUiEvent : UiEvent() {
    data object OnSeeIncomeClicked : BudgetUiEvent()
    data object OnSeeExpensesClicked : BudgetUiEvent()

}