package com.example.mobvcviko1.viewmodels
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.mobvcviko1.Utils.Evento
import com.example.mobvcviko1.data.DataRepository
import com.example.mobvcviko1.data.db.entities.UserEntity
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: DataRepository) : ViewModel() {

    val feed_items: LiveData<List<UserEntity>?> =
        liveData {
            loading.postValue(true)
            repository.apiListGeofence(null)
            loading.postValue(false)
            emitSource(repository.getUsers())

        }

    val loading = MutableLiveData(false)

    private val _message = MutableLiveData<Evento<String>>()
    val message: LiveData<Evento<String>>
        get() = _message

    fun updateItems(context: Context?) {
        viewModelScope.launch {
            loading.postValue(true)
            _message.postValue(Evento(repository.apiListGeofence(context)))
            loading.postValue(false)
        }
    }
}
