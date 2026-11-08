package sa.factory.ruyaacademysmarted.engine

import java.security.MessageDigest
import kotlin.math.pow

enum class ProximityZone(val zoneArabic: String, val maxDistanceMeters: Double) {
    IMMEDIATE("ملاصق للمقعد الذكي", 0.6),
    NEAR("داخل نطاق القاعة الدراسية", 2.8),
    FAR("عند مدخل القاعة أو الممر", 6.0),
    OUT_OF_RANGE("خارج النطاق المعتمد", Double.MAX_VALUE)
}

data class BleBeaconSignal(
    val beaconUuid: String,
    val majorId: Int,
    val minorId: Int,
    val measuredPowerAt1m: Int = -59, // Standard Apple / Eddystone reference
    val currentRssiDbm: Int,
    val timestampMillis: Long = System.currentTimeMillis(),
    val antiSpoofSignature: String
)

data class StudentAttendanceRecord(
    val studentId: String,
    val studentNameArabic: String,
    val courseCode: String,
    val assignedBeaconMinor: Int,
    var firstSeenMillis: Long,
    var lastSeenMillis: Long,
    var estimatedDistanceMeters: Double,
    var currentZone: ProximityZone,
    var isConfirmedPresent: Boolean = false
) {
    val dwellTimeSeconds: Long
        get() = ((lastSeenMillis - firstSeenMillis) / 1000).coerceAtLeast(0)
}

class BleClassroomAttendanceEngine(
    private val classroomBeaconUuid: String = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
    private val pathLossExponent: Double = 2.4, // Standard indoor classroom environment
    private val minimumDwellSecondsForConfirmation: Long = 60
) {

    private val studentRoster = mutableMapOf<String, StudentAttendanceRecord>()

    fun registerStudent(studentId: String, nameArabic: String, course: String, beaconMinor: Int) {
        val now = System.currentTimeMillis()
        studentRoster[studentId] = StudentAttendanceRecord(
            studentId = studentId,
            studentNameArabic = nameArabic,
            courseCode = course,
            assignedBeaconMinor = beaconMinor,
            firstSeenMillis = now,
            lastSeenMillis = now,
            estimatedDistanceMeters = 99.0,
            currentZone = ProximityZone.OUT_OF_RANGE,
            isConfirmedPresent = false
        )
    }

    fun estimateDistance(measuredPowerAt1m: Int, currentRssiDbm: Int): Double {
        if (currentRssiDbm == 0) return -1.0
        val ratio = (measuredPowerAt1m - currentRssiDbm).toDouble() / (10.0 * pathLossExponent)
        val rawDistance = 10.0.pow(ratio)
        return Math.round(rawDistance * 100.0) / 100.0
    }

    fun classifyProximity(distanceMeters: Double): ProximityZone {
        return when {
            distanceMeters <= ProximityZone.IMMEDIATE.maxDistanceMeters -> ProximityZone.IMMEDIATE
            distanceMeters <= ProximityZone.NEAR.maxDistanceMeters -> ProximityZone.NEAR
            distanceMeters <= ProximityZone.FAR.maxDistanceMeters -> ProximityZone.FAR
            else -> ProximityZone.OUT_OF_RANGE
        }
    }

    fun verifyBeaconSignature(uuid: String, major: Int, minor: Int, signature: String, secretKey: String): Boolean {
        val payload = "$uuid:$major:$minor:$secretKey"
        val digest = MessageDigest.getInstance("SHA-256")
        val expected = digest.digest(payload.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        return expected.equals(signature, ignoreCase = true)
    }

    fun processSignal(studentId: String, signal: BleBeaconSignal, currentEpochMillis: Long): Boolean {
        val record = studentRoster[studentId] ?: return false
        if (signal.beaconUuid != classroomBeaconUuid || signal.minorId != record.assignedBeaconMinor) {
            return false
        }

        val distance = estimateDistance(signal.measuredPowerAt1m, signal.currentRssiDbm)
        val zone = classifyProximity(distance)

        record.lastSeenMillis = currentEpochMillis
        record.estimatedDistanceMeters = distance
        record.currentZone = zone

        // Presence confirmed if student is inside the classroom (IMMEDIATE or NEAR) and dwell >= threshold
        if ((zone == ProximityZone.IMMEDIATE || zone == ProximityZone.NEAR) &&
            record.dwellTimeSeconds >= minimumDwellSecondsForConfirmation
        ) {
            record.isConfirmedPresent = true
        }

        return true
    }

    fun getStudent(studentId: String): StudentAttendanceRecord? = studentRoster[studentId]

    fun getAttendanceSummary(): Map<String, Any> {
        val total = studentRoster.size
        val present = studentRoster.values.count { it.isConfirmedPresent }
        val percentage = if (total > 0) Math.round((present.toDouble() / total.toDouble()) * 1000.0) / 10.0 else 0.0

        return mapOf(
            "totalEnrolled" to total,
            "confirmedPresent" to present,
            "absentCount" to (total - present),
            "attendancePercentage" to percentage
        )
    }
}
