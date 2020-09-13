package com.androiddevs.runningappyt.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androiddevs.runningappyt.db.Run
import com.androiddevs.runningappyt.other.SortType
import com.androiddevs.runningappyt.repositories.MainRepository
import kotlinx.coroutines.launch

class MainViewModel @ViewModelInject constructor(
    val mainRepository: MainRepository
) : ViewModel() {

    private val runsSortedByteDate = mainRepository.getAllRunsSortedByDate()
    private val runsSortedByDistance = mainRepository.getAllRunsSortedByDistance()
    private val runsSortedByteAvgSpeed = mainRepository.getAllRunsSortedByAvgSpeed()
    private val runsSortedByCaloriesBurned = mainRepository.getAllRunsSortedByCaloriesBurned()
    private val runsSortedByTimeMillis = mainRepository.getAllRunsSortedByTimeInMillis()

    val runs = MediatorLiveData<List<Run>>()
    var sortType = SortType.DATE

    init {
        runs.addSource(runsSortedByteDate) { result ->
            if (sortType == SortType.DATE) {
                result?.let {
                    runs.value = it
                }
            }
        }

        runs.addSource(runsSortedByteAvgSpeed) { result ->
            if (sortType == SortType.AVG_SPEED) {
                result?.let {
                    runs.value = it
                }
            }
        }
        runs.addSource(runsSortedByCaloriesBurned) { result ->
            if (sortType == SortType.CALORIES_BURNED) {
                result?.let {
                    runs.value = it
                }
            }
        }
        runs.addSource(runsSortedByDistance) { result ->
            if (sortType == SortType.DISTANCE) {
                result?.let {
                    runs.value = it
                }
            }
        }
        runs.addSource(runsSortedByTimeMillis) { result ->
            if (sortType == SortType.RUNNING_TIME) {
                result?.let {
                    runs.value = it
                }
            }
        }


    }


    fun sortRuns(sortType: SortType) = when (sortType) {
        SortType.DATE -> runsSortedByteDate.value?.let { runs.value = it }
        SortType.RUNNING_TIME -> runsSortedByTimeMillis.value?.let { runs.value = it }
        SortType.AVG_SPEED -> runsSortedByteAvgSpeed.value?.let { runs.value = it }
        SortType.CALORIES_BURNED -> runsSortedByCaloriesBurned.value?.let { runs.value = it }
        SortType.DISTANCE -> runsSortedByDistance.value?.let { runs.value = it }
    }.also {
        this.sortType=sortType
    }


    fun insertRun(run: Run) = viewModelScope.launch {
        mainRepository.insertRun(run)
    }


}