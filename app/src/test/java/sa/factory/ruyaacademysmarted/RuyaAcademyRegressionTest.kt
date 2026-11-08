package sa.factory.ruyaacademysmarted

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.ruyaacademysmarted.data.repository.RuyaAcademyRepositoryImpl
import sa.factory.ruyaacademysmarted.engine.*
import sa.factory.ruyaacademysmarted.hardware.HardwareSensorDriver
import sa.factory.ruyaacademysmarted.model.RuyaAcademyItem

class RuyaAcademyRegressionTest {

    @Test
    fun testCheckoutCalculationAndVat15() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = RuyaAcademyRepositoryImpl(sensorDriver)

        val item1 = RuyaAcademyItem("c1", "دورة مصطلحات النحو العربي المتقدمة", "شاملة 150 سؤالا مرجعيا", 120.0, "معتمد", "دورات النحو واللغة")
        val item2 = RuyaAcademyItem("c2", "ورشة الخط العربي والرسم الرقمي", "تدريب تفاعلي مباشر", 180.0, "معتمد", "ورش العمل المباشرة")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada Contactless EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
        assertTrue(order.orderId.startsWith("ORD-"))
    }

    @Test
    fun testStylusPalmRejection() {
        val engine = SmartStylusDigitizerProtocolEngine()

        val palmPoint = StylusPoint(x = 250.0f, y = 600.0f, pressureRaw = 2000, contactAreaMm2 = 18.5f)
        val tipPoint = StylusPoint(x = 100.0f, y = 200.0f, pressureRaw = 2000, contactAreaMm2 = 1.2f)
        val fingerPoint = StylusPoint(x = 100.0f, y = 200.0f, pressureRaw = 800, contactAreaMm2 = 4.0f)

        assertEquals(TouchContactType.PALM_REJECTED, engine.classifyContact(palmPoint, hasPenProximity = true))
        assertEquals(TouchContactType.STYLUS_TIP, engine.classifyContact(tipPoint, hasPenProximity = true))
        assertEquals(TouchContactType.FINGER_TOUCH, engine.classifyContact(fingerPoint, hasPenProximity = false))
    }

    @Test
    fun testStylusNormalizedPressure() {
        val zeroPressure = StylusPoint(0f, 0f, pressureRaw = 0)
        val midPressure = StylusPoint(0f, 0f, pressureRaw = 2048)
        val maxPressure = StylusPoint(0f, 0f, pressureRaw = 4095)
        val overflowPressure = StylusPoint(0f, 0f, pressureRaw = 5000)

        assertEquals(0.0f, zeroPressure.normalizedPressure, 0.001f)
        assertEquals(0.5f, midPressure.normalizedPressure, 0.01f)
        assertEquals(1.0f, maxPressure.normalizedPressure, 0.001f)
        assertEquals(1.0f, overflowPressure.normalizedPressure, 0.001f)
    }

    @Test
    fun testStylusStrokeVelocity() {
        val engine = SmartStylusDigitizerProtocolEngine()
        val now = 1000000L
        val p1 = StylusPoint(100.0f, 100.0f, 1000, timestampMillis = now)
        val p2 = StylusPoint(130.0f, 140.0f, 1200, timestampMillis = now + 10) // distance = 50px in 10ms = 5.0 px/ms

        val velocity = engine.calculateStrokeVelocity(p1, p2)
        assertEquals(5.0f, velocity, 0.05f)
    }

    @Test
    fun testStylusBezierSmoothing() {
        val engine = SmartStylusDigitizerProtocolEngine()
        val now = System.currentTimeMillis()
        val points = listOf(
            StylusPoint(50f, 50f, 1000, timestampMillis = now),
            StylusPoint(100f, 80f, 2000, timestampMillis = now + 10),
            StylusPoint(150f, 120f, 3000, timestampMillis = now + 20)
        )

        val smoothed = engine.applyBezierSmoothing(points, StylusPenMode.PEN_FINE)
        assertTrue("Smoothed points should not be empty", smoothed.isNotEmpty())
        assertEquals(3, smoothed.size)
        // Midpoint of (50,50) and (100,80) is (75, 65)
        assertEquals(75f, smoothed[0].x, 0.01f)
        assertEquals(65f, smoothed[0].y, 0.01f)
    }

    @Test
    fun testArabicDiacriticDetection() {
        val engine = SmartStylusDigitizerProtocolEngine()
        val now = System.currentTimeMillis()

        // Fast slash from top-right to bottom-left (dx = -20, dy = +25, length < 80px) -> Fathah
        val fathahStroke = listOf(
            StylusPoint(100f, 50f, 1500, timestampMillis = now),
            StylusPoint(90f, 62f, 1800, timestampMillis = now + 5),
            StylusPoint(80f, 75f, 1200, timestampMillis = now + 10)
        )

        val stroke = engine.processFinishedStroke(fathahStroke, StylusPenMode.PEN_FINE)
        assertNotNull(stroke.detectedGrammarDiacritic)
        assertTrue(stroke.detectedGrammarDiacritic!!.contains("Fathah"))
    }

    @Test
    fun testBleBeaconDistanceEstimation() {
        val engine = BleClassroomAttendanceEngine(pathLossExponent = 2.0)

        // Measured power at 1m is -59 dBm
        // If current RSSI is -59 dBm, distance should be 1.0 meter
        val dist1m = engine.estimateDistance(-59, -59)
        assertEquals(1.0, dist1m, 0.01)

        // If RSSI is -65 dBm (weaker), distance should be > 1.0 meter
        val distFar = engine.estimateDistance(-59, -65)
        assertTrue(distFar > 1.0)
    }

    @Test
    fun testBleBeaconProximityZones() {
        val engine = BleClassroomAttendanceEngine()

        assertEquals(ProximityZone.IMMEDIATE, engine.classifyProximity(0.4))
        assertEquals(ProximityZone.NEAR, engine.classifyProximity(1.8))
        assertEquals(ProximityZone.FAR, engine.classifyProximity(4.5))
        assertEquals(ProximityZone.OUT_OF_RANGE, engine.classifyProximity(8.0))
    }

    @Test
    fun testBleClassroomAttendanceDwellConfirmation() {
        val engine = BleClassroomAttendanceEngine(minimumDwellSecondsForConfirmation = 60)
        val now = 1700000000000L

        engine.registerStudent("STD-01", "عبد الله بن سعيد", "NAHW-201", 501)
        val record = engine.getStudent("STD-01")
        assertNotNull(record)
        assertFalse("Initial state must be unconfirmed", record!!.isConfirmedPresent)

        // Student arrives, signal detected in NEAR zone (-62 dBm) at t=0
        val sig1 = BleBeaconSignal("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0", 1, 501, -59, -62, now, "sig")
        engine.processSignal("STD-01", sig1, now)
        assertFalse("Immediately upon arrival, attendance should not yet be confirmed", record.isConfirmedPresent)

        // Student dwells for 75 seconds (> 60s threshold) in NEAR zone
        val later = now + (75 * 1000)
        val sig2 = BleBeaconSignal("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0", 1, 501, -59, -60, later, "sig")
        engine.processSignal("STD-01", sig2, later)
        assertTrue("After 75 seconds in classroom, attendance must be confirmed", record.isConfirmedPresent)

        val summary = engine.getAttendanceSummary()
        assertEquals(1, summary["confirmedPresent"])
        assertEquals(100.0, summary["attendancePercentage"])
    }

    @Test
    fun testBleAntiSpoofSignatureAndDriverFlow() = runBlocking {
        val driver = HardwareSensorDriver()
        val attendance = driver.attendanceEngine

        val uuid = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"
        val major = 1
        val minor = 101
        val secret = "RUYA-ACADEMY-KEY-2026"

        // Compute valid signature
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val expectedSig = md.digest("$uuid:$major:$minor:$secret".toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)

        assertTrue(attendance.verifyBeaconSignature(uuid, major, minor, expectedSig, secret))
        assertFalse(attendance.verifyBeaconSignature(uuid, major, minor, "INVALID-SIG-1234", secret))

        // Verify driver flow emissions
        val initialFrame = driver.observePeripheralStream().first()
        assertEquals("Smart Stylus Digitizer 4096-Level + BLE Beacon", initialFrame.peripheralName)
        assertEquals("CONNECTING", initialFrame.connectionState)
    }
}
