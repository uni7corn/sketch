package com.github.panpf.sketch.common.decode.internal

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Point
import android.os.Build
import android.os.Build.VERSION_CODES
import com.github.panpf.sketch.SLog
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.common.DecodeException
import com.github.panpf.sketch.common.ImageRequest
import com.github.panpf.sketch.common.ImageResult
import com.github.panpf.sketch.common.ImageType
import com.github.panpf.sketch.common.ListenerInfo
import com.github.panpf.sketch.common.LoadableRequest
import com.github.panpf.sketch.common.datasource.DataSource
import com.github.panpf.sketch.common.decode.DecodeResult
import com.github.panpf.sketch.common.decode.Decoder
import com.github.panpf.sketch.load.ImageInfo
import com.github.panpf.sketch.load.Resize
import com.github.panpf.sketch.util.supportBitmapRegionDecoder

class BitmapFactoryDecoder(
    private val sketch: Sketch,
    private val request: LoadableRequest,
    private val dataSource: DataSource,
) : Decoder {

    private val resizeCalculator = ResizeCalculator()
    private val sizeCalculator = ImageSizeCalculator()
    private val bitmapPoolHelper = sketch.bitmapPoolHelper

    companion object {
        const val MODULE = "BitmapFactoryDecoder"
    }

    override suspend fun decode(): DecodeResult {
        val imageInfo = readImageInfo()
        val resize = request.resize
        val imageType = ImageType.valueOfMimeType(imageInfo.mimeType)
        val bitmapPoolHelper = sketch.bitmapPoolHelper

        val imageOrientationCorrector =
            ImageOrientationCorrector.fromExifOrientation(imageInfo.exifOrientation)

        val decodeOptions = Options().apply {
            if (request.inPreferQualityOverSpeed == true) {
                inPreferQualityOverSpeed = true
            }

            val newConfig = request.bitmapConfig?.getConfigByMimeType(imageInfo.mimeType)
            if (newConfig != null) {
                inPreferredConfig = newConfig
            }
        }

        val bitmap = if (resize != null && shouldUseRegionDecoder(resize, imageInfo, imageType)) {
            decodeUseRegion(resize, imageInfo, decodeOptions, imageOrientationCorrector)
        } else {
            decodeUseBitmapFactory(imageInfo, decodeOptions, imageOrientationCorrector)
        }

        val correctedOrientationBitmap =
            imageOrientationCorrector?.rotateBitmap(bitmap, bitmapPoolHelper.bitmapPool) ?: bitmap
        if (correctedOrientationBitmap !== bitmap) {
            bitmapPoolHelper.freeBitmapToPool(bitmap)
        }

        val resizeBitmap = tryResize(correctedOrientationBitmap, resize)
        if (resizeBitmap !== correctedOrientationBitmap) {
            bitmapPoolHelper.freeBitmapToPool(correctedOrientationBitmap)
        }

        return DecodeResult(resizeBitmap, imageInfo, decodeOptions)
    }

    private fun readImageInfo(): ImageInfo {
        val boundOptions = Options()
        boundOptions.inJustDecodeBounds = true
        dataSource.decodeBitmap(boundOptions)
        if (boundOptions.outWidth <= 1 || boundOptions.outHeight <= 1) {
            val message = "Invalid image size. size=%dx%d, uri=%s"
                .format(boundOptions.outWidth, boundOptions.outHeight, request.uri)
            SLog.em(MODULE, message)
            throw DecodeException(message)
        }

        val exifOrientation: Int =
            ImageOrientationCorrector.readExifOrientation(boundOptions.outMimeType, dataSource)
        return ImageInfo(
            boundOptions.outMimeType,
            boundOptions.outWidth,
            boundOptions.outHeight,
            exifOrientation
        )
    }

    private fun shouldUseRegionDecoder(
        resize: Resize, imageInfo: ImageInfo, imageType: ImageType?
    ): Boolean {
        if (imageType?.supportBitmapRegionDecoder() == true) {
            val resizeAspectRatio =
                "%.1f".format((resize.width.toFloat() / resize.height.toFloat()))
            val imageAspectRatio =
                "%.1f".format((imageInfo.width.toFloat() / imageInfo.height.toFloat()))
            return resizeAspectRatio != imageAspectRatio
        }
        return false
    }

    private fun decodeUseRegion(
        resize: Resize,
        imageInfo: ImageInfo,
        decodeOptions: Options,
        imageOrientationCorrector: ImageOrientationCorrector?
    ): Bitmap {
        val imageSize = Point(imageInfo.width, imageInfo.height)

        if (Build.VERSION.SDK_INT <= VERSION_CODES.M && !decodeOptions.inPreferQualityOverSpeed) {
            decodeOptions.inPreferQualityOverSpeed = true
        }

        imageOrientationCorrector?.rotateSize(imageSize)

        val resizeMapping = resizeCalculator.calculator(
            imageWidth = imageSize.x,
            imageHeight = imageSize.y,
            resizeWidth = resize.width,
            resizeHeight = resize.height,
            scaleType = resize.scaleType,
            exactlySame = false
        )
        val resizeMappingSrcWidth = resizeMapping.srcRect.width()
        val resizeMappingSrcHeight = resizeMapping.srcRect.height()

        val resizeInSampleSize = sizeCalculator.calculateInSampleSize(
            resizeMappingSrcWidth, resizeMappingSrcHeight, resize.width, resize.height
        )
        decodeOptions.inSampleSize = resizeInSampleSize

        imageOrientationCorrector
            ?.reverseRotateRect(resizeMapping.srcRect, imageSize.x, imageSize.y)

        if (request.disabledBitmapPool != true) {
            bitmapPoolHelper.setInBitmapForRegionDecoder(
                decodeOptions, resizeMappingSrcWidth, resizeMappingSrcHeight
            )
        }

        val bitmap = try {
            dataSource.decodeRegionBitmap(resizeMapping.srcRect, decodeOptions)
        } catch (throwable: Throwable) {
            val inBitmap = decodeOptions.inBitmap
            when {
                inBitmap != null && isInBitmapError(throwable, true) -> {
                    val message = "Bitmap region decode error. Because inBitmap. uri=%s"
                        .format(request.uri)
                    SLog.emt(MODULE, throwable, message)

                    decodeOptions.inBitmap = null
                    bitmapPoolHelper.freeBitmapToPool(inBitmap)
                    try {
                        dataSource.decodeRegionBitmap(resizeMapping.srcRect, decodeOptions)
                    } catch (throwable2: Throwable) {
                        val message2 = "Bitmap region decode error. uri=%s".format(request.uri)
                        SLog.emt(MODULE, throwable2, message2)
                        throw DecodeException(message2, throwable2)
                    }
                }
                isSrcRectError(throwable, imageSize.x, imageSize.y, resizeMapping.srcRect) -> {
                    val message =
                        "Bitmap region decode error. Because srcRect. imageInfo=%s, resize=%s, srcRect=%s, uri=%s"
                            .format(
                                imageInfo,
                                request.resize, resizeMapping.srcRect, request.uri
                            )
                    SLog.emt(MODULE, throwable, message)
                    throw DecodeException(message, throwable)
                }
                else -> {
                    val message = "Bitmap region decode error. uri=%s".format(request.uri)
                    SLog.emt(MODULE, throwable, message)
                    throw DecodeException(message, throwable)
                }
            }
        }
        if (bitmap == null) {
            val message = "Bitmap region decode return null. uri=%s".format(request.uri)
            SLog.em(MODULE, message)
            throw DecodeException(message)
        }
        if (bitmap.width <= 1 || bitmap.height <= 1) {
            bitmap.recycle()
            val message = "Invalid image size. size=%dx%d, uri=%s"
                .format(bitmap.width, bitmap.height, request.uri)
            SLog.em(MODULE, message)
            throw DecodeException(message)
        }
        return bitmap
    }

    private fun decodeUseBitmapFactory(
        imageInfo: ImageInfo,
        decodeOptions: Options,
        imageOrientationCorrector: ImageOrientationCorrector?
    ): Bitmap {
        val resize = request.resize
        val maxSize = request.maxSize
        val imageSize = Point(imageInfo.width, imageInfo.height)
        imageOrientationCorrector?.rotateSize(imageSize)

        val maxSizeInSampleSize = maxSize?.let {
            sizeCalculator.calculateInSampleSize(
                imageSize.x, imageSize.y, it.width, it.height
            )
        } ?: 1
        val resizeInSampleSize = resize?.let {
            sizeCalculator.calculateInSampleSize(
                imageSize.x, imageSize.y, it.width, it.height
            )
        } ?: 1
        decodeOptions.inSampleSize = maxSizeInSampleSize.coerceAtLeast(resizeInSampleSize)

        // Set inBitmap from bitmap pool
        if (request.disabledBitmapPool != true) {
            bitmapPoolHelper.setInBitmap(
                decodeOptions, imageSize.x, imageSize.y, imageInfo.mimeType
            )
        }

        val bitmap: Bitmap? = try {
            dataSource.decodeBitmap(decodeOptions)
        } catch (throwable: Throwable) {
            val inBitmap = decodeOptions.inBitmap
            if (inBitmap != null && isInBitmapError(throwable, false)) {
                val message = "Bitmap decode error. Because inBitmap. uri=%s"
                    .format(request.uri)
                SLog.emt(MODULE, throwable, message)

                decodeOptions.inBitmap = null
                bitmapPoolHelper.freeBitmapToPool(inBitmap)
                try {
                    dataSource.decodeBitmap(decodeOptions)
                } catch (throwable2: Throwable) {
                    val message2 = "Bitmap decode error. uri=%s".format(request.uri)
                    SLog.emt(MODULE, throwable2, message2)
                    throw DecodeException(message2, throwable2)
                }
            } else {
                val message = "Bitmap decode error. uri=%s".format(request.uri)
                SLog.emt(MODULE, throwable, message)
                throw DecodeException(message, throwable)
            }
        }
        if (bitmap == null) {
            val message = "Bitmap decode return null. uri=%s".format(request.uri)
            SLog.em(MODULE, message)
            throw DecodeException(message)
        }
        if (bitmap.width <= 1 || bitmap.height <= 1) {
            bitmap.recycle()
            val message = "Invalid image size. size=%dx%d, uri=%s"
                .format(bitmap.width, bitmap.height, request.uri)
            SLog.em(MODULE, message)
            throw DecodeException(message)
        }
        return bitmap
    }

    private fun tryResize(bitmap: Bitmap, resize: Resize?): Bitmap {
        return when (resize?.sizeMode) {
            Resize.SizeMode.ASPECT_RATIO_SAME -> {
                val resizeAspectRatio =
                    "%.1f".format((resize.width.toFloat() / resize.height.toFloat()))
                val bitmapAspectRatio =
                    "%.1f".format((bitmap.width.toFloat() / bitmap.height.toFloat()))
                if (resizeAspectRatio != bitmapAspectRatio) {
                    resize(bitmap, resize)
                } else {
                    bitmap
                }
            }
            Resize.SizeMode.EXACTLY_SAME -> {
                if (resize.width != bitmap.width || resize.height != bitmap.height) {
                    resize(bitmap, resize)
                } else {
                    bitmap
                }
            }
            else -> {
                bitmap
            }
        }
    }

    // todo Try resize and rotateBitmap together
    private fun resize(bitmap: Bitmap, resize: Resize): Bitmap {
        val mapping = ResizeCalculator().calculator(
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            resizeWidth = resize.width,
            resizeHeight = resize.height,
            scaleType = resize.scaleType,
            exactlySame = resize.sizeMode == Resize.SizeMode.EXACTLY_SAME
        )
        val config = bitmap.config ?: ARGB_8888
        val bitmapPool = sketch.bitmapPoolHelper.bitmapPool
        val resizeBitmap = bitmapPool.getOrMake(mapping.imageWidth, mapping.imageHeight, config)
        val canvas = Canvas(resizeBitmap)
        canvas.drawBitmap(bitmap, mapping.srcRect, mapping.destRect, null)
        return resizeBitmap
    }

    class Factory : Decoder.Factory {

        override fun create(
            sketch: Sketch,
            request: ImageRequest,
            listenerInfo: ListenerInfo<ImageRequest, ImageResult>?,
            dataSource: DataSource,
        ): Decoder? = if (request is LoadableRequest) {
            BitmapFactoryDecoder(sketch, request, dataSource)
        } else {
            null
        }
    }
}