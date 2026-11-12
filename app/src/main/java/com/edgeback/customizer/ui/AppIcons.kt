package com.edgeback.customizer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object AppIcons {
    val Home: ImageVector by lazy {
        icon("Home") {
            polygon(10f to 20f, 10f to 14f, 14f to 14f, 14f to 20f, 19f to 20f, 19f to 12f, 22f to 12f, 12f to 3f, 2f to 12f, 5f to 12f, 5f to 20f)
        }
    }

    val Edit: ImageVector by lazy {
        icon("Edit") {
            polygon(3f to 17.25f, 3f to 21f, 6.75f to 21f, 17.81f to 9.94f, 14.06f to 6.19f)
            polygon(15.5f to 4.75f, 19.25f to 8.5f, 20.71f to 7.04f, 20.71f to 5.63f, 18.37f to 3.29f, 16.96f to 3.29f)
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            val teeth = 8
            val step = 2 * PI / teeth
            val pts = ArrayList<Pair<Float, Float>>()
            for (t in 0 until teeth) {
                val a = step * t
                pts += point(12f, 12f, 7.2f, a - step * 0.30)
                pts += point(12f, 12f, 9.8f, a - step * 0.18)
                pts += point(12f, 12f, 9.8f, a + step * 0.18)
                pts += point(12f, 12f, 7.2f, a + step * 0.30)
            }
            polygon(pts)
            polygon(circle(12f, 12f, 3.2f))
        }
    }

    val Search: ImageVector by lazy {
        icon("Search") {
            polygon(circle(10f, 10f, 6.5f))
            polygon(circle(10f, 10f, 4.5f))
            polygon(14.2f to 15.6f, 15.6f to 14.2f, 21f to 19.6f, 19.6f to 21f)
        }
    }

    val CheckCircle: ImageVector by lazy {
        icon("CheckCircle") {
            polygon(circle(12f, 12f, 10f))
            polygon(10f to 17f, 5f to 12f, 6.41f to 10.59f, 10f to 14.17f, 17.59f to 6.59f, 19f to 8f)
        }
    }

    val Warning: ImageVector by lazy {
        icon("Warning") {
            polygon(12f to 2f, 23f to 21f, 1f to 21f)
            polygon(11f to 10f, 13f to 10f, 13f to 15f, 11f to 15f)
            polygon(11f to 17f, 13f to 17f, 13f to 19f, 11f to 19f)
        }
    }

    val Star: ImageVector by lazy {
        icon("Star") {
            val pts = ArrayList<Pair<Float, Float>>()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) 10f else 4.2f
                pts += point(12f, 12.5f, r, -PI / 2 + i * PI / 5)
            }
            polygon(pts)
        }
    }

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd, pathBuilder = block)
        }.build()

    private fun point(cx: Float, cy: Float, r: Float, angle: Double) =
        (cx + r * cos(angle)).toFloat() to (cy + r * sin(angle)).toFloat()

    private fun circle(cx: Float, cy: Float, r: Float, n: Int = 28): List<Pair<Float, Float>> =
        List(n) { point(cx, cy, r, 2 * PI * it / n) }

    private fun PathBuilder.polygon(vararg pts: Pair<Float, Float>) = polygon(pts.toList())

    private fun PathBuilder.polygon(pts: List<Pair<Float, Float>>) {
        moveTo(pts[0].first, pts[0].second)
        for (i in 1 until pts.size) lineTo(pts[i].first, pts[i].second)
        close()
    }
}
