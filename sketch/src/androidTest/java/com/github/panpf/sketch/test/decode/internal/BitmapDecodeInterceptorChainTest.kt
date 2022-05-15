package com.github.panpf.sketch.test.decode.internal

import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.decode.internal.BitmapDecodeInterceptorChain
import com.github.panpf.sketch.decode.internal.BitmapEngineDecodeInterceptor
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.internal.RequestExtras
import com.github.panpf.sketch.test.getContextAndSketch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BitmapDecodeInterceptorChainTest {

    @Test
    fun testIntercept() {
        val (context, sketch) = getContextAndSketch()
        val interceptors = listOf(BitmapEngineDecodeInterceptor())
        val loadRequest = LoadRequest(context, newAssetUri("sample.jpeg"))
        val requestExtras = RequestExtras()
        val chain =
            BitmapDecodeInterceptorChain(sketch, loadRequest, requestExtras, null, interceptors, 0)
        val result = runBlocking {
            chain.proceed()
        }
        Assert.assertEquals(1291, result.bitmap.width)
        Assert.assertEquals(1936, result.bitmap.height)
        Assert.assertEquals(
            "ImageInfo(width=1291, height=1936, mimeType='image/jpeg')",
            result.imageInfo.toString()
        )
        Assert.assertEquals(ExifInterface.ORIENTATION_NORMAL, result.exifOrientation)
        Assert.assertEquals(DataFrom.LOCAL, result.dataFrom)
        Assert.assertNull(result.transformedList)
    }
}