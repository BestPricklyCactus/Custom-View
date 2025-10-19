package otus.homework.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries
import kotlin.math.atan2
import kotlin.math.sqrt

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var pieHeight: Int = 0
    private var pieWidth: Int = 0
    private var payChartData: List<PieChartData> = emptyList()
    private var totalAmount: Int = 0
    private var colorSet: EnumEntries<ColorSet> = enumEntries<ColorSet>()
    private var pieElements: MutableList<PieElement> = mutableListOf()

    private var paint: Paint = Paint()
    private val paintText: Paint = Paint()
    private var rectf: RectF = RectF()
    private var selectedCategory: String? = null
    private var onCategoryClickListener: ((String) -> Unit)? = null


    fun setOnCategoryClickListener(listener: (String) -> Unit) {
        onCategoryClickListener = listener
    }

    fun setData(data: List<PieChartData>) {
        payChartData = data
        initPieElements()
    }

    init {
        paintText.color = Color.BLACK
        paintText.textSize = 30f
        paintText.textAlign = Paint.Align.CENTER
    }

    private fun initPieElements() {
        val categories = payChartData.groupBy { it.category }
        totalAmount = payChartData.sumOf { it.amount }
        var counter = 0;
        var startAngle = 0f;
        for (it in categories) {
            val categoryAmount = it.value.sumOf { it.amount }
            val partSize = categoryAmount / (totalAmount * 1f)
            val angle = partSize * 360f;
            pieElements.add(PieElement(colorSet.get(counter), startAngle, angle, it.key, categoryAmount))
            counter++;
            startAngle = startAngle + angle;
        }
    }

    data class PieElement(
        val color: ColorSet,
        val startAngle: Float,
        val angle: Float,
        val category: String,
        val amount: Int
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val midHeight = height / 2f
        val midWidth = width / 2f
        val radius = maxOf(midHeight, midWidth) / 2.0f

        rectf.set(
            midWidth - radius,
            midHeight - radius,
            midWidth + radius,
            midHeight + radius
        )
        for (item in pieElements) {
            drowPieElement(canvas, getPiePaint(paint, item), rectf, item)
            if(item.category == selectedCategory){
                drowPieElement(canvas, getSelectPiePaint(), rectf, item)
                canvas.drawText( "${item.category}: ${item.amount} руб", midWidth, midHeight - 1.7f*radius, paintText)
            }
        }
    }

    fun getPiePaint(paint: Paint, pieElement: PieElement): Paint {
        paint.reset()

        paint.color = pieElement.color.hexCode.toColorInt()
        if(pieElement.category == selectedCategory){
            paint.alpha = 255
        } else {
            paint.alpha = 127
        }
        paint.style = Paint.Style.FILL_AND_STROKE
        return paint
    }

    fun getSelectPiePaint(): Paint {
        paint.reset()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.BLACK
        paint.strokeJoin = Paint.Join.BEVEL
        return paint
    }

    fun drowPieElement(
        canvas: Canvas,
        paint: Paint,
        rect: RectF,
        pieElement: PieElement
    ) {
        val path: Path = Path()
        path.addArc(rect, pieElement.startAngle, pieElement.angle)
        canvas.drawArc(rect, pieElement.startAngle, pieElement.angle, true, paint)
        canvas.drawTextOnPath(pieElement.angle.toUInt().toString() + "%", path, 0f,0f, paintText)
        invalidate()

    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val midHeight = height / 2f
            val midWidth = width / 2f
            val dx = event.x - midWidth
            val dy = event.y - midHeight
            val dist = sqrt(dx * dx + dy * dy)


            val radius = maxOf(midHeight, midWidth) / 2.0f
            val innerRadius = radius - radius / 2
            val outerRadius = radius + radius / 2

            if (dist in innerRadius..outerRadius) {
                val touchAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360) % 360
                val slice = pieElements.firstOrNull {
                    touchAngle in it.startAngle..(it.startAngle + it.angle)
                }
                slice?.let {
                    selectedCategory = it.category
                    onCategoryClickListener?.invoke(it.category)
                    Log.d("PieChartView", "category: $selectedCategory")
                    //Toast.makeText(context, it.category, Toast.LENGTH_SHORT).show()
                    invalidate()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable("super", super.onSaveInstanceState())
            putString("selectedCategory", selectedCategory)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? Bundle)?.let {
            selectedCategory = it.getString("selectedCategory", null)
            super.onRestoreInstanceState(it.getParcelable("super"))
        }
    }
}
