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

package com.github.panpf.sketch.sample.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams
import androidx.core.view.updateLayoutParams
import com.github.panpf.sketch.SketchImageView
import com.github.panpf.sketch.zoom.tile.Tile
import com.github.panpf.sketch.zoom.tile.crossWith
import com.github.panpf.tools4a.dimen.ktx.dp2pxF
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class TileMapImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : SketchImageView(context, attrs, defStyle) {

    private val tileBoundsPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f.dp2pxF
    }
    private val previewVisibleRect = Rect()
    private val imageVisibleRect = Rect()
    private val mapPreviewVisibleRect = Rect()
    private val tileDrawRect = Rect()
    private var zoomView: MyZoomImageView? = null

    init {
        updateDisplayOptions {
            resizeSize(600, 600)
        }
    }

    private val detector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            location(e.x, e.y)
            return true
        }
    })

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val zoomAbility = zoomView?.zoomAbility ?: return
        val imageSize = zoomAbility.imageSize ?: return
        val tileList = zoomAbility.tileList?.takeIf { it.isNotEmpty() } ?: return
        val viewWidth = width.takeIf { it > 0 } ?: return
        val previewVisibleRect = previewVisibleRect
            .apply { zoomAbility.getVisibleRect(this) }
            .takeIf { !it.isEmpty } ?: return
        val previewSize = zoomAbility.previewSize ?: return
        val targetScale = imageSize.width.toFloat() / viewWidth

        val previewScaled = imageSize.width / previewSize.width.toFloat()
        val imageVisibleRect = imageVisibleRect.apply {
            set(
                floor(previewVisibleRect.left * previewScaled).toInt(),
                floor(previewVisibleRect.top * previewScaled).toInt(),
                ceil(previewVisibleRect.right * previewScaled).toInt(),
                ceil(previewVisibleRect.bottom * previewScaled).toInt()
            )
        }

        val drawTileBounds: (Tile, Boolean) -> Unit = { tile, cross ->
            val tileBitmap = tile.bitmap
            val tileSrcRect = tile.srcRect
            val tileDrawRect = tileDrawRect.apply {
                set(
                    floor(tileSrcRect.left / targetScale).toInt(),
                    floor(tileSrcRect.top / targetScale).toInt(),
                    floor(tileSrcRect.right / targetScale).toInt(),
                    floor(tileSrcRect.bottom / targetScale).toInt()
                )
            }
            val boundsColor = if (cross) {
                when {
                    tileBitmap != null -> Color.GREEN
                    tile.loadJob?.isActive == true -> Color.YELLOW
                    else -> Color.RED
                }
            } else {
                Color.BLUE
            }
            tileBoundsPaint.color = boundsColor
            canvas.drawRect(tileDrawRect, tileBoundsPaint)
        }
        tileList.forEach { tile ->
            if (!tile.srcRect.crossWith(imageVisibleRect)) {
                drawTileBounds(tile, false)
            }
        }
        tileList.forEach { tile ->
            if (tile.srcRect.crossWith(imageVisibleRect)) {
                drawTileBounds(tile, true)
            }
        }

        val mapScaled = imageSize.width / viewWidth.toFloat()
        val mapPreviewVisibleRect = mapPreviewVisibleRect.apply {
            set(
                floor(imageVisibleRect.left / mapScaled).toInt(),
                floor(imageVisibleRect.top / mapScaled).toInt(),
                ceil(imageVisibleRect.right / mapScaled).toInt(),
                ceil(imageVisibleRect.bottom / mapScaled).toInt()
            )
        }
        tileBoundsPaint.color = Color.parseColor("#800080")
        canvas.drawRect(mapPreviewVisibleRect, tileBoundsPaint)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        resetViewSize()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        return true
    }

    fun setZoomImageView(zoomView: MyZoomImageView) {
        this.zoomView = zoomView
        zoomView.zoomAbility.addOnMatrixChangeListener {
            invalidate()
        }
        zoomView.zoomAbility.addOnTileChangedListener {
            invalidate()
        }
    }

    private fun resetViewSize(): Boolean {
        val drawable = drawable ?: return true
        val zoomView = zoomView ?: return true

        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        val zoomViewWidth = zoomView.width
        val zoomViewHeight = zoomView.height
        if ((zoomViewWidth / drawableWidth.toFloat()) < (zoomViewHeight / drawableHeight.toFloat())) {
            val viewWidth = zoomViewWidth / (if (zoomViewWidth < zoomViewHeight) 3 else 2)
            val viewHeight = (drawableHeight * (viewWidth / drawableWidth.toFloat())).roundToInt()
            updateLayoutParams<LayoutParams> {
                width = viewWidth
                height = viewHeight
            }
        } else {
            val viewHeight = zoomViewHeight / (if (zoomViewWidth < zoomViewHeight) 2 else 3)
            val viewWidth = (drawableWidth * (viewHeight / drawableHeight.toFloat())).roundToInt()
            updateLayoutParams<LayoutParams> {
                width = viewWidth
                height = viewHeight
            }
        }
        return true
    }

    private fun location(x: Float, y: Float) {
        val zoomView = zoomView ?: return
        val viewWidth = width.takeIf { it > 0 } ?: return
        val viewHeight = height.takeIf { it > 0 } ?: return
        val drawable = zoomView.drawable
            ?.takeIf { it.intrinsicWidth != 0 && it.intrinsicHeight != 0 }
            ?: return

        val widthScale = drawable.intrinsicWidth.toFloat() / viewWidth
        val heightScale = drawable.intrinsicHeight.toFloat() / viewHeight
        val realX = x * widthScale
        val realY = y * heightScale

        zoomView.zoomAbility.location(realX, realY, animate = true)
    }
}