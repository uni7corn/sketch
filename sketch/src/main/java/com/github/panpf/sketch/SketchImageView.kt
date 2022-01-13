package com.github.panpf.sketch

import android.content.Context
import android.util.AttributeSet
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.viewability.AbilityImageView

open class SketchImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AbilityImageView(context, attrs) {
    override fun submitRequest(request: DisplayRequest) {
        val newRequest = request.newDisplayRequest {
            target(this@SketchImageView)
        }
        context.sketch.enqueueDisplay(newRequest)
    }
}