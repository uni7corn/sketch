package com.github.panpf.sketch.compose

import com.github.panpf.sketch.compose.request.internal.JvmBitmapToComposeBitmapRequestInterceptor
import com.github.panpf.sketch.compose.request.internal.SkiaBitmapToComposeBitmapRequestInterceptor
import com.github.panpf.sketch.compose.resize.ComposeResizeOnDrawHelper
import com.github.panpf.sketch.compose.transition.ComposeCrossfadeTransition
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.ImageRequest.Builder
import com.github.panpf.sketch.request.crossfade
import com.github.panpf.sketch.request.resizeOnDraw

actual fun updateRequestBuilder(request: ImageRequest, builder: Builder) {
    val transitionFactory = request.transitionFactory
    val crossfade = request.crossfade
    if (transitionFactory == null && crossfade != null) {
        builder.transitionFactory(
            ComposeCrossfadeTransition.Factory(
                durationMillis = crossfade.durationMillis,
                fadeStart = crossfade.fadeStart,
                preferExactIntrinsicSize = crossfade.preferExactIntrinsicSize,
                alwaysUse = crossfade.alwaysUse,
            )
        )
    }

    val resizeOnDrawHelper = request.resizeOnDrawHelper
    val resizeOnDraw = request.resizeOnDraw
    if (resizeOnDrawHelper == null && resizeOnDraw == true) {
        builder.resizeOnDraw(ComposeResizeOnDrawHelper)
    }

    builder.mergeComponents {
        addRequestInterceptor(JvmBitmapToComposeBitmapRequestInterceptor())
        addRequestInterceptor(SkiaBitmapToComposeBitmapRequestInterceptor())
    }
}