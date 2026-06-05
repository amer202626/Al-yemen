package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "yemen_directory_database"
        ).fallbackToDestructiveMigration().build()
    }

    // Streams
    val categories: Flow<List<Category>> = db.categoryDao().getAllCategories()
    val providers: Flow<List<ServiceProvider>> = db.serviceProviderDao().getAllProviders()
    val banners: Flow<List<Banner>> = db.bannerDao().getAllBanners()
    val activeBanners: Flow<List<Banner>> = db.bannerDao().getActiveBanners()
    val moderators: Flow<List<Moderator>> = db.moderatorDao().getAllModerators()
    val complaints: Flow<List<Complaint>> = db.complaintDao().getAllComplaints()
    val chatMessages: Flow<List<ChatMessage>> = db.chatMessageDao().getAllMessages()
    val settingsFlow: Flow<AppSettings?> = db.appSettingsDao().getSettingsFlow()

    // Seeds
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        // App Settings Seeding
        val existingSettings = db.appSettingsDao().getSettingsDirect()
        if (existingSettings == null) {
            db.appSettingsDao().insertSettings(AppSettings())
        }

        // Moderators Seeding
        val existingMods = db.moderatorDao().getAllModerators().firstOrNull()
        if (existingMods.isNullOrEmpty()) {
            db.moderatorDao().insertModerator(
                Moderator(
                    username = "WAM2026",
                    passwordHex = "maher736462",
                    permissions = "ALL",
                    canEditCategories = true,
                    canDeleteProviders = true
                )
            )
            db.moderatorDao().insertModerator(
                Moderator(
                    username = "admin",
                    passwordHex = "admin",
                    permissions = "ALL",
                    canEditCategories = true,
                    canDeleteProviders = false
                )
            )
        }

        // Categories Seeding
        val existingCats = db.categoryDao().getAllCategories().firstOrNull()
        if (existingCats.isNullOrEmpty()) {
            val cats = listOf(
                Category(nameAr = "الكهرباء والشبكات", nameEn = "Electricians & Networks", sortOrder = 1, imageBase64 = "⚡"),
                Category(nameAr = "السباكة والصرف", nameEn = "Plumbing Services", sortOrder = 2, imageBase64 = "🔧"),
                Category(nameAr = "صيانة الأجهزة والهواتف", nameEn = "Phone & Laptop Maintenance", sortOrder = 3, imageBase64 = "📱"),
                Category(nameAr = "خياطة وتطريز", nameEn = "Sewing & Fashion Designing", sortOrder = 4, imageBase64 = "🧵"),
                Category(nameAr = "صيانة السيارات وميكانيك", nameEn = "Car Maintenance", sortOrder = 5, imageBase64 = "🚗"),
                Category(nameAr = "الأسر المنتجة والأطعمة", nameEn = "Productive Families & Catering", sortOrder = 6, imageBase64 = "🍲")
            )
            for (c in cats) {
                db.categoryDao().insertCategory(c)
            }
        }

        // Providers Seeding
        val existingProviders = db.serviceProviderDao().getAllProviders().firstOrNull()
        if (existingProviders.isNullOrEmpty()) {
            // Seed a few dummy providers
            val dummy1 = ServiceProvider(
                name = "ماهر محمد",
                profileImageBase64 = "👨‍🔧",
                phoneNumber = "777123456",
                neighborhood = "حدة، صنعاء",
                workAddress = "شارع حارتنا بجوار مستشفى اليمن للعيون",
                mainCategoryId = 1,
                isPending = false,
                isPremium = true,
                rating = 4.8f,
                ratingCount = 12
            )
            val dummy2 = ServiceProvider(
                name = "أم هاني للطبخ اليمني والخبز",
                profileImageBase64 = "🍲",
                phoneNumber = "733445566",
                neighborhood = "شيراتون، صنعاء",
                workAddress = "حي السقيا، صنعاء القديمة",
                mainCategoryId = 6,
                isPending = false,
                rating = 4.9f,
                ratingCount = 8
            )
            db.serviceProviderDao().insertProvider(dummy1)
            db.serviceProviderDao().insertProvider(dummy2)
        }
    }

    // Settings
    suspend fun getSettingsDirect(): AppSettings = withContext(Dispatchers.IO) {
        db.appSettingsDao().getSettingsDirect() ?: AppSettings()
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        db.appSettingsDao().insertSettings(settings)
    }

    // Categories
    suspend fun addCategory(cat: Category) = withContext(Dispatchers.IO) {
        db.categoryDao().insertCategory(cat)
    }

    suspend fun updateCategory(cat: Category) = withContext(Dispatchers.IO) {
        db.categoryDao().updateCategory(cat)
    }

    suspend fun deleteCategory(cat: Category) = withContext(Dispatchers.IO) {
        db.categoryDao().deleteCategory(cat)
    }

    // Providers
    suspend fun addProvider(p: ServiceProvider) = withContext(Dispatchers.IO) {
        db.serviceProviderDao().insertProvider(p)
    }

    suspend fun updateProvider(p: ServiceProvider) = withContext(Dispatchers.IO) {
        db.serviceProviderDao().insertProvider(p) // Room's insert handles REPLACE on conflict
    }

    suspend fun deleteProvider(p: ServiceProvider) = withContext(Dispatchers.IO) {
        db.serviceProviderDao().deleteProvider(p)
    }

    // Banners
    suspend fun addBanner(b: Banner) = withContext(Dispatchers.IO) {
        db.bannerDao().insertBanner(b)
    }

    suspend fun updateBanner(b: Banner) = withContext(Dispatchers.IO) {
        db.bannerDao().updateBanner(b)
    }

    suspend fun deleteBanner(b: Banner) = withContext(Dispatchers.IO) {
        db.bannerDao().deleteBanner(b)
    }

    // Moderators
    suspend fun addModerator(m: Moderator) = withContext(Dispatchers.IO) {
        db.moderatorDao().insertModerator(m)
    }

    suspend fun updateModerator(m: Moderator) = withContext(Dispatchers.IO) {
        db.moderatorDao().updateModerator(m)
    }

    suspend fun deleteModerator(m: Moderator) = withContext(Dispatchers.IO) {
        db.moderatorDao().deleteModerator(m)
    }

    // Complaints
    suspend fun addComplaint(c: Complaint) = withContext(Dispatchers.IO) {
        db.complaintDao().insertComplaint(c)
    }

    suspend fun deleteComplaint(c: Complaint) = withContext(Dispatchers.IO) {
        db.complaintDao().deleteComplaint(c)
    }

    // Chats
    suspend fun insertChatMessage(msg: ChatMessage) = withContext(Dispatchers.IO) {
        db.chatMessageDao().insertMessage(msg)
    }

    suspend fun clearAllChatLogs() = withContext(Dispatchers.IO) {
        db.chatMessageDao().deleteAllMessages()
    }

    suspend fun scheduleAutomaticChatPrune(cutoffMs: Long) = withContext(Dispatchers.IO) {
        db.chatMessageDao().deleteOldMessages(cutoffMs)
    }

    // CSV Database Backup Engine
    suspend fun exportToCsvString(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("=== CATEGORIES ===\n")
        val cats = db.categoryDao().getAllCategories().firstOrNull() ?: emptyList()
        for (c in cats) {
            sb.append("${c.id},${c.nameAr},${c.nameEn},${c.imageBase64},${c.sortOrder},${c.parentId ?: "null"}\n")
        }

        sb.append("=== PROVIDERS ===\n")
        val provs = db.serviceProviderDao().getAllProviders().firstOrNull() ?: emptyList()
        for (p in provs) {
            sb.append("${p.id},${p.name},${p.phoneNumber},${p.neighborhood},${p.workAddress},${p.mainCategoryId},${p.subCategoryId ?: "null"},${p.isPending},${p.isBlocked},${p.isPremium},${p.rating},${p.ratingCount},${p.profileImageBase64.take(50)}\n")
        }

        sb.append("=== MODERATORS ===\n")
        val mods = db.moderatorDao().getAllModerators().firstOrNull() ?: emptyList()
        for (m in mods) {
            sb.append("${m.id},${m.username},${m.passwordHex},${m.permissions},${m.canEditCategories},${m.canDeleteProviders}\n")
        }

        sb.toString()
    }

    // Restore Database from string
    suspend fun restoreFromCsvString(csv: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val lines = csv.lineSequence().iterator()
            var section = ""
            while (lines.hasNext()) {
                val origLine = lines.next().trim()
                if (origLine.isEmpty()) continue
                if (origLine.startsWith("===")) {
                    section = origLine
                    continue
                }

                val parts = origLine.split(",")
                when (section) {
                    "=== CATEGORIES ===" -> {
                        if (parts.size >= 5) {
                            val id = parts[0].toIntOrNull() ?: continue
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
                        if (parts.size >= 8) {
                            val id = parts[0].toIntOrNull() ?: continue
                            val name = parts[1]
                            val phone = parts[2]
                            val neighborhood = parts[3]
                            val address = parts[4]
                            val mCat = parts[5].toIntOrNull() ?: 0
                            val sCat = if (parts[6] != "null") parts[6].toIntOrNull() else null
                            val isP = parts[7].toBoolean()
                            val isB = if (parts.size > 8) parts[8].toBoolean() else false
                            val isPr = if (parts.size > 9) parts[9].toBoolean() else false
                            val rat = if (parts.size > 10) parts[10].toFloatOrNull() ?: 5f else 5f
                            val ratCnt = if (parts.size > 11) parts[11].toIntOrNull() ?: 1 else 1

                            val existingAndLoaded = db.serviceProviderDao().getAllProviders().firstOrNull()?.find { it.id == id }
                            val pImg = existingAndLoaded?.profileImageBase64 ?: "👨‍🔧"

                            val prov = ServiceProvider(
                                id = id, name = name, phoneNumber = phone, neighborhood = neighborhood,
                                workAddress = address, mainCategoryId = mCat, subCategoryId = sCat,
                                isPending = isP, isBlocked = isB, isPremium = isPr, rating = rat, ratingCount = ratCnt,
                                profileImageBase64 = pImg
                            )
                            db.serviceProviderDao().insertProvider(prov)
                        }
                    }
                    "=== MODERATORS ===" -> {
                        if (parts.size >= 4) {
                            val id = parts[0].toIntOrNull() ?: continue
                            val user = parts[1]
                            val pass = parts[2]
                            val perm = parts[3]
                            val canEditCats = if (parts.size > 4) parts[4].toBoolean() else true
                            val canDelProvs = if (parts.size > 5) parts[5].toBoolean() else true

                            val mod = Moderator(
                                id = id, username = user, passwordHex = pass, permissions = perm,
                                canEditCategories = canEditCats, canDeleteProviders = canDelProvs
                            )
                            db.moderatorDao().insertModerator(mod)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
