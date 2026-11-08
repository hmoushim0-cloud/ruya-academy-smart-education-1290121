package sa.factory.ruyaacademysmarted.engine

import java.util.UUID
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

enum class StylusPenMode(val modeCode: String, val modeArabic: String, val defaultWidthPx: Float) {
    PEN_FINE("FINE", "قلم حبر دقيق للتدوين", 2.0f),
    CALLIGRAPHY_CHISEL("CAL", "قلم خط عربي مشطوف", 6.5f),
    HIGHLIGHTER("HLT", "قلم تحديد وإبراز", 14.0f),
    ERASER("ERS", "ممحاة إلكترونية ذكية", 20.0f),
    LASER_POINTER("LSR", "مؤشر ليزري تفاعلي", 4.0f)
}

enum class TouchContactType {
    STYLUS_TIP,
    STYLUS_ERASER,
    FINGER_TOUCH,
    PALM_REJECTED
}

data class StylusPoint(
    val x: Float,
    val y: Float,
    val pressureRaw: Int, // 0 .. 4095
    val tiltXDeg: Float = 0.0f, // -60.0 .. +60.0
    val tiltYDeg: Float = 0.0f, // -60.0 .. +60.0
    val timestampMillis: Long = System.currentTimeMillis(),
    val contactAreaMm2: Float = 1.0f
) {
    val normalizedPressure: Float
        get() = (pressureRaw.coerceIn(0, 4095) / 4095.0f)
}

data class SmoothedVectorPoint(
    val x: Float,
    val y: Float,
    val dynamicWidthPx: Float
)

data class StylusStroke(
    val strokeId: String = "STRK-${UUID.randomUUID().toString().take(8).uppercase()}",
    val penMode: StylusPenMode,
    val rawPoints: List<StylusPoint>,
    val smoothedPoints: List<SmoothedVectorPoint>,
    val averagePressure: Float,
    val totalLengthPx: Float,
    val detectedGrammarDiacritic: String? = null
)

class SmartStylusDigitizerProtocolEngine {

    companion object {
        const val MAX_PRESSURE_LEVEL = 4095
        const val PALM_REJECTION_AREA_THRESHOLD_MM2 = 12.0f
    }

    private val completedStrokes = mutableListOf<StylusStroke>()

    fun classifyContact(point: StylusPoint, hasPenProximity: Boolean): TouchContactType {
        if (point.contactAreaMm2 > PALM_REJECTION_AREA_THRESHOLD_MM2) {
            return TouchContactType.PALM_REJECTED
        }
        if (!hasPenProximity) {
            return TouchContactType.FINGER_TOUCH
        }
        return TouchContactType.STYLUS_TIP
    }

    fun calculateStrokeVelocity(p1: StylusPoint, p2: StylusPoint): Float {
        val dt = (p2.timestampMillis - p1.timestampMillis).coerceAtLeast(1)
        val distance = hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()).toFloat()
        return distance / dt // px per millisecond
    }

    fun applyBezierSmoothing(points: List<StylusPoint>, penMode: StylusPenMode): List<SmoothedVectorPoint> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) {
            val p = points.first()
            val width = penMode.defaultWidthPx * (0.5f + p.normalizedPressure * 0.8f)
            return listOf(SmoothedVectorPoint(p.x, p.y, width))
        }

        val smoothed = mutableListOf<SmoothedVectorPoint>()
        val baseWidth = penMode.defaultWidthPx

        for (i in 0 until points.size - 1) {
            val pCurrent = points[i]
            val pNext = points[i + 1]

            val dynamicWidth = if (penMode == StylusPenMode.CALLIGRAPHY_CHISEL) {
                // Calligraphy nib effect: width depends on angle of movement relative to tilt
                val angle = atan2((pNext.y - pCurrent.y).toDouble(), (pNext.x - pCurrent.x).toDouble())
                val modulation = kotlin.math.abs(kotlin.math.sin(angle)).toFloat()
                baseWidth * (0.4f + modulation * 1.2f) * (0.6f + pNext.normalizedPressure * 0.5f)
            } else {
                baseWidth * (0.5f + pNext.normalizedPressure * 0.9f)
            }

            // Midpoint quadratic interpolation
            val midX = (pCurrent.x + pNext.x) / 2.0f
            val midY = (pCurrent.y + pNext.y) / 2.0f

            smoothed.add(SmoothedVectorPoint(midX, midY, dynamicWidth))
        }

        // Add final anchor point
        val pLast = points.last()
        smoothed.add(SmoothedVectorPoint(pLast.x, pLast.y, baseWidth * (0.5f + pLast.normalizedPressure * 0.8f)))
        return smoothed
    }

    fun detectArabicDiacritic(points: List<StylusPoint>): String? {
        if (points.size < 2) return null
        val start = points.first()
        val end = points.last()
        val dx = end.x - start.x
        val dy = end.y - start.y
        val totalLength = points.zipWithNext { a, b -> hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()) }.sum()

        // Short stroke analysis (< 80px)
        if (totalLength < 80.0) {
            return when {
                // Horizontal slash top-right to bottom-left: Fathah
                dx < -5.0f && dy > 5.0f -> "فتحة (Fathah)"
                // Diagonal stroke below line: Kasrah
                dx < -5.0f && dy < -5.0f -> "كسرة (Kasrah)"
                // Tiny circle or loop: Sukun
                totalLength in 20.0..60.0 && hypot(dx.toDouble(), dy.toDouble()) < 15.0 -> "سكون (Sukun)"
                // Small curl: Dammah
                else -> "حركة إعرابية (Diacritic)"
            }
        }
        return null
    }

    fun processFinishedStroke(rawPoints: List<StylusPoint>, penMode: StylusPenMode): StylusStroke {
        val validPoints = rawPoints.filter { it.contactAreaMm2 <= PALM_REJECTION_AREA_THRESHOLD_MM2 }
        val smoothed = applyBezierSmoothing(validPoints, penMode)
        val avgPressure = if (validPoints.isNotEmpty()) {
            validPoints.map { it.normalizedPressure }.average().toFloat()
        } else 0.0f

        val totalLength = validPoints.zipWithNext { a, b ->
            hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
        }.sum()

        val diacritic = detectArabicDiacritic(validPoints)
        val stroke = StylusStroke(
            penMode = penMode,
            rawPoints = validPoints,
            smoothedPoints = smoothed,
            averagePressure = Math.round(avgPressure * 100.0f) / 100.0f,
            totalLengthPx = Math.round(totalLength * 10.0f) / 10.0f,
            detectedGrammarDiacritic = diacritic
        )
        completedStrokes.add(stroke)
        return stroke
    }

    fun getStrokeCount(): Int = completedStrokes.size
}
