package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameAr: String,
    val nameEn: String,
    val imageBase64: String = "", // Holds base64 or icon name
    val sortOrder: Int = 0,
    val parentId: Int? = null // For subcategories
)

@Entity(tableName = "service_providers")
data class ServiceProvider(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // Triple Name
    val phone: String, // Phone / WhatsApp
    val mainCategoryId: Int, // The category it belongs to
    val address: String, // Workplace address
    val neighborhood: String, // Residential area
    val latitude: Double? = null,
    val longitude: Double? = null,
    val profileImageBase64: String = "", // Captured profile picture
    val idCardImageBase64: String = "", // Optional Identity card picture
    val isApproved: Boolean = false, // True = Approved, False = Pending reviewer
    val isRejected: Boolean = false,
    val rejectionReason: String? = null,
    val isPinned: Boolean = false, // Appears first in category
    val isRecommended: Boolean = false, // Appears in "Recommended" section
    val isVerified: Boolean = false, // Blue Checkmark badge
    val isBlocked: Boolean = false, // Banned from interaction
    val averageRating: Float = 0f,
    val ratingCount: Int = 0,
    val hasMonthlySubscription: Boolean = false, // Star/Premium tag
    val loyaltyPoints: Int = 0
)

@Entity(tableName = "banners")
data class Banner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "IMAGE", "VIDEO", "TEXT"
    val content: String, // Base64 content or Text description / YouTube Link
    val durationSeconds: Int = 5,
    val targetUrl: String = "",
    val bannerSize: String = "MEDIUM" // "SMALL", "MEDIUM", "LARGE"
)

@Entity(tableName = "complaints")
data class Complaint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int,
    val providerName: String,
    val userPhone: String,
    val complaintText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String, // "USER" or "ADMIN" or "PROVIDER_12"
    val senderName: String,
    val receiverId: String, // "ADMIN" or "USER"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "moderators")
data class Moderator(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHex: String,
    val permissions: String = "ALL" // "ALL", "READ_ONLY"
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val appName: String = "دليل اليمن",
    val themeChoice: String = "COSMIC_SILVER", // "COSMIC_SILVER", "LUXURY_GOLD", "ELEGANT_EMERALD", "CUSTOM"
    val customPrimaryColor: String = "#80939C",
    val customBackgroundColor: String = "#121A1E",
    val customSecondaryColor: String = "#0288D1",
    val customFontFamily: String = "DEFAULT", // "DEFAULT", "MONOSPACE", "SERIF", "SANS_SERIF"
    val adFooterText: String = "MAW 777644670",
    val isFooterHidden: Boolean = false,
    val welcomeMessage: String = "مرحباً بكم في دليل اليمن لربط المهنيين والأسر المنتجة!",
    val supportPhone: String = "777644670",
    val supportEmail: String = "support@wam.ye",
    val supportWhatsapp: String = "777644670",
    val appLogo: String = "DEFAULT", // Custom base64 or status
    val fontColor: String = "#FFFFFF", // Default text color in fields
    val fontStyle: String = "BOLD", // BOLD, NORMAL, MONOSPACE
    val isAssistantHidden: Boolean = false,
    val assistantSize: Float = 56f,
    val assistantPositionX: Float = 0.8f, // Fractional right/left position
    val assistantPositionY: Float = 0.82f, // Fractional top/bottom position
    val assistantIcon: String = "🤖", // Custom assistant symbol
    val aboutIcon: String = "ℹ️", // Custom icon
    val enableFCMNotifications: Boolean = true,
    val isMaintenanceMode: Boolean = false,
    val is2faEnabled: Boolean = false,
    val permittedDevices: String = "Emulator_Device,My_Main_Phone",
    val loyaltyPointUserBalance: Int = 120, // Points earned by customer actions
    
    // Guest browsing & Radius limiters
    val isGuestBrowsingEnabled: Boolean = true,
    val isDataSavingMode: Boolean = false,
    val isRadiusSearchMaxLimited: Int = 100,
    
    // Top app bar icons config (comma separated list of tags in order: e.g. "HOME,LOGIN,REGISTER,LANG,REFRESH")
    val topAppBarConfigItems: String = "HOME,LOGIN,REGISTER,LANG,REFRESH",
    val activeLanguage: String = "AR", // "AR" / "EN"

    // Real-time chat & footer customization
    val chatIconSize: Float = 56f,
    val chatIconColor: String = "#00C853",
    val isChatIconHidden: Boolean = false,
    val isChatIconDeleted: Boolean = false,
    val footerTransparency: Float = 1.0f,
    val footerFontSize: Float = 11f,
    val footerHeightScale: Float = 1.0f,

    // Blocklist, welcome screen controls, suspension & subscriptions
    val blockedProviderIds: String = "",
    val blockedUserIdentifiers: String = "",
    val isAllProvidersSuspended: Boolean = false,
    val welcomeMessageFontSize: Float = 14f,
    val welcomeMessageGravity: String = "CENTER", // "CENTER", "START", "END"
    val welcomeImageBase64: String = "",
    val isWelcomeImageEnabled: Boolean = false,
    val isSubscriptionFeatureEnabled: Boolean = true
)

// --- DAOS ---

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY sortOrder ASC")
    suspend fun getParentCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder ASC")
    suspend fun getSubCategories(parentId: Int): List<Category>
}

@Dao
interface ServiceProviderDao {
    @Query("SELECT * FROM service_providers WHERE isApproved = 1 AND isBlocked = 0 ORDER BY isPinned DESC, hasMonthlySubscription DESC, id DESC")
    fun getActiveProvidersFlow(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE isApproved = 0 AND isRejected = 0 ORDER BY id DESC")
    fun getPendingProvidersFlow(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers ORDER BY id DESC")
    fun getAllProvidersFlow(): Flow<List<ServiceProvider>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ServiceProvider): Long

    @Update
    suspend fun updateProvider(provider: ServiceProvider)

    @Delete
    suspend fun deleteProvider(provider: ServiceProvider)

    @Query("SELECT * FROM service_providers WHERE id = :id")
    suspend fun getProviderById(id: Int): ServiceProvider?
}

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners ORDER BY id DESC")
    fun getAllBannersFlow(): Flow<List<Banner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: Banner)

    @Update
    suspend fun updateBanner(banner: Banner)

    @Delete
    suspend fun deleteBanner(banner: Banner)
}

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY timestamp DESC")
    fun getAllComplaintsFlow(): Flow<List<Complaint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: Complaint)

    @Delete
    suspend fun deleteComplaint(complaint: Complaint)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)
}

@Dao
interface ModeratorDao {
    @Query("SELECT * FROM moderators ORDER BY id DESC")
    fun getAllModeratorsFlow(): Flow<List<Moderator>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModerator(moderator: Moderator)

    @Update
    suspend fun updateModerator(moderator: Moderator)

    @Delete
    suspend fun deleteModerator(moderator: Moderator)
}

// --- DATABASE HOLDER ---

@Database(
    entities = [
        Category::class,
        ServiceProvider::class,
        Banner::class,
        Complaint::class,
        ChatMessage::class,
        AppSettings::class,
        Moderator::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun serviceProviderDao(): ServiceProviderDao
    abstract fun bannerDao(): BannerDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun moderatorDao(): ModeratorDao
}
