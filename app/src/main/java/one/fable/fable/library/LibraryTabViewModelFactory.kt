package one.fable.fable.library

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class LibraryTabViewModelFactory(private val tabPosition : Int) :ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryTabViewModel::class.java)){
            return LibraryTabViewModel(tabPosition) as T
        }
        throw  IllegalArgumentException("Unknown ViewModel Class")
    }
}