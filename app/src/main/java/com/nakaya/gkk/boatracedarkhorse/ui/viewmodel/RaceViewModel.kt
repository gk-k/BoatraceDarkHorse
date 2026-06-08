package com.nakaya.gkk.boatracedarkhorse.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.YearMonth

// Data classes for hit rate information
@Serializable
data class HitRate(
    val hits: Long = 0,
    val rate: Double = 0.0,
    val ticket_type: String = ""
)

@Serializable
data class PlaceWithHitRate(
    val name: String,
    val hitRates: List<HitRate>
)

class RaceViewModel : ViewModel() {

    private val _dates = MutableStateFlow<List<String>>(emptyList())
    val dates = _dates.asStateFlow()

    // Updated to hold PlaceWithHitRate
    private val _places = MutableStateFlow<List<PlaceWithHitRate>>(emptyList())
    val places = _places.asStateFlow()

    private val _races = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val races = _races.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private val _noMoreData = MutableStateFlow(false)
    val noMoreData = _noMoreData.asStateFlow()

    private var lastLoadedMonth: YearMonth? = null

    sealed class RaceEvent {
        object NoMoreData : RaceEvent()
    }

    private val _events = Channel<RaceEvent>()
    val events = _events.receiveAsFlow()

    private val firestore = Firebase.firestore

    fun fetchDates(isRefresh: Boolean = false) {
        if (_isRefreshing.value || _isLoadingMore.value) return

        viewModelScope.launch {
            val now = YearMonth.now()
            val isInitial = isRefresh || _dates.value.isEmpty()

            if (isInitial) {
                _isRefreshing.value = true
                _noMoreData.value = false
                lastLoadedMonth = now.minusMonths(1) // 今月と先月分を対象にする
                
                val startDate = lastLoadedMonth!!.atDay(1).toString()
                
                firestore.collection("predictions/dark_horse/race_date")
                    .whereGreaterThanOrEqualTo("race_date", startDate)
                    .orderBy("race_date", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        _dates.value = snapshot.map { it.id }
                        _isRefreshing.value = false
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        _isRefreshing.value = false
                    }
            } else {
                if (_noMoreData.value) {
                    _events.send(RaceEvent.NoMoreData)
                    return@launch
                }
                
                _isLoadingMore.value = true
                val targetMonth = lastLoadedMonth?.minusMonths(1) ?: return@launch
                val startDate = targetMonth.atDay(1).toString()
                val endDate = lastLoadedMonth!!.atDay(1).toString()

                firestore.collection("predictions/dark_horse/race_date")
                    .whereGreaterThanOrEqualTo("race_date", startDate)
                    .whereLessThan("race_date", endDate)
                    .orderBy("race_date", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val newDates = snapshot.map { it.id }
                        if (newDates.isEmpty()) {
                            // この月が空なら、さらに過去にデータがあるか確認
                            checkIfNoMoreData(startDate)
                        } else {
                            _dates.value = _dates.value + newDates
                            lastLoadedMonth = targetMonth
                            _isLoadingMore.value = false
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        _isLoadingMore.value = false
                    }
            }
        }
    }

    private fun checkIfNoMoreData(beforeDate: String) {
        firestore.collection("predictions/dark_horse/race_date")
            .whereLessThan("race_date", beforeDate)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    _noMoreData.value = true
                    _isLoadingMore.value = false
                    viewModelScope.launch {
                        _events.send(RaceEvent.NoMoreData)
                    }
                } else {
                    // まだ過去にデータがある（この月がたまたま空だった）場合はさらに遡る
                    lastLoadedMonth = lastLoadedMonth?.minusMonths(1)
                    _isLoadingMore.value = false
                    fetchDates(false)
                }
            }
            .addOnFailureListener {
                _isLoadingMore.value = false
            }
    }

    // Updated to fetch hit rates
    fun fetchPlaces(date: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _places.value = emptyList()
            firestore.collection("predictions/dark_horse/race_date/${date}/place").get()
                .addOnSuccessListener { placesSnapshot ->
                    val placeNames = placesSnapshot.map { it.id }
                    val placesWithHitRates = mutableListOf<PlaceWithHitRate>()

                    if (placeNames.isEmpty()) {
                        _places.value = emptyList()
                        _isRefreshing.value = false
                        return@addOnSuccessListener
                    }

                    var tasksCompleted = 0
                    for (placeName in placeNames) {
                        firestore.collection("hit_rate/dark_horse/place/${placeName}/rate").get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val hitRates = task.result.map { doc -> doc.toObject<HitRate>() }
                                    placesWithHitRates.add(PlaceWithHitRate(name = placeName, hitRates = hitRates))
                                } else {
                                    task.exception?.printStackTrace()
                                }

                                tasksCompleted++
                                if (tasksCompleted == placeNames.size) {
                                    _places.value = placesWithHitRates
                                    _isRefreshing.value = false
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    _places.value = emptyList()
                    _isRefreshing.value = false
                }
        }
    }

    fun fetchRaces(date: String, place: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            firestore.collection("predictions/dark_horse/race_date/${date}/place/${place}/races")
                .get()
                .addOnSuccessListener { documents ->
                    val racesMap = mutableMapOf<String, List<Int>>()
                    documents.forEach { document ->
                        val rankingList = document.get("ranking") as? List<Long> ?: emptyList()
                        racesMap[document.id] = rankingList.map { it.toInt() }
                    }
                    _races.value = racesMap.toSortedMap(compareBy { it.removeSuffix("R").toInt() })
                    _isRefreshing.value = false
                }
                .addOnFailureListener { e ->
                    Log.e("RaceList", "fetchRaces: ", e)
                    e.printStackTrace()
                    _races.value = emptyMap()
                    _isRefreshing.value = false
                }
        }
    }
}
