package com.mapbox.navigation.dropin.component.marker

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.util.HapticFeedback
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class FreeDriveLongPressMapComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val mockGesturesPlugin: GesturesPlugin = mockk(relaxed = true)
    private val mockMapView: MapView = mockk {
        every { gestures } returns mockGesturesPlugin
    }
    private val mockMapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { navigationOptions } returns mockk {
            every { applicationContext } returns mockk(relaxed = true)
        }
    }

    lateinit var sut: FreeDriveLongPressMapComponent
    private lateinit var testStore: TestStore

    @Before
    fun setUp() {
        mockkObject(HapticFeedback)
        every { HapticFeedback.create(any()) } returns mockk(relaxed = true)
        testStore = spyk(TestStore())

        sut = FreeDriveLongPressMapComponent(testStore, mockMapView)
    }

    @After
    fun tearDown() {
        unmockkObject(HapticFeedback)
    }

    @Test
    fun `should register OnMapLongClickListener in onAttached`() {
        sut.onAttached(mockMapboxNavigation)

        verify { mockGesturesPlugin.addOnMapLongClickListener(any()) }
    }

    @Test
    fun `should unregister OnMapLongClickListener in onDetached`() {
        sut.onAttached(mockMapboxNavigation)

        sut.onDetached(mockMapboxNavigation)

        verify { mockGesturesPlugin.removeOnMapLongClickListener(any()) }
    }

    @Test
    fun `onMapLongClick should update view model state`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        sut.onAttached(mockMapboxNavigation)

        val point = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(point)

        verifyOrder {
            testStore.dispatch(DestinationAction.SetDestination(Destination(point)))
            testStore.dispatch(RoutesAction.SetRoutes(emptyList()))
            testStore.dispatch(
                NavigationStateAction.Update(NavigationState.DestinationPreview)
            )
        }
    }
}
