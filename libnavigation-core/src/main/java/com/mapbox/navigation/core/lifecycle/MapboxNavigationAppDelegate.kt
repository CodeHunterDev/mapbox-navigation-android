package com.mapbox.navigation.core.lifecycle

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.utils.internal.logI
import kotlin.reflect.KClass

/**
 * This is a testable version of [MapboxNavigationApp]. Please refer to the singleton
 * for documented functions and expected behaviors.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxNavigationAppDelegate {
    private val mapboxNavigationOwner by lazy { MapboxNavigationOwner() }
    private val carAppLifecycleOwner by lazy { CarAppLifecycleOwner() }

    val lifecycleOwner: LifecycleOwner by lazy { carAppLifecycleOwner }

    var isSetup = false

    fun setup(navigationOptionsProvider: NavigationOptionsProvider) = apply {
        if (carAppLifecycleOwner.isConfigurationChanging()) {
            return this
        }

        if (isSetup) {
            logI(
                """
                    MapboxNavigationApp.setup was ignored because it has already been setup.
                    If you want to use new NavigationOptions, you must first call
                    MapboxNavigationApp.disable() and then call MapboxNavigationApp.setup(..).
                    Calling setup multiple times, is harmless otherwise.
                """.trimIndent(),
                LOG_CATEGORY
            )
            return this
        }

        mapboxNavigationOwner.setup(navigationOptionsProvider)
        carAppLifecycleOwner.lifecycle.addObserver(mapboxNavigationOwner.carAppLifecycleObserver)
        isSetup = true
    }

    fun attachAllActivities(application: Application) {
        carAppLifecycleOwner.attachAllActivities(application)
    }

    fun disable() {
        isSetup = false
        carAppLifecycleOwner.lifecycle.removeObserver(mapboxNavigationOwner.carAppLifecycleObserver)
        mapboxNavigationOwner.disable()
    }

    fun attach(lifecycleOwner: LifecycleOwner) {
        carAppLifecycleOwner.attach(lifecycleOwner)
    }

    fun detach(lifecycleOwner: LifecycleOwner) {
        carAppLifecycleOwner.detach(lifecycleOwner)
    }

    fun registerObserver(mapboxNavigationObserver: MapboxNavigationObserver) {
        mapboxNavigationOwner.register(mapboxNavigationObserver)
    }

    fun unregisterObserver(mapboxNavigationObserver: MapboxNavigationObserver) {
        mapboxNavigationOwner.unregister(mapboxNavigationObserver)
    }

    fun current(): MapboxNavigation? = mapboxNavigationOwner.current()

    fun <T : MapboxNavigationObserver> getObserver(clazz: Class<T>): T =
        mapboxNavigationOwner.getObserver(clazz)

    fun <T : MapboxNavigationObserver> getObserver(kClass: KClass<T>): T =
        mapboxNavigationOwner.getObserver(kClass)

    fun <T : MapboxNavigationObserver> getObservers(clazz: Class<T>): List<T> =
        mapboxNavigationOwner.getObservers(clazz)

    fun <T : MapboxNavigationObserver> getObservers(kClass: KClass<T>): List<T> =
        mapboxNavigationOwner.getObservers(kClass)

    private companion object {
        private const val LOG_CATEGORY = "MapboxNavigationAppDelegate"
    }
}
