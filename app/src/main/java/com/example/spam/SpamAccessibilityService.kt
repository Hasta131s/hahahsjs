package com.example.spam

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class SpamAccessibilityService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var catcherView: View? = null
    private lateinit var overlayParams: WindowManager.LayoutParams

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var spamJob: Job? = null
    private var countdownJob: Job? = null

    // For lifecycle owner implementation in service
    private var lifecycleOwner: MyLifecycleOwner? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        SpamState.isServiceRunning.value = true
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        showOverlay()
        
        // Listen to state changes
        serviceScope.launch {
            SpamState.isRunning.collectLatest { running ->
                if (running) {
                    executeSpamming()
                } else {
                    spamJob?.cancel()
                    spamJob = null
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        stopSpamming()
        hideOverlay()
        hideCatcher()
        SpamState.isServiceRunning.value = false
        lifecycleOwner?.onDestroy()
        serviceScope.cancel()
    }

    // Displays the floating panel
    private fun showOverlay() {
        if (overlayView != null) return

        lifecycleOwner = MyLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // We match WRAP_CONTENT to keep the floating view neat and tiny
        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Color(0xFFE040FB),
                        secondary = Color(0xFF00E5FF),
                        surface = Color(0xFF1E1E2E)
                    )
                ) {
                    FloatingPanelContent(
                        onDrag = { dx, dy ->
                            overlayParams.x += dx.toInt()
                            overlayParams.y += dy.toInt()
                            if (overlayView != null) {
                                windowManager.updateViewLayout(overlayView, overlayParams)
                            }
                        },
                        onClose = {
                            stopSelf()
                        },
                        onStartMarking = {
                            startMarkingCountdown()
                        },
                        onFocusChanged = { focusable ->
                            updateFocusable(focusable)
                        }
                    )
                }
            }
        }

        // Set lifecycle tree components
        lifecycleOwner?.let { owner ->
            composeView.setViewTreeLifecycleOwner(owner)
            composeView.setViewTreeViewModelStoreOwner(owner)
            composeView.setViewTreeSavedStateRegistryOwner(owner)
        }

        overlayView = composeView
        windowManager.addView(overlayView, overlayParams)
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Already removed or not added
            }
            overlayView = null
        }
    }

    // Toggle focusing so editing fields and writing via keyboard works perfectly
    private fun updateFocusable(focusable: Boolean) {
        if (overlayView == null) return
        val currentFlags = overlayParams.flags
        overlayParams.flags = if (focusable) {
            currentFlags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            currentFlags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        try {
            windowManager.updateViewLayout(overlayView, overlayParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Begins the countdown before capturing target location
    private fun startMarkingCountdown() {
        if (SpamState.isMarkingMode.value) return
        SpamState.isMarkingMode.value = true
        SpamState.countdownSeconds.value = 3

        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            // Hide the primary floating panel so it doesn't block the screen under the countdown
            hideOverlay()

            while (SpamState.countdownSeconds.value > 0) {
                delay(1000)
                SpamState.countdownSeconds.value -= 1
            }

            // Show catcher full screen overlay to intercept the next tap
            showTouchCatcher()
        }
    }

    // Spawns a full-screen helper to capture next screen touch
    private fun showTouchCatcher() {
        if (catcherView != null) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val catcherParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.FILL
        }

        val containerLifecycle = MyLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        val catcherComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x73000000)) // Nice semi-translucent smoke background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xE6121212),
                            tonalElevation = 8.dp,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Koordinat Seç",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "DOKUNMA ALANI",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ekranda spam yapmak istediğiniz hedef noktaya dokunarak işaretleyin.",
                                    fontSize = 14.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.widthIn(max = 280.dp),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        catcherComposeView.setViewTreeLifecycleOwner(containerLifecycle)
        catcherComposeView.setViewTreeViewModelStoreOwner(containerLifecycle)
        catcherComposeView.setViewTreeSavedStateRegistryOwner(containerLifecycle)

        // Custom touch listener intercepts touch and records coordinates
        catcherComposeView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val rx = event.rawX
                val ry = event.rawY
                
                SpamState.targetX.value = rx
                SpamState.targetY.value = ry
                SpamState.isMarkingMode.value = false
                
                Toast.makeText(this@SpamAccessibilityService, "Hedef Konum Kaydedildi: X: ${rx.toInt()}, Y: ${ry.toInt()}", Toast.LENGTH_SHORT).show()
                
                // Hide catcher and restore primary overlay
                hideCatcher()
                showOverlay()
                true
            } else {
                false
            }
        }

        catcherView = catcherComposeView
        windowManager.addView(catcherView, catcherParams)
    }

    private fun hideCatcher() {
        catcherView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Already removed
            }
            catcherView = null
        }
    }

    private fun executeSpamming() {
        spamJob?.cancel()
        spamJob = serviceScope.launch {
            val count = SpamState.spamCount.value
            val text = SpamState.spamText.value
            val interval = SpamState.spamIntervalMs.value
            val x = SpamState.targetX.value
            val y = SpamState.targetY.value

            if (x == null || y == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SpamAccessibilityService, "Lütfen önce hedef tıklama alanı işaretleyin!", Toast.LENGTH_LONG).show()
                }
                SpamState.isRunning.value = false
                return@launch
            }

            for (i in 1..count) {
                if (!SpamState.isRunning.value) break

                // 1. Dispatch custom coordinate click
                dispatchClick(x, y)

                // 2. Wait 200ms for system focus to settle
                delay(200)

                // 3. Write or paste the text
                if (text.isNotEmpty() && SpamState.isRunning.value) {
                    val success = writeTextToFocusedNode(text)
                    if (!success) {
                        // Fallback to clipboard & paste
                        copyToClipboard(text)
                        performPasteAction()
                    }
                }

                // Wait for the specified interval rate
                delay(interval)
            }

            SpamState.isRunning.value = false
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SpamAccessibilityService, "Spam döngüsü tamamlandı!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dispatch click gesture using Path API
    private fun dispatchClick(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
        dispatchGesture(builder.build(), null, null)
    }

    // Try injecting directly into focused node via ACTION_SET_TEXT
    private fun writeTextToFocusedNode(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            rootNode.recycle()
            return success
        }
        rootNode.recycle()
        return false
    }

    private fun copyToClipboard(text: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("spam", text)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Trigger paste on focused node fallback
    private fun performPasteAction(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            focusedNode.recycle()
            rootNode.recycle()
            return success
        }
        rootNode.recycle()
        return false
    }

    private fun stopSpamming() {
        SpamState.isRunning.value = false
        spamJob?.cancel()
        spamJob = null
    }
}

