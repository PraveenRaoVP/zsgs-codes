/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.example.sleeptracker.sleepquality

import android.example.sleeptracker.database.SleepDatabaseDao
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SleepQualityViewModel(
    private val sleepQualityKey: Long = 0L,
    val database: SleepDatabaseDao
) : ViewModel() {
    private val viewModelJob = Job()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private val _navigateToSleepTracker = MutableLiveData<Boolean?>()
    val navigateToSleepTracker: LiveData<Boolean?>
        get() = _navigateToSleepTracker

    fun doneNavigating() {
        _navigateToSleepTracker.value = null
    }

    fun onSetSleepQuality(quality: Int) {
        viewModelScope.launch {// instead of creating a new coroutine scope, we can use the viewModelScope instead of ui scope. This way, the coroutine will be canceled when the ViewModel is destroyed.
            withContext(Dispatchers.IO) { // this is a database operation, so it should be done in the IO context
                val tonight = database.get(sleepQualityKey) ?: return@withContext
                tonight.sleepQuality = quality
                database.update(tonight)
            }
            _navigateToSleepTracker.value = true // this is written outside the withContext block because it is not a database operation
        }
    }

}