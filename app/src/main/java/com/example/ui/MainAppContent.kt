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
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
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
                                    fontSize = 11.sp, // Reduced by 50% compared to standard base body
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
                            fontSize = 8.sp,
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
            AlertDialog(
                onDismissRequest = { showAssistantOverlayDialog = false },
                title = { Text("🤖 المساعد الذكي التفاعلي لدليل اليمن") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "مرحباً بكم! أنا مساعدكم التفاعلي، أعمل بدون الحاجة لإنترنت لتمكينكم من استعراض الدليل بكل أريحية. إليكم بعض النصائح السريعة:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Divider(color = GrayBorder)

                        Text("📁 كيفية البحث الفعال:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("يمكنك استخدام شريط الفلاتر للفرز بالحي السكني، أو الفئات، أو المسافة التقريبية لضمان مهنيين الأقرب لك.", fontSize = 11.sp)

                        Text("👤 تقديم طلب كمهني:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("انقر على أيقونة 👤 وقم برفع صورتك الشخصية وصورة بطاقة هويتك، وسيقوم المشرفون بتفعيل حسابك فورا بشارة زرقاء ✔️.", fontSize = 11.sp)

                        Text("🔑 الإعدادات والمصادقة السرية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("الدخول للمشرفين من أيقونة 🔐 للتعديل. أما المالك فمن خلال البوابة الخلفية الحصرية المحمية تماماً.", fontSize = 11.sp)
                        
                        Text("📞 التواصل والدعم المباشر:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("رقم الدعم الفني المعتمد للتواصل والمشاركة هو: ${settings.supportPhone}.", fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { showAssistantOverlayDialog = false }) {
                        Text("فهمت، شكراً لك !")
                    }
                }
            )
        }
    }
}
