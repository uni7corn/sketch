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
package com.github.panpf.sketch.request

const val ALLOW_NULL_IMAGE_KEY = "sketch#allow_null_image"

/**
 * Configure whether to allow setting null Drawable to ImageView
 */
fun ImageRequest.Builder.allowNullImage(allow: Boolean = true): ImageRequest.Builder {
    return if (allow) {
        setParameter(key = ALLOW_NULL_IMAGE_KEY, value = true, cacheKey = null)
    } else {
        removeParameter(key = ALLOW_NULL_IMAGE_KEY)
    }
}

/**
 * Configure whether to allow setting null Drawable to ImageView
 */
fun ImageOptions.Builder.allowNullImage(allow: Boolean = true): ImageOptions.Builder {
    return if (allow) {
        setParameter(key = ALLOW_NULL_IMAGE_KEY, value = true, cacheKey = null)
    } else {
        removeParameter(key = ALLOW_NULL_IMAGE_KEY)
    }
}

/**
 * Whether to allow setting null Drawable to ImageView
 */
val ImageRequest.allowSetNullDrawable: Boolean
    get() = parameters?.value(ALLOW_NULL_IMAGE_KEY) ?: false

/**
 * Whether to allow setting null Drawable to ImageView
 */
val ImageOptions.allowSetNullDrawable: Boolean
    get() = parameters?.value(ALLOW_NULL_IMAGE_KEY) ?: false