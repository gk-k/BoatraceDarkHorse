package com.nakaya.gkk.boatracedarkhorse.ui

//import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.nakaya.gkk.boatracedarkhorse.R
import com.nakaya.gkk.boatracedarkhorse.ui.viewmodel.RaceViewModel

import kotlin.random.Random


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceScreen(
    viewModel: RaceViewModel,
    date: String,
    onPlaceSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val places by viewModel.places.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var expandedPlaceName by remember { mutableStateOf<String?>(null) }
    var enlargedPlaceInfo by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val adInterstitial = rememberAdInterstitial()

    var adFrequency by remember { mutableStateOf(0.4) } // デフォルト値は0.0

    LaunchedEffect(Unit) {

        viewModel.fetchPlaces(date)
        adInterstitial.load()
        try {

            // Remote Configから値を取得して有効化
            val success = Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener {

                if (it.isSuccessful) {
                    // 成功したら、取得した値で状態を更新
                    adFrequency = Firebase.remoteConfig.getDouble("interstitial_ad_frequency")
                }
            }
        } catch (e: Exception) {
            println("Remote Config fetch failed, using default. $e")
        }
    }

    val showAdAndNavigate: (String) -> Unit = { placeName ->
        if (Random.nextFloat() < adFrequency) {
            adInterstitial.show {
                onPlaceSelected(placeName)
            }
        } else {
            onPlaceSelected(placeName)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchPlaces(date) },
            state = pullRefreshState,
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
                items(places) { placeWithHitRate ->
                    val isExpanded = expandedPlaceName == placeWithHitRate.name
                    Box(
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
                            .clickable { showAdAndNavigate(placeWithHitRate.name) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(getPlaceIconDrawable(placeWithHitRate.name)),
                                    contentDescription = placeWithHitRate.name,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            enlargedPlaceInfo =
                                                getPlaceIconDrawable(placeWithHitRate.name) to placeWithHitRate.name
                                        }
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = placeWithHitRate.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAdAndNavigate(placeWithHitRate.name) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = "予想を見る",
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("予想を見る")
                                }
                                Button(
                                    onClick = {
                                        expandedPlaceName =
                                            if (isExpanded) null else placeWithHitRate.name
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val icon =
                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                                    Icon(
                                        icon,
                                        contentDescription = if (isExpanded) "閉じる" else "直近１年のこの場所の的中率を確認する",
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(if (isExpanded) "閉じる" else "直近１年のこの場所の的中率を確認する")
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "的中率",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    placeWithHitRate.hitRates.forEach { hitRate ->
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = hitRate.ticket_type,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                LinearProgressIndicator(
                                                    progress = { (hitRate.rate / 100.0).toFloat() },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "${hitRate.rate}%",
                                                    fontSize = 16.sp,
                                                    modifier = Modifier.width(50.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    enlargedPlaceInfo?.let { (iconId, name) ->
        Dialog(onDismissRequest = { enlargedPlaceInfo = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(iconId),
                        contentDescription = name,
                        modifier = Modifier.size(256.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getPlaceIconDrawable(placeName: String): Int {
    // TODO: ご自身のアイコンリソースに合わせて、このマッピングを完成させてください
    return when (placeName) {
        "からつ" -> R.drawable.karatu
        "とこなめ" -> R.drawable.tokoname
        "びわこ" -> R.drawable.biwako
        "まるがめ" -> R.drawable.marugame
        "三国" -> R.drawable.mikuni
        "下関" -> R.drawable.simonoseki
        "住之江" -> R.drawable.sumienoe
        "多摩川" -> R.drawable.tamagawa
        "大村" -> R.drawable.omura
        "宮島" -> R.drawable.miyajima
        "平和島" -> R.drawable.heiwajima
        "徳山" -> R.drawable.tokuyama
        "戸田" -> R.drawable.toda
        "桐生" -> R.drawable.kiryu
        "尼崎" -> R.drawable.amagasaki
        "児島" -> R.drawable.kojima
        "江戸川" -> R.drawable.edogawa
        "津" -> R.drawable.tu
        "浜名湖" -> R.drawable.hamanako
        "福岡" -> R.drawable.fukuoka
        "芦屋" -> R.drawable.ashiya
        "若松" -> R.drawable.wakamatu
        "蒲郡" -> R.drawable.gamagoori
        "鳴門" -> R.drawable.naruto
        else -> R.drawable.app_icon // ここにデフォルトのアイコンを指定してください
    }
}