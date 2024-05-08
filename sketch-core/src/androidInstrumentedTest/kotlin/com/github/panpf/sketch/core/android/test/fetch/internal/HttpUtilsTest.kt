/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.sketch.core.android.test.fetch.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.fetch.internal.writeAllWithProgress
import com.github.panpf.sketch.fetch.internal.getMimeType
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.test.utils.ProgressListenerSupervisor
import com.github.panpf.sketch.test.utils.SlowInputStream
import com.github.panpf.sketch.test.utils.content
import com.github.panpf.sketch.test.utils.getTestContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

@RunWith(AndroidJUnit4::class)
class HttpUtilsTest {

    @Test
    fun testWriteAllWithProgress() {
        val context = getTestContext()
        val string = "abcdefghijklmnopqrstuvwxyz"
        val progressListener = ProgressListenerSupervisor()

        progressListener.callbackActionList.clear()
        Assert.assertEquals(listOf<String>(), progressListener.callbackActionList)
        val outputStream = ByteArrayOutputStream()
        runBlocking {
            writeAllWithProgress(
                sink = outputStream.sink().buffer(),
                content = SlowInputStream(string.byteInputStream(), 100).content(),
                request = ImageRequest(context, "http://sample.com/sample.jpeg") {
                    progressListener(progressListener)
                },
                contentLength = string.length.toLong(),
                bufferSize = ceil(string.length / 3f).toInt(),
            )
            delay(100)
        }
        Assert.assertEquals(outputStream.toString(), string)
        Assert.assertEquals(listOf("9", "26"), progressListener.callbackActionList)

        progressListener.callbackActionList.clear()
        Assert.assertEquals(listOf<String>(), progressListener.callbackActionList)
        val outputStream2 = ByteArrayOutputStream()
        runBlocking {
            writeAllWithProgress(
                sink = outputStream2.sink().buffer(),
                content = SlowInputStream(string.byteInputStream(), 100).content(),
                request = ImageRequest(context, "http://sample.com/sample.jpeg"),
                contentLength = string.length.toLong(),
                bufferSize = ceil(string.length / 3f).toInt(),
            )
            delay(100)
        }
        Assert.assertEquals(outputStream2.toString(), string)
        Assert.assertEquals(listOf<String>(), progressListener.callbackActionList)

        progressListener.callbackActionList.clear()
        Assert.assertEquals(listOf<String>(), progressListener.callbackActionList)
        val outputStream3 = ByteArrayOutputStream()
        runBlocking {
            writeAllWithProgress(
                sink = outputStream3.sink().buffer(),
                content = SlowInputStream(string.byteInputStream(), 100).content(),
                request = ImageRequest(context, "http://sample.com/sample.jpeg") {
                    progressListener(progressListener)
                },
                bufferSize = ceil(string.length / 3f).toInt(),
                contentLength = 0
            )
            delay(100)
        }
        Assert.assertEquals(outputStream3.toString(), string)
        Assert.assertEquals(listOf<String>(), progressListener.callbackActionList)

        progressListener.callbackActionList.clear()
        Assert.assertEquals(listOf<String>(), progressListener.callbackActionList)
        val outputStream4 = ByteArrayOutputStream()
        runBlocking {
            writeAllWithProgress(
                sink = outputStream4.sink().buffer(),
                content = SlowInputStream(string.byteInputStream(), 400).content(),
                request = ImageRequest(context, "http://sample.com/sample.jpeg") {
                    progressListener(progressListener)
                },
                bufferSize = ceil(string.length / 3f).toInt(),
                contentLength = string.length.toLong()
            )
            delay(100)
        }
        Assert.assertEquals(outputStream4.toString(), string)
        Assert.assertEquals(listOf("9", "18", "26"), progressListener.callbackActionList)
    }

    @Test
    fun testGetMimeType() {
        Assert.assertEquals("image/jpeg", getMimeType("http://sample.com/sample.jpeg", null))
        Assert.assertEquals("image/jpeg", getMimeType("http://sample.com/sample.jpeg", ""))
        Assert.assertEquals("image/jpeg", getMimeType("http://sample.com/sample.jpeg", " "))

        Assert.assertEquals(
            "image/jpeg",
            getMimeType("http://sample.com/sample.jpeg", "text/plain; charset=utf-8")
        )
        Assert.assertEquals("image/png", getMimeType("http://sample.com/sample.jpeg", "image/png"))
    }
}