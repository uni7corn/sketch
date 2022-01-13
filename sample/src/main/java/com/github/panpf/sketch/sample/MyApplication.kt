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

package com.github.panpf.sketch.sample

import androidx.multidex.MultiDexApplication
import com.github.panpf.sketch.BuildConfig
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.SketchFactory
import com.github.panpf.sketch.decode.video.VideoFrameDecoder
import com.github.panpf.sketch.extensions.PauseLoadWhenScrollingDisplayInterceptor
import com.github.panpf.sketch.extensions.SaveCellularTrafficDisplayInterceptor
import com.github.panpf.sketch.gif.GifDrawableDecoder
import com.github.panpf.sketch.http.OkHttpStack
import com.github.panpf.sketch.util.Logger
import com.github.panpf.sketch.util.Logger.Level.DEBUG
import com.tencent.bugly.crashreport.CrashReport

class MyApplication : MultiDexApplication(), SketchFactory {

    override fun onCreate() {
        super.onCreate()
        CrashReport.initCrashReport(baseContext, "900007777", BuildConfig.DEBUG)    // todo 不用了
    }

    override fun newSketch(): Sketch = Sketch.new(this) {
        logger(Logger(DEBUG))
        httpStack(OkHttpStack.Builder().build())
        addDisplayInterceptor(SaveCellularTrafficDisplayInterceptor())
        addDisplayInterceptor(PauseLoadWhenScrollingDisplayInterceptor())
        components {
            addBitmapDecoder(VideoFrameDecoder.Factory())
            addDrawableDecoder(GifDrawableDecoder.Factory())
        }
    }
}