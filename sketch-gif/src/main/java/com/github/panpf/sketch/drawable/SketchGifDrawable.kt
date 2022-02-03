package com.github.panpf.sketch.drawable

/*
 * Copyright (C) 2019 panpf <panpfpanpf@outlook.com>
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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.appcompat.graphics.drawable.DrawableWrapper
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.Transformed
import com.github.panpf.sketch.request.DataFrom

@SuppressLint("RestrictedApi")
class SketchGifDrawable constructor(
    override val requestKey: String,
    override val requestUri: String,
    private val imageInfo: ImageInfo,
    private val exifOrientation: Int,
    override val imageDataFrom: DataFrom,
    private val movieDrawable: MovieDrawable,
) : DrawableWrapper(movieDrawable), SketchAnimatableDrawable {

    override val imageWidth: Int
        get() = imageInfo.width

    override val imageHeight: Int
        get() = imageInfo.height

    override val imageMimeType: String
        get() = imageInfo.mimeType

    override val imageExifOrientation: Int
        get() = exifOrientation

    override val bitmapWidth: Int
        get() = movieDrawable.bitmapWidth

    override val bitmapHeight: Int
        get() = movieDrawable.bitmapHeight

    override val bitmapByteCount: Int
        get() = movieDrawable.bitmapByteCount

    override val bitmapConfig: Bitmap.Config?
        get() = movieDrawable.bitmapConfig

    override val transformedList: List<Transformed>? = null

    override fun start() = movieDrawable.start()

    override fun stop() = movieDrawable.stop()

    override fun isRunning(): Boolean = movieDrawable.isRunning

    override fun registerAnimationCallback(callback: Animatable2Compat.AnimationCallback) {
        movieDrawable.registerAnimationCallback(callback)
    }

    override fun unregisterAnimationCallback(callback: Animatable2Compat.AnimationCallback): Boolean {
        return movieDrawable.unregisterAnimationCallback(callback)
    }

    override fun clearAnimationCallbacks() = movieDrawable.clearAnimationCallbacks()
}