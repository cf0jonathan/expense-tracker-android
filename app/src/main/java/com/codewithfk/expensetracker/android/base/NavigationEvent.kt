package com.codewithfk.expensetracker.android.base

sealed class NavigationEvent {
    object NavigateBack : NavigationEvent()
}

sealed class AddExpenseNavigationEvent : NavigationEvent() {
    object MenuOpenedClicked : AddExpenseNavigationEvent()
}

sealed class DeleteTransactionNavigationEvent : NavigationEvent() {
    object NavigateToDeleteTransaction : DeleteTransactionNavigationEvent()
}

sealed class HomeNavigationEvent : NavigationEvent() {
    object NavigateToAddExpense : HomeNavigationEvent()
    object NavigateToAddIncome : HomeNavigationEvent()
    object NavigateToSeeAll : HomeNavigationEvent()

    object NavigateToDeleteTransaction : HomeNavigationEvent()
}