package com.codewithfk.expensetracker.android.feature.Budget

import android.util.Log
import android.view.LayoutInflater
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.codewithfk.expensetracker.android.R
import com.codewithfk.expensetracker.android.base.AddExpenseNavigationEvent
import com.codewithfk.expensetracker.android.base.HomeNavigationEvent
import com.codewithfk.expensetracker.android.base.NavigationEvent
import com.codewithfk.expensetracker.android.data.model.ExpenseEntity
import com.codewithfk.expensetracker.android.feature.Budget.BudgetCardItem
import com.codewithfk.expensetracker.android.feature.Budget.BudgetCardRowItem
import com.codewithfk.expensetracker.android.feature.Budget.BudgetUiEvent
import com.codewithfk.expensetracker.android.feature.Budget.BudgetViewModel
import com.codewithfk.expensetracker.android.feature.home.HomeUiEvent
import com.codewithfk.expensetracker.android.feature.home.HomeViewModel
import com.codewithfk.expensetracker.android.feature.home.MultiFloatingActionButton
import com.codewithfk.expensetracker.android.feature.home.TransactionItem
import com.codewithfk.expensetracker.android.utils.Utils
import com.codewithfk.expensetracker.android.feature.home.TransactionList
import com.codewithfk.expensetracker.android.feature.stats.StatsViewModel
import com.codewithfk.expensetracker.android.ui.theme.Green
import com.codewithfk.expensetracker.android.ui.theme.LightGrey
import com.codewithfk.expensetracker.android.ui.theme.Red
import com.codewithfk.expensetracker.android.ui.theme.Typography
import com.codewithfk.expensetracker.android.ui.theme.Zinc
import com.codewithfk.expensetracker.android.widget.ExpenseTextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import java.text.DateFormat
import java.util.Calendar

@Composable
fun BudgetScreen(navController: NavController, viewModel: BudgetViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.NavigateBack -> navController.popBackStack()
                HomeNavigationEvent.NavigateToSeeAllIncome -> {
                    navController.navigate("/all_income")
                }

                HomeNavigationEvent.NavigateToSeeAllExpenses -> {
                    navController.navigate("/all_expenses")
                }

                else -> {}
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (nameRow, list, card, topBar, add) = createRefs()
            Image(painter = painterResource(id = R.drawable.ic_topbar), contentDescription = null,
                modifier = Modifier.constrainAs(topBar) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                })
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                .constrainAs(nameRow) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {
                ExpenseTextView(
                    text = "Budget",
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.Center),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            val state = viewModel.expenses.collectAsState(initial = emptyList())
            val expense = viewModel.getTotalExpense(state.value)
            val income = viewModel.getTotalIncome(state.value)
            val balance = viewModel.getBudgetLeft(state.value)
            val savings = viewModel.getSavingsPercentage(state.value)
            val menuExpanded = remember { mutableStateOf(false) }
            BudgetCardItem(
                modifier = Modifier.constrainAs(card) {
                    top.linkTo(nameRow.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
                balance = balance, income = income, expense = expense, savings = savings,
                onSeeIncomeClicked = {
                    viewModel.onEvent(BudgetUiEvent.OnSeeIncomeClicked)
                },
                onSeeExpensesClicked = {
                    viewModel.onEvent(BudgetUiEvent.OnSeeExpensesClicked)
                }
            )
        }
    }
}

@Composable
fun BudgetCardItem(
    modifier: Modifier,
    balance: String, income: String, expense: String, savings: String,
    onSeeIncomeClicked: () -> Unit,
    onSeeExpensesClicked: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Zinc)
            .padding(16.dp)
    ) {
        val menuExpanded = remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column (
                modifier = Modifier
            ) {
                Row {
                    ExpenseTextView(
                        text = "Remaining Money in the Budget",
                        color = Color.White,
                        style = Typography.titleMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        painter = painterResource(id = R.drawable.dots_menu),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).clickable {
                            menuExpanded.value = true
                        },
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                    )
                        Box() {
                            DropdownMenu(
                                expanded = menuExpanded.value,
                                onDismissRequest = { menuExpanded.value = false },
                                offset = androidx.compose.ui.unit.DpOffset(x = 400.dp, y = 0.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { ExpenseTextView(text = "Income") },
                                    onClick = {
                                        menuExpanded.value = false
                                        onSeeIncomeClicked.invoke()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { ExpenseTextView(text = "Expenses") },
                                    onClick = {
                                        menuExpanded.value = false
                                        onSeeExpensesClicked.invoke()
                                    }
                                )
                            }
                        }
                }
                Spacer(modifier = Modifier.size(8.dp))
                ExpenseTextView(
                    text = balance, color = Color.White, style = Typography.headlineLarge
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)

        ) {
            BudgetCardRowItem(
                modifier = Modifier
                    .align(Alignment.CenterStart),
                title = "Budget",
                amount = income,
                imaget = R.drawable.ic_income
            )
            Spacer(modifier = Modifier.size(8.dp))
            BudgetCardRowItem(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                title = "Saved",
                amount = savings,
                imaget = R.drawable.ic_income
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BudgetCardRowItem(
                modifier = Modifier
                    .align(Alignment.CenterStart),
                title = "Income",
                amount = income,
                imaget = R.drawable.ic_income
            )
            Spacer(modifier = Modifier.size(8.dp))

            BudgetCardRowItem(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                title = "Expenses",
                amount = expense,
                imaget = R.drawable.ic_expense
            )
        }


    }
}
@Composable
fun BudgetCardRowItem(modifier: Modifier, title: String, amount: String, imaget: Int) {
    Column(modifier = modifier) {
        Row {

            Image(
                painter = painterResource(id = imaget),
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(8.dp))
            ExpenseTextView(text = title, color = Color.White, style = Typography.bodyLarge)
        }
        Row {
            Spacer(modifier = Modifier.size(4.dp))
            ExpenseTextView(
                text = amount,
                color = Color.White,
                style = Typography.titleLarge
            )
        }
    }
}