package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.*
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var localRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        localRepository =
            RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_getReminder_verifyCorrectData() = runBlocking {
        // GIVEN - Insert a reminder.
        val reminder = ReminderDTO("Reminder to meet Lionel Messi", "some description", "Eiffel Tower Paris", 48.859605453592486, 2.294072402754437)
        localRepository.saveReminder(reminder)

        // WHEN - Get the reminder by id from the local repo.
        val result = localRepository.getReminder(reminder.id)

        assertThat(result, not(nullValue()))
        result as Result.Success
        assertThat(result.data.id, `is`(reminder.id))
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getReminder_nonExistId_returnError() = runBlocking {
        // WHEN load non exist reminder by id
        val result = localRepository.getReminder("1")

        // THEN
        Assert.assertThat(result.succeeded, `is`(false))
        result as Result.Error
        assertThat(result.message, Matchers.`is`("Reminder not found!"))
    }

    @Test
    fun saveReminder_getReminders_verifyAllReminders() = runBlocking {
        // GIVEN - Insert 3 different reminders.
        val reminder1 = ReminderDTO("title 1", "description 1", "location 1", 10.55, 10.552)
        val reminder2 = ReminderDTO("title 2", "description 2", "location 2", 37.3333, 4.4)
        val reminder3 = ReminderDTO("title 3", "description 3", "location 3", 40.333,2.2)
        localRepository.saveReminder(reminder1)
        localRepository.saveReminder(reminder2)
        localRepository.saveReminder(reminder3)

        // WHEN load all reminders
        val result = localRepository.getReminders()

        // THEN all reminder is succeeded loaded
        Assert.assertThat(result.succeeded, `is`(true))
        result as Result.Success
        assertThat(result.data.size, `is`(3))
        assertThat(result.data[0].id, `is`(reminder1.id))
        assertThat(result.data[1].id, `is`(reminder2.id))
        assertThat(result.data[2].id, `is`(reminder3.id))
    }

    @Test
    fun saveReminders_deleteAllReminders_verifyEmpty() = runBlocking {
        // GIVEN - Insert 2 different reminders.
        val reminder1 = ReminderDTO("title 1", "description 1", "location 1", 10.55, 10.552)
        val reminder2 = ReminderDTO("title 2", "description 2", "location 2", 37.3333, 4.4)
        localRepository.saveReminder(reminder1)
        localRepository.saveReminder(reminder2)

        // WHEN load all reminders
        val result1 = localRepository.getReminders() as Result.Success

        // THEN loaded 2 items
        assertThat(result1.data.size, `is`(2))

        // WHEN delete all reminders
        localRepository.deleteAllReminders()

        // THEN all items is deleted
        val result2 = localRepository.getReminders() as Result.Success
        assertThat(result2.data.size, `is`(0))
    }
}