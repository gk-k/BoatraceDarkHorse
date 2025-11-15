package com.nakaya.gkk.boatracedarkhorse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.google.android.gms.ads.AdSize
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.nakaya.gkk.boatracedarkhorse.ui.AdBanner
import com.nakaya.gkk.boatracedarkhorse.ui.DateScreen
import com.nakaya.gkk.boatracedarkhorse.ui.PlaceScreen
import com.nakaya.gkk.boatracedarkhorse.ui.RaceListScreen
import com.nakaya.gkk.boatracedarkhorse.ui.theme.BoatraceDarkHorseTheme
import com.nakaya.gkk.boatracedarkhorse.ui.viewmodel.RaceViewModel

private lateinit var analytics: FirebaseAnalytics

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        analytics = Firebase.analytics

        enableEdgeToEdge()
        setContent {
            BoatraceDarkHorseTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(R.drawable.haikei),
                        contentDescription = "background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    val viewModel = remember { RaceViewModel() }

                    var selectedDate by remember { mutableStateOf<String?>(null) }
                    var selectedPlace by remember { mutableStateOf<String?>(null) }

                    val path = listOfNotNull(selectedDate, selectedPlace).joinToString(" / ")

                    val onBack: (() -> Unit)? = when {
                        selectedPlace != null -> {
                            { selectedPlace = null }
                        }

                        selectedDate != null -> {
                            { selectedDate = null }
                        }

                        else -> null
                    }

                    Scaffold(
                        bottomBar = {
                            AdBanner(
                                modifier = Modifier.fillMaxWidth(),
                                adSize = AdSize.BANNER,
                                //【重要】これはテスト用のIDです。リリース時には自分のAdMob広告ユニットIDに置き換えてください。
                                adUnitId = "ca-app-pub-6804870782191249/8317703244"
                            )
                        },
                        topBar = {
                            if (selectedDate != null) {
                                TopAppBar(
                                    title = {
                                        Text(path)
                                    },
                                    navigationIcon = {
                                        if (onBack != null) {
                                            IconButton(onClick = onBack) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back"
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.White.copy(alpha = 0.5f),
                                        titleContentColor = Color.Black,
                                        navigationIconContentColor = Color.Black
                                    )
                                )
                            }
                        },
                        containerColor = Color.Transparent
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                        ) {
                            when {
                                selectedDate == null -> {
                                    DateScreen(viewModel) { date ->
                                        selectedDate = date
                                        selectedPlace = null
                                    }
                                }

                                selectedPlace == null -> {
                                    PlaceScreen(viewModel, selectedDate!!, onPlaceSelected = { place ->
                                        selectedPlace = place
                                    }, onBack = { selectedDate = null })
                                }

                                else -> {
                                    RaceListScreen(
                                        viewModel,
                                        selectedDate!!,
                                        selectedPlace!!,
                                        onBack = { selectedPlace = null })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
