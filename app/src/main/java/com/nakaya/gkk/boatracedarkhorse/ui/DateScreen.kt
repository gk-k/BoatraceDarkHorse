package com.nakaya.gkk.boatracedarkhorse.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nakaya.gkk.boatracedarkhorse.ui.viewmodel.RaceViewModel
import java.time.DayOfWeek
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DateScreen(viewModel: RaceViewModel, onDateSelected: (String) -> Unit) {
    val dates by viewModel.dates.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()


    LaunchedEffect(Unit) {
        viewModel.fetchDates()
    }

    val groupedDates = dates.groupBy { it.substring(0, 7) } // Group by YYYY-MM

    PullToRefreshBox (
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.fetchDates() },
        state = pullRefreshState,
        modifier = Modifier
            .fillMaxWidth(),
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = pullRefreshState
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedDates.forEach { (yearMonth, monthDates) ->
                stickyHeader {
                    val (year, month) = yearMonth.split("-")
                    Text(
                        text = "${year}年${month}月",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.7f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(monthDates) { date ->
                    val localDate = LocalDate.parse(date)
                    val dayOfMonth = localDate.dayOfMonth
                    val dayOfWeekJapanese = when (localDate.dayOfWeek) {
                        DayOfWeek.MONDAY -> "月"
                        DayOfWeek.TUESDAY -> "火"
                        DayOfWeek.WEDNESDAY -> "水"
                        DayOfWeek.THURSDAY -> "木"
                        DayOfWeek.FRIDAY -> "金"
                        DayOfWeek.SATURDAY -> "土"
                        DayOfWeek.SUNDAY -> "日"
                        else -> "日付エラー"
                    }
                    val displayText =
                        "${dayOfMonth.toString().padStart(2, '0')}日 ($dayOfWeekJapanese)"

                    Text(
                        text = displayText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDateSelected(date) }
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.8f),
                                        Color.White.copy(alpha = 0.5f)
                                    )
                                )
                            )
                            .padding(16.dp),
                        fontSize = 24.sp
                    )
                }
            }
        }

    }
}
