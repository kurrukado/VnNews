package com.example.vnnews

import com.example.vnnews.data.local.Converters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for Room [Converters] (Unit 6: Room TypeConverters).
 * Verifies serialization and deserialization between [Date] and epoch millisecond [Long].
 */
class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun fromTimestamp_withValidEpochMillis_returnsCorrectDate() {
        val epochMillis = 1730000000000L
        val date = converters.fromTimestamp(epochMillis)

        assertNotNull(date)
        assertEquals(epochMillis, date?.time)
    }

    @Test
    fun fromTimestamp_withNull_returnsNull() {
        val result = converters.fromTimestamp(null)
        assertNull(result)
    }

    @Test
    fun fromTimestamp_withEpochZero_returnsEpochDate() {
        val result = converters.fromTimestamp(0L)

        assertNotNull(result)
        assertEquals(0L, result?.time)
    }

    @Test
    fun fromTimestamp_withNegativeTimestamp_returnsPreEpochDate() {
        val negativeMillis = -86400000L // 1 day before Jan 1, 1970
        val result = converters.fromTimestamp(negativeMillis)

        assertNotNull(result)
        assertEquals(negativeMillis, result?.time)
    }

    @Test
    fun dateToTimestamp_withValidDate_returnsCorrectEpochMillis() {
        val epochMillis = 1730000000000L
        val date = Date(epochMillis)

        val result = converters.dateToTimestamp(date)

        assertNotNull(result)
        assertEquals(epochMillis, result)
    }

    @Test
    fun dateToTimestamp_withNullDate_returnsNull() {
        val result = converters.dateToTimestamp(null)
        assertNull(result)
    }

    @Test
    fun roundTripConversion_fromTimestampToDateAndBack_preservesExactValue() {
        val originalTimestamp = 1698380000000L
        val date = converters.fromTimestamp(originalTimestamp)
        val roundTripTimestamp = converters.dateToTimestamp(date)

        assertEquals(originalTimestamp, roundTripTimestamp)
    }

    @Test
    fun roundTripConversion_fromDateToTimestampAndBack_preservesExactValue() {
        val originalDate = Date(1698380000000L)
        val timestamp = converters.dateToTimestamp(originalDate)
        val roundTripDate = converters.fromTimestamp(timestamp)

        assertEquals(originalDate, roundTripDate)
        assertEquals(originalDate.time, roundTripDate?.time)
    }
}
