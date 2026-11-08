package sa.factory.ruyaacademysmarted.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.ruyaacademysmarted.engine.*
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "Smart Stylus Digitizer 4096-Level + BLE Beacon"

    val stylusEngine = SmartStylusDigitizerProtocolEngine()
    val attendanceEngine = BleClassroomAttendanceEngine()

    init {
        // Seed default students into attendance engine
        attendanceEngine.registerStudent("STD-801", "أحمد بن عبد الله السديري", "ARAB-101", 101)
        attendanceEngine.registerStudent("STD-802", "سارة بنت فيصل الشمري", "ARAB-101", 102)
        attendanceEngine.registerStudent("STD-803", "محمد بن سلطان القحطاني", "ARAB-101", 103)

        // Seed initial stylus sample stroke
        val now = System.currentTimeMillis()
        val points = listOf(
            StylusPoint(120.0f, 340.0f, 1500, -10.0f, 15.0f, now, 1.2f),
            StylusPoint(125.0f, 350.0f, 2800, -12.0f, 16.0f, now + 15, 1.4f),
            StylusPoint(135.0f, 370.0f, 3950, -15.0f, 18.0f, now + 30, 1.5f),
            StylusPoint(140.0f, 385.0f, 2100, -8.0f, 12.0f, now + 45, 1.3f)
        )
        stylusEngine.processFinishedStroke(points, StylusPenMode.CALLIGRAPHY_CHISEL)
    }

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "BLE Digitizer GATT handshaking on UUID 0x18F0..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "$peripheral: ONLINE | READY (Pressure: 4096 Levels)"))

        var frameIndex = 0
        while (true) {
            delay(1500)
            frameIndex++
            val strokes = stylusEngine.getStrokeCount()
            val stats = attendanceEngine.getAttendanceSummary()
            val enrolled = stats["totalEnrolled"] ?: 3
            val present = stats["confirmedPresent"] ?: 0
            val jitter = Random.nextInt(1, 8)

            val payload = "Digitizer [OK] | Pressure: 4096-Lvl | Strokes: $strokes | Attendance: $present/$enrolled | Jitter: +${jitter}ms #$frameIndex"
            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
