package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.SingleLiveEvent
import kotlinx.coroutines.launch

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {
    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    val selectedPOI = MutableLiveData<PointOfInterest>()

    val addGeoFencingRequest = SingleLiveEvent<ReminderDataItem>()

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        selectedPOI.value = null

        addGeoFencingRequest.value = null
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrBlank()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrBlank()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }

    fun onSaveReminder(reminderDataItem: ReminderDataItem) {
        if (validateEnteredData(reminderDataItem)) {
            addGeoFencingRequest.value = reminderDataItem
        }
    }

    fun onAddGeofencingSucceeded(reminderData: ReminderDataItem) {
        showSnackBarInt.value = R.string.geofences_added
        saveReminder(reminderData)
    }

    fun onAddGeofencingFailed() {
        showSnackBarInt.value = R.string.geofences_not_added
    }

    fun onSaveLocation(poi: PointOfInterest?) {
        selectedPOI.value = poi
        navigationCommand.value = NavigationCommand.Back
    }
}