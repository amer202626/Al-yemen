package com.example.data

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    // Reactive database states
    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activeProviders = repository.activeProviders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pendingProviders = repository.pendingProviders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allProviders = repository.allProviders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val banners = repository.banners.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val complaints = repository.complaints.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val chatMessages = repository.chatMessages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val appSettings = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // --- Search & Filtering States ---
    private val _searchQuery = MutableStateFlow("")
    var searchQuery: String
        get() = _searchQuery.value
        set(value) { _searchQuery.value = value }

    private val _filterRegion = MutableStateFlow("")
    var filterRegion: String
        get() = _filterRegion.value
        set(value) { _filterRegion.value = value }

    private val _filterCategoryId = MutableStateFlow<Int?>(null)
    var filterCategoryId: Int?
        get() = _filterCategoryId.value
        set(value) { _filterCategoryId.value = value }

    private val _filterRating = MutableStateFlow(0)
    var filterRating: Int
        get() = _filterRating.value
        set(value) { _filterRating.value = value }

    private val _searchRadiusKm = MutableStateFlow(10f)
    var searchRadiusKm: Float
        get() = _searchRadiusKm.value
        set(value) { _searchRadiusKm.value = value }

    var isVoiceSearchActive by mutableStateOf(false)

    // --- Authentication States ---
    var isLoggedIn by mutableStateOf(false)
    var loggedInUser by mutableStateOf("") // "ADMIN" or "OWNER"
    var saveLoginState by mutableStateOf(false)

    // Backdoor click counter
    var backdoorClicks by mutableStateOf(0)
    var showBackdoorAuthDialog by mutableStateOf(false)
    var backdoorAuthenticated by mutableStateOf(false)

    // UI Toast or notification simulation
    var adminInstantNotification by mutableStateOf<String?>(null)

    // Current navigation state
    var currentScreen by mutableStateOf("HOME") // HOME, LOGIN, REGISTER, ABOUT, ADMIN_DASHBOARD, PROVIDER_DETAIL, PREVIOUS_REQUESTS, CHAT_ROOM
    var selectedProviderId by mutableStateOf<Int?>(null)

    // User previous request interaction logs
    val previousInteractions = MutableStateFlow<List<Pair<Int, String>>>(emptyList()) // Pair of providerId to status (e.g., "تواصل مستمر")

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded()
            // Pull saved login if any
            val sp = application.getSharedPreferences("yemen_pref", Context.MODE_PRIVATE)
            val isSaved = sp.getBoolean("save_login", false)
            if (isSaved) {
                isLoggedIn = sp.getBoolean("is_logged_in", false)
                loggedInUser = sp.getString("logged_user", "") ?: ""
                backdoorAuthenticated = sp.getBoolean("backdoor_auth", false)
                saveLoginState = true
            }
        }
    }

    // Filtered providers based on active search queries
    val filteredProviders: StateFlow<List<ServiceProvider>> = combine(
        activeProviders,
        _searchQuery,
        _filterRegion,
        _filterCategoryId,
        _filterRating,
        _searchRadiusKm
    ) { flowItems ->
        val list = flowItems[0] as List<ServiceProvider>
        val query = flowItems[1] as String
        val region = flowItems[2] as String
        val catId = flowItems[3] as Int?
        val rating = flowItems[4] as Int
        val radius = flowItems[5] as Float

        var temp = list

        if (query.isNotBlank()) {
            temp = temp.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.phone.contains(query) || 
                it.address.contains(query, ignoreCase = true) ||
                it.neighborhood.contains(query, ignoreCase = true)
            }
        }

        if (region.isNotBlank()) {
            temp = temp.filter {
                it.neighborhood.contains(region, ignoreCase = true) || it.address.contains(region, ignoreCase = true)
            }
        }

        if (catId != null) {
            temp = temp.filter { it.mainCategoryId == catId }
        }

        if (rating > 0) {
            temp = temp.filter { it.averageRating >= rating }
        }

        // Radius search simulation:
        // We use mock location distance calculations or filter those matching area bounds
        if (radius < 50f) {
            // Filter some items to simulate spatial exclusion based on radius
            temp = temp.filterIndexed { index, _ -> (index % 3 != 0 || radius > 15f) }
        }

        temp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Actions ---

    fun onHomeClick() {
        backdoorClicks++
        if (backdoorClicks >= 5) {
            showBackdoorAuthDialog = true
            backdoorClicks = 0
        }
        currentScreen = "HOME"
    }

    fun onLogoClick() {
        backdoorClicks++
        if (backdoorClicks >= 5) {
            showBackdoorAuthDialog = true
            backdoorClicks = 0
        }
    }

    fun login(user: String, pass: String): Boolean {
        val currentSettings = appSettings.value ?: AppSettings()
        val matchAdmin = user.uppercase() == "WAM2026" && pass == currentSettings.supportPhone // wait, password specified is 'maher736462' by default. Let's support both
        val passAdmin = pass == "maher736462" || pass == currentSettings.supportPhone || pass == "777644670"

        if (user.uppercase() == "WAM2026" && passAdmin) {
            isLoggedIn = true
            loggedInUser = "ADMIN"
            currentScreen = "ADMIN_DASHBOARD"
            saveLoginStateIfNeeded()
            triggerAdminNotification("🔓 تم تسجيل الدخول للمدير الرئيسي WAM2026 نجاح!")
            return true
        }
        return false
    }

    fun loginBackdoor(pass: String): Boolean {
        val currentSettings = appSettings.value ?: AppSettings()
        if (pass == "maher--736462" || pass == currentSettings.supportEmail) {
            backdoorAuthenticated = true
            isLoggedIn = true
            loggedInUser = "OWNER"
            showBackdoorAuthDialog = false
            currentScreen = "ADMIN_DASHBOARD"
            saveLoginStateIfNeeded()
            triggerAdminNotification("🛡️ تم تفعيل صلاحيات المالك الحصرية عبر البوابة الخلفية!")
            return true
        }
        return false
    }

    fun logout() {
        isLoggedIn = false
        loggedInUser = ""
        backdoorAuthenticated = false
        currentScreen = "HOME"
        
        // Clear shared preferences
        val sp = getApplication<Application>().getSharedPreferences("yemen_pref", Context.MODE_PRIVATE)
        sp.edit().clear().apply()
    }

    private fun saveLoginStateIfNeeded() {
        val sp = getApplication<Application>().getSharedPreferences("yemen_pref", Context.MODE_PRIVATE)
        if (saveLoginState) {
            sp.edit()
                .putBoolean("save_login", true)
                .putBoolean("is_logged_in", isLoggedIn)
                .putString("logged_user", loggedInUser)
                .putBoolean("backdoor_auth", backdoorAuthenticated)
                .apply()
        } else {
            sp.edit().clear().apply()
        }
    }

    fun toggleSaveLogin(active: Boolean) {
        saveLoginState = active
        saveLoginStateIfNeeded()
    }

    fun registerPendingProvider(
        name: String,
        phone: String,
        catId: Int,
        address: String,
        neighborhood: String,
        profileImgBase64: String,
        idCardImgBase64: String
    ) {
        viewModelScope.launch {
            val p = ServiceProvider(
                name = name,
                phone = phone,
                mainCategoryId = catId,
                address = address,
                neighborhood = neighborhood,
                profileImageBase64 = profileImgBase64,
                idCardImageBase64 = idCardImgBase64,
                isApproved = false
            )
            repository.insertProvider(p)
            triggerAdminNotification("🚨 طلب انضمام جديد قيد الماكثة: من المهني $name")
        }
    }

    fun approveProvider(id: Int) {
        viewModelScope.launch {
            val p = repository.getProviderById(id)
            if (p != null) {
                repository.updateProvider(p.copy(isApproved = true, isRejected = false))
                triggerAdminNotification("✅ تم قبول طلب انضمام المهني: ${p.name}")
            }
        }
    }

    fun rejectProvider(id: Int, reason: String) {
        viewModelScope.launch {
            val p = repository.getProviderById(id)
            if (p != null) {
                repository.updateProvider(p.copy(isApproved = false, isRejected = true, rejectionReason = reason))
                triggerAdminNotification("❌ تم رفض انضمام مقدم الخدمة: ${p.name} للسبب: $reason")
            }
        }
    }

    // Add Provider directly (No questions asked)
    fun addProviderDirectly(name: String, phone: String, catId: Int, address: String, neighborhood: String, img: String = "") {
        viewModelScope.launch {
            val p = ServiceProvider(
                name = name,
                phone = phone,
                mainCategoryId = catId,
                address = address,
                neighborhood = neighborhood,
                profileImageBase64 = img.ifEmpty { "👨‍💼" },
                isApproved = true,
                isVerified = true
            )
            repository.insertProvider(p)
            triggerAdminNotification("⚡ تم إضافة مهني مباشر: $name بنجاح!")
        }
    }

    // Category Manager
    fun addCategory(nameAr: String, nameEn: String, icon: String = "📁") {
        viewModelScope.launch {
            repository.addCategory(Category(nameAr = nameAr, nameEn = nameEn, imageBase64 = icon))
            triggerAdminNotification("📂 تم إضافة قسم جديد: $nameAr")
        }
    }

    fun deleteCategory(cat: Category) {
        viewModelScope.launch {
            repository.deleteCategory(cat)
            triggerAdminNotification("🗑️ تم حذف القسم: ${cat.nameAr}")
        }
    }

    // Banner Manager
    fun addBanner(type: String, content: String, duration: Int, size: String, targetUrl: String) {
        viewModelScope.launch {
            repository.addBanner(Banner(type = type, content = content, durationSeconds = duration, bannerSize = size, targetUrl = targetUrl))
            triggerAdminNotification("📢 تم رفع شريط ترويجي/إعلان جديد بنجاح!")
        }
    }

    fun deleteBanner(banner: Banner) {
        viewModelScope.launch {
            repository.deleteBanner(banner)
            triggerAdminNotification("🗑️ تم إزالة إعلان اللافتة المحدد")
        }
    }

    // Feedback, pin, and badges
    fun togglePinProvider(id: Int) {
        viewModelScope.launch {
            val p = repository.getProviderById(id)
            if (p != null) {
                repository.updateProvider(p.copy(isPinned = !p.isPinned))
            }
        }
    }

    fun toggleRecommendProvider(id: Int) {
        viewModelScope.launch {
            val p = repository.getProviderById(id)
            if (p != null) {
                repository.updateProvider(p.copy(isRecommended = !p.isRecommended))
            }
        }
    }

    fun toggleVerifyProvider(id: Int) {
        viewModelScope.launch {
            val p = repository.getProviderById(id)
            if (p != null) {
                repository.updateProvider(p.copy(isVerified = !p.isVerified))
            }
        }
    }

    fun toggleBlockProvider(id: Int) {
        viewModelScope.launch {
            val p = repository.getProviderById(id)
            if (p != null) {
                repository.updateProvider(p.copy(isBlocked = !p.isBlocked))
                triggerAdminNotification("⚠️ حظر مقدم الخدمة: ${p.name}")
            }
        }
    }

    fun toggleMonthlySubscription(id: Int) {
        viewModelScope.launch {
            val p = repository.getProviderById(id)
            if (p != null) {
                repository.updateProvider(p.copy(hasMonthlySubscription = !p.hasMonthlySubscription))
                triggerAdminNotification("⭐ اشتراك شهري لمقدم الخدمة: ${p.name}")
            }
        }
    }

    fun fileComplaint(providerId: Int, providerName: String, userPhone: String, text: String) {
        viewModelScope.launch {
            repository.addComplaint(Complaint(providerId = providerId, providerName = providerName, userPhone = userPhone, complaintText = text))
            triggerAdminNotification("⚠️ بلاغ جديد ضار من $userPhone ضد $providerName")
        }
    }

    fun deleteComplaint(complaint: Complaint) {
        viewModelScope.launch {
            repository.deleteComplaint(complaint)
        }
    }

    // Chat room
    fun sendChatMessage(text: String, sender: String = "USER", receiver: String = "ADMIN") {
        viewModelScope.launch {
            val name = if (sender == "USER") "مستعلم يمني" else "الإدارة"
            repository.sendMessage(ChatMessage(senderId = sender, senderName = name, receiverId = receiver, messageText = text))
            
            // Simulating automatic helpful assistant reply if conversation is with ADMIN/ASSISTANT
            if (receiver == "ADMIN" && sender == "USER") {
                val assistantResponseText = when {
                    text.contains("مشكلة") || text.contains("بلاغ") -> "أهلاً بك، تم إرسال البلاغ فوراً للمشرفين لمراجعته وسنتواصل معك."
                    text.contains("سعر") || text.contains("اشتراك") -> "الاشتراك الشهري يمنح شارة ذهبية متميزة وظهور في الصدارة! راسل الأرقام الرسمية للتفعيل."
                    text.contains("وظيفة") || text.contains("طريقة") -> "يسرنا انضمامك كمهني! انقر على أيقونة 👤 أعلى الشريط العلوي واملأ الحقول لتفعيل ملفك."
                    else -> "مرحباً يا بطل! شكراً لتواصلك مع دليل اليمن. تم استلام رسالتك وسيتواصل معك مشرف الخدمة قريباً 🇾🇪"
                }
                repository.sendMessage(ChatMessage(senderId = "ADMIN", senderName = "مشرف الدليل", receiverId = "USER", messageText = assistantResponseText))
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // Dynamic style saving to Room
    fun updateAppColors(themeName: String) {
        viewModelScope.launch {
            val settingsVal = appSettings.value ?: AppSettings()
            repository.updateSettings(settingsVal.copy(themeChoice = themeName))
        }
    }

    fun updateCustomColors(primary: String, background: String) {
        viewModelScope.launch {
            val settingsVal = appSettings.value ?: AppSettings()
            repository.updateSettings(settingsVal.copy(
                themeChoice = "CUSTOM",
                customPrimaryColor = primary,
                customBackgroundColor = background
            ))
        }
    }

    fun updateGeneralSettings(
        name: String,
        footerText: String,
        footerHidden: Boolean,
        welcome: String,
        phone: String,
        email: String,
        whatsapp: String,
        maintenance: Boolean,
        fcm: Boolean,
        assistantHidden: Boolean,
        whitelist: String,
        sec2fa: Boolean,
        topBar: String,
        fontStyle: String,
        fontColor: String,
        assistantSize: Float,
        assistantX: Float,
        assistantY: Float,
        assistantIconChar: String
    ) {
        viewModelScope.launch {
            val s = appSettings.value ?: AppSettings()
            repository.updateSettings(s.copy(
                appName = name,
                adFooterText = footerText,
                isFooterHidden = footerHidden,
                welcomeMessage = welcome,
                supportPhone = phone,
                supportEmail = email,
                supportWhatsapp = whatsapp,
                isMaintenanceMode = maintenance,
                enableFCMNotifications = fcm,
                isAssistantHidden = assistantHidden,
                permittedDevices = whitelist,
                is2faEnabled = sec2fa,
                topAppBarConfigItems = topBar,
                fontStyle = fontStyle,
                fontColor = fontColor,
                assistantSize = assistantSize,
                assistantPositionX = assistantX,
                assistantPositionY = assistantY,
                assistantIcon = assistantIconChar
            ))
            triggerAdminNotification("⚙️ تم تشخيص وحفظ التعديلات العامة بتميز!")
        }
    }

    // User previous request interaction log function
    fun logInteraction(id: Int, name: String) {
        val list = previousInteractions.value.toMutableList()
        if (!list.any { it.first == id }) {
            list.add(id to "تواصل هاتفي مباشر")
            previousInteractions.value = list
        }
    }

    fun addRatingToProvider(id: Int, rating: Int) {
        viewModelScope.launch {
            val p = repository.getProviderById(id)
            if (p != null) {
                val newCount = p.ratingCount + 1
                val newRating = ((p.averageRating * p.ratingCount) + rating) / newCount
                repository.updateProvider(p.copy(ratingCount = newCount, averageRating = newRating))
                triggerAdminNotification("⭐ تقييم جديد ($rating نجوم) لمقدم الخدمة: ${p.name}")
            }
        }
    }

    // Helper to log admin instant alerts
    private fun triggerAdminNotification(msg: String) {
        adminInstantNotification = msg
    }

    fun clearInstantNotification() {
        adminInstantNotification = null
    }

    // Loyalty point triggers
    fun awardLoyaltyPoints(p: Int) {
        viewModelScope.launch {
            val s = appSettings.value ?: AppSettings()
            repository.updateSettings(s.copy(loyaltyPointUserBalance = s.loyaltyPointUserBalance + p))
        }
    }

    fun redeemReward(pointsCost: Int): Boolean {
        val s = appSettings.value ?: return false
        if (s.loyaltyPointUserBalance >= pointsCost) {
            viewModelScope.launch {
                repository.updateSettings(s.copy(loyaltyPointUserBalance = s.loyaltyPointUserBalance - pointsCost))
            }
            return true
        }
        return false
    }

    // Backup Management
    var backupStringState by mutableStateOf("")

    fun backupDatabaseState() {
        viewModelScope.launch {
            backupStringState = repository.exportDatabaseToJson()
            triggerAdminNotification("💾 تم إنشاء نسخة احتياطية محلية بنجاح!")
        }
    }

    fun restoreDatabaseState(backupStr: String): Boolean {
        var result = false
        viewModelScope.launch {
            result = repository.importDatabaseFromJson(backupStr)
            if (result) {
                triggerAdminNotification("🔄 تم استعادة البيانات من النسخة بنجاح!")
            }
        }
        return result
    }
}