// Custom Lifecycle registry for floating views inside android Service
class MyLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        controller.performRestore(null)
    }

    fun onCreate() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onStart() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = store
    override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingPanelContent(
    onDrag: (Float, Float) -> Unit,
    onClose: () -> Unit,
    onStartMarking: () -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    val targetX by SpamState.targetX.collectAsState()
    val targetY by SpamState.targetY.collectAsState()
    val text by SpamState.spamText.collectAsState()
    val count by SpamState.spamCount.collectAsState()
    val interval by SpamState.spamIntervalMs.collectAsState()
    val isRunning by SpamState.isRunning.collectAsState()
    val isMarkingMode by SpamState.isMarkingMode.collectAsState()
    val countdown by SpamState.countdownSeconds.collectAsState()

    var textInput by remember { mutableStateOf(text) }
    var countInput by remember { mutableStateOf(count.toString()) }
    var intervalInput by remember { mutableStateOf(interval.toString()) }

    var isKeyboardActive by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEB161622)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Drag and Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFE040FB),
                                Color(0xFF00E5FF)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Sürükle",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Spam Panel",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        onFocusChanged(!isKeyboardActive)
                        isKeyboardActive = !isKeyboardActive
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isKeyboardActive) Icons.Default.Lock else Icons.Default.Lock,
                        contentDescription = "Klavye Odağı",
                        tint = if (isKeyboardActive) Color(0xFF00E5FF) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text to Spam input
            TextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    SpamState.spamText.value = it
                },
                label = { Text("Spam Metni", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x33000000),
                    unfocusedContainerColor = Color(0x1A000000),
                    focusedIndicatorColor = Color(0xFFE040FB),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLabelColor = Color(0xFFE040FB),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Spam inputs (Count and Interval)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = countInput,
                    onValueChange = {
                        countInput = it
                        it.toIntOrNull()?.let { num ->
                            SpamState.spamCount.value = num
                        }
                    },
                    label = { Text("Tekrar", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x33000000),
                        unfocusedContainerColor = Color(0x1A000000),
                        focusedIndicatorColor = Color(0xFF00E5FF),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                )

                TextField(
                    value = intervalInput,
                    onValueChange = {
                        intervalInput = it
                        it.toLongOrNull()?.let { time ->
                            SpamState.spamIntervalMs.value = time
                        }
                    },
                    label = { Text("Hız (ms)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x33000000),
                        unfocusedContainerColor = Color(0x1A000000),
                        focusedIndicatorColor = Color(0xFF00E5FF),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Marking Target button
            Button(
                onClick = onStartMarking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMarkingMode) Color(0xFFFF5252) else Color(0x3300E5FF)
                ),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isMarkingMode) "Hedefe Dokun ($countdown)" else "Metin Alanını İşaretle",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            targetX?.let { tx ->
                targetY?.let { ty ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Konum: (${tx.toInt()}, ${ty.toInt()})",
                        fontSize = 11.sp,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // START
                Button(
                    onClick = {
                        SpamState.isRunning.value = true
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        disabledContainerColor = Color(0x334CAF50)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Başlat",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Başlat", fontSize = 12.sp, color = Color.White)
                }

                // STOP
                Button(
                    onClick = {
                        SpamState.isRunning.value = false
                    },
                    enabled = isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336),
                        disabledContainerColor = Color(0x33F44336)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Durdur",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Durdur", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
