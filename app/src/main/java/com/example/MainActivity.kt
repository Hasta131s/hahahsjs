package com.example

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spam.SpamAccessibilityService
import com.example.spam.SpamState
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun onResume() {
        super.onResume()
        // Refresh checks on resume
        checkPermissions()
    }

    private var hasOverlayPermissionState = mutableStateOf(false)
    private var isAccessibilityEnabledState = mutableStateOf(false)

    private fun checkPermissions() {
        val overlay = Settings.canDrawOverlays(this)
        val accessibility = isAccessibilityServiceEnabled(this, SpamAccessibilityService::class.java)
        
        if (overlay && accessibility && !(hasOverlayPermissionState.value && isAccessibilityEnabledState.value)) {
            SpamState.isOverlayVisible.value = true
        }
        
        hasOverlayPermissionState.value = overlay
        isAccessibilityEnabledState.value = accessibility
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFFEF7FF), // Professional Polish light surface
                    topBar = {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF7FF))
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFF6750A4), shape = RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Spam Asistanı",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1B20),
                                        letterSpacing = (-0.5).sp
                                    )
                                }
                                IconButton(
                                    onClick = { },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Daha Fazla",
                                        tint = Color(0xFF1D1B20)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFEADDFF))
                        }
                    },
                    bottomBar = {
                        Column {
                            HorizontalDivider(color = Color(0xFFEADDFF))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3EDF7))
                                    .navigationBarsPadding()
                                    .height(64.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFEADDFF), shape = RoundedCornerShape(16.dp))
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Ana Sayfa",
                                            tint = Color(0xFF6750A4),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Text("Ana Sayfa", fontSize = 11.sp, color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Ayarlar",
                                        tint = Color(0xFF49454F),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text("Ayarlar", fontSize = 11.sp, color = Color(0xFF49454F))
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Rehber",
                                        tint = Color(0xFF49454F),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text("Rehber", fontSize = 11.sp, color = Color(0xFF49454F))
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    MainDashboardScreen(
                        hasOverlayPermission = hasOverlayPermissionState.value,
                        isAccessibilityEnabled = isAccessibilityEnabledState.value,
                        onRequestOverlay = { requestOverlayPermission() },
                        onRequestAccessibility = { requestAccessibilityPermission() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val expectedComponentName = ComponentName(context, service)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }
}

@Composable
fun MainDashboardScreen(
    hasOverlayPermission: Boolean,
    isAccessibilityEnabled: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Branding Header (M3 look)
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFFF7F2FA), shape = CircleShape)
                .border(1.dp, Color(0xFFEADDFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "SwiftSpammer Asistanı",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D1B20),
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Arka planda çalışan koordinat bazlı tıklayıcı ve otomatik yapıştırıcı",
            fontSize = 12.sp,
            color = Color(0xFF49454F),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Session Stats Row (Extracted from Design HTML)
        Text(
            text = "OTURUM İSTATİSTİKLERİ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6750A4),
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 1
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEADDFF))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "TOPLAM GÖNDERİM", fontSize = 10.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "14,204", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF6750A4))
                }
            }

            // Stat 2
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEADDFF))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "AKTİF SÜRE", fontSize = 10.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "02:45", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF6750A4))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Requirements/Permissions Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF7F2FA),
            border = BorderStroke(1.dp, Color(0xFFEADDFF))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Gerekli İzinler",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1D1B20)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Uygulamanın aktif otomasyon yapabilmesi için aşağıdaki koşullar gereklidir.",
                    fontSize = 12.sp,
                    color = Color(0xFF49454F),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Permission Item 1
                PermissionRow(
                    title = "Ekran Üstü Çizim İzni",
                    desc = "Uygulamalar üzerinde yüzen kontrol panelini barındırmak için gereklidir.",
                    isGranted = hasOverlayPermission,
                    onGrantClick = onRequestOverlay
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEADDFF))
                Spacer(modifier = Modifier.height(12.dp))

                // Permission Item 2
                PermissionRow(
                    title = "Erişilebilirlik Servisi",
                    desc = "Koordinat bazlı dokunma ve otomatik giriş simülasyonları için kullanılır.",
                    isGranted = isAccessibilityEnabled,
                    onGrantClick = onRequestAccessibility
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val allGranted = hasOverlayPermission && isAccessibilityEnabled
        val isOverlayVisible by SpamState.isOverlayVisible.collectAsState()

        AnimatedVisibility(
            visible = allGranted,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F8E9), // Light green elegant card
                    border = BorderStroke(1.dp, Color(0xFFDCEDC8))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Başarılı",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Asistan Hazır!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "Gerekli tüm izinler sağlandı. Yüzen kontrol panelini istediğiniz zaman aşağıdaki butonla başlatabilirsiniz.",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        SpamState.isOverlayVisible.value = !isOverlayVisible
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverlayVisible) Color(0xFFBA1A1A) else Color(0xFF6750A4),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isOverlayVisible) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOverlayVisible) "YÜZEN PANELİ KAPAT" else "YÜZEN PANELİ BAŞLAT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Guide / Manual Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF7F2FA),
            border = BorderStroke(1.dp, Color(0xFFEADDFF))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Asistan Kullanım Rehberi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1D1B20)
                )

                Spacer(modifier = Modifier.height(16.dp))

                GuideStep(
                    number = "1",
                    text = "Gerekli iki izni yukarıdan aktif edin. Panel otomatik olarak ekrana yerleşecektir."
                )
                GuideStep(
                    number = "2",
                    text = "Yüzen panel üstünde yer alan kilit butonuna (🔒) tıklayarak klavye odağını açın ve spam metnini, sayısını, hız ayarlarını yapın."
                )
                GuideStep(
                    number = "3",
                    text = "Metin Alanını İşaretle butonuna basıp, 3 saniyelik sayaç bitiminde hedef uygulamanın (messenger veya chat arayüzündeki) mesaj yazma alanına tıklayın."
                )
                GuideStep(
                    number = "4",
                    text = "Artık 'Başlat' butonuna tıklayarak milisaniye gecikmeli spam döngülerini arka planda güvenle yürütebilirsiniz."
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun PermissionRow(
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (isGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1D1B20)
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF49454F),
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!isGranted) {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "İzin Ver",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Aktif",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun GuideStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color(0xFF6750A4), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF49454F),
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// Border utility to keep style clean and support M3 shape rendering easily
@Composable
fun BoxBorder(width: androidx.compose.ui.unit.Dp, color: Color) = Modifier.border(width, color, RoundedCornerShape(16.dp))
