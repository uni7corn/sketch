package com.github.panpf.sketch.sample.ui.video

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.panpf.assemblyadapter.BindingItemFactory
import com.github.panpf.sketch.displayImage
import com.github.panpf.sketch.request.updateDisplayImageOptions
import com.github.panpf.sketch.request.videoFramePercentDuration
import com.github.panpf.sketch.sample.R
import com.github.panpf.sketch.sample.R.drawable
import com.github.panpf.sketch.sample.databinding.ItemVideoBinding
import com.github.panpf.sketch.sample.model.VideoInfo
import com.github.panpf.sketch.stateimage.pauseLoadWhenScrollingError
import com.github.panpf.sketch.stateimage.saveCellularTrafficError

class LocalVideoItemFactory :
    BindingItemFactory<VideoInfo, ItemVideoBinding>(VideoInfo::class) {

    override fun createItemViewBinding(
        context: Context,
        inflater: LayoutInflater,
        parent: ViewGroup
    ) = ItemVideoBinding.inflate(inflater, parent, false)

    override fun initItem(
        context: Context,
        binding: ItemVideoBinding,
        item: BindingItem<VideoInfo, ItemVideoBinding>
    ) {
        binding.videoItemIconImage.updateDisplayImageOptions {
            placeholder(R.drawable.im_placeholder)
            error(drawable.im_error) {
                saveCellularTrafficError(drawable.im_save_cellular_traffic)
                pauseLoadWhenScrollingError()
            }
            videoFramePercentDuration(0.5f)
        }
    }

    override fun bindItemData(
        context: Context,
        binding: ItemVideoBinding,
        item: BindingItem<VideoInfo, ItemVideoBinding>,
        bindingAdapterPosition: Int,
        absoluteAdapterPosition: Int,
        data: VideoInfo
    ) {
        binding.videoItemIconImage.displayImage(data.path)
        binding.videoItemNameText.text = data.title
        binding.videoItemSizeText.text = data.getTempFormattedSize(context)
        binding.videoItemDateText.text = data.tempFormattedDate
        binding.videoItemDurationText.text = data.tempFormattedDuration
    }
}