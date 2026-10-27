package com.example.vnnews.fake

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDataStore(initialPreferences: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val _data = MutableStateFlow(initialPreferences)
    override val data: Flow<Preferences> = _data

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val transformed = transform(_data.value)
        _data.value = transformed
        return transformed
    }
}
