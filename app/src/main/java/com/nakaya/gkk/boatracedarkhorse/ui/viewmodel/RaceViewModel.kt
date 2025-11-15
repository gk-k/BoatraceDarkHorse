package com.nakaya.gkk.boatracedarkhorse.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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

    private val firestore = Firebase.firestore

    fun fetchDates() {
        viewModelScope.launch {
            _isRefreshing.value = true
            firestore.collection("predictions/dark_horse/race_date")
                .orderBy("race_date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener {
                    _dates.value = it.map { doc -> doc.id }
                    _isRefreshing.value = false
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    _dates.value = emptyList()
                    _isRefreshing.value = false
                }
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
