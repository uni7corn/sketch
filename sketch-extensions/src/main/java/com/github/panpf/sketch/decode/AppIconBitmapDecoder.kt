package com.github.panpf.sketch.decode

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.annotation.WorkerThread
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.DataFrom.LOCAL
import com.github.panpf.sketch.decode.internal.appliedResize
import com.github.panpf.sketch.fetch.AppIconUriFetcher
import com.github.panpf.sketch.fetch.AppIconUriFetcher.AppIconDataSource
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.internal.RequestContext
import com.github.panpf.sketch.util.toNewBitmap

/**
 * Extract the icon of the installed app and convert it to Bitmap
 */
class AppIconBitmapDecoder(
    private val sketch: Sketch,
    private val request: ImageRequest,
    private val packageName: String,
    private val versionCode: Int,
) : BitmapDecoder {

    @WorkerThread
    override suspend fun decode(): BitmapDecodeResult {
        val packageManager = request.context.packageManager
        val packageInfo: PackageInfo = try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            throw Exception("Not found PackageInfo by '$packageName'", e)
        }
        @Suppress("DEPRECATION")
        if (packageInfo.versionCode != versionCode) {
            throw Exception("App versionCode mismatch, ${packageInfo.versionCode} != $versionCode")
        }
        val iconDrawable = packageInfo.applicationInfo.loadIcon(packageManager)
            ?: throw Exception("loadIcon return null '$packageName'")
        val bitmap = iconDrawable.toNewBitmap(
            sketch.bitmapPool,
            request.bitmapConfig?.getConfig(AppIconUriFetcher.MIME_TYPE)
        )
        val imageInfo = ImageInfo(bitmap.width, bitmap.height, AppIconUriFetcher.MIME_TYPE, 0)
        return BitmapDecodeResult(bitmap, imageInfo, LOCAL).appliedResize(sketch, request.resize)
    }

    class Factory : BitmapDecoder.Factory {

        override fun create(
            sketch: Sketch,
            request: ImageRequest,
            requestContext: RequestContext,
            fetchResult: FetchResult
        ): BitmapDecoder? {
            val dataSource = fetchResult.dataSource
            return if (
                AppIconUriFetcher.MIME_TYPE.equals(fetchResult.mimeType, ignoreCase = true)
                && dataSource is AppIconDataSource
            ) {
                AppIconBitmapDecoder(
                    sketch,
                    request,
                    dataSource.packageName,
                    dataSource.versionCode
                )
            } else {
                null
            }
        }

        override fun toString(): String = "AppIconBitmapDecoder"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }
}