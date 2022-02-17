package com.github.panpf.sketch.sample.item

import android.content.Context
import android.graphics.Point
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.load
import coil.request.CachePolicy
import com.github.panpf.assemblyadapter.BindingItemFactory
import com.github.panpf.sketch.sample.R
import com.github.panpf.sketch.sample.bean.Photo
import com.github.panpf.sketch.sample.databinding.ItemImageBinding
import com.github.panpf.tools4a.display.ktx.getScreenWidth
import kotlin.math.roundToInt

class CoilPhotoItemFactory : BindingItemFactory<Photo, ItemImageBinding>(Photo::class) {

    private var itemSize: Point? = null

    override fun createItemViewBinding(
        context: Context,
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemImageBinding {
        if (itemSize == null && parent is RecyclerView) {
            val screenWidth = context.getScreenWidth()
            val gridDivider = context.resources.getDimensionPixelSize(R.dimen.grid_divider)
            itemSize = when (val layoutManager = parent.layoutManager) {
                is GridLayoutManager -> {
                    val spanCount = layoutManager.spanCount
                    val itemSize1 = (screenWidth - (gridDivider * (spanCount + 1))) / spanCount
                    Point(itemSize1, itemSize1)
                }
                is StaggeredGridLayoutManager -> {
                    val spanCount = layoutManager.spanCount
                    val itemSize1 = (screenWidth - (gridDivider * (spanCount + 1))) / spanCount
                    Point(itemSize1, -1)
                }
                else -> {
                    Point(screenWidth, -1)
                }
            }
        }
        return ItemImageBinding.inflate(inflater, parent, false)
    }

    override fun initItem(
        context: Context,
        binding: ItemImageBinding,
        item: BindingItem<Photo, ItemImageBinding>
    ) {
//        binding.imageItemImageView.apply {
//            setClickRedisplayAndIgnoreSaveCellularTraffic(true)
//            updateDisplayOptions {
//                placeholderImage(R.drawable.im_placeholder)
////                svgBackgroundColor(Color.WHITE)
//                errorImage(R.drawable.im_error) {
//                    saveCellularTrafficErrorImage(R.drawable.im_save_cellular_traffic)
//                    pauseLoadWhenScrollingErrorImage()
//                }
//                crossfadeTransition()
//            }
//        }
    }

    override fun bindItemData(
        context: Context,
        binding: ItemImageBinding,
        item: BindingItem<Photo, ItemImageBinding>,
        bindingAdapterPosition: Int,
        absoluteAdapterPosition: Int,
        data: Photo
    ) {
        binding.imageItemImageView.apply {
            updateLayoutParams<LayoutParams> {
                val photoWidth = data.width
                val photoHeight = data.height
                val itemSize = itemSize!!
                if (photoWidth != null && photoHeight != null) {
                    width = itemSize.x
                    height = if (itemSize.y == -1) {
                        val previewAspectRatio = photoWidth.toFloat() / photoHeight.toFloat()
                        (itemSize.x / previewAspectRatio).roundToInt()
                    } else {
                        itemSize.y
                    }
                } else {
                    width = itemSize.x
                    height = itemSize.x
                }
            }

            load(data.firstThumbnailUrl) {
                placeholder(R.drawable.im_placeholder)
                error(R.drawable.im_placeholder)
                memoryCachePolicy(CachePolicy.DISABLED)
                crossfade(true)
//                val scale = Scale.valueOf(appSettingsService.resizeScale.value)
//                when (appSettingsService.resizePrecision.value) {
//                    "LESS_PIXELS" -> {
//                        size(precision = LESS_PIXELS, scale = scale)
//                    }
//                    "KEEP_ASPECT_RATIO" -> {
//                        resizeByViewBounds(precision = KEEP_ASPECT_RATIO, scale = scale)
//                    }
//                    "EXACTLY" -> {
//                        resizeByViewBounds(precision = EXACTLY, scale = scale)
//                    }
//                    "LONG_IMAGE_CROP" -> {
//                        resizeByViewBounds(
//                            precisionDecider = longImageClipPrecision(precision = KEEP_ASPECT_RATIO),
//                            scale = scale
//                        )
//                    }
//                    "ORIGINAL" -> {
//                        resize(null)
//                    }
//                    else -> {
//                        resize(null)
//                    }
//                }
            }
        }
    }
}