package com.example.vnnews

import com.example.vnnews.data.local.UserPreferencesRepository
import com.example.vnnews.fake.FakeDataStore
import com.example.vnnews.ui.MainViewModel
import com.example.vnnews.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var mainViewModel: MainViewModel

    @Before
    fun setUp() {
        fakeDataStore = FakeDataStore()
        userPreferencesRepository = UserPreferencesRepository(fakeDataStore)
        mainViewModel = MainViewModel(userPreferencesRepository)
    }

    @Test
    fun isDarkTheme_initiallyEmitsNull() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            mainViewModel.isDarkTheme.collect()
        }
        assertNull(mainViewModel.isDarkTheme.value)
    }

    @Test
    fun toggleTheme_switchesThemePreferenceAndEmitsNewValue() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            mainViewModel.isDarkTheme.collect()
        }

        // Initially toggle from false (light) to true (dark)
        mainViewModel.toggleTheme(currentDark = false)
        assertEquals(true, mainViewModel.isDarkTheme.value)

        // Toggle from true (dark) to false (light)
        mainViewModel.toggleTheme(currentDark = true)
        assertEquals(false, mainViewModel.isDarkTheme.value)
    }
}
