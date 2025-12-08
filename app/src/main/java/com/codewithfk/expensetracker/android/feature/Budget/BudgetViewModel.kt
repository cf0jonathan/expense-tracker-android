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