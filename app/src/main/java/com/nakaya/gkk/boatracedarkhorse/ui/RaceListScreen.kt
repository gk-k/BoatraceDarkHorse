package com.nakaya.gkk.boatracedarkhorse.ui

//import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nakaya.gkk.boatracedarkhorse.ui.viewmodel.RaceViewModel
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceListScreen(viewModel: RaceViewModel, date: String, place: String, onBack: () -> Unit) {
    val races by viewModel.races.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(date, place) {
        viewModel.fetchRaces(date, place)

    }

    Column(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            onRefresh = { viewModel.fetchRaces(date, place) },
            modifier = Modifier
                .weight(1f)
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
                items(races.entries.toList()) { (race, ranking) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.8f),
                                        Color.White.copy(alpha = 0.5f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(text = race, fontSize = 20.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "予想着順:", fontSize = 26.sp)
                            ranking.forEach { boatNumber ->
                                BoatNumber(boatNumber.toString())
                            }
                        }
                    }
                }
            }


        }
    }
}

@Composable
fun BoatNumber(number: String) {
    val backgroundColor = when (number) {
        "1" -> Color.White
        "2" -> Color.Black
        "3" -> Color.Red
        "4" -> Color.Blue
        "5" -> Color.Yellow
        "6" -> Color.Green
        else -> Color.Gray
    }
    val textColor = when (number) {
        "1", "5" -> Color.Black
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = textColor,
            fontSize = 22.sp
        )
    }
}
