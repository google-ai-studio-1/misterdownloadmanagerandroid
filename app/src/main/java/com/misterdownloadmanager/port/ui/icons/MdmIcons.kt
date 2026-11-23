package com.misterdownloadmanager.port.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object MdmIcons {
    val Download: ImageVector
        get() = ImageVector.Builder("download", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black), stroke = null, pathFillType = PathFillType.NonZero) {
                moveTo(11f, 3f)
                lineTo(13f, 3f)
                lineTo(13f, 11f)
                lineTo(16f, 11f)
                lineTo(12f, 15f)
                lineTo(8f, 11f)
                lineTo(11f, 11f)
                close()
            }
            path(fill = SolidColor(Color.Black), stroke = null, pathFillType = PathFillType.NonZero) {
                moveTo(5f, 17f)
                lineTo(19f, 17f)
                lineTo(19f, 19f)
                lineTo(5f, 19f)
                close()
            }
        }.build()

    val Pause: ImageVector
        get() = ImageVector.Builder("pause", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7f, 5f)
                lineTo(10f, 5f)
                lineTo(10f, 19f)
                lineTo(7f, 19f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(14f, 5f)
                lineTo(17f, 5f)
                lineTo(17f, 19f)
                lineTo(14f, 19f)
                close()
            }
        }.build()

    val Play: ImageVector
        get() = ImageVector.Builder("play", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 5f)
                lineTo(19f, 12f)
                lineTo(8f, 19f)
                close()
            }
        }.build()

    val Delete: ImageVector
        get() = ImageVector.Builder("delete", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 7f)
                lineTo(18f, 7f)
                lineTo(17f, 20f)
                lineTo(7f, 20f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 4f)
                lineTo(15f, 4f)
                lineTo(15f, 6f)
                lineTo(9f, 6f)
                close()
            }
        }.build()

    val Folder: ImageVector
        get() = ImageVector.Builder("folder", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 5f)
                lineTo(10f, 5f)
                lineTo(12f, 7f)
                lineTo(21f, 7f)
                lineTo(21f, 19f)
                lineTo(3f, 19f)
                close()
            }
        }.build()

    val Settings: ImageVector
        get() = ImageVector.Builder("settings", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 5.5f)
                arcTo(6.5f, 6.5f, 0f, true, true, 12f, 18.5f)
                arcTo(6.5f, 6.5f, 0f, true, true, 12f, 5.5f)
                close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 8.8f)
                arcTo(3.2f, 3.2f, 0f, true, true, 12f, 15.2f)
                arcTo(3.2f, 3.2f, 0f, true, true, 12f, 8.8f)
                close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                moveTo(18.5f, 12f)
                lineTo(21f, 12f)
                moveTo(16.6f, 16.6f)
                lineTo(18.4f, 18.4f)
                moveTo(12f, 18.5f)
                lineTo(12f, 21f)
                moveTo(7.4f, 16.6f)
                lineTo(5.6f, 18.4f)
                moveTo(5.5f, 12f)
                lineTo(3f, 12f)
                moveTo(7.4f, 7.4f)
                lineTo(5.6f, 5.6f)
                moveTo(12f, 5.5f)
                lineTo(12f, 3f)
                moveTo(16.6f, 7.4f)
                lineTo(18.4f, 5.6f)
            }
        }.build()

    val Search: ImageVector
        get() = ImageVector.Builder("search", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(10.5f, 4f)
                arcTo(6.5f, 6.5f, 0f, true, true, 10.5f, 17f)
                arcTo(6.5f, 6.5f, 0f, true, true, 10.5f, 4f)
                close()
                moveTo(15.5f, 15.5f)
                lineTo(21f, 21f)
            }
        }.build()

    val Link: ImageVector
        get() = ImageVector.Builder("link", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(10f, 14f)
                arcTo(4f, 4f, 0f, true, true, 14f, 10f)
                moveTo(9f, 15f)
                lineTo(15f, 9f)
            }
        }.build()

    val Speed: ImageVector
        get() = ImageVector.Builder("speed", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 16f)
                arcTo(8f, 8f, 0f, true, true, 20f, 16f)
                moveTo(12f, 16f)
                lineTo(16f, 10f)
            }
        }.build()

    val Torrent: ImageVector
        get() = ImageVector.Builder("torrent", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 3f)
                lineTo(6f, 11f)
                arcTo(6f, 6f, 0f, false, false, 18f, 11f)
                lineTo(18f, 3f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 3f)
                lineTo(10f, 3f)
                lineTo(10f, 9f)
                lineTo(6f, 9f)
                close()
                moveTo(18f, 3f)
                lineTo(14f, 3f)
                lineTo(14f, 9f)
                lineTo(18f, 9f)
                close()
            }
        }.build()

    val File: ImageVector
        get() = ImageVector.Builder("file", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 2f)
                lineTo(14f, 2f)
                lineTo(20f, 8f)
                lineTo(20f, 22f)
                lineTo(6f, 22f)
                close()
                moveTo(14f, 2f)
                lineTo(14f, 8f)
                lineTo(20f, 8f)
                close()
            }
        }.build()

    val Check: ImageVector
        get() = ImageVector.Builder("check", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 12.5f)
                lineTo(9.5f, 18f)
                lineTo(20f, 6.5f)
            }
        }.build()

    val Add: ImageVector
        get() = ImageVector.Builder("add", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 5f)
                lineTo(13f, 5f)
                lineTo(13f, 11f)
                lineTo(19f, 11f)
                lineTo(19f, 13f)
                lineTo(13f, 13f)
                lineTo(13f, 19f)
                lineTo(11f, 19f)
                lineTo(11f, 13f)
                lineTo(5f, 13f)
                lineTo(5f, 11f)
                lineTo(11f, 11f)
                close()
            }
        }.build()

    val Close: ImageVector
        get() = ImageVector.Builder("close", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }.build()

    val Globe: ImageVector
        get() = ImageVector.Builder("globe", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, true, true, 12f, 21f)
                arcTo(9f, 9f, 0f, true, true, 12f, 3f)
                close()
                moveTo(3f, 12f)
                lineTo(21f, 12f)
                moveTo(12f, 3f)
                arcTo(4f, 9f, 0f, true, true, 12f, 21f)
                arcTo(4f, 9f, 0f, true, true, 12f, 3f)
                close()
            }
        }.build()

    val Lock: ImageVector
        get() = ImageVector.Builder("lock", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7f, 10f)
                lineTo(7f, 7f)
                arcTo(5f, 5f, 0f, true, true, 17f, 7f)
                lineTo(17f, 10f)
                lineTo(17f, 11f)
                lineTo(7f, 11f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 11f)
                lineTo(19f, 11f)
                lineTo(19f, 20f)
                lineTo(5f, 20f)
                close()
            }
        }.build()

    val Archive: ImageVector
        get() = ImageVector.Builder("archive", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 8f)
                lineTo(4f, 8f)
                close()
                moveTo(5f, 8f)
                lineTo(6f, 20f)
                lineTo(18f, 20f)
                lineTo(19f, 8f)
                close()
                moveTo(10f, 11f)
                lineTo(14f, 11f)
                lineTo(14f, 13f)
                lineTo(10f, 13f)
                close()
            }
        }.build()

    val Video: ImageVector
        get() = ImageVector.Builder("video", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 6f)
                lineTo(15f, 6f)
                lineTo(15f, 18f)
                lineTo(3f, 18f)
                close()
                moveTo(17f, 9f)
                lineTo(21f, 6.5f)
                lineTo(21f, 17.5f)
                lineTo(17f, 15f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 10f)
                lineTo(13f, 12f)
                lineTo(8f, 14f)
                close()
            }
        }.build()

    val Category: ImageVector
        get() = ImageVector.Builder("category", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 4f)
                lineTo(10f, 4f)
                lineTo(10f, 10f)
                lineTo(4f, 10f)
                close()
                moveTo(14f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 10f)
                lineTo(14f, 10f)
                close()
                moveTo(4f, 14f)
                lineTo(10f, 14f)
                lineTo(10f, 20f)
                lineTo(4f, 20f)
                close()
                moveTo(14f, 14f)
                lineTo(20f, 14f)
                lineTo(20f, 20f)
                lineTo(14f, 20f)
                close()
            }
        }.build()

    val Refresh: ImageVector
        get() = ImageVector.Builder("refresh", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(20f, 12f)
                arcTo(8f, 8f, 0f, true, true, 12f, 4f)
                arcTo(8f, 8f, 0f, true, true, 4f, 12f)
                moveTo(4f, 4f)
                lineTo(4f, 9f)
                lineTo(9f, 9f)
            }
        }.build()
}
