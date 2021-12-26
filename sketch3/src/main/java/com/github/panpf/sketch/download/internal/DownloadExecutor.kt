package com.github.panpf.sketch.download.internal

import androidx.annotation.WorkerThread
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.common.ImageRequest
import com.github.panpf.sketch.common.Listener
import com.github.panpf.sketch.common.ProgressListener
import com.github.panpf.sketch.common.internal.ListenerDelegate
import com.github.panpf.sketch.download.*
import kotlinx.coroutines.CancellationException

class DownloadExecutor(private val sketch: Sketch) {

    @WorkerThread
    suspend fun execute(
        request: DownloadRequest,
        listener: Listener<DownloadRequest, DownloadData>?,
        httpFetchProgressListener: ProgressListener<ImageRequest>?,
    ): DownloadResult {
        val listenerDelegate = listener?.run {
            ListenerDelegate(this)
        }

        try {
            listenerDelegate?.onStart(request)

            val result: DownloadResult = DownloadInterceptorChain(
                initialRequest = request,
                interceptors = sketch.downloadInterceptors,
                index = 0,
                request = request,
            ).proceed(sketch, request, httpFetchProgressListener)

            if (listenerDelegate != null) {
                when (result) {
                    is DownloadSuccessResult -> {
                        listenerDelegate.onSuccess(request, result.data)
                    }
                    is DownloadErrorResult -> {
                        listenerDelegate.onError(request, result.throwable)
                    }
                }
            }
            return result
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                listenerDelegate?.onCancel(request)
                throw throwable
            } else {
                listenerDelegate?.onError(request, throwable)
                return DownloadErrorResult(throwable)
            }
        }
    }
}