package com.github.panpf.sketch.request.internal

import androidx.annotation.MainThread
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.ByteArrayDataSource
import com.github.panpf.sketch.datasource.DiskCacheDataSource
import com.github.panpf.sketch.decode.internal.BitmapDecodeInterceptorChain
import com.github.panpf.sketch.decode.internal.DrawableDecodeInterceptorChain
import com.github.panpf.sketch.decode.internal.newMemoryCacheHelper
import com.github.panpf.sketch.drawable.SketchCountBitmapDrawable
import com.github.panpf.sketch.drawable.internal.tryToResizeDrawable
import com.github.panpf.sketch.fetch.HttpUriFetcher
import com.github.panpf.sketch.request.DisplayData
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DownloadData
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.ImageData
import com.github.panpf.sketch.request.LoadData
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.RequestDepth.MEMORY
import com.github.panpf.sketch.request.RequestInterceptor
import com.github.panpf.sketch.request.toDisplayData
import com.github.panpf.sketch.request.toLoadData
import com.github.panpf.sketch.target.DisplayTarget
import com.github.panpf.sketch.target.DownloadTarget
import com.github.panpf.sketch.target.LoadTarget
import kotlinx.coroutines.withContext

class EngineRequestInterceptor : RequestInterceptor {

    @MainThread
    override suspend fun intercept(chain: RequestInterceptor.Chain): ImageData =
        when (val request = chain.request) {
            is DisplayRequest -> display(chain.sketch, request, chain.requestExtras)
            is LoadRequest -> load(chain.sketch, request, chain)
            is DownloadRequest -> download(chain.sketch, request)
            else -> throw UnsupportedOperationException("Unsupported ImageRequest: ${request::class.java}")
        }

    private suspend fun display(
        sketch: Sketch,
        request: DisplayRequest,
        requestExtras: RequestExtras,
    ): DisplayData {
        /* check memory cache */
        val result = newMemoryCacheHelper(sketch, request)?.read()
        if (result != null) {
            val drawable = result.drawable
            if (drawable is SketchCountBitmapDrawable) {
                requestExtras.putCountDrawablePendingManagerKey(request.key)
                sketch.countDrawablePendingManager
                    .mark("EngineRequestInterceptor", request.key, drawable)
            }
            return result.toDisplayData()
        }
        val requestDepth = request.depth
        if (requestDepth >= MEMORY) {
            throw RequestDepthException(request, requestDepth, request.depthFrom)
        }

        /* callback target start */
        val target = request.target
        if (target is DisplayTarget) {
            val placeholderDrawable = request.placeholderImage
                ?.getDrawable(sketch, request, null)
                ?.tryToResizeDrawable(sketch, request)
            target.onStart(placeholderDrawable)
        }

        /* load */
        return withContext(sketch.decodeTaskDispatcher) {
            DrawableDecodeInterceptorChain(
                sketch = sketch,
                request = request,
                requestExtras = requestExtras,
                fetchResult = null,
                interceptors = sketch.drawableDecodeInterceptors,
                index = 0,
            ).proceed().toDisplayData()
        }
    }

    private suspend fun load(
        sketch: Sketch,
        request: LoadRequest,
        chain: RequestInterceptor.Chain
    ): LoadData {
        /* callback target start */
        val target = request.target
        if (target is LoadTarget) {
            target.onStart()
        }

        /* load */
        return withContext(sketch.decodeTaskDispatcher) {
            BitmapDecodeInterceptorChain(
                sketch = sketch,
                request = request,
                requestExtras = chain.requestExtras,
                fetchResult = null,
                interceptors = sketch.bitmapDecodeInterceptors,
                index = 0,
            ).proceed().toLoadData()
        }
    }

    private suspend fun download(sketch: Sketch, request: DownloadRequest): DownloadData {
        /* callback target start */
        val target = request.target
        if (target is DownloadTarget) {
            target.onStart()
        }

        /* download */
        return withContext(sketch.decodeTaskDispatcher) {
            val fetcher = sketch.components.newFetcher(request)
            if (fetcher !is HttpUriFetcher) {
                throw IllegalArgumentException("Download only support HTTP and HTTPS uri: ${request.uriString}")
            }

            val fetchResult = fetcher.fetch()
            when (val source = fetchResult.dataSource) {
                is ByteArrayDataSource -> DownloadData.Bytes(source.data, fetchResult.from)
                is DiskCacheDataSource -> DownloadData.Cache(
                    source.diskCacheSnapshot,
                    fetchResult.from
                )
                else -> throw IllegalArgumentException("The unknown source: ${source::class.qualifiedName}")
            }
        }
    }

    override fun toString(): String = "EngineRequestInterceptor"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}