/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
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

package me.xiaopan.sketch.request;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.concurrent.locks.ReentrantLock;

import me.xiaopan.sketch.Sketch;
import me.xiaopan.sketch.drawable.FixedBitmapDrawable;
import me.xiaopan.sketch.drawable.RecyclerDrawable;
import me.xiaopan.sketch.drawable.SketchBitmapDrawable;
import me.xiaopan.sketch.drawable.SketchGifDrawable;
import me.xiaopan.sketch.util.SketchUtils;

/**
 * 显示请求
 */
public class DisplayRequest extends LoadRequest {
    private DisplayAttrs displayAttrs;
    private DisplayOptions displayOptions;
    private RequestAndViewBinder requestAndViewBinder;
    private DisplayListener displayListener;

    private DisplayResult displayResult;

    public DisplayRequest(
            Sketch sketch, RequestAttrs requestAttrs,
            DisplayAttrs displayAttrs, DisplayOptions displayOptions,
            RequestAndViewBinder requestAndViewBinder, DisplayListener displayListener,
            DownloadProgressListener downloadProgressListener) {
        super(sketch, requestAttrs, displayOptions, null, downloadProgressListener);

        this.displayAttrs = displayAttrs;
        this.displayOptions = displayOptions;
        this.requestAndViewBinder = requestAndViewBinder;
        this.displayListener = displayListener;

        this.requestAndViewBinder.setDisplayRequest(this);
        setLogName("DisplayRequest");
    }

    /**
     * 获取显示属性
     */
    public DisplayAttrs getDisplayAttrs() {
        return displayAttrs;
    }

    /**
     * 获取显示选项
     */
    @Override
    public DisplayOptions getOptions() {
        return displayOptions;
    }

    @Override
    public boolean isCanceled() {
        if (super.isCanceled()) {
            return true;
        }

        // 绑定关系已经断了就直接取消请求
        if (requestAndViewBinder.isBroken()) {
            canceled(CancelCause.BIND_DISCONNECT);
            return true;
        }

        return false;
    }

    @Override
    public void failed(FailedCause failedCause) {
        if (displayListener != null || displayOptions.getFailedImageHolder() != null) {
            setFailedCause(failedCause);
            postRunFailed();
        } else {
            super.failed(failedCause);
        }
    }

    @Override
    public void canceled(CancelCause cancelCause) {
        super.canceled(cancelCause);

        if (displayListener != null) {
            postRunCanceled();
        }
    }

    @Override
    protected void postRunFailed() {
        setStatus(Status.WAIT_DISPLAY);
        super.postRunFailed();
    }

    @Override
    protected void postRunCompleted() {
        setStatus(Status.WAIT_DISPLAY);
        super.postRunCompleted();
    }

    @Override
    protected void runLoad() {
        if (isCanceled()) {
            if (Sketch.isDebugMode()) {
                printLogW("canceled", "runLoad", "display request just started");
            }
            return;
        }

        // 要使用内存缓存就必须上锁
        ReentrantLock memoryCacheEditLock = null;
        if (!displayOptions.isDisableCacheInDisk()) {
            setStatus(Status.GET_MEMORY_CACHE_EDIT_LOCK);
            memoryCacheEditLock = getSketch().getConfiguration().getMemoryCache().getEditLock(getAttrs().getId());
            if (memoryCacheEditLock != null) {
                memoryCacheEditLock.lock();
            }
        }

        load();

        // 解锁
        if (memoryCacheEditLock != null) {
            memoryCacheEditLock.unlock();
        }
    }

    private void load() {
        if (isCanceled()) {
            if (Sketch.isDebugMode()) {
                printLogW("canceled", "runDownload", "get memory cache edit lock after");
            }
            return;
        }

        // 检查内存缓存
        if (!displayOptions.isDisableCacheInMemory()) {
            setStatus(Status.CHECK_MEMORY_CACHE);
            Drawable cacheDrawable = getSketch().getConfiguration().getMemoryCache().get(getAttrs().getId());
            if (cacheDrawable != null) {
                RecyclerDrawable recyclerDrawable = (RecyclerDrawable) cacheDrawable;
                if (!recyclerDrawable.isRecycled()) {
                    if (Sketch.isDebugMode()) {
                        printLogI("from memory get drawable", "runLoad", "drawableInfo=" + recyclerDrawable.getInfo());
                    }
                    displayResult = new DisplayResult(cacheDrawable, ImageFrom.MEMORY_CACHE, recyclerDrawable.getMimeType());
                    displayCompleted();
                    return;
                } else {
                    getSketch().getConfiguration().getMemoryCache().remove(getAttrs().getId());
                    if (Sketch.isDebugMode()) {
                        printLogE("memory cache drawable recycled", "runLoad", "drawableInfo=" + recyclerDrawable.getInfo());
                    }
                }
            }
        }

        // 加载
        super.runLoad();
    }

