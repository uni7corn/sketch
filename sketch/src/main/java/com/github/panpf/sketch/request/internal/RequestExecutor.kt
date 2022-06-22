package com.github.panpf.sketch.request.internal

import android.view.View
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.annotation.MainThread
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.drawable.internal.tryToResizeDrawable
import com.github.panpf.sketch.request.DisplayData
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.DownloadData
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.DownloadResult
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.ImageResult
import com.github.panpf.sketch.request.LoadData
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.LoadResult
import com.github.panpf.sketch.target.DisplayTarget
import com.github.panpf.sketch.target.DownloadTarget
import com.github.panpf.sketch.target.LoadTarget
import com.github.panpf.sketch.target.Target
import com.github.panpf.sketch.target.ViewTarget
import com.github.panpf.sketch.transition.TransitionTarget
import com.github.panpf.sketch.util.SketchException
import com.github.panpf.sketch.util.UnknownException
import com.github.panpf.sketch.util.asOrNull
import com.github.panpf.sketch.util.awaitStarted
import com.github.panpf.sketch.util.requiredMainThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.job
import kotlin.coroutines.coroutineContext

class RequestExecutor {

    companion object {
        const val MODULE = "RequestExecutor"
    }

    @MainThread
    suspend fun execute(sketch: Sketch, request: ImageRequest, enqueue: Boolean): ImageResult {
        requiredMainThread()

        val requestContext = RequestContext(request)

        // globalImageOptions
        sketch.globalImageOptions?.let {
            requestContext.addRequest(requestContext.lastRequest.newBuilder().global(it).build())
        }

        // Wrap the request to manage its lifecycle.
        val requestDelegate =
            requestDelegate(sketch, requestContext.lastRequest, coroutineContext.job)
        requestDelegate.assertActive()

        val target = requestContext.lastRequest.target
        try {
            val uriString = requestContext.lastRequest.uriString
            if (uriString.isEmpty() || uriString.isBlank()) {
                throw UriInvalidException(uriString, "Request uri is empty or blank: $uriString")
            }

            // Set up the request's lifecycle observers. Cancel the request when destroy
            requestDelegate.start()

            // Enqueued requests suspend until the lifecycle is started.
            if (enqueue) {
                requestContext.lastRequest.lifecycle.awaitStarted()
            }

            // resolve resize size
            if (requestContext.lastRequest.resizeSize == null) {
                val resizeSize = requestContext.lastRequest.resizeSizeResolver?.size()
                if (resizeSize != null) {
                    requestContext.addRequest(requestContext.lastRequest.newRequest {
                        resizeSize(resizeSize)
                    })
                }
            }

            onStart(sketch, requestContext.lastRequest)

            val data = RequestInterceptorChain(
                sketch = sketch,
                initialRequest = requestContext.lastRequest,
                request = requestContext.lastRequest,
                requestContext = requestContext,
                interceptors = sketch.components.requestInterceptorList,
                index = 0,
            ).proceed(requestContext.lastRequest)

            val successResult = when (data) {
                is DisplayData -> DisplayResult.Success(
                    request = requestContext.lastRequest,
                    drawable = data.drawable.tryToResizeDrawable(
                        sketch,
                        requestContext.lastRequest
                    ),
                    imageInfo = data.imageInfo,
                    imageExifOrientation = data.imageExifOrientation,
                    dataFrom = data.dataFrom,
                    transformedList = data.transformedList
                )
                is LoadData -> LoadResult.Success(
                    request = requestContext.lastRequest,
                    bitmap = data.bitmap,
                    imageInfo = data.imageInfo,
                    imageExifOrientation = data.imageExifOrientation,
                    dataFrom = data.dataFrom,
                    transformedList = data.transformedList
                )
                is DownloadData -> DownloadResult.Success(
                    requestContext.lastRequest,
                    data,
                    data.dataFrom
                )
                else -> throw UnsupportedOperationException("Unsupported ImageData: ${data::class.java}")
            }
            // todo 返回最后的 request
            onSuccess(sketch, requestContext.lastRequest, target, successResult)
            return successResult
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                onCancel(sketch, requestContext.lastRequest)
                throw throwable
            } else {
                throwable.printStackTrace()
                val exception = throwable.asOrNull<SketchException>()
                    ?: UnknownException(throwable.toString(), throwable)
                val errorResult = when (requestContext.lastRequest) {
                    is DisplayRequest -> {
                        val errorDrawable = requestContext.lastRequest.error
                            ?.getDrawable(sketch, requestContext.lastRequest, exception)
                            ?.tryToResizeDrawable(sketch, requestContext.lastRequest)
                            ?: requestContext.lastRequest.placeholder
                                ?.getDrawable(sketch, requestContext.lastRequest, exception)
                                ?.tryToResizeDrawable(sketch, requestContext.lastRequest)
                        DisplayResult.Error(requestContext.lastRequest, errorDrawable, exception)
                    }
                    is LoadRequest -> LoadResult.Error(requestContext.lastRequest, exception)
                    is DownloadRequest -> DownloadResult.Error(
                        requestContext.lastRequest,
                        exception
                    )
                    else -> throw UnsupportedOperationException("Unsupported ImageRequest: ${requestContext.lastRequest::class.java}")
                }

                onError(sketch, requestContext.lastRequest, target, errorResult)
                return errorResult
            }
        } finally {
            requestContext.completeCountDrawable("RequestCompleted")
            requestDelegate.finish()
        }
    }

    private fun onStart(sketch: Sketch, request: ImageRequest) {
        sketch.logger.d(MODULE) {
            "Request started. ${request.key}"
        }
        request.listener?.onStart(request)
    }

    private fun onSuccess(
        sketch: Sketch,
        request: ImageRequest,
        target: Target?,
        result: ImageResult.Success
    ) {
        sketch.logger.d(MODULE) {
            if (result is DisplayResult.Success) {
                "Request Successful. ${result.drawable}. ${request.key}"
            } else {
                "Request Successful. ${request.uriString}"
            }
        }
        when {
            target is DisplayTarget && result is DisplayResult.Success -> {
                transition(target, result) {
                    target.onSuccess(result.drawable)
                }
            }
            target is LoadTarget && result is LoadResult.Success -> {
                target.onSuccess(result.bitmap)
            }
            target is DownloadTarget && result is DownloadResult.Success -> {
                target.onSuccess(result.data)
            }
        }
        request.listener?.onSuccess(request, result)
    }

    private fun onError(
        sketch: Sketch,
        request: ImageRequest,
        target: Target?,
        result: ImageResult.Error
    ) {
        sketch.logger.e(MODULE, result.exception) {
            "Request failed. ${result.exception.message}. ${request.key}"
        }
        when {
            target is DisplayTarget && result is DisplayResult.Error -> {
                transition(target, result) {
                    target.onError(result.drawable)
                }
            }
            target is LoadTarget && result is LoadResult.Error -> {
                target.onError(result.exception)
            }
            target is DownloadTarget && result is DownloadResult.Error -> {
                target.onError(result.exception)
            }
        }
        request.listener?.onError(request, result)
    }

    private fun onCancel(sketch: Sketch, request: ImageRequest) {
        sketch.logger.d(MODULE) {
            "Request canceled. ${request.key}"
        }
        request.listener?.onCancel(request)
    }

    private fun transition(
        target: Target?,
        result: DisplayResult,
        setDrawable: () -> Unit
    ) {
        if (target !is TransitionTarget) {
            setDrawable()
            return
        }

        if (result.drawable == null) {
            setDrawable()
            return
        }

        val fitScale =
            target.asOrNull<ViewTarget<View>>()?.view.asOrNull<ImageView>()?.fitScale ?: true
        val transition = result.request.transition?.create(target, result, fitScale)
        if (transition == null) {
            setDrawable()
            return
        }

        transition.transition()
    }

    private val ImageView.fitScale: Boolean
        get() = when (scaleType) {
            ScaleType.FIT_START, ScaleType.FIT_CENTER, ScaleType.FIT_END, ScaleType.CENTER_INSIDE -> true
            else -> false
        }
}