# Location Reminder Project

## Project Requirements
In this project, you will create a TODO list app with location reminders that remind the user to do something when the user is at a specific location. 

The app will require the user to create an account and login to set and access reminders.

#### User Authentication: 
- Create Login and Registration screens: 
  + Login screen allows users to login using email or a Google Account
  + If the user does not exist, the app navigates to a Registration screen
- Enable user accounts using Firebase Authentication and Firebase UI.
  - The project includes a FirebaseUI dependency
  - Authentication is enabled through the Firebase console.

#### Map & Geofencing:

- Create a Map view that shows the user's current location
  - A screen that shows a map and asks the user to allow the location permission to show his location on the map.
  - After user granted permission, move camera to current user location.
  - The app works on all the different Android versions including Android Q.
- Add functionality to allow the user to select POIs or any locations to set reminders
  - The app asks the user to select a location or POI on the map and add a new marker at that location
  - Upon saving, the selected location is returned to the Save Reminder page and the user is asked to input the title and description for the reminder.
  - When the reminder is saved, a geofencing request is created.
- Style the map
  - Map Styling has been updated using the map styling wizard to generate a nice looking map.
  - Users have the option to change map type: `MAP_TYPE_NORMAL`, `MAP_TYPE_HYBRID`, `MAP_TYPE_SATELLITE`, `MAP_TYPE_TERRAIN`
- Display a notification when a selected POI is reached
  - When the user enters a geofence, a reminder is retrieved from the local storage and a notification showing the reminder title will appear, even if the app is not open.

#### Reminders:

- Add a screen to create reminders
  - Reminder data includes title and description.
  - The user-entered data will be captured using live data and data binding.
  - `RemindersLocalRepository` is used to save the reminder to the local DB. And the geofencing request will be created after confirmation.
- Add a list view that displays the reminders
  - All reminders in the location DB is displayed
  - If the location DB is empty, a no data indicator is displayed.
  - User can navigate from this screen to another screen to create a new reminder.
- Display details about a reminder when saved locations are reached and the user clicked on the notification.
  - When the user clicks a notification, when he clicks on it, a new screen appears to display the reminder details.

#### Testing

- Test the ViewModels, Coroutines, and LiveData
  - `RemindersListViewModelTest` or `SaveReminderViewModelTest` are present in the `test` package that tests the functions inside the view model.
  - Live data objects are tested using `shouldReturnError` and `check_loading` testing functions.
- Create a FakeDataSource to replace the Data Layer and test the app in isolation.
  - Project repo contains a `FakeDataSource` class that acts as a test double for the `LocalDataSource`.
- Use Espresso and Mockito to test the app UI and Fragments Navigation, Tests include:
  - Automation Testing using ViewMatchers and ViewInteractions to simulate user interactions with the app.
  - Testing for Snackbar and Toast messages.
  - Testing the fragments’ navigation.
  - The testing classes are at `androidTest` package.
- Test DAO and Repository classes:
  - Testing uses `Room.inMemoryDatabaseBuilder` to create a Room DB instance.
  - Testing covers:
    - inserting and retrieving data using DAO.
    - predictable errors like data not found.

## Techical Skills

- MVVM + ViewModel + LiveData + Data Binding
- Room Database
- Kotlin Coroutines
- Navigation component between Fragments
- [Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/start) - Display Google Map, click on Point of Interest.
  - [Custom Google Map Style](https://mapstyle.withgoogle.com/)
  - [Get device location](https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial#get-the-location-of-the-android-device-and-position-the-map) , move camera to current user location.
- Location permission: Manifest permission & Runtime permission
- [Geofencing API](https://developers.google.com/location-context/geofencing) - Provide contextual experiences when users enter or leave an area of interest.
- Local Test, Instrument Test using Mockito and Espresso.
- [Koin](https://github.com/InsertKoinIO/koin) - A pragmatic lightweight dependency injection framework for Kotlin.
- [FirebaseUI Authentication](https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md) - FirebaseUI provides a drop-in auth solution that handles the UI flows for signing.
- [JobIntentService](https://developer.android.com/reference/androidx/core/app/JobIntentService) - Run background service from the background application, Compatible with >= Android O.

## How to build the app

1. Clone this repository.
2. Open `starter` folder via Android Studio IDE
3. Go to `google_maps_api.xml` file, replace with your api key under `google_maps_key` variable

## Demo

#### Demo main features

https://user-images.githubusercontent.com/6292433/129083589-7926208d-4c1e-4ece-b5d1-fecb4fc72da0.mp4

#### Demo Espresso Testing `RemindersActivityTest`


https://user-images.githubusercontent.com/6292433/129083750-e4840d24-9cb7-40a5-9dc8-e758f5fbaa4b.mp4


## License
Mai Thanh Hiep & Udacity

Course link: https://www.udacity.com/course/android-kotlin-developer-nanodegree--nd940
