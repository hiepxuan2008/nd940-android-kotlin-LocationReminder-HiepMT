package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.GeofencingConstants
import com.udacity.project4.utils.PermissionUtils
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding

    private var map: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentMarker: Marker? = null
    private var currentCircleMarker: Circle? = null
    private var currentPOI: PointOfInterest? = null
    val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        binding.btnSaveLocation.setOnClickListener {
            _viewModel.onSaveLocation(currentPOI)
        }
        binding.btnSaveLocation.visibility = View.GONE

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        // Build the map.
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        _viewModel.selectedPOI.value?.let { poi ->
            updateCurrentPoi(poi)
        }

        setMapClick(map)
        setPoiClick(map)
        setMapStyle(map)
        requestUserLocationAndMoveCamera()
    }

    private fun isFineLocationPermissionGranted(): Boolean {
        return PermissionUtils.isGranted(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @SuppressLint("MissingPermission")
    private fun requestUserLocationAndMoveCamera() {
        when {
            isFineLocationPermissionGranted() -> {
                map?.isMyLocationEnabled = true
                enableLocationServiceAndMoveCameraToUserLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.location_permission_required_rationale,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(android.R.string.ok) {
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_LOCATION_PERMISSION
                        )
                    }.show()
            }
            else -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        }
    }

    private fun setMapClick(map: GoogleMap?) {
        map?.setOnMapClickListener { latLng ->
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses.size > 0) {
                binding.btnSaveLocation.visibility = View.VISIBLE
                val address: String = addresses[0].getAddressLine(0)
                updateCurrentPoi(PointOfInterest(latLng, null, address))
            }
        }
    }

    private fun setPoiClick(map: GoogleMap?) {
        map?.setOnPoiClickListener { poi ->
            binding.btnSaveLocation.visibility = View.VISIBLE
            updateCurrentPoi(poi)
        }
    }

    private fun updateCurrentPoi(poi: PointOfInterest) {
        currentMarker?.remove()
        currentCircleMarker?.remove()

        currentPOI = poi

        // Add Marker
        currentMarker = map?.addMarker(
            MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)
        )
        currentMarker?.showInfoWindow()

        // Add circle range
        currentCircleMarker = map?.addCircle(
            CircleOptions()
                .center(poi.latLng)
                .radius(GeofencingConstants.GEOFENCE_RADIUS_IN_METERS)
                .fillColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.geofencing_circle_fill_color
                    )
                )
                .strokeColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.geofencing_circle_stroke_color
                    )
                )
        )
    }

    private fun setMapStyle(map: GoogleMap?) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.google_map_style)
            )
            if (success != true) {
                Timber.e("Style parsing failed.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                // Check if location permissions are granted and if so enable the
                // location data layer.
                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestUserLocationAndMoveCamera()
                } else {
                    // Show messages to telling the user why your app actually requires the location permission.
                    // In case they previously chose "Deny & don't ask again",
                    // tell your users where to manually enable the location permission.
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        R.string.location_permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(R.string.settings) {
                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }.show()
                }
            }
        }
    }

    // Ref: https://developer.android.com/training/location/retrieve-current
    var retrieveLocationRetry = 0
    val RETRIE_LOCATION_MAXIMUM = 5
    val RETRIE_LOCATION_DELAY = 1000L
    private fun fetchLocationAndMoveCamera() {
        Timber.d("HiepMT: Come getDeviceLocationAndMoveCamera")
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (isFineLocationPermissionGranted()) {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    DEFAULT_ZOOM.toFloat()
                                )
                            )
                            retrieveLocationRetry = 0
                        } else {
                            Timber.e("Location is null!")
                            // Sometime, it can't get location immediately, need some delay
                            if (retrieveLocationRetry < RETRIE_LOCATION_MAXIMUM) {
                                retrieveLocationRetry += 1
                                uiHandler.postDelayed({
                                    fetchLocationAndMoveCamera()
                                }, RETRIE_LOCATION_DELAY)
                            }
                        }
                    }
            }
        } catch (e: SecurityException) {
            Timber.e(e)
        }
    }

    /*
   *  Uses the Location Client to check the current state of location settings, and gives the user
   *  the opportunity to turn on location services within our app.
   */
    private fun enableLocationServiceAndMoveCameraToUserLocation(needResolve: Boolean = true) {
        Timber.d("Come enableLocationServiceAndMoveCameraToUserLocation")
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
                startIntentSenderForResult(exception.resolution.intentSender,
                    REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null)
            } else {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.location_required_for_select_location_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    enableLocationServiceAndMoveCameraToUserLocation()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                fetchLocationAndMoveCamera()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_TURN_DEVICE_LOCATION_ON -> {
                enableLocationServiceAndMoveCameraToUserLocation(false)
            }
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 1002
        private const val DEFAULT_ZOOM = 16
    }
}
