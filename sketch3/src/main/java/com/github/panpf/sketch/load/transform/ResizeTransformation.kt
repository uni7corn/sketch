package com.github.panpf.sketch.load.transform

import android.graphics.Bitmap
import com.github.panpf.sketch.common.LoadableRequest
import com.github.panpf.sketch.load.Resize

class ResizeTransformation(val resize: Resize) : Transformation {
    override val cacheKey: String
        get() = TODO("Not yet implemented")

    override suspend fun transform(request: LoadableRequest, input: Bitmap): Bitmap {
        TODO("Not yet implemented")
    }
}