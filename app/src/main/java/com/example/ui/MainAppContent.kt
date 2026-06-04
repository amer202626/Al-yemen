package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Database states
    val categoriesList by viewModel.categories.collectAsState()
    val activeProvidersList = viewModel.filteredProviders.collectAsState().value
    val bannersList by viewModel.banners.collectAsState()
    val settingsState by viewModel.appSettings.collectAsState()

    val settings = settingsState ?: AppSettings()

    // Double back tap handle
    var lastBackPressTime by remember { mutableStateOf(0L) }

    BackHandler(enabled = true) {
        if (viewModel.currentScreen != "HOME") {
            viewModel.currentScreen = "HOME"
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000) {
                // Exit app
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "🇾🇪 اضغط مرة أخرى للخروج من دليل اليمن", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Direct floating direct overlay assistant dialog state
    var showAssistantOverlayDialog by remember { mutableStateOf(false) }
    var assistantOffsetStateX by remember { mutableStateOf(0f) }
    var assistantOffsetStateY by remember { mutableStateOf(0f) }

    YemenTheme(
        themeChoice = settings.themeChoice,
        customPrimaryHex = settings.customPrimaryColor,
        customBgHex = settings.customBackgroundColor
    ) {
        Scaffold(
            topBar = {
                // --- CUSTOM TOP APP BAR ---
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Title bar with App Logo (clickable and counts backdoor entries)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clickable { viewModel.onLogoClick() } // Backdoor clickable logo
                            ) {
                                // Dynamic App Launcher Logo text representation
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🇾🇪", fontSize = 18.sp)
                                }
                                Text(
                                    text = settings.appName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = if (settings.fontStyle == "MONOSPACE") FontFamily.Monospace else FontFamily.Default
                                )
                            }

                            // Dynamic quick language display badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.DarkGray)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (settings.activeLanguage == "AR") "عربي" else "EN",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Icons Strip according to topAppBarConfigItems
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Order of configuration items
                            val items = settings.topAppBarConfigItems.split(",")
                            for (item in items) {
                                when (item.uppercase().trim()) {
                                    "HOME" -> {
                                        IconButton(onClick = { viewModel.onHomeClick() }) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🏠", fontSize = 18.sp)
                                                Text("الرئيسية", fontSize = 8.sp, color = if (viewModel.currentScreen == "HOME") MaterialTheme.colorScheme.primary else Color.Gray)
                                            }
                                        }
                                    }
                                    "LOGIN" -> {
                                        IconButton(onClick = {
                                            viewModel.currentScreen = if (viewModel.isLoggedIn) "ADMIN_DASHBOARD" else "LOGIN"
                                        }) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(if (viewModel.isLoggedIn) "🛡️" else "🔐", fontSize = 18.sp)
                                                Text(if (viewModel.isLoggedIn) "اللوحة" else "دخول", fontSize = 8.sp, color = if (viewModel.currentScreen == "LOGIN") MaterialTheme.colorScheme.primary else Color.Gray)
                                            }
                                        }
                                    }
                                    "REGISTER" -> {
                                        IconButton(onClick = { viewModel.currentScreen = "REGISTER" }) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("👤", fontSize = 18.sp)
                                                Text("انضمام", fontSize = 8.sp, color = if (viewModel.currentScreen == "REGISTER") MaterialTheme.colorScheme.primary else Color.Gray)
                                            }
                                        }
                                    }
                                    "LANG" -> {
                                        IconButton(
                                            onClick = {
                                                val nextLang = if (settings.activeLanguage == "AR") "EN" else "AR"
                                                scope.launch {
                                                    viewModel.updateGeneralSettings(
                                                        name = settings.appName,
                                                        footerText = settings.adFooterText,
                                                        footerHidden = settings.isFooterHidden,
                                                        welcome = settings.welcomeMessage,
                                                        phone = settings.supportPhone,
                                                        email = settings.supportEmail,
                                                        whatsapp = settings.supportWhatsapp,
                                                        maintenance = settings.isMaintenanceMode,
                                                        fcm = settings.enableFCMNotifications,
                                                        assistantHidden = settings.isAssistantHidden,
                                                        whitelist = settings.permittedDevices,
                                                        sec2fa = settings.is2faEnabled,
                                                        topBar = settings.topAppBarConfigItems,
                                                        fontStyle = settings.fontStyle,
                                                        fontColor = settings.fontColor,
                                                        assistantSize = settings.assistantSize,
                                                        assistantX = settings.assistantPositionX,
                                                        assistantY = settings.assistantPositionY,
                                                        assistantIconChar = settings.assistantIcon
                                                    ).run {
                                                        val repo = AppRepository(context)
                                                        val s = repo.getAppSettingsDirect()
                                                        repo.updateSettings(s.copy(activeLanguage = nextLang))
                                                    }
                                                }
                                                Toast.makeText(context, "🌐 تم تغيير اللغة الفعالة للواجهة!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🌐", fontSize = 18.sp)
                                                Text("اللغة", fontSize = 8.sp)
                                            }
                                        }
                                    }
                                    "REFRESH" -> {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    Toast.makeText(context, "🔄 تم تحديث جميع المجموعات الفورية ولحظية الآن!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🔄", fontSize = 18.sp)
                                                Text("تحديث", fontSize = 8.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // --- CUSTOM INTEGRATED FOOTER AND ASSISTANTS ---
                val trans = settings.footerTransparency
                val fontS = settings.footerFontSize.sp
                val padV = (10 * settings.footerHeightScale).dp
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = trans),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = padV, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Side: About App link Icon (ℹ️ / custom aboutIcon)
                            IconButton(
                                onClick = { viewModel.currentScreen = "ABOUT" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(settings.aboutIcon, fontSize = 20.sp)
                            }

                            // Center Side: Advertising FooterText (Customizable, reduced size by 50%!)
                            if (!settings.isFooterHidden) {
                                Text(
                                    text = settings.adFooterText,
                                    fontSize = fontS,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Right Side: Previous user booking/service interactions icon
                            IconButton(
                                onClick = { viewModel.currentScreen = "PREVIOUS_REQUESTS" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("📋", fontSize = 20.sp)
                            }
                        }
                        
                        Text(
                            text = "صنع بكل فخر يمني 🇾🇪 - WAM 2026",
                            fontSize = (0.7f * settings.footerFontSize).sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            floatingActionButton = {
                // --- FLOATING ACTION ASSISTANT BUTTON (🤖) ---
                if (!settings.isAssistantHidden) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    assistantOffsetStateX.roundToInt(),
                                    assistantOffsetStateY.roundToInt()
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    assistantOffsetStateX += dragAmount.x
                                    assistantOffsetStateY += dragAmount.y
                                }
                            }
                            .size(settings.assistantSize.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { showAssistantOverlayDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(settings.assistantIcon, fontSize = 20.sp)
                            Text("خدمات", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.surface)
                        }
                    }
                }
            }
        ) { innerPadding ->
            // Toast / Alert simulator of backend notifications
            LaunchedEffect(key1 = viewModel.adminInstantNotification) {
                if (viewModel.adminInstantNotification != null) {
                    Toast.makeText(context, viewModel.adminInstantNotification, Toast.LENGTH_LONG).show()
                    viewModel.clearInstantNotification()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Render the swapped screens
                when (viewModel.currentScreen) {
                    "HOME" -> {
                        HomeScreen(
                            viewModel = viewModel,
                            settings = settings,
                            categoriesList = categoriesList,
                            providersList = activeProvidersList,
                            bannersList = bannersList
                        )
                    }
                    "REGISTER" -> {
                        RegisterScreen(
                            viewModel = viewModel,
                            settings = settings,
                            categoriesList = categoriesList
                        )
                    }
                    "LOGIN" -> {
                        LoginScreen(
                            viewModel = viewModel,
                            settings = settings
                        )
                    }
                    "ABOUT" -> {
                        AboutScreen(viewModel = viewModel, settings = settings)
                    }
                    "PREVIOUS_REQUESTS" -> {
                        PreviousRequestsScreen(viewModel = viewModel, settings = settings)
                    }
                    "PROVIDER_DETAIL" -> {
                        viewModel.selectedProviderId?.let { id ->
                            ProviderDetailScreen(
                                viewModel = viewModel,
                                settings = settings,
                                providerId = id
                            )
                        } ?: run {
                            viewModel.currentScreen = "HOME"
                        }
                    }
                    "CHAT_ROOM" -> {
                        ChatScreen(viewModel = viewModel, settings = settings)
                    }
                    "ADMIN_DASHBOARD" -> {
                        if (viewModel.isLoggedIn) {
                            AdminDashboardScreen(
                                viewModel = viewModel,
                                settings = settings
                            )
                        } else {
                            viewModel.currentScreen = "LOGIN"
                        }
                    }
                }

                // FLOATING CIRCULAR POPUP ICON FOR CHAT (Direct interaction)
                if (!settings.isChatIconDeleted && !settings.isChatIconHidden) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 1.dp)
                            .size(settings.chatIconSize.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(settings.chatIconColor, Color(0xFF00C853)))
                            .clickable {
                                viewModel.currentScreen = "CHAT_ROOM"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💬", fontSize = (settings.chatIconSize * 0.45f).sp, color = Color.White)
                    }
                }

                // Invisible Overlay floating popups or panels
            }
        }

        // --- BACKDOOR AUTHENTICATION POPUP DIALOG --- (Absolutely Secret)
        if (viewModel.showBackdoorAuthDialog) {
            var backdoorPassField by remember { mutableStateOf("") }
            var backdoorRememberField by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { viewModel.showBackdoorAuthDialog = false },
                title = { Text("🔑 الدخول السري المرموق") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("الرجاء إدخال رمز العبور السري الحصري للمالك للولوج المباشر:")
                        OutlinedTextField(
                            value = backdoorPassField,
                            onValueChange = { backdoorPassField = it },
                            label = { Text("كلمة المرور الخاصة:") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = backdoorRememberField,
                                onCheckedChange = { backdoorRememberField = it }
                            )
                            Text("تذكر حفظ صلاحياتي بالبوابة", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveLoginState = backdoorRememberField
                            val ok = viewModel.loginBackdoor(backdoorPassField)
                            if (ok) {
                                Toast.makeText(context, "✓ مرحباً بعودتك يا مالك الدليل!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "❌ رمز المرور المدخل غير صحيح!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("افتح البوابة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showBackdoorAuthDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // --- SMART ASSISTANT INTERACTIVE FAQ MODAL DIALOG ---
        if (showAssistantOverlayDialog) {
            var assistantInputText by remember { mutableStateOf("") }
            var assistantChatHistory by remember {
                mutableStateOf(
                    listOf(
                        "ASSISTANT" to "مرحباً بك! أنا مساعد دليل اليمن السريع 🤖. كيف يمكنني مساعدتك اليوم؟"
                    )
                )
            }

            AlertDialog(
                onDismissRequest = { showAssistantOverlayDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🤖", fontSize = 24.sp)
                        Text("المساعد التفاعلي الذكي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Chat messages stream
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (msg in assistantChatHistory) {
                                    val isMe = msg.first == "USER"
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        Surface(
                                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 0.dp,
                                                bottomEnd = if (isMe) 0.dp else 12.dp
                                            ),
                                            tonalElevation = 1.dp
                                        ) {
                                            Text(
                                                text = msg.second,
                                                fontSize = 12.sp,
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Quick action chips
                        Text("💡 أسئلة شائعة اقترحها لك:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val suggestQuestions = listOf(
                                "ماهي الأقسام",
                                "كيف أتصل بمقدم خدمة",
                                "ما هو رقم الدعم"
                            )
                            for (q in suggestQuestions) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable {
                                            val query = q
                                            val reply = when {
                                                query.contains("أقسام") || query.contains("اقسام") -> {
                                                    "الأقسام والمهن المتاحة بالدليل حالياً هي: \n" + categoriesList.joinToString("\n") { "• ${it.imageBase64} ${it.nameAr}" }
                                                }
                                                query.contains("أتصل") || query.contains("اتصل") -> {
                                                    "للاتصال بأي مهني:\n1. اختر القسم المناسب.\n2. انقر على ملف المهني.\n3. اضغط زر الاتصال الأخضر للتواصل مباشرة عبر الهاتف أو الواتساب."
                                                }
                                                query.contains("الدعم") || query.contains("دعم") -> {
                                                    "رقم الدعم الفني الرسمي لدليل اليمن هو: ${settings.supportPhone} \n(MAW 777644670) - راسلنا لحل أي مشكلة فوراً."
                                                }
                                                else -> "أهلاً بك! دليل اليمن يرحب باستفسارك المتميز."
                                            }
                                            assistantChatHistory = assistantChatHistory + (query to reply)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(q, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Input control
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = assistantInputText,
                                onValueChange = { assistantInputText = it },
                                placeholder = { Text("اكتب استفسارك هنا...") },
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (assistantInputText.isNotBlank()) {
                                        val query = assistantInputText.trim()
                                        val reply = when {
                                            query.contains("أقسام") || query.contains("اقسام") -> {
                                                "الأقسام والمهن المتاحة بالدليل حالياً هي: \n" + categoriesList.joinToString("\n") { "• ${it.imageBase64} ${it.nameAr}" }
                                            }
                                            query.contains("أتصل") || query.contains("اتصل") || query.contains("تواصل") -> {
                                                "للاتصال بأي مهني:\n1. اختر القسم المناسب.\n2. انقر على ملف المهني.\n3. اضغط زر الاتصال الأخضر للتواصل مباشرة عبر الهاتف أو الواتساب."
                                            }
                                            query.contains("الدعم") || query.contains("دعم") || query.contains("رقم") -> {
                                                "رقم الدعم الفني الرسمي لدليل اليمن هو: ${settings.supportPhone} \n(MAW 777644670) - راسلنا لحل أي مشكلة فوراً."
                                            }
                                            else -> "تعديل الإدارة: مرحباً بك! تساؤلك قيد التحليل والدعم. يمكنك الاتصال بخط المساعدة الرسمي ${settings.supportPhone} للحصول على دعم مخصص وسريع!"
                                        }
                                        assistantChatHistory = assistantChatHistory + (query to reply)
                                        assistantInputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Text("✉️", color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAssistantOverlayDialog = false }) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
