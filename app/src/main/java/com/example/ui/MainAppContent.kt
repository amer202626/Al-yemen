package com.example.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: AppViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val providersList by viewModel.providers.collectAsState()
    val bannersList by viewModel.banners.collectAsState()
    val moderatorsList by viewModel.moderators.collectAsState()
    val complaintsList by viewModel.complaints.collectAsState()

    var activeScreen by remember { mutableStateOf("HOME") }
    var selectedProviderForDetail by remember { mutableStateOf<ServiceProvider?>(null) }
    var activeChatWithProvider by remember { mutableStateOf<ServiceProvider?>(null) }

    // Admin login form states
    var adminUserField by remember { mutableStateOf("") }
    var adminPassField by remember { mutableStateOf("") }

    // AI Assistant simulation state
    var showAiChatbox by remember { mutableStateOf(false) }
    var aiQueryInput by remember { mutableStateOf("") }
    val aiLogList = remember { mutableStateListOf<Pair<String, Boolean>>(
        "أهلاً بك يا غالي! أنا مساعدك الذكي اليمني في المنصة، اسألني عن أي مهندس، سباك، أو كهربائي لمساعدتك!" to false
    ) }

    // --- 1. IN-APP UPDATE CHECK DIALOGUE ---
    var showUpdateDialog by remember { mutableStateOf(false) }
    val packageVersionCode = 1 // App default version

    LaunchedEffect(settings.latestVersionCode) {
        if (settings.latestVersionCode > packageVersionCode) {
            showUpdateDialog = true
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("🔄 تحديث جديد متوفر!") },
            text = { Text("يوجد تحديث جديد للنسخة رقم ${settings.latestVersionCode} بالمنصة، هل تريد تحميله وتثبيته فوراً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        try {
                            val request = DownloadManager.Request(Uri.parse(settings.latestVersionUrl))
                                .setTitle("تحميل تحديث دليل اليمن")
                                .setDescription("تحميل ملف APK التحديث اللحظي...")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "yemen_directory_update.apk")

                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                            Toast.makeText(context, "📥 بدأ تحميل التحديث الجديد بالخلفية! يمكنك تثبيته من مجلد الداونلود فور الاكتمال.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "فشل بدء التحميل التلقائي: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("تحميل التحديث 📥")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("قرأت لاحقاً")
                }
            }
        )
    }

    // --- Dynamic Background Theme Wrapping ---
    YemenDirectoryTheme(activeTheme = settings.activeTheme) {
        val txtColor = when (settings.fontColor) {
            "LIGHT_GOLD" -> LightGoldColor
            "VIBRANT_SILVER" -> VibrantSilverColor
            else -> BrightWhiteColor
        }

        val scope = rememberCoroutineScope()
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "دليل اليمن 🗺️",
                                fontWeight = FontWeight.Bold,
                                color = txtColor,
                                fontSize = 16.sp
                            )
                            if (viewModel.loggedInUser.isNotBlank()) {
                                Text(
                                    text = "بصلاحيات المشرف: ${viewModel.loggedInUser}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(Color.Black, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // --- THE FIVE NAVIGATION ICONS ROW ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Refresh / Update Action Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeScreen == "REFRESH") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        Toast.makeText(context, "🔄 جاري تحديث السجلات وفحص التحديثات اللحظية...", Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            viewModel.triggerReseed()
                                            delay(500)
                                            Toast.makeText(context, "✅ جميع البيانات محدثة ولحظية!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔄", fontSize = 20.sp)
                            }

                            // 2. Globe / Home Browser Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeScreen == "HOME") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        activeScreen = "HOME"
                                        selectedProviderForDetail = null
                                        activeChatWithProvider = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🌐", fontSize = 20.sp)
                            }

                            // 3. Register Profile Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeScreen == "REGISTER") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        activeScreen = "REGISTER"
                                        selectedProviderForDetail = null
                                        activeChatWithProvider = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 20.sp)
                            }

                            // 4. Admin Key Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeScreen == "ADMIN") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        activeScreen = "ADMIN"
                                        selectedProviderForDetail = null
                                        activeChatWithProvider = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔐", fontSize = 20.sp)
                            }

                            // 5. My Contacts Screen Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeScreen == "MY_CONTACTS") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        activeScreen = "MY_CONTACTS"
                                        selectedProviderForDetail = null
                                        activeChatWithProvider = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏠", fontSize = 20.sp)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 0.5.dp)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Far Left Button: Clipboard icon
                            IconButton(
                                onClick = {
                                    activeScreen = "MY_CONTACTS"
                                    selectedProviderForDetail = null
                                    activeChatWithProvider = null
                                }
                            ) {
                                Text("📋", fontSize = 24.sp)
                            }

                            // Center Content: WAM custom Arabic footers
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "MAW 777644670",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Far Right Button: Info icon
                            IconButton(
                                onClick = {
                                    activeScreen = "ABOUT"
                                    selectedProviderForDetail = null
                                    activeChatWithProvider = null
                                }
                            ) {
                                Text("ℹ️", fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // --- SCREEN ROUTING MANAGER ---
                when {
                    activeChatWithProvider != null -> {
                        LiveChatScreen(
                            viewModel = viewModel,
                            receiver = activeChatWithProvider!!,
                            onBack = { activeChatWithProvider = null }
                        )
                    }

                    selectedProviderForDetail != null -> {
                        ProviderDetailScreen(
                            p = selectedProviderForDetail!!,
                            viewModel = viewModel,
                            settings = settings,
                            categoriesList = categoriesList,
                            onBack = { selectedProviderForDetail = null },
                            onOpenChatWithProvider = { activeChatWithProvider = it }
                        )
                    }

                    else -> {
                        when (activeScreen) {
                            "HOME" -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    settings = settings,
                                    categoriesList = categoriesList,
                                    providersList = providersList,
                                    allBannersList = bannersList,
                                    onSelectProvider = { selectedProviderForDetail = it }
                                )
                            }

                            "REGISTER" -> {
                                RegisterScreen(viewModel, settings, categoriesList)
                            }

                            "ABOUT" -> {
                                TabAboutInformationScreen(settings, context)
                            }

                            "MY_CONTACTS" -> {
                                MyContactsScreen(
                                    onBackToHome = { activeScreen = "HOME" }
                                )
                            }

                            "ADMIN" -> {
                                if (viewModel.loggedInUser.isNotBlank()) {
                                    AdminDashboardScreen(
                                        viewModel = viewModel,
                                        settings = settings,
                                        categoriesList = categoriesList,
                                        providersList = providersList,
                                        bannersList = bannersList,
                                        moderatorsList = moderatorsList,
                                        complaintsList = complaintsList
                                    )
                                } else {
                                    // Render pristine Admin credential forms
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(18.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text("🛡️ تسجيل دخول الإدارة والمدراء والمالك", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Char(34).toString().let { Color.White })
                                                OutlinedTextField(
                                                    value = adminUserField,
                                                    onValueChange = { adminUserField = it },
                                                    label = { Text("اسم المستخدم المشرف") },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = adminPassField,
                                                    onValueChange = { adminPassField = it },
                                                    label = { Text("كلمة مرور الدخول") },
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Button(
                                                    onClick = {
                                                        val ok = viewModel.login(adminUserField, adminPassField)
                                                        if (ok) {
                                                            adminUserField = ""
                                                            adminPassField = ""
                                                        } else {
                                                            Toast.makeText(context, "❌ اسم المستخدم أو رمز المرور غير معتمد بمقاطعة اليمن!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("تسجيل الدخول الآمن")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- FLOATING AI ASSISTANT, CHAT & ABOUT TRIGGERS (DYNAMIC ORDERING & CONFIG) ---
                val orderList = remember(settings.iconOrder) {
                    settings.iconOrder.split(",").map { it.trim() }.filter { it.isNotBlank() }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    orderList.forEach { type ->
                        when (type) {
                            "AI" -> {
                                if (!settings.isAiIconHidden) {
                                    val aiSize = iconSizeAnimation(settings.aiIconSize)
                                    Box(
                                        modifier = Modifier
                                            .size(aiSize.dp)
                                            .applyEffect(settings.customAiIconEffect)
                                            .clip(CircleShape)
                                            .background(CharcoalGoldPrimary)
                                            .clickable { showAiChatbox = !showAiChatbox },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ProviderImage(
                                            imgStr = settings.customAiIconBase64.ifBlank { "🤖" },
                                            modifier = Modifier.fillMaxSize(),
                                            textStyle = TextStyle(fontSize = (aiSize / 2).sp)
                                        )
                                    }
                                }
                            }
                            "CHAT" -> {
                                if (!settings.isChatIconHidden) {
                                    val chatSize = iconSizeAnimation(settings.chatIconSize)
                                    Box(
                                        modifier = Modifier
                                            .size(chatSize.dp)
                                            .applyEffect(settings.customChatIconEffect)
                                            .clip(CircleShape)
                                            .background(Color(0xFF25D366))
                                            .clickable {
                                                activeScreen = "HOME"
                                                selectedProviderForDetail = null
                                                activeChatWithProvider = null
                                                Toast.makeText(context, "استخدم البحث لإيجاد المهني والدردشة بروح يمنية طيبة!", Toast.LENGTH_SHORT).show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ProviderImage(
                                            imgStr = settings.customChatIconBase64.ifBlank { "💬" },
                                            modifier = Modifier.fillMaxSize(),
                                            textStyle = TextStyle(fontSize = (chatSize / 2).sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- CHATTER SIMULATED AI POPUP LAYER ---
                if (showAiChatbox) {
                    AlertDialog(
                        onDismissRequest = { showAiChatbox = false },
                        title = { Text("🤖 مساعد دليل اليمن الذكي") },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(180.dp)
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(6.dp)
                                ) {
                                    val state = rememberScrollState()
                                    Column(modifier = Modifier.verticalScroll(state)) {
                                        for (log in aiLogList) {
                                            Text(
                                                text = (if (log.second) "👤 أنت: " else "🤖 الذكاء: ") + log.first,
                                                fontSize = 11.sp,
                                                color = if (log.second) Color.LightGray else CharcoalGoldAccent,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = aiQueryInput,
                                    onValueChange = { aiQueryInput = it },
                                    placeholder = { Text("اسألني: من هم أفضل السباكين بصنعاء؟") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (aiQueryInput.isNotBlank()) {
                                    val userQuery = aiQueryInput.trim()
                                    aiLogList.add(userQuery to true)
                                    aiQueryInput = ""
                                    
                                    // Generate highly robust simulated directory replies
                                    val reply = generateAiSimulatedResp(userQuery, providersList, categoriesList)
                                    aiLogList.add(reply to false)
                                }
                            }) {
                                Text("إرسال")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAiChatbox = false }) {
                                Text("إغلاق")
                            }
                        }
                    )
                }

                // --- POPUP NOTIFICATION CENTER ALERT ---
                if (viewModel.adminInstantNotification.isNotBlank()) {
                    LaunchedEffect(viewModel.adminInstantNotification) {
                        delay(4000)
                        viewModel.adminInstantNotification = ""
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = viewModel.adminInstantNotification,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// --- Dynamic rotation or glow visual effects mapper ---
@Composable
fun Modifier.applyEffect(effect: String): Modifier {
    if (effect == "ROTATE") {
        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        return this.graphicsLayer { rotationZ = rotation }
    }
    if (effect == "GLOW") {
        val infiniteTransition = rememberInfiniteTransition()
        val scalePulse by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        return this.scale(scalePulse)
    }
    return this
}

// Helper icons mapping size calculation
@Composable
fun iconSizeAnimation(pctSize: Float): Float {
    // defaults baseline to 60dp, maps percentage scale
    return (60f * (pctSize / 100f)).coerceAtLeast(30f).coerceAtMost(100f)
}

// Simulated intelligent search agent
fun generateAiSimulatedResp(query: String, providers: List<ServiceProvider>, categories: List<Category>): String {
    val q = query.lowercase()
    if (q.contains("سباك") || q.contains("plumb")) {
        val plumbs = providers.filter { it.mainCategoryId == 2 && !it.isBlocked }
        if (plumbs.isNotEmpty()) {
            return "وجدت لك عدد ${plumbs.size} سباكين معتمدين باليمن! منهم المهندس ${plumbs.first().name} المتواجد بـ ${plumbs.first().neighborhood} للتواصل: ${plumbs.first().phoneNumber}."
        }
    }
    if (q.contains("أفضل") || q.contains("مميز") || q.contains("rate") || q.contains("best")) {
        val top = providers.filter { !it.isBlocked }.maxByOrNull { it.rating }
        if (top != null) {
            return "المهني الأعلى تقييماً بالمنصة حالياً هو ${top.name} بتقييم ${top.rating} نجوم! يعمل بمجال تخصص مميز ويمكنك الاتصال به مباشرة."
        }
    }
    return "باقي التخصصات المتاحة هي: الكهرباء، الصيانة، والخياطة. ابحث عن منطقتك بصنعاء أو ادخل الملف للاتصال بالشركاء مباشرة!"
}

// --- ABOUT & INFO VIEW SCREEN --- (Cleans up copyright footer representation cleanly)
@Composable
fun TabAboutInformationScreen(settings: AppSettings, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (settings.isAboutImageReplacesContent && settings.aboutImageBase64.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                ProviderImage(
                    imgStr = settings.aboutImageBase64,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            if (settings.aboutImageBase64.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    ProviderImage(
                        imgStr = settings.aboutImageBase64,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            if (!settings.isAboutContentTextDeleted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = settings.aboutTitle.ifBlank { "دليل اليمن لربط المهنيين ومزودي الخدمات" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalGoldPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = settings.aboutSubtitle.ifBlank { "الاصدار المستمر الآمن: V1.0.0" },
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("💡 تفاصيل ومعلومات التطبيق:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val detailsStr = settings.aboutDetails.ifBlank {
                            "• يتيح لك التطبيق تصفح كافة مقدمي الخدمات المهنية باليمن والاطلاع على أرقام هواتفهم ومواقع عملهم بشكل مجاني تماماً.\n• يمكنك التقييم وترك البلاغات لمساعدة المشرفين على تحسين وضمان جودة الخدمات بالبلاد.\n• للاستفسار أو الدعم الفني، تواصل مع فريق الإشراف أو المالك الرئيسي عبر الحساب المعتمد."
                        }
                        detailsStr.split("\n").forEach { line ->
                            if (line.isNotBlank()) {
                                Text(line, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyContactsScreen(onBackToHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "طلبـات الخـدمة السـابقة والتـواصل:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "تعرض هذه الصفحة المهنيين ومزودي الخدمات الزراعية الذين قمت بالتواصل معهم مسبقاً لمتابعة الحالة.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📬", fontSize = 48.sp)
                Text(
                    text = "لم تتواصل مع أي مهني حتى الآن.",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("عودة للرئيسية 🏠", fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}
