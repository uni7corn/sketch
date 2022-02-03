package com.github.panpf.sketch.test.fetch

import android.widget.ImageView
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.FileDataSource
import com.github.panpf.sketch.fetch.FileUriFetcher
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.LoadRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileUriFetcherTest {

    @Test
    fun testNewUri() {
        Assert.assertEquals(
            "file:///sdcard/sample.jpg",
            newFileUri("/sdcard/sample.jpg")
        )
        Assert.assertEquals(
            "file:///sdcard1/sample1.jpg",
            newFileUri("/sdcard1/sample1.jpg")
        )
    }

    @Test
    fun testFactory() {
        val context = InstrumentationRegistry.getContext()
        val sketch = Sketch.new(context)
        val fileUri = "file:///sdcard/sample.jpg"
        val filePath = "/sdcard/sample.jpg"
        val ftpUri = "ftp:///sample.com/sample.jpg"
        val contentUri = "content://sample_app/sample"
        val imageView = ImageView(context)

        val httpUriFetcherFactory = FileUriFetcher.Factory()
        httpUriFetcherFactory.create(sketch, LoadRequest(fileUri))!!.apply {
            Assert.assertEquals("/sdcard/sample.jpg", this.file.path)
        }
        httpUriFetcherFactory.create(sketch, LoadRequest(filePath))!!.apply {
            Assert.assertEquals("/sdcard/sample.jpg", this.file.path)
        }
        httpUriFetcherFactory.create(sketch, DisplayRequest(fileUri, imageView))!!.apply {
            Assert.assertEquals("/sdcard/sample.jpg", this.file.path)
        }
        httpUriFetcherFactory.create(sketch, DisplayRequest(filePath, imageView))!!.apply {
            Assert.assertEquals("/sdcard/sample.jpg", this.file.path)
        }
        Assert.assertNull(httpUriFetcherFactory.create(sketch, DownloadRequest(fileUri)))
        Assert.assertNull(httpUriFetcherFactory.create(sketch, DownloadRequest(filePath)))
        Assert.assertNull(httpUriFetcherFactory.create(sketch, LoadRequest(ftpUri)))
        Assert.assertNull(httpUriFetcherFactory.create(sketch, LoadRequest(contentUri)))
    }

    @Test
    fun testFetch() {
        val context = InstrumentationRegistry.getContext()
        val sketch = Sketch.new(context)
        val fetcherFactory = FileUriFetcher.Factory()
        val fileUri = "file:///sdcard/sample.jpg"

        val fetcher = fetcherFactory.create(sketch, LoadRequest(fileUri))!!
        val source = runBlocking {
            fetcher.fetch().dataSource
        }
        Assert.assertTrue(source is FileDataSource)
    }
}