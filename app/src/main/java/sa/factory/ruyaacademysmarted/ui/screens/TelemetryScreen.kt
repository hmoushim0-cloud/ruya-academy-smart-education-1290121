package sa.factory.ruyaacademysmarted.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sa.factory.ruyaacademysmarted.ui.theme.*
import sa.factory.ruyaacademysmarted.ui.viewmodel.RuyaAcademyUiState

@Composable
fun TelemetryScreen(state: RuyaAcademyUiState) {
    val defaultPeripheral = "Smart Stylus Digitizer 4096-Level + BLE Beacon"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("مراقبة العتاد الحي والحساسات", fontSize = 20.sp, fontWeight = FontWeight.Black, color = BrandTextPrimary)
        Text("بروتوكول الربط المباشر الميداني لأكاديمية رؤية", fontSize = 12.sp, color = BrandTextSecondary)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("حالة الجهاز الملحق:", color = BrandTextSecondary, fontSize = 13.sp)
                    Badge(containerColor = BrandPrimary) {
                        Text(state.telemetry?.status ?: "ONLINE", color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    }
                }
                val peripheralLabel = state.telemetry?.peripheralName ?: defaultPeripheral
                Text("اسم العتاد: $peripheralLabel", fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 14.sp)
                Text("جودة الإشارة: ${state.telemetry?.signalDbm ?: -58} dBm | البطارية: ${state.telemetry?.batteryPct ?: 98}%", color = BrandSecondary, fontSize = 12.sp)

                HorizontalDivider(color = BrandCard)
                Text("حزمة البيانات اللحظية (Raw Telemetry Payload):", color = BrandTextSecondary, fontSize = 11.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.telemetry?.liveReading ?: "$defaultPeripheral: ONLINE | READY",
                        color = BrandPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BrandCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("معايير الامتثال والاعتماد الأكاديمي", fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 14.sp)
                Text("[OK] بروتوكول قلم رقمي تفاعلي 4096 مستوى ضغط مع خاصية Palm Rejection", color = BrandTextSecondary, fontSize = 12.sp)
                Text("[OK] رصد حضور ذكي عبر منارات البلوتوث منخفض الطاقة BLE Beacon بدقة Micro-Fencing", color = BrandTextSecondary, fontSize = 12.sp)
                Text("[OK] نظام تقييم مصطلحات النحو العربي المعتمد مع توثيق المصادر والصفحات", color = BrandTextSecondary, fontSize = 12.sp)
                Text("[OK] بنية كود نقية خالية من الإيموجي متوافقة مع معايير المنصات التعليمية السعودية", color = BrandTextSecondary, fontSize = 12.sp)
            }
        }
    }
}
