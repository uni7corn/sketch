package com.github.panpf.sketch.sample.ui.viewer.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.sample.eventService
import com.github.panpf.sketch.sample.image.ImageType.DETAIL
import com.github.panpf.sketch.sample.image.setApplySettings
import com.github.panpf.sketch.sample.model.ImageDetail
import com.github.panpf.sketch.sample.prefsService
import com.github.panpf.sketch.stateimage.ThumbnailMemoryCacheStateImage
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.compose.rememberZoomState
import com.github.panpf.zoomimage.compose.zoom.ScrollBarSpec
import com.github.panpf.zoomimage.zoom.ReadMode
import kotlin.math.roundToInt

@Composable
fun ImageViewer(imageDetail: ImageDetail, onClick: () -> Unit) {
    val context = LocalContext.current
    val prefsService = context.prefsService
    val showOriginImage by prefsService.showOriginImage.stateFlow.collectAsState()
    val scrollBarEnabled by prefsService.scrollBarEnabled.stateFlow.collectAsState()
    val readModeEnabled by prefsService.readModeEnabled.stateFlow.collectAsState()
    val showTileBounds by prefsService.showTileBounds.stateFlow.collectAsState()
    val zoomState = rememberZoomState().apply {
        LaunchedEffect(showTileBounds) {
            subsampling.showTileBounds = showTileBounds
        }
        val readMode by remember {
            derivedStateOf {
                if (readModeEnabled) ReadMode.Default else null
            }
        }
        LaunchedEffect(readMode) {
            zoomable.readMode = readMode
        }
        LaunchedEffect(Unit) {
            context.eventService.viewerPagerRotateEvent.collect {
                zoomable.rotate(zoomable.transform.rotation.roundToInt() + 90)
            }
        }
    }
    LaunchedEffect(Unit) {
        context.eventService.viewerPagerInfoEvent.collect {
            // todo info
        }
    }
    val imageUrl by remember {
        derivedStateOf {
            if (showOriginImage) {
                imageDetail.originUrl
            } else {
                imageDetail.mediumUrl ?: imageDetail.originUrl
            }
        }
    }
    val scrollBar by remember {
        derivedStateOf {
            if (scrollBarEnabled) ScrollBarSpec.Default else null
        }
    }
    SketchZoomAsyncImage(
        request = DisplayRequest(context, imageUrl) {
            setApplySettings(DETAIL)
            placeholder(ThumbnailMemoryCacheStateImage(imageDetail.thumbnailUrl))
            crossfade()
        },
        contentDescription = "view image",
        modifier = Modifier.fillMaxSize(),
        state = zoomState,
        scrollBar = scrollBar,
        onTap = { onClick() }
    )
}