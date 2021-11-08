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

package com.github.panpf.sketch.zoom;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Scroller;

import androidx.annotation.NonNull;

import com.github.panpf.sketch.SLog;
import com.github.panpf.sketch.util.SketchUtils;

/**
 * 定位执行器
 */
class LocationRunner implements Runnable {
    @NonNull
    private ImageZoomer imageZoomer;
    @NonNull
    private ScaleDragHelper scaleDragHelper;

    @NonNull
    private Scroller scroller;
    private int currentX;
    private int currentY;

    LocationRunner(@NonNull ImageZoomer imageZoomer, @NonNull ScaleDragHelper scaleDragHelper) {
        this.scroller = new Scroller(imageZoomer.getImageView().getContext(), new AccelerateDecelerateInterpolator());
        this.imageZoomer = imageZoomer;
        this.scaleDragHelper = scaleDragHelper;
    }

    /**
     * 定位到预览图上指定的位置
     */
    void location(int startX, int startY, int endX, int endY) {
        currentX = startX;
        currentY = startY;

        scroller.startScroll(startX, startY, endX - startX, endY - startY, 300);

        ImageView imageView = imageZoomer.getImageView();
        imageView.removeCallbacks(this);
        imageView.post(this);
    }

    @Override
    public void run() {
        // remaining post that should not be handled
        if (scroller.isFinished()) {
            if (SLog.isLoggable(SLog.VERBOSE)) {
                SLog.vm(ImageZoomer.MODULE, "finished. location run");
            }
            return;
        }

        if (!imageZoomer.isWorking()) {
            SLog.wm(ImageZoomer.MODULE, "not working. location run");
            scroller.forceFinished(true);
            return;
        }

        if (!scroller.computeScrollOffset()) {
            if (SLog.isLoggable(SLog.VERBOSE)) {
                SLog.vm(ImageZoomer.MODULE, "scroll finished. location run");
            }
            return;
        }

        final int newX = scroller.getCurrX();
        final int newY = scroller.getCurrY();
        final float dx = currentX - newX;
        final float dy = currentY - newY;
        scaleDragHelper.translateBy(dx, dy);
        currentX = newX;
        currentY = newY;

        // Post On animation
        SketchUtils.postOnAnimation(imageZoomer.getImageView(), this);
    }

    boolean isRunning() {
        return !scroller.isFinished();
    }

    void cancel() {
        scroller.forceFinished(true);
        ImageView imageView = imageZoomer.getImageView();
        imageView.removeCallbacks(this);
    }
}