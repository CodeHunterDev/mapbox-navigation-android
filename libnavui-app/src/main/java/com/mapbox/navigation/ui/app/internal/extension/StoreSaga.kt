package com.mapbox.navigation.ui.app.internal.extension

import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.Middleware
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.reflect.KClass

suspend fun Store.takeAction(predicate: (action: Action) -> Boolean): Action =
    suspendCancellableCoroutine { cont ->
        val m = object : Middleware {
            override fun onDispatch(state: State, action: Action): Boolean {
                if (predicate(action)) {
                    cont.resume(action)
                    unregisterMiddleware(this)
                }
                return false
            }
        }
        registerMiddleware(m)
        cont.invokeOnCancellation { unregisterMiddleware(m) }
    }

internal suspend fun <T : Action> Store.takeAction(actionCls: KClass<T>): T =
    takeAction { actionCls.isInstance(it) } as T

internal fun <T : Action> Store.takeEveryAction(
    coroutineScope: CoroutineScope,
    actionCls: KClass<T>,
    block: suspend (action: T) -> Unit
): Job {
    return coroutineScope.launch {
        while (isActive) {
            val action = takeAction(actionCls)
            block(action)
        }
    }
}
