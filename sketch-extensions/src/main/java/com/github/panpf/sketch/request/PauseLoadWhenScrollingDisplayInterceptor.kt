package com.github.panpf.sketch.request

import androidx.annotation.MainThread
import com.github.panpf.sketch.request.RequestDepth.NETWORK
import com.github.panpf.sketch.request.RequestInterceptor.Chain

class PauseLoadWhenScrollingDisplayInterceptor : RequestInterceptor<DisplayRequest, DisplayData> {

    companion object {
        var scrolling = false
    }

    var enabled = true

    @MainThread
    override suspend fun intercept(chain: Chain<DisplayRequest, DisplayData>): DisplayData {
        val request = chain.request
        val requestDepth = request.depth
        val finalRequest = if (
            enabled
            && scrolling
            && request.isPauseLoadWhenScrolling
            && !request.isIgnoredPauseLoadWhenScrolling
            && requestDepth < RequestDepth.MEMORY
        ) {
            request.newDisplayRequest {
                depth(RequestDepth.MEMORY)
                setDepthFromPauseLoadWhenScrolling()
            }
        } else {
            request
        }
        return chain.proceed(finalRequest)
    }

    override fun toString(): String = "PauseLoadWhenScrollingDisplayInterceptor"
}