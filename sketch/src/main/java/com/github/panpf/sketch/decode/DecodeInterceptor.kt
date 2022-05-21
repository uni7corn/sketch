package com.github.panpf.sketch.decode

import androidx.annotation.WorkerThread
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.internal.RequestContext

fun interface DecodeInterceptor<RESULT : DecodeResult> {

    @WorkerThread
    suspend fun intercept(chain: Chain<RESULT>): RESULT

    interface Chain<RESULT : DecodeResult> {

        val sketch: Sketch

        val request: ImageRequest

        val requestContext: RequestContext

        val fetchResult: FetchResult?

        @WorkerThread
        suspend fun proceed(): RESULT
    }
}