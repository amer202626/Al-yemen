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

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application.applicationContext)

    // User session
    var loggedInUser by mutableStateOf("") // "ADMIN", "OWNER" or moderator username
    var adminInstantNotification by mutableStateOf("")

    // Active screen navigation/tab
    var activeTabOfApp by mutableStateOf("HOME") // HOME, CHAT, ABOUT, ADMIN

    // Search & Filters Flow
    private val _searchQuery = MutableStateFlow("")
    var searchQuery: String
        get() = _searchQuery.value
        set(value) { _searchQuery.value = value }

    var filterCategoryId by mutableStateOf<Int?>(null)
    var filterRegion by mutableStateOf<String?>(null)
    var searchRadiusKm by mutableStateOf(50f)

    // Flows from Repository
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providers: StateFlow<List<ServiceProvider>> = repository.providers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val banners: StateFlow<List<Banner>> = repository.banners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBanners: StateFlow<List<Banner>> = repository.activeBanners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moderators: StateFlow<List<Moderator>> = repository.moderators
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val complaints: StateFlow<List<Complaint>> = repository.complaints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsState: StateFlow<AppSettings> = repository.settingsFlow
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Simulated local states
    var appCallsCount by mutableStateOf(0)
    var activeUsersCount by mutableStateOf(34) // Starting baseline of active Yemen user nodes
    var backupStringState by mutableStateOf("")

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
            // Restore session
            val sp = getApplication<Application>().getSharedPreferences("yemen_dir_prefs", Context.MODE_PRIVATE)
            loggedInUser = sp.getString("logged_user", "") ?: ""
            appCallsCount = sp.getInt("calls_count", 18)
            activeUsersCount = sp.getInt("active_users", 42)
        }
    }

    // --- Authentication & Session Workflows ---

    fun login(user: String, pass: String): Boolean {
        if (user.isBlank() || pass.isBlank()) return false
        val formattedUser = user.trim()
        val formattedPass = pass.trim()

        if (formattedUser.equals("owner", ignoreCase = true) && formattedPass == "123456") {
            loggedInUser = "OWNER"
            saveSession()
            triggerAdminNotification("🔓 تم تسجيل دخول المالك الرئيسي!")
            return true
        }

        val foundMod = moderators.value.find { it.username.equals(formattedUser, ignoreCase = true) && it.passwordHex == formattedPass }
        if (foundMod != null) {
            loggedInUser = foundMod.username
            saveSession()
            triggerAdminNotification("🛡️ تم تسجيل الدخول للمشرف: $user بنجاح!")
            return true
        }
        return false
    }

    fun logout() {
        loggedInUser = ""
        saveSession()
    }

    private fun saveSession() {
        val sp = getApplication<Application>().getSharedPreferences("yemen_dir_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("logged_user", loggedInUser)
            .putInt("calls_count", appCallsCount)
            .putInt("active_users", activeUsersCount)
            .apply()
    }

    fun incrementCallCounter() {
        appCallsCount++
        saveSession()
    }

    fun triggerAdminNotification(msg: String) {
        adminInstantNotification = msg
    }

    // --- Permissions Guards ---

    fun canCurrentAdminEditCategories(): Boolean {
        if (loggedInUser.equals("OWNER", ignoreCase = true)) return true
        val currentMod = moderators.value.find { it.username.equals(loggedInUser, ignoreCase = true) }
        return currentMod?.canEditCategories ?: false
    }

    fun canCurrentAdminDeleteProviders(): Boolean {
        if (loggedInUser.equals("OWNER", ignoreCase = true)) return true
        val currentMod = moderators.value.find { it.username.equals(loggedInUser, ignoreCase = true) }
        return currentMod?.canDeleteProviders ?: false
    }

    // --- Settings and Theme State Controllers ---

    fun saveAppSettingsDirect(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            triggerAdminNotification("⚙️ تم تحديث ومزامنة شكل وإعدادات التطبيق!")
        }
    }

    // --- Database Backup System ---

    fun backupDatabaseState() {
        viewModelScope.launch {
            backupStringState = repository.exportToCsvString()
            triggerAdminNotification("💾 تم إنشاء نسخة احتياطية من قاعدة البيانات بنجاح!")
        }
    }

    suspend fun restoreDatabase(csv: String): Boolean {
        val success = repository.restoreFromCsvString(csv)
        if (success) {
            triggerAdminNotification("🔄 تم استعادة البيانات من النسخة بنجاح!")
        }
        return success
    }

    // --- Category Administration ---

    fun addCategoryDirectFlow(cat: Category) {
        viewModelScope.launch {
            repository.addCategory(cat)
            triggerAdminNotification("📂 تم إضافة قسم جديد: ${cat.nameAr}")
        }
    }

    fun updateCategoryDirectFlow(cat: Category) {
        viewModelScope.launch {
            repository.updateCategory(cat)
            triggerAdminNotification("✏️ تم تعديل القسم: ${cat.nameAr} بنجاح!")
        }
    }

    fun deleteCategory(cat: Category) {
        viewModelScope.launch {
            repository.deleteCategory(cat)
            triggerAdminNotification("🗑️ تم حذف القسم: ${cat.nameAr}")
        }
    }

    // --- Provider Actions ---

    fun registerProvider(p: ServiceProvider) {
        viewModelScope.launch {
            repository.addProvider(p)
            triggerAdminNotification("🚨 طلب انضمام جديد قيد المراجعة: ${p.name}")
        }
    }

    fun updateProvider(p: ServiceProvider) {
        viewModelScope.launch {
            repository.updateProvider(p)
            triggerAdminNotification("👤 تم تحديث بيانات مقدم الخدمة: ${p.name}")
        }
    }

    fun deleteProvider(p: ServiceProvider) {
        viewModelScope.launch {
            repository.deleteProvider(p)
            triggerAdminNotification("🗑️ تم إزالة مقدم الخدمة: ${p.name}")
        }
    }

    // --- Moderator Lifecycle ---

    fun addModerator(username: String, passwordHex: String, permissions: String, canEditCategories: Boolean, canDeleteProviders: Boolean) {
        viewModelScope.launch {
            val mod = Moderator(
                username = username,
                passwordHex = passwordHex,
                permissions = permissions,
                canEditCategories = canEditCategories,
                canDeleteProviders = canDeleteProviders
            )
            repository.addModerator(mod)
            triggerAdminNotification("🛡️ تم إضافة المشرف الجديد: $username")
        }
    }

    fun updateModerator(mod: Moderator) {
        viewModelScope.launch {
            repository.updateModerator(mod)
            triggerAdminNotification("✏️ تم تحديث بيانات المشرف: ${mod.username}")
        }
    }

    fun deleteModerator(mod: Moderator) {
        viewModelScope.launch {
            repository.deleteModerator(mod)
            triggerAdminNotification("🗑️ تم إزالة حساب المشرف: ${mod.username}")
        }
    }

    // --- Campaign Banners ---

    fun addBanner(title: String, content: String, duration: Int, redirectUrl: String, size: String, type: String) {
        viewModelScope.launch {
            val banner = Banner(
                title = title,
                content = content,
                durationSeconds = duration,
                redirectUrl = redirectUrl,
                size = size,
                type = type,
                isActive = true
            )
            repository.addBanner(banner)
            triggerAdminNotification("📢 تم رفع إعلان ترويجي جديد بنجاح!")
        }
    }

    fun deleteBanner(banner: Banner) {
        viewModelScope.launch {
            repository.deleteBanner(banner)
            triggerAdminNotification("🗑️ تم إزالة إعلان اللافتة المحدد")
        }
    }

    // --- Complaints Handling ---

    fun addComplaint(userPhone: String, providerId: Int, providerName: String, details: String) {
        viewModelScope.launch {
            val c = Complaint(
                userPhone = userPhone,
                providerId = providerId,
                providerName = providerName,
                details = details
            )
            repository.addComplaint(c)
            triggerAdminNotification("⚠️ بلاغ جديد من $userPhone ضد $providerName")
        }
    }

    fun deleteComplaint(c: Complaint) {
        viewModelScope.launch {
            repository.deleteComplaint(c)
            triggerAdminNotification("🗑️ تم إزالة البلاغ")
        }
    }

    // --- Live Chat system & Auto Pruning ---

    fun sendChatMessage(senderId: String, receiverId: String, message: String) {
        viewModelScope.launch {
            val msg = ChatMessage(
                senderId = senderId,
                receiverId = receiverId,
                message = message,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(msg)
        }
    }

    fun clearAllChatRecords() {
        viewModelScope.launch {
            repository.clearAllChatLogs()
            triggerAdminNotification("🧼 تم تصفية وحذف سجلات المحادثات نهائياً لخصوصية الأعضاء!")
        }
    }

    fun pruneOldChatLogs(daysOld: Int) {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - (daysOld.toLong() * 24 * 60 * 60 * 1000)
            repository.scheduleAutomaticChatPrune(cutoff)
            triggerAdminNotification("🧼 تم جدولة مسح المحادثات القديمة بنجاح!")
        }
    }
}
