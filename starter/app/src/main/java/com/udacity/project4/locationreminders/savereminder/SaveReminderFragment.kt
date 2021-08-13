package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.ext.ifSupportsFromAndroidQ
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.GeofencingConstants
import com.udacity.project4.utils.PermissionUtils
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient
    private var tobeAddedReminderDataItem: ReminderDataItem? = null


    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        observeViewModel()

        return binding.root
    }

    private fun observeViewModel() {
        _viewModel.addGeoFencingRequest.observe(viewLifecycleOwner, Observer { item ->
            item?.let {
                checkPermissionsAndStartGeofencing(it)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val selectedPoi = _viewModel.selectedPOI.value
            val location = selectedPoi?.name
            val latitude = selectedPoi?.latLng?.latitude
            val longitude = selectedPoi?.latLng?.longitude

            val reminderDataItem = ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude
            )

            _viewModel.onSaveReminder(reminderDataItem)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    /*
*  When we get the result from asking the user to turn on device location, we call
*  checkDeviceLocationSettingsAndStartGeofence again to make sure it's actually on, but
*  we don't resolve the check to keep the user from seeing an endless loop.
*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    /*
     * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
     * the background permission as well.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            val explanationResId = if (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE)
                R.string.location_background_permission_geo_fencing
            else
                R.string.location_permission_geo_fencing

            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                explanationResId,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    /**
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current hint isn't yet active.
     */
    fun checkPermissionsAndStartGeofencing(reminderDataItem: ReminderDataItem) {
        tobeAddedReminderDataItem = reminderDataItem

        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartGeofence(needResolve: Boolean = true) {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(
            LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_LOW_POWER
            }
        )
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && needResolve) {
                startIntentSenderForResult(exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null)
            } else {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.location_required_for_geofencing_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForClue()
            }
        }
    }

    /*
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundGranted = PermissionUtils.isGranted(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        var backgroundPermissionApproved = true
        ifSupportsFromAndroidQ {
            backgroundPermissionApproved = PermissionUtils.isGranted(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        return foregroundGranted && backgroundPermissionApproved
    }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        var requestCode = REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        ifSupportsFromAndroidQ {
            permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
            requestCode = REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
        }
        requestPermissions(
            permissionsArray,
            requestCode
        )
    }

    /*
     * Adds a Geofence for the current clue if needed, and removes any existing Geofence. This
     * method should be called after the user has granted the location permission.  If there are
     * no more geofences, we remove the geofence and let the viewmodel know that the ending hint
     * is now "active."
     */
    @SuppressLint("MissingPermission")
    private fun addGeofenceForClue() {
        val reminderDataItem = tobeAddedReminderDataItem
        tobeAddedReminderDataItem = null
        reminderDataItem ?: return

        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                _viewModel.onAddGeofencingSucceeded(reminderDataItem)
                Timber.d("Add Geofence ${geofence.requestId}")
            }
            addOnFailureListener {
                _viewModel.onAddGeofencingFailed()
            }
        }
    }


    companion object {
        const val ACTION_GEOFENCE_EVENT = "action.ADD.GEOFENCE.LOCATION_REMINDER"
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    }
}