    @Override
    protected void loadCompleted() {
        LoadResult loadResult = getLoadResult();
        if (loadResult != null && loadResult.getBitmap() != null) {
            Bitmap bitmap = loadResult.getBitmap();

            if (bitmap.isRecycled()) {
                if (Sketch.isDebugMode()) {
                    printLogE("decode failed", "loadCompleted", "bitmap recycled",
                            "bitmapInfo=", SketchUtils.getInfo(null, bitmap, loadResult.getMimeType()));
                }
                failed(FailedCause.BITMAP_RECYCLED);
                return;
            }

            SketchBitmapDrawable bitmapDrawable = new SketchBitmapDrawable(bitmap);
            bitmapDrawable.setImageId(getAttrs().getId());
            bitmapDrawable.setImageUri(getAttrs().getUri());
            bitmapDrawable.setOriginWidth(loadResult.getOriginWidth());
            bitmapDrawable.setOriginHeight(loadResult.getOriginHeight());
            bitmapDrawable.setMimeType(loadResult.getMimeType());

            // 放入内存缓存中
            if (!displayOptions.isDisableCacheInMemory() && getAttrs().getId() != null) {
                getSketch().getConfiguration().getMemoryCache().put(getAttrs().getId(), bitmapDrawable);
            }

            displayResult = new DisplayResult(bitmapDrawable, loadResult.getImageFrom(), loadResult.getMimeType());
            displayCompleted();
        } else if (loadResult != null && loadResult.getGifDrawable() != null) {
            SketchGifDrawable gifDrawable = loadResult.getGifDrawable();

            if (gifDrawable.isRecycled()) {
                if (Sketch.isDebugMode()) {
                    printLogE("decode failed", "loadCompleted",
                            "gif drawable recycled",
                            "gifInfo=", SketchUtils.getInfo(gifDrawable));
                }
                failed(FailedCause.GIF_DRAWABLE_RECYCLED);
                return;
            }

            // GifDrawable不能放入内存缓存中，因为GifDrawable需要依赖Callback才能播放，
            // 如果缓存的话就会出现一个GifDrawable被显示在多个ImageView上的情况，这时候就只有最后一个能正常播放

            displayResult = new DisplayResult(gifDrawable, loadResult.getImageFrom(), loadResult.getMimeType());
            displayCompleted();
        } else {
            if (Sketch.isDebugMode()) {
                printLogE("are all null", "loadCompleted");
            }
            failed(FailedCause.DECODE_FAIL);
        }
    }

    protected void displayCompleted() {
        if (displayResult.getDrawable() instanceof RecyclerDrawable) {
            RecyclerDrawable recyclerDrawable = (RecyclerDrawable) displayResult.getDrawable();
            boolean fromMemoryCache = displayResult.getImageFrom() == ImageFrom.MEMORY_CACHE;
            String callingStation = fromMemoryCache ? "displayCompleted:fromMemory" : "displayCompleted:new";
            recyclerDrawable.setIsWaitDisplay(callingStation, true);
        }

        postRunCompleted();
    }

    @Override
    protected void runCompletedInMainThread() {
        if (isCanceled()) {
            if (Sketch.isDebugMode()) {
                printLogW("canceled", "runCompletedInMainThread");
            }

            // 更新等待显示的引用计数
            if (displayResult != null && displayResult.getDrawable() instanceof RecyclerDrawable) {
                RecyclerDrawable recyclerDrawable = (RecyclerDrawable) displayResult.getDrawable();
                recyclerDrawable.setIsWaitDisplay("completedCallback:cancel", false);
            }
            return;
        }

        setStatus(Status.COMPLETED);

        // 显示图片
        if (displayResult != null && displayResult.getDrawable() != null) {
            Drawable completedDrawable = displayResult.getDrawable();
            boolean isFixedSize = SketchUtils.isFixedSize(
                    displayOptions.getImageDisplayer(),
                    displayAttrs.getFixedSize(),
                    displayAttrs.getScaleType());
            if (completedDrawable instanceof SketchBitmapDrawable && isFixedSize) {
                SketchBitmapDrawable recycleCompletedDrawable = (SketchBitmapDrawable) completedDrawable;
                completedDrawable = new FixedBitmapDrawable(recycleCompletedDrawable, displayAttrs.getFixedSize());
            }

            ImageViewInterface viewInterface = requestAndViewBinder.getImageViewInterface();
            if (Sketch.isDebugMode()) {
                printLogI("image display completed", "runCompletedInMainThread",
                        displayResult.getImageFrom().name(),
                        (completedDrawable instanceof RecyclerDrawable) ? ((RecyclerDrawable) completedDrawable).getInfo() : "unknown",
                        "viewHashCode=" + Integer.toHexString(viewInterface.hashCode()));
            }

            displayOptions.getImageDisplayer().display(viewInterface, completedDrawable);
        } else {
            if (Sketch.isDebugMode()) {
                printLogD("completedDrawable is null", "runCompletedInMainThread");
            }
        }

        // 更新等待显示的引用计数
        if (displayResult.getDrawable() instanceof RecyclerDrawable) {
            RecyclerDrawable recyclerDrawable = (RecyclerDrawable) displayResult.getDrawable();
            recyclerDrawable.setIsWaitDisplay("completedCallback", false);
        }

        if (displayListener != null) {
            displayListener.onCompleted(displayResult.getImageFrom(), displayResult.getMimeType());
        }
    }

    @Override
    protected void runFailedInMainThread() {
        if (isCanceled()) {
            if (Sketch.isDebugMode()) {
                printLogW("canceled", "runFailedInMainThread");
            }
            return;
        }

        setStatus(Status.FAILED);

        // 显示失败图片
        if (displayOptions.getFailedImageHolder() != null) {
            Drawable failedDrawable = displayOptions.getFailedImageHolder().getDrawable(
                    getSketch().getConfiguration().getContext(),
                    displayOptions.getImageDisplayer(),
                    displayAttrs.getFixedSize(),
                    displayAttrs.getScaleType());
            displayOptions.getImageDisplayer().display(requestAndViewBinder.getImageViewInterface(), failedDrawable);
        } else {
            if (Sketch.isDebugMode()) {
                printLogW("failedDrawable is null", "runFailedInMainThread");
            }
        }

        if (displayListener != null) {
            displayListener.onFailed(getFailedCause());
        }
    }

    @Override
    protected void runCanceledInMainThread() {
        if (displayListener != null) {
            displayListener.onCanceled(getCancelCause());
        }
    }
}