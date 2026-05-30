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
        hasOverlayPermissionState.value = Settings.canDrawOverlays(this)
        isAccessibilityEnabledState.value = isAccessibilityServiceEnabled(this, SpamAccessibilityService::class.java)
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0F0F1A) // Space Slate background
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Branding Logo / Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFE040FB),
                            Color(0x00E040FB)
                        )
                    ),
                    shape = CircleShape
                )
                .border(2.dp, Color(0xFF00E5FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SPAM ASİSTANI",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.SansSerif
        )

        Text(
            text = "Erişilebilirlik ve Otomasyon Asistanı",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF00E5FF),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Permission list card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E2E),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Gereksinimler",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Spam panelinin ekran üzerinde çalışabilmesi için aşağıdaki iki iznin verilmesi zorunludur.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Permission Item 1
                PermissionRow(
                    title = "Ekran Üstü Çizim İzni",
                    desc = "Diğer uygulamaların üzerinde yüzen spam panelini göstermek için gereklidir.",
                    isGranted = hasOverlayPermission,
                    onGrantClick = onRequestOverlay
                )

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = Color(0x1AFFFFFF))

                Spacer(modifier = Modifier.height(12.dp))

                // Permission Item 2
                PermissionRow(
                    title = "Erişilebilirlik Servisi",
                    desc = "Belirtilen koordinatlara otomatik tıklamak ve spam metinlerini yapıştırmak için gereklidir.",
                    isGranted = isAccessibilityEnabled,
                    onGrantClick = onRequestAccessibility
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // System state info card
        val allGranted = hasOverlayPermission && isAccessibilityEnabled

        AnimatedVisibility(
            visible = allGranted,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0x334CAF50),
                border = BorderStroke(1.dp, Color(0xFF4CAF50))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Asistan Hazır!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Yüzen panel ekranınızda otomatik olarak açıldı. Hedef uygulamayı açıp spam yapmaya başlayabilirsiniz.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Guide / Manual Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161622),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Nasıl Kullanılır?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                GuideStep(
                    number = "1",
                    text = "Yukarıdaki bölümlerden her iki izni de aktif edin."
                )
                GuideStep(
                    number = "2",
                    text = "Ekrandaki yüzen panelde yer alan kilit ikonuna (🔒) basarak panel klavyeyi aktifleştirin ve spam metninizi ve tekrar sayısını girin."
                )
                GuideStep(
                    number = "3",
                    text = "Target uygulamanızı (oyun, mesajlaşma) açıp 'Metin Alanını İşaretle' butonuna basın ve ardından 3 saniye içinde spam yapılacak metin kutusunun üzerine tıklayın."
                )
                GuideStep(
                    number = "4",
                    text = "'Başlat' (▶) butonuna dokunarak spam işlemini arka planda başlatabilirsiniz. 'Durdur' (⏸) ile dilediğiniz an durdurabilirsiniz."
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
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
                    if (isGranted) Color(0x334CAF50) else Color(0x33F44336),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!isGranted) {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "İzin Ver",
                    fontSize = 11.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
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
                .background(Color(0xFFE040FB), shape = CircleShape),
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
            color = Color.LightGray,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// Border utility to keep style clean and support M3 shape rendering easily
@Composable
fun BoxBorder(width: androidx.compose.ui.unit.Dp, color: Color) = Modifier.border(width, color, RoundedCornerShape(16.dp))
