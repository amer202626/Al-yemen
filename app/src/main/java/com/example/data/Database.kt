package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameAr: String = "",
    val nameEn: String = "",
    val imageBase64: String = "", // Holds base64 or icon name
    val sortOrder: Int = 0,
    val parentId: Int? = null,
    val isPinned: Boolean = false
)

@Entity(tableName = "service_providers")
data class ServiceProvider(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val profileImageBase64: String = "",
    val idCardImageBase64: String = "",
    val phoneNumber: String = "",
    val neighborhood: String = "",
    val workAddress: String = "",
    val mainCategoryId: Int = 0,
    val subCategoryId: Int? = null,
    val isPending: Boolean = true,
    val isBlocked: Boolean = false,
    val isPremium: Boolean = false,
    val rating: Float = 5.0f,
    val ratingCount: Int = 1,
    val latitude: Double = 15.35 // Sana'a latitude as default
) {
    // Add non-entity fields via custom getters/setter
    val longitude: Double get() = 44.20 // Sana'a longitude
}

@Entity(tableName = "banners")
data class Banner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val content: String = "", // Base64 content or YouTube/Image/Video
    val durationSeconds: Int = 5,
    val redirectUrl: String = "",
    val size: String = "MEDIUM", // "SMALL", "MEDIUM", "LARGE"
    val type: String = "IMAGE", // "IMAGE", "VIDEO"
    val isActive: Boolean = true
)

@Entity(tableName = "moderators")
data class Moderator(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHex: String,
    val permissions: String = "ALL", // "ALL", "READ_ONLY"
    val canEditCategories: Boolean = true,
    val canDeleteProviders: Boolean = true
)

@Entity(tableName = "complaints")
data class Complaint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userPhone: String = "",
    val providerId: Int = 0,
    val providerName: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String = "", // "USER" or "PROVIDER_X"
    val receiverId: String = "", // "PROVIDER_X" or "USER"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val appIconSize: Float = 100f,
    val aiIconSize: Float = 50f, // reduced by 50% as requested
    val chatIconSize: Float = 70f, // reduced by 30% as requested
    val aiIconPositionX: Float = 0f,
    val aiIconPositionY: Float = 0f,
    val chatIconPositionX: Float = 0f,
    val chatIconPositionY: Float = 0f,
    val isAiIconHidden: Boolean = false,
    val isChatIconHidden: Boolean = false,
    val footerSize: Float = 60f,
    val footerBackgroundImageBase64: String = "",
    val welcomeImageBase64: String = "",
    val activeTheme: String = "COSMIC_SLATE", // "COSMIC_SLATE", "CHARCOAL_GOLD", "ROYAL_EMERALD"
    val fontColor: String = "BRIGHT_WHITE", // "BRIGHT_WHITE", "LIGHT_GOLD", "VIBRANT_SILVER"
    val isChatEnabledGlobal: Boolean = true,
    val chatDisabledMessage: String = "عذراً، تم تعطيل خدمة المحادثة الفورية مؤقتاً لتحديث النظام.",
    val disabledChatProviderIds: String = "", // comma-separated like "1,2"
    val isMapEnabled: Boolean = true,
    val customAiIconBase64: String = "",
    val customChatIconBase64: String = "",
    val customAiIconEffect: String = "NONE", // "NONE", "GLOW", "SHADOW", "BLUR", "ROTATE"
    val customChatIconEffect: String = "NONE",
    val latestVersionUrl: String = "https://example.com/yemendir.apk",
    val latestVersionCode: Int = 2,
    
    // New parameters for flexible floating icons ordering, sizing, deleting
    val customAboutIconBase64: String = "",
    val isAboutIconHidden: Boolean = false,
    val aboutIconSize: Float = 60f,
    val customAboutIconEffect: String = "NONE",
    val iconOrder: String = "AI,CHAT,ABOUT", // Order and inclusion configuration

    // About Page customizable fields
    val aboutTitle: String = "دليل اليمن لربط المهنيين ومزودي الخدمات",
    val aboutSubtitle: String = "الاصدار المستمر الآمن: V1.0.0",
    val aboutDetails: String = "• يتيح لك التطبيق تصفح كافة مقدمي الخدمات المهنية باليمن والاطلاع على أرقام هواتفهم ومواقع عملهم بشكل مجاني تماماً.\n• يمكنك التقييم وترك البلاغات لمساعدة المشرفين على تحسين وضمان جودة الخدمات بالبلاد.\n• للاستفسار أو الدعم الفني، تواصل مع فريق الإشراف أو المالك الرئيسي عبر الحساب المعتمد.",
    val aboutImageBase64: String = "",
    val isAboutContentTextDeleted: Boolean = false,
    val isAboutImageReplacesContent: Boolean = false
)

// --- DAO Definitions ---

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isPinned DESC, sortOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface ServiceProviderDao {
    @Query("SELECT * FROM service_providers")
    fun getAllProviders(): Flow<List<ServiceProvider>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ServiceProvider)

    @Update
    suspend fun updateProvider(provider: ServiceProvider)

    @Delete
    suspend fun deleteProvider(provider: ServiceProvider)
}

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners WHERE isActive = 1")
    fun getActiveBanners(): Flow<List<Banner>>

    @Query("SELECT * FROM banners")
    fun getAllBanners(): Flow<List<Banner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: Banner)

    @Update
    suspend fun updateBanner(banner: Banner)

    @Delete
    suspend fun deleteBanner(banner: Banner)
}

@Dao
interface ModeratorDao {
    @Query("SELECT * FROM moderators")
    fun getAllModerators(): Flow<List<Moderator>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModerator(moderator: Moderator)

    @Update
    suspend fun updateModerator(moderator: Moderator)

    @Delete
    suspend fun deleteModerator(moderator: Moderator)
}

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY timestamp DESC")
    fun getAllComplaints(): Flow<List<Complaint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: Complaint)

    @Delete
    suspend fun deleteComplaint(complaint: Complaint)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoffTime")
    suspend fun deleteOldMessages(cutoffTime: Long)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)
}

@Database(
    entities = [
        Category::class,
        ServiceProvider::class,
        Banner::class,
        Moderator::class,
        Complaint::class,
        ChatMessage::class,
        AppSettings::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun serviceProviderDao(): ServiceProviderDao
    abstract fun bannerDao(): BannerDao
    abstract fun moderatorDao(): ModeratorDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun appSettingsDao(): AppSettingsDao
}
