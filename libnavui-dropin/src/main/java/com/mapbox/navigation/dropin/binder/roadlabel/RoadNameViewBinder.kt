package com.mapbox.navigation.dropin.binder.roadlabel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.roadlabel.RoadNameLabelComponent
import com.mapbox.navigation.dropin.databinding.MapboxRoadNameLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

@ExperimentalPreviewMapboxNavigationAPI
internal class RoadNameViewBinder(
    private val context: NavigationViewContext
) : UIBinder {

    private val loadedMapStyle = context.mapStyleLoader.loadedMapStyle

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_road_name_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxRoadNameLayoutBinding.bind(viewGroup)

        return reloadOnChange(
            loadedMapStyle,
            context.styles.roadNameTextAppearance,
            context.styles.roadNameBackground
        ) { style, appearance, background ->
            if (style != null) {
                val store = context.store
                RoadNameLabelComponent(store, binding.roadNameView, style, appearance, background)
            } else {
                null
            }
        }
    }
}
