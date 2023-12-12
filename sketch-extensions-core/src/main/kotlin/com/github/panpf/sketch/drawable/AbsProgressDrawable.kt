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
package com.github.panpf.sketch.drawable

import android.graphics.Canvas
import android.os.SystemClock
import androidx.annotation.FloatRange
import com.github.panpf.sketch.util.format

abstract class AbsProgressDrawable(
    private val stepAnimationDuration: Int = DEFAULT_STEP_ANIMATION_DURATION,
    private val hideWhenCompleted: Boolean = true,
) : ProgressDrawable() {

    companion object {
        const val DEFAULT_STEP_ANIMATION_DURATION = 300
    }

    private var stepAnimationRunning: Boolean = false
    private var stepAnimationStartProgress: Float? = null
    private var stepAnimationProgress: Float? = null
    private var stepAnimationEndProgress: Float? = null
    private var stepAnimationStartTimeMillis = 0L
    private var hide = false

    final override var progress: Float = 0f
        set(value) {
            val oldValue = field
            val newValue = value.format(1).coerceIn(-1f, 1f)
            field = newValue
            if (newValue != oldValue) {
                hide = false
                if (oldValue == -1f) {
                    // Show new progress now
                    stepAnimationRunning = false
                    stepAnimationProgress = null
                } else if (newValue == -1f) {
                    // Hide content now
                    stepAnimationRunning = false
                    stepAnimationProgress = null
                } else if (oldValue == 0f && newValue == 1f && hideWhenCompleted) {
                    // The progress goes directly from 0 to 1f, and hide the content after completion,
                    // skip the animation and hide the content directly.
                    hide = true
                    stepAnimationRunning = false
                    stepAnimationProgress = null
                } else if (newValue > oldValue) {
                    // The progress increases and the animation starts from the current progress
                    val stepAnimationProgress = stepAnimationProgress
                    val newStepAnimationStartProgress =
                        if (stepAnimationRunning && stepAnimationProgress != null)
                            stepAnimationProgress else oldValue
                    stepAnimationStartProgress = newStepAnimationStartProgress
                    stepAnimationEndProgress = newValue
                    stepAnimationStartTimeMillis = SystemClock.uptimeMillis()
                    stepAnimationRunning = true
                } else {
                    // The progress decreases, no animation is needed
                    stepAnimationRunning = false
                    stepAnimationProgress = null
                }
                invalidateSelf()
            }
        }

    override fun draw(canvas: Canvas) {
        if (hide) return

        val stepAnimationDone: Boolean
        val drawProgress: Float
        if (stepAnimationRunning) {
            val elapsedTime = SystemClock.uptimeMillis() - stepAnimationStartTimeMillis
            val stepProgress = (elapsedTime / stepAnimationDuration.toDouble()).coerceIn(0.0, 1.0)
            stepAnimationDone = stepProgress >= 1
            val animationStartProgress = stepAnimationStartProgress!!
            val animationEndProgress = stepAnimationEndProgress!!
            val addDrawProgress =
                ((animationEndProgress - animationStartProgress) * stepProgress).toFloat()
            drawProgress = animationStartProgress + addDrawProgress
            stepAnimationProgress = drawProgress
        } else {
            stepAnimationDone = false
            drawProgress = progress
            stepAnimationProgress = null
        }

        // todo Displayed only when support progress is greater than 0
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        if (drawProgress >= 0f && drawProgress <= 1f) {
            drawProgress(canvas, drawProgress)
        }

        if (stepAnimationRunning) {
            if (stepAnimationDone) {
                stepAnimationRunning = false
                stepAnimationProgress = null
            } else {
                invalidateSelf()
            }
        }

        if (!stepAnimationRunning && drawProgress >= 1f && hideWhenCompleted) {
            hide = true
            invalidateSelf()
        }
    }

    abstract fun drawProgress(
        canvas: Canvas,
        @FloatRange(from = 0.0, to = 1.0) drawProgress: Float
    )

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        if (changed && !visible) {
            if (stepAnimationRunning) {
                stepAnimationRunning = false
                stepAnimationProgress = null
                invalidateSelf()
            }
        }
        return changed
    }
}