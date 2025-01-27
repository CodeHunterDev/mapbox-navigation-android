package com.mapbox.navigation.dropin.binder.infopanel

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelBinder(
    private val headerBinder: UIBinder?,
    private val contentBinder: UIBinder?
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val binders = mutableListOf<MapboxNavigationObserver>()
        if (headerBinder != null) {
            binders.add(headerBinder.bind(viewGroup.findViewById(R.id.infoPanelHeader)))
        }
        if (contentBinder != null) {
            binders.add(contentBinder.bind(viewGroup.findViewById(R.id.infoPanelContent)))
        }
        return navigationListOf(*binders.toTypedArray())
    }
}
