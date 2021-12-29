package com.github.panpf.sketch.util

import android.annotation.TargetApi
import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES10
import android.opengl.GLES20
import android.os.Build
import com.github.panpf.sketch.common.ImageType
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext
import kotlin.math.ceil
import kotlin.math.floor

fun ImageType.supportBitmapRegionDecoder(): Boolean =
    this == ImageType.JPEG || this == ImageType.PNG || this == ImageType.WEBP

/**
 * 获取 [Bitmap] 占用内存大小，单位字节
 */
val Bitmap.byteCountCompat: Int
    get() {
        // bitmap.isRecycled()过滤很关键，在4.4以及以下版本当bitmap已回收时调用其getAllocationByteCount()方法将直接崩溃
        return when {
            this.isRecycled -> 0
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> this.allocationByteCount
            else -> this.byteCount
        }
    }

/**
 * 根据宽、高和配置计算所占用的字节数
 */
fun computeByteCount(width: Int, height: Int, config: Bitmap.Config?): Int {
    return width * height * config.getBytesPerPixel()
}

/**
 * 获取指定配置单个像素所占的字节数
 */
fun Bitmap.Config?.getBytesPerPixel(): Int {
    // A bitmap by decoding a gif has null "config" in certain environments.
    val config = this ?: Bitmap.Config.ARGB_8888
    return when {
        config == Bitmap.Config.ALPHA_8 -> 1
        config == Bitmap.Config.RGB_565 || config == Bitmap.Config.ARGB_4444 -> 2
        config == Bitmap.Config.ARGB_8888 -> 4
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.RGBA_F16 -> 8
        else -> 4
    }
}

/**
 * 获取修剪级别的名称
 */
fun getTrimLevelName(level: Int): String = when (level) {
    ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE"
    ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "MODERATE"
    ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
    ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
    ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
    ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
    ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
    else -> "UNKNOWN"
}

fun Any.toHexString(): String =
    Integer.toHexString(this.hashCode())

fun calculateSamplingSize(value1: Int, inSampleSize: Int): Int {
    return ceil((value1 / inSampleSize.toFloat()).toDouble()).toInt()
}

fun calculateSamplingSizeForRegion(value1: Int, inSampleSize: Int): Int {
    return floor((value1 / inSampleSize.toFloat()).toDouble()).toInt()
}


/**
 * OpenGL 所允许的图片的最大尺寸(单边长)
 */
val openGLMaxTextureSize: Int by lazy {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            openGLMaxTextureSizeJB1
        } else {
            openGLMaxTextureSizeBase
        }
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }.takeIf { it != 0 } ?: 4096
}

// TROUBLE! No config found.

// To make a context current, which we will need later,
// you need a rendering surface, even if you don't actually plan to render.
// To satisfy this requirement, create a small offscreen (Pbuffer) surface:

// Next, create the context:

// Ready to make the context current now:

// If all of the above succeeded (error checking was omitted), you can make your OpenGL calls now:

// Once you're all done, you can tear down everything:
// Then get a hold of the default display, and initialize.
// This could get more complex if you have to deal with devices that could have multiple displays,
// but will be sufficient for a typical phone/tablet:
@get:TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
private val openGLMaxTextureSizeJB1: Int
    // Next, we need to find a config. Since we won't use this context for rendering,
    // the exact attributes aren't very critical:
    get() {
        // Then get a hold of the default display, and initialize.
        // This could get more complex if you have to deal with devices that could have multiple displays,
        // but will be sufficient for a typical phone/tablet:
        val dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val vers = IntArray(2)
        EGL14.eglInitialize(dpy, vers, 0, vers, 1)

        // Next, we need to find a config. Since we won't use this context for rendering,
        // the exact attributes aren't very critical:
        val configAttr = intArrayOf(
            EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
            EGL14.EGL_LEVEL, 0,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfig = IntArray(1)
        EGL14.eglChooseConfig(
            dpy, configAttr, 0,
            configs, 0, 1, numConfig, 0
        )
        if (numConfig[0] == 0) {
            // TROUBLE! No config found.
        }
        val config = configs[0]

        // To make a context current, which we will need later,
        // you need a rendering surface, even if you don't actually plan to render.
        // To satisfy this requirement, create a small offscreen (Pbuffer) surface:
        val surfAttr = intArrayOf(
            EGL14.EGL_WIDTH, 64,
            EGL14.EGL_HEIGHT, 64,
            EGL14.EGL_NONE
        )
        val surf = EGL14.eglCreatePbufferSurface(dpy, config, surfAttr, 0)

        // Next, create the context:
        val ctxAttrib = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        val ctx = EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0)

        // Ready to make the context current now:
        EGL14.eglMakeCurrent(dpy, surf, surf, ctx)

        // If all of the above succeeded (error checking was omitted), you can make your OpenGL calls now:
        val maxSize = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0)

        // Once you're all done, you can tear down everything:
        EGL14.eglMakeCurrent(
            dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroySurface(dpy, surf)
        EGL14.eglDestroyContext(dpy, ctx)
        EGL14.eglTerminate(dpy)
        return maxSize[0]
    }// missing in EGL10// TROUBLE! No config found.

// In JELLY_BEAN will collapse
private val openGLMaxTextureSizeBase: Int
    get() {
        // In JELLY_BEAN will collapse
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            return 0
        }
        val egl = EGLContext.getEGL() as EGL10
        val dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        val vers = IntArray(2)
        egl.eglInitialize(dpy, vers)
        val configAttr = intArrayOf(
            EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER,
            EGL10.EGL_LEVEL, 0,
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_NONE
        )
        val configs = arrayOfNulls<javax.microedition.khronos.egl.EGLConfig>(1)
        val numConfig = IntArray(1)
        egl.eglChooseConfig(dpy, configAttr, configs, 1, numConfig)
        if (numConfig[0] == 0) {
            // TROUBLE! No config found.
        }
        val config = configs[0]
        val surfAttr = intArrayOf(
            EGL10.EGL_WIDTH, 64,
            EGL10.EGL_HEIGHT, 64,
            EGL10.EGL_NONE
        )
        val surf = egl.eglCreatePbufferSurface(dpy, config, surfAttr)
        val EGL_CONTEXT_CLIENT_VERSION = 0x3098 // missing in EGL10
        val ctxAttrib = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, 1,
            EGL10.EGL_NONE
        )
        val ctx = egl.eglCreateContext(dpy, config, EGL10.EGL_NO_CONTEXT, ctxAttrib)
        egl.eglMakeCurrent(dpy, surf, surf, ctx)
        val maxSize = IntArray(1)
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0)
        egl.eglMakeCurrent(
            dpy,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_CONTEXT
        )
        egl.eglDestroySurface(dpy, surf)
        egl.eglDestroyContext(dpy, ctx)
        egl.eglTerminate(dpy)
        return maxSize[0]
    }