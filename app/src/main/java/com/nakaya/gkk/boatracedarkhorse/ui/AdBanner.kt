package com.nakaya.gkk.boatracedarkhorse.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * AdMobのバナー広告を表示するためのComposable。
 *
 * @param modifier Composableに適用するModifier。
 * @param adSize 表示する広告のサイズ。
 * @param adUnitId AdMobで発行した広告ユニットID。
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adSize: AdSize,
    adUnitId: String
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            // Android Viewの世界でAdViewを生成
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                // 広告リクエストを作成してロード
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
