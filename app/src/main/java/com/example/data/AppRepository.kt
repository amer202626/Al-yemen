package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class AppRepository(private val context: Context) {

    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "yemen_directory.db"
    ).fallbackToDestructiveMigration().build()

    // Expose flows from DAOs
    val categories: Flow<List<Category>> = db.categoryDao().getAllCategoriesFlow()
    val activeProviders: Flow<List<ServiceProvider>> = db.serviceProviderDao().getActiveProvidersFlow()
    val pendingProviders: Flow<List<ServiceProvider>> = db.serviceProviderDao().getPendingProvidersFlow()
    val allProviders: Flow<List<ServiceProvider>> = db.serviceProviderDao().getAllProvidersFlow()
    val banners: Flow<List<Banner>> = db.bannerDao().getAllBannersFlow()
    val complaints: Flow<List<Complaint>> = db.complaintDao().getAllComplaintsFlow()
    val chatMessages: Flow<List<ChatMessage>> = db.chatMessageDao().getAllMessagesFlow()
    val settings: Flow<AppSettings?> = db.appSettingsDao().getSettingsFlow()
    val moderators: Flow<List<Moderator>> = db.moderatorDao().getAllModeratorsFlow()

    // Seeding default values
    suspend fun initializeDatabaseIfNeeded() {
        val currentSettings = db.appSettingsDao().getSettingsDirect()
        if (currentSettings == null) {
            // No settings. Create initial settings
            db.appSettingsDao().insertSettings(AppSettings())
            db.moderatorDao().insertModerator(Moderator(username = "WAM2026", passwordHex = "maher736462", permissions = "ALL"))
            
            // Seed Categories
            val defaultCats = listOf(
                Category(nameAr = "الكهرباء والشبكات", nameEn = "Electricians & Networks", sortOrder = 1, imageBase64 = "⚡"),
                Category(nameAr = "السباكة والصرف", nameEn = "Plumbing Services", sortOrder = 2, imageBase64 = "🔧"),
                Category(nameAr = "صيانة الأجهزة والهواتف", nameEn = "Phone & Laptop Maintenance", sortOrder = 3, imageBase64 = "📱"),
                Category(nameAr = "خياطة وتطريز", nameEn = "Sewing & Fashion Designing", sortOrder = 4, imageBase64 = "🧵"),
                Category(nameAr = "صيانة السيارات وميكانيك", nameEn = "Car Maintenance", sortOrder = 5, imageBase64 = "🚗"),
                Category(nameAr = "الأسر المنتجة والأطعمة", nameEn = "Productive Families & Catering", sortOrder = 6, imageBase64 = "🍲")
            )
            for (cat in defaultCats) {
                db.categoryDao().insertCategory(cat)
            }

            // Seed some approved providers to look ready-made and professional!
            val p1 = ServiceProvider(
                name = "ماهر محمد طاهر",
                phone = "777644670",
                mainCategoryId = 1,
                address = "صنعاء - جولة الرويشان",
                neighborhood = "صنعاء القديمة",
                isApproved = true,
                isVerified = true,
                isPinned = true,
                isRecommended = true,
                averageRating = 5.0f,
                ratingCount = 12,
                profileImageBase64 = "👨‍🔧",
                hasMonthlySubscription = true
            )
            val p2 = ServiceProvider(
                name = "أم أحمد للمأكولات اليمنية",
                phone = "771234567",
                mainCategoryId = 6,
                address = "عدن - كريتر",
                neighborhood = "حي القطيع",
                isApproved = true,
                isVerified = true,
                isPinned = false,
                isRecommended = true,
                averageRating = 4.8f,
                ratingCount = 9,
                profileImageBase64 = "🍲",
                hasMonthlySubscription = false
            )
            val p3 = ServiceProvider(
                name = "عبدالله يحيى مقبل",
                phone = "733445566",
                mainCategoryId = 3,
                address = "تعز - شارع جمال",
                neighborhood = "حي المسبح",
                isApproved = true,
                isVerified = false,
                isPinned = false,
                isRecommended = false,
                averageRating = 4.2f,
                ratingCount = 3,
                profileImageBase64 = "📱",
                hasMonthlySubscription = true
            )
            db.serviceProviderDao().insertProvider(p1)
            db.serviceProviderDao().insertProvider(p2)
            db.serviceProviderDao().insertProvider(p3)

            // Seed banners
            val banner1 = Banner(
                type = "TEXT",
                content = "🎉 مرحباً بكم في دليل اليمن للخدمات المنزلية وتفعيل الأسر المنتجة الفوري!",
                durationSeconds = 6,
                targetUrl = "https://example.com/welcome",
                bannerSize = "MEDIUM"
            )
            val banner2 = Banner(
                type = "TEXT",
                content = "📢 إعلان ممول: احصل على خصم 20% على خدمات الصيانة والتركيب مع المهندس ماهر اليماني!",
                durationSeconds = 7,
                targetUrl = "https://example.com/maher",
                bannerSize = "LARGE"
            )
            db.bannerDao().insertBanner(banner1)
            db.bannerDao().insertBanner(banner2)
        }
    }

    // --- Moderator Management ---
    suspend fun addModerator(mod: Moderator) = db.moderatorDao().insertModerator(mod)
    suspend fun updateModerator(mod: Moderator) = db.moderatorDao().updateModerator(mod)
    suspend fun deleteModerator(mod: Moderator) = db.moderatorDao().deleteModerator(mod)

    // --- Category Management ---
    suspend fun addCategory(cat: Category) = db.categoryDao().insertCategory(cat)
    suspend fun updateCategory(cat: Category) = db.categoryDao().updateCategory(cat)
    suspend fun deleteCategory(cat: Category) = db.categoryDao().deleteCategory(cat)

    // --- Service Provider Management ---
    suspend fun getProviderById(id: Int) = db.serviceProviderDao().getProviderById(id)
    suspend fun insertProvider(provider: ServiceProvider) = db.serviceProviderDao().insertProvider(provider)
    suspend fun updateProvider(provider: ServiceProvider) = db.serviceProviderDao().updateProvider(provider)
    suspend fun deleteProvider(provider: ServiceProvider) = db.serviceProviderDao().deleteProvider(provider)

    // --- Banner Management ---
    suspend fun addBanner(banner: Banner) = db.bannerDao().insertBanner(banner)
    suspend fun updateBanner(banner: Banner) = db.bannerDao().updateBanner(banner)
    suspend fun deleteBanner(banner: Banner) = db.bannerDao().deleteBanner(banner)

    // --- Complaint Management ---
    suspend fun addComplaint(complaint: Complaint) = db.complaintDao().insertComplaint(complaint)
    suspend fun deleteComplaint(complaint: Complaint) = db.complaintDao().deleteComplaint(complaint)

    // --- Chat Management ---
    suspend fun sendMessage(msg: ChatMessage) = db.chatMessageDao().insertMessage(msg)
    suspend fun clearChat() = db.chatMessageDao().clearAllMessages()

    // --- Settings Management ---
    suspend fun getAppSettingsDirect(): AppSettings {
        return db.appSettingsDao().getSettingsDirect() ?: AppSettings()
    }
    suspend fun updateSettings(settings: AppSettings) {
        db.appSettingsDao().insertSettings(settings)
    }

    // Backup & Restore Simulation (JSON/String representation for portable recovery)
    suspend fun exportDatabaseToJson(): String {
        val s = getAppSettingsDirect()
        val cats = db.categoryDao().getAllCategoriesFlow().firstOrNull() ?: emptyList()
        val provs = db.serviceProviderDao().getAllProvidersFlow().firstOrNull() ?: emptyList()
        val complaintsList = db.complaintDao().getAllComplaintsFlow().firstOrNull() ?: emptyList()
        val bannersList = db.bannerDao().getAllBannersFlow().firstOrNull() ?: emptyList()

        // Construct a simple, clear text format to serve as a secure portable backup string
        val sb = StringBuilder()
        sb.append("YEMEN_BACKUP_V1\n")
        sb.append("=== SETTINGS ===\n")
        sb.append("appName=${s.appName};themeChoice=${s.themeChoice};adFooterText=${s.adFooterText};supportPhone=${s.supportPhone};supportEmail=${s.supportEmail};supportWhatsapp=${s.supportWhatsapp};isMaintenanceMode=${s.isMaintenanceMode}\n")
        sb.append("=== CATEGORIES ===\n")
        for (c in cats) {
            sb.append("${c.id},${c.nameAr},${c.nameEn},${c.imageBase64},${c.sortOrder},${c.parentId ?: "null"}\n")
        }
        sb.append("=== PROVIDERS ===\n")
        for (p in provs) {
            sb.append("${p.id}|${p.name}|${p.phone}|${p.mainCategoryId}|${p.address}|${p.neighborhood}|${p.isApproved}|${p.isRejected}|${p.isPinned}|${p.isRecommended}|${p.isVerified}|${p.averageRating}|${p.hasMonthlySubscription}\n")
        }
        sb.append("=== END ===")
        return sb.toString()
    }

    suspend fun importDatabaseFromJson(backupStr: String): Boolean {
        try {
            if (!backupStr.startsWith("YEMEN_BACKUP_V1")) return false
            val lines = backupStr.lines()
            var section = ""
            for (line in lines) {
                if (line.isBlank()) continue
                if (line.startsWith("===")) {
                    section = line.trim()
                    continue
                }
                when (section) {
                    "=== SETTINGS ===" -> {
                        val parts = line.split(";")
                        val map = parts.associate {
                            val kv = it.split("=")
                            if (kv.size == 2) kv[0] to kv[1] else "" to ""
                        }
                        val current = getAppSettingsDirect()
                        val updated = current.copy(
                            appName = map["appName"] ?: current.appName,
                            themeChoice = map["themeChoice"] ?: current.themeChoice,
                            adFooterText = map["adFooterText"] ?: current.adFooterText,
                            supportPhone = map["supportPhone"] ?: current.supportPhone,
                            supportEmail = map["supportEmail"] ?: current.supportEmail,
                            supportWhatsapp = map["supportWhatsapp"] ?: current.supportWhatsapp,
                            isMaintenanceMode = (map["isMaintenanceMode"] ?: "false").toBoolean()
                        )
                        updateSettings(updated)
                    }
                    "=== CATEGORIES ===" -> {
                        val parts = line.split(",")
                        if (parts.size >= 5) {
                            val id = parts[0].toIntOrNull() ?: 0
                            val nameAr = parts[1]
                            val nameEn = parts[2]
                            val img = parts[3]
                            val sort = parts[4].toIntOrNull() ?: 0
                            val parent = if (parts.size > 5 && parts[5] != "null") parts[5].toIntOrNull() else null
                            
                            val cat = Category(id = id, nameAr = nameAr, nameEn = nameEn, imageBase64 = img, sortOrder = sort, parentId = parent)
                            db.categoryDao().insertCategory(cat)
                        }
                    }
                    "=== PROVIDERS ===" -> {
                        val parts = line.split("|")
                        if (parts.size >= 12) {
                            val id = parts[0].toIntOrNull() ?: 0
                            val name = parts[1]
                            val phone = parts[2]
                            val catId = parts[3].toIntOrNull() ?: 1
                            val addr = parts[4]
                            val neigh = parts[5]
                            val isAppr = parts[6].toBoolean()
                            val isRej = parts[7].toBoolean()
                            val isPin = parts[8].toBoolean()
                            val isRec = parts[9].toBoolean()
                            val isVer = parts[10].toBoolean()
                            val rating = parts[11].toFloatOrNull() ?: 0f
                            val sub = if (parts.size > 12) parts[12].toBoolean() else false

                            val p = ServiceProvider(
                                id = id, name = name, phone = phone, mainCategoryId = catId,
                                address = addr, neighborhood = neigh, isApproved = isAppr, isRejected = isRej,
                                isPinned = isPin, isRecommended = isRec, isVerified = isVer, averageRating = rating,
                                hasMonthlySubscription = sub
                            )
                            db.serviceProviderDao().insertProvider(p)
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
