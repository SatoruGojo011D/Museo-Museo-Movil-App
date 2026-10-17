package cl.duoc.museo.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import cl.duoc.museo.R
import cl.duoc.museo.data.model.MuseumMapItem
import kotlin.math.hypot

class IberoamericaMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val seaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.map_sea)
    }
    private val landPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.map_land)
        style = Paint.Style.FILL
    }
    private val coastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.map_coast)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.map_grid)
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.map_label)
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            12f,
            resources.displayMetrics,
        )
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }
    private val museums = mutableListOf<MuseumMapItem>()
    private var selectedMuseumId: String? = null

    var onMuseumSelected: ((String) -> Unit)? = null

    fun setMuseums(items: List<MuseumMapItem>, selectedId: String?) {
        museums.clear()
        museums.addAll(items)
        selectedMuseumId = selectedId
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), seaPaint)
        drawGrid(canvas)
        drawLand(canvas, listOf(
            -118.0 to 53.0,
            -110.0 to 48.0,
            -104.0 to 39.0,
            -97.0 to 30.0,
            -91.0 to 19.0,
            -86.0 to 16.0,
            -82.0 to 9.0,
            -78.0 to 8.0,
            -78.0 to 17.0,
            -86.0 to 23.0,
            -98.0 to 27.0,
            -108.0 to 34.0,
            -117.0 to 43.0,
        ))
        drawLand(canvas, listOf(
            -79.0 to 10.0,
            -71.0 to 12.0,
            -62.0 to 8.0,
            -51.0 to 3.0,
            -47.0 to -8.0,
            -51.0 to -19.0,
            -56.0 to -29.0,
            -63.0 to -40.0,
            -70.0 to -54.0,
            -76.0 to -44.0,
            -78.0 to -31.0,
            -80.0 to -16.0,
        ))
        drawLand(canvas, listOf(
            -11.0 to 44.0,
            2.0 to 44.0,
            7.0 to 40.0,
            3.0 to 36.0,
            -6.0 to 36.0,
            -10.0 to 39.0,
        ))
        canvas.drawText("IBEROAMÉRICA", width * 0.49f, height * 0.48f, labelPaint)
        drawMarkers(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        for (longitude in -100..20 step 20) {
            val x = project(longitude.toDouble(), 0.0).first
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (latitude in -40..40 step 20) {
            val y = project(0.0, latitude.toDouble()).second
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }

    private fun drawLand(canvas: Canvas, coordinates: List<Pair<Double, Double>>) {
        if (coordinates.isEmpty()) return
        val path = Path()
        coordinates.forEachIndexed { index, (longitude, latitude) ->
            val (x, y) = project(longitude, latitude)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, landPaint)
        canvas.drawPath(path, coastPaint)
    }

    private fun drawMarkers(canvas: Canvas) {
        val density = resources.displayMetrics.density
        museums.forEach { museum ->
            val (x, y) = project(museum.lng, museum.lat)
            val selected = museum.id == selectedMuseumId
            markerPaint.color = ContextCompat.getColor(context, R.color.white)
            canvas.drawCircle(x, y, if (selected) 12f * density else 10f * density, markerPaint)
            markerPaint.color = ContextCompat.getColor(
                context,
                if (selected) R.color.accent_gold_dark else R.color.primary,
            )
            canvas.drawCircle(x, y, if (selected) 8f * density else 6f * density, markerPaint)
        }
    }

    private fun project(longitude: Double, latitude: Double): Pair<Float, Float> {
        val x = ((longitude + 125.0) / 160.0 * width).toFloat()
        val y = ((55.0 - latitude) / 110.0 * height).toFloat()
        return x to y
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true

        val hitRadius = 36f * resources.displayMetrics.density
        val nearest = museums
            .map { museum ->
                val (x, y) = project(museum.lng, museum.lat)
                museum to hypot((event.x - x).toDouble(), (event.y - y).toDouble())
            }
            .filter { it.second <= hitRadius }
            .minByOrNull { it.second }

        if (nearest != null) {
            selectedMuseumId = nearest.first.id
            onMuseumSelected?.invoke(nearest.first.id)
            invalidate()
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
