package com.nakaya.gkk.boatracedarkhorse.ui

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

// インタースティシャル広告ユニットID
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6804870782191249/1396716457" // Android用ID

/**
 * Android 側の AdInterstitial 実装
 * Context (Activity) が必要になるため、コンストラクタで受け取る
 */
class AdInterstitial(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var onAdDismissedCallback: (() -> Unit)? = null

    fun load() {
        if (interstitialAd != null) return

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    setupFullScreenContentCallback()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    println("Interstitial ad failed to load: ${loadAdError.message}")
                    interstitialAd = null
                }
            }
        )
    }

    private fun setupFullScreenContentCallback() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // 広告が閉じられた
                interstitialAd = null // 広告は使い捨て
                onAdDismissedCallback?.invoke()
                onAdDismissedCallback = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // 表示失敗
                interstitialAd = null
                onAdDismissedCallback?.invoke() // 失敗時もコールバック
                onAdDismissedCallback = null
            }
        }
    }

    fun show(onAdDismissed: () -> Unit) {
        this.onAdDismissedCallback = onAdDismissed

        // Android の広告表示には Activity が必要
        val activity = context as? Activity

        if (interstitialAd != null && activity != null) {
            interstitialAd?.show(activity)
        } else {
            println("Interstitial ad not ready or context is not an Activity.")
            // 広告が表示されなかった場合も、コールバックを即時実行
            onAdDismissedCallback?.invoke()
            onAdDismissedCallback = null
        }
    }
}

/**
 * Composable 内で AdInterstitial (Android) のインスタンスを
 * 簡単に取得するためのヘルパー
 */
@Composable
fun rememberAdInterstitial(): AdInterstitial {
    val context = LocalContext.current
    return remember {
        AdInterstitial(context)
    }
}