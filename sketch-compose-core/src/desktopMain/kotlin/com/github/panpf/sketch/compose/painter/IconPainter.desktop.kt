package com.github.panpf.sketch.compose.painter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.painterResource


@Composable
fun rememberIconPainter(
    iconPath: String,
    backgroundPath: String? = null,
    iconSize: Size? = null,
    iconTint: Color? = null,
): IconPainter {
    val icon = painterResource(iconPath)
    val background = backgroundPath?.let { painterResource(it) }
    return remember(iconPath, backgroundPath, iconSize, iconTint) {
        IconPainter(icon, background, iconSize, iconTint)
    }
}

@Composable
fun rememberIconPainter(
    iconPath: String,
    background: Color? = null,
    iconSize: Size? = null,
    iconTint: Color? = null,
): IconPainter {
    val icon = painterResource(iconPath)
    return remember(iconPath, background, iconSize, iconTint) {
        val backgroundPainter = background?.let { ColorPainter(it) }
        IconPainter(icon, backgroundPainter, iconSize, iconTint)
    }
}

@Composable
fun rememberAnimatableIconPainter(
    iconPath: String,
    backgroundPath: String? = null,
    iconSize: Size? = null,
    iconTint: Color? = null,
): AnimatableIconPainter {
    val icon = painterResource(iconPath)
    val background = backgroundPath?.let { painterResource(it) }
    return remember(iconPath, backgroundPath, iconSize, iconTint) {
        AnimatableIconPainter(icon, background, iconSize, iconTint)
    }
}

@Composable
fun rememberAnimatableIconPainter(
    iconPath: String,
    background: Color? = null,
    iconSize: Size? = null,
    iconTint: Color? = null,
): AnimatableIconPainter {
    val icon = painterResource(iconPath)
    return remember(iconPath, background, iconSize, iconTint) {
        val backgroundPainter = background?.let { ColorPainter(it) }
        AnimatableIconPainter(icon, backgroundPainter, iconSize, iconTint)
    }
}