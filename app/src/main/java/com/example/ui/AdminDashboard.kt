package com.example.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

// --- Compression for Admin Pictures upload ---
fun compressAndResizeImageAdmin(bytes: ByteArray): String {
    try {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        val maxDimension = 500
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var newWidth = originalWidth
        var newHeight = originalHeight

        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            if (originalWidth > originalHeight) {
                newWidth = maxDimension
                newHeight = (originalHeight * (maxDimension.toFloat() / originalWidth.toFloat())).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (originalWidth * (maxDimension.toFloat() / originalHeight.toFloat())).toInt()
            }
        }

        val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        val outputStream = java.io.ByteArrayOutputStream()
        resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 65, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    }
}

@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    settings: AppSettings,
    categoriesList: List<Category>,
    providersList: List<ServiceProvider>,
    bannersList: List<Banner>,
    moderatorsList: List<Moderator>,
    complaintsList: List<Complaint>
) {
    val context = LocalContext.current
    var selectedDashboardTab by remember { mutableStateOf("STATS") }

    // Text Color
    val txtColor = when (settings.fontColor) {
        "LIGHT_GOLD" -> LightGoldColor
        "VIBRANT_SILVER" -> VibrantSilverColor
        else -> BrightWhiteColor
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // --- STATISTICS METRICS HEADERS AT TOP ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "📊 إحصائيات الأداء ودليل اليمن الحية:",
                    color = txtColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Metric 1
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Active Users", fontSize = 9.sp, color = Color.Gray)
                        Text("${viewModel.activeUsersCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("مستخدم نشط 👥", fontSize = 8.sp, color = Color.LightGray)
                    }
                    Divider(modifier = Modifier.width(1.dp).height(24.dp).align(Alignment.CenterVertically), color = Color.Gray)
                    // Metric 2
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Providers", fontSize = 9.sp, color = Color.Gray)
                        Text("${providersList.filter { !it.isPending }.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text("مهني معتمد 🛠️", fontSize = 8.sp, color = Color.LightGray)
                    }
                    Divider(modifier = Modifier.width(1.dp).height(24.dp).align(Alignment.CenterVertically), color = Color.Gray)
                    // Metric 3
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Calls Count", fontSize = 9.sp, color = Color.Gray)
                        Text("${viewModel.appCallsCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CharcoalGoldPrimary)
                        Text("إتصالات تواصل 📞", fontSize = 8.sp, color = Color.LightGray)
                    }
                    Divider(modifier = Modifier.width(1.dp).height(24.dp).align(Alignment.CenterVertically), color = Color.Gray)
                    // Metric 4
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Pending Req", fontSize = 9.sp, color = Color.Gray)
                        val pendingCount = providersList.filter { it.isPending }.size
                        Text("$pendingCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (pendingCount > 0) MaterialTheme.colorScheme.error else Color.White)
                        Text("طلبات قيد النظر ⏳", fontSize = 8.sp, color = Color.LightGray)
                    }
                }
            }
        }

        // --- DASHBOARD NAVIGATION CHIPS ---
        val tabs = listOf(
            Triple("STATS", "عام 📊", "الاحصائيات والادارة العامة"),
            Triple("PENDING", "مراجعة ⏳", "طلبات الإعتماد"),
            Triple("PROVIDERS", "المهنيين 🛠️", "إحصاء خدمات الدليل"),
            Triple("CATEGORIES", "الأقسام 📂", "إدارة التخصصات والخدمات"),
            Triple("BANNERS", "اللافتات إعلانات 📢", "إعلانات الشريط"),
            Triple("COMPLAINTS", "البلاغات ⚠️", "شكاوى المستخدمين ضد المهنيين"),
            Triple("MODERATORS", "المشرفين 🛡️", "المشرفين والصلاحيات الحية"),
            Triple("THEMES", "أيقونات ومظهر 🎨", "ألوان وحجم الأيقونات والدردشة"),
            Triple("BACKUPS", "خصوصية وبيانات 🧼", "مسح سجلات، تصدير، احتياطي")
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs) { t ->
                FilterChip(
                    selected = selectedDashboardTab == t.first,
                    onClick = { selectedDashboardTab = t.first },
                    label = { Text(t.second) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- ACTIVE DASHBOARD PAGE VIEW CONTENT ---
        Box(modifier = Modifier.weight(1f)) {
            when (selectedDashboardTab) {
                "STATS" -> TabGeneralWelcomeScreen(viewModel)
                "PENDING" -> TabPendingApprovalScreen(viewModel, providersList, categoriesList)
                "PROVIDERS" -> TabManageProvidersScreen(viewModel, providersList, categoriesList)
                "CATEGORIES" -> TabManageCategoriesScreen(viewModel, categoriesList)
                "BANNERS" -> TabManageBannersCampaigns(viewModel, bannersList)
                "COMPLAINTS" -> TabComplaintsMonitor(viewModel, complaintsList)
                "MODERATORS" -> TabModeratorsManagement(viewModel, moderatorsList)
                "THEMES" -> TabThemeIconConfiguration(viewModel, settings, context)
                "BACKUPS" -> TabDatabasePrivacyBackups(viewModel, settings, context)
            }
        }
    }
}

// --- SUB-SCREENS FOR EACH ADMIN TAB CONTROL ---

@Composable
fun TabGeneralWelcomeScreen(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🏠 لوحة تحكم صاحب العمل والمدراء", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    "بصفتك مديراً، يمكنك مراجعة طلبات الانضمام، الموافقة على الطلبات المقدمة، تعديل الرسوم، تخصيص درجات تقييم الأعضاء والتحكم السلس في مظاهر وأنظمة تشغيل تطبيق دليل اليمن.",
                    fontSize = 12.sp, color = Color.Gray
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("👥 تواصل سريع وتجريبي للتقييم:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Button(onClick = { viewModel.activeUsersCount += (3..8).random() }) {
                    Text("محاكاة زيادة عدد المستخدمين النشطين 🚀")
                }
                Button(onClick = { viewModel.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("تسجيل الخروج من لوحة التحكم 🔒")
                }
            }
        }
    }
}

@Composable
fun TabPendingApprovalScreen(viewModel: AppViewModel, list: List<ServiceProvider>, categories: List<Category>) {
    val pendings = list.filter { it.isPending }
    if (pendings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("🎉 لا توجد حالياً طلبات مهنيين جديدة معلقة قيد المراجعة.", color = Color.Gray, fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pendings) { p ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp))) {
                                ProviderImage(p.profileImageBase64, modifier = Modifier.fillMaxSize())
                            }
                            Column {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("📞 الهاتف: ${p.phoneNumber}", fontSize = 12.sp, color = Color.Gray)
                                Text("📍 الموقع: ${p.neighborhood}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        // Display ID card if available
                        if (p.idCardImageBase64.isNotBlank()) {
                            Text(" بطاقة الهوية الوطنية المرفقة للمهني للتوثيق:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CharcoalGoldPrimary)
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp))) {
                                ProviderImage(p.idCardImageBase64, modifier = Modifier.fillMaxSize())
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.updateProvider(p.copy(isPending = false)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("موافقة واعتماد ✔")
                            }

                            Button(
                                onClick = { viewModel.deleteProvider(p) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("رفض وحذف ✖")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabManageProvidersScreen(viewModel: AppViewModel, list: List<ServiceProvider>, categories: List<Category>) {
    val approved = list.filter { !it.isPending }
    val context = LocalContext.current
    var editingProvider by remember { mutableStateOf<ServiceProvider?>(null) }

    // Forms fields for edits
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editNeigh by remember { mutableStateOf("") }
    var editAddr by remember { mutableStateOf("") }

    if (approved.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا يوجد مهندسون أو مقدمو خدمات معتمدون حالياً.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(approved) { p ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp))) {
                            ProviderImage(p.profileImageBase64, modifier = Modifier.fillMaxSize())
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("📞 ${p.phoneNumber} | 📍 ${p.neighborhood}", fontSize = 11.sp, color = Color.Gray)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Edit Modal trigger
                            IconButton(onClick = {
                                editingProvider = p
                                editName = p.name
                                editPhone = p.phoneNumber
                                editNeigh = p.neighborhood
                                editAddr = p.workAddress
                            }) {
                                Text("✏️", fontSize = 16.sp)
                            }

                            // Delete button guarded with privileges validator
                            IconButton(
                                onClick = {
                                    if (viewModel.canCurrentAdminDeleteProviders()) {
                                        viewModel.deleteProvider(p)
                                        Toast.makeText(context, "🗑️ تم إزالة مقدم الخدمة فوراً بنجاح ومزامنة الدليل!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "❌ عذراً! لا تملك صلاحيات لحذف وإلغاء مقدمي الخدمات.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            ) {
                                Text("🗑️", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Popup Edit Provider Dialog Box
    if (editingProvider != null) {
        val activeTarget = editingProvider!!
        AlertDialog(
            onDismissRequest = { editingProvider = null },
            title = { Text("✍️ تعديل ملف مقدم الخدمة الجغرافي") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("اسم مقدم الخدمة") })
                    OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("رقم الهاتف") })
                    OutlinedTextField(value = editNeigh, onValueChange = { editNeigh = it }, label = { Text("الحي / المدينة") })
                    OutlinedTextField(value = editAddr, onValueChange = { editAddr = it }, label = { Text("عنوان العمل") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank() && editPhone.isNotBlank()) {
                        val updated = activeTarget.copy(
                            name = editName,
                            phoneNumber = editPhone,
                            neighborhood = editNeigh,
                            workAddress = editAddr
                        )
                        viewModel.updateProvider(updated)
                        editingProvider = null
                    }
                }) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProvider = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun TabManageCategoriesScreen(viewModel: AppViewModel, list: List<Category>) {
    val context = LocalContext.current
    var catNameAr by remember { mutableStateOf("") }
    var catEmoji by remember { mutableStateOf("⚡") }
    var isSubCategory by remember { mutableStateOf(false) }
    var selectedParentId by remember { mutableStateOf<Int?>(null) }
    var isCatPinned by remember { mutableStateOf(false) }

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editCatNameAr by remember { mutableStateOf("") }
    var editCatEmoji by remember { mutableStateOf("") }
    var editIsSubCategory by remember { mutableStateOf(false) }
    var editSelectedParentId by remember { mutableStateOf<Int?>(null) }
    var editIsCatPinned by remember { mutableStateOf(false) }

    val mainCategories = list.filter { it.parentId == null }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("➕ إضافة قسم أو تخصص مهني جديد:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = catNameAr, onValueChange = { catNameAr = it }, label = { Text("الاسم باللغة العربية") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = catEmoji, onValueChange = { catEmoji = it }, label = { Text("رمز/إيموجي") }, modifier = Modifier.weight(0.4f))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSubCategory, onCheckedChange = { isSubCategory = it })
                Text("هذا قسم فرعي (ليس تخصصاً أساسياً)", fontSize = 11.sp)
            }

            if (isSubCategory) {
                Text("اختر القسم الأساسي الذي تتبع له هذه المهنة الفرعية:", fontSize = 10.sp, color = CharcoalGoldAccent)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(mainCategories) { main ->
                        FilterChip(
                            selected = selectedParentId == main.id,
                            onClick = { selectedParentId = main.id },
                            label = { Text("${main.imageBase64} ${main.nameAr}") }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isCatPinned, onCheckedChange = { isCatPinned = it })
                Text("📌 تثبيت هذا التخصص في المقدمة (يظهر أول الكشف للجمهور)", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    if (viewModel.canCurrentAdminEditCategories()) {
                        if (catNameAr.isNotBlank()) {
                            val newCat = Category(
                                nameAr = catNameAr,
                                nameEn = "",
                                imageBase64 = catEmoji,
                                parentId = if (isSubCategory) selectedParentId else null,
                                isPinned = isCatPinned
                            )
                            viewModel.addCategoryDirectFlow(newCat)
                            catNameAr = ""
                            catEmoji = "⚡"
                            isSubCategory = false
                            selectedParentId = null
                            isCatPinned = false
                        } else {
                            Toast.makeText(context, "الرجاء كتابة اسم التخصص أولاً", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "❌ عذراً! لا تملك صلاحية إضافة أو تعديل الأقسام والخدمات.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ وإضافة التخصص للجمهور ✨")
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text("📂 الأقسام والتصنيفات الحالية للخدمات والمهن باليمن:", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxHeight(0.6f)
    ) {
        items(list) { cat ->
            val isParent = cat.parentId == null
            val parentName = if (!isParent) mainCategories.find { it.id == cat.parentId }?.nameAr else null

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (cat.isPinned) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(cat.imageBase64, fontSize = 20.sp)
                            Text(cat.nameAr, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (cat.isPinned) {
                                Text("📌 مثبت", fontSize = 9.sp, color = CharcoalGoldPrimary)
                            }
                            if (isParent) {
                                Text("🟢 رئيسي", fontSize = 9.sp, color = Color.Green)
                            }
                        }
                        if (!isParent) {
                            Text("↳ قسم فرعي من: ${parentName ?: "تخصص رئيسي"}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (viewModel.canCurrentAdminEditCategories()) {
                                    editingCategory = cat
                                    editCatNameAr = cat.nameAr
                                    editCatEmoji = cat.imageBase64
                                    editIsSubCategory = cat.parentId != null
                                    editSelectedParentId = cat.parentId
                                    editIsCatPinned = cat.isPinned
                                } else {
                                    Toast.makeText(context, "❌ لا تملك صلاحية تعديل الأقسام.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("✏️", fontSize = 16.sp)
                        }
                        IconButton(
                            onClick = {
                                if (viewModel.canCurrentAdminEditCategories()) {
                                    viewModel.deleteCategory(cat)
                                } else {
                                    Toast.makeText(context, "❌ عذراً! لا تملك صلاحية حذف الأقسام.", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Text("🗑️", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue overlay for editing category elements
    if (editingCategory != null) {
        val cat = editingCategory!!
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("✏️ تعديل تخصص: ${cat.nameAr}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = editCatNameAr, onValueChange = { editCatNameAr = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editCatEmoji, onValueChange = { editCatEmoji = it }, label = { Text("أيقونة") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editIsSubCategory, onCheckedChange = { editIsSubCategory = it })
                        Text("هذا قسم فرعي", fontSize = 11.sp)
                    }

                    if (editIsSubCategory) {
                        Text("القسم الأساسي التابع له:", fontSize = 10.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(mainCategories.filter { it.id != cat.id }) { main ->
                                FilterChip(
                                    selected = editSelectedParentId == main.id,
                                    onClick = { editSelectedParentId = main.id },
                                    label = { Text("${main.imageBase64} ${main.nameAr}") }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editIsCatPinned, onCheckedChange = { editIsCatPinned = it })
                        Text("📌 تثبيت هذا التخصص في المقدمة", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = cat.copy(
                            nameAr = editCatNameAr,
                            imageBase64 = editCatEmoji,
                            parentId = if (editIsSubCategory) editSelectedParentId else null,
                            isPinned = editIsCatPinned
                        )
                        viewModel.updateCategoryDirectFlow(updated)
                        editingCategory = null
                    }
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun TabManageBannersCampaigns(viewModel: AppViewModel, list: List<Banner>) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var imageBase64String by remember { mutableStateOf("") }
    var durationSecs by remember { mutableStateOf("5") }
    var sizeStr by remember { mutableStateOf("MEDIUM") } // SMALL, MEDIUM, LARGE
    var typeStr by remember { mutableStateOf("IMAGE") } // IMAGE, VIDEO
    var redirectLink by remember { mutableStateOf("") }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val cmpBase64 = compressAndResizeImageAdmin(bytes)
                    imageBase64String = cmpBase64
                    Toast.makeText(context, "🖼️ تم تحميل وتصغير مظهر الإعلان بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل قراءة الملف", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📢 رفع حملة أو شريط لافتة إعلانية مميزة بالقمة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان الإعلان") }, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { imageLauncher.launch("image/*") }, modifier = Modifier.weight(1.2f)) {
                            Text("تحميل مظهر اللافتة 🖼️")
                        }
                        OutlinedTextField(
                            value = durationSecs,
                            onValueChange = { durationSecs = it },
                            label = { Text("مدة العرض (ثواني)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (imageBase64String.isNotBlank()) {
                        Text("تم اختيار مظهر البانر بنجاح! حجم معقول ومضغوط.", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(value = redirectLink, onValueChange = { redirectLink = it }, label = { Text("رابط التوجيه (اختياري)") }, modifier = Modifier.fillMaxWidth())

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حجم ومساحة البانر:", fontSize = 12.sp)
                        Row {
                            RadioButton(selected = sizeStr == "SMALL", onClick = { sizeStr = "SMALL" })
                            Text("صغير (S)", fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(4.dp))
                            RadioButton(selected = sizeStr == "MEDIUM", onClick = { sizeStr = "MEDIUM" })
                            Text("متوسط (M)", fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(4.dp))
                            RadioButton(selected = sizeStr == "LARGE", onClick = { sizeStr = "LARGE" })
                            Text("كبير (L)", fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("طبيعة المحتوى:", fontSize = 12.sp)
                        Row {
                            RadioButton(selected = typeStr == "IMAGE", onClick = { typeStr = "IMAGE" })
                            Text("صورة", fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(selected = typeStr == "VIDEO", onClick = { typeStr = "VIDEO" })
                            Text("فيديو ترويجي", fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.addBanner(
                                    title = title,
                                    content = if (typeStr == "VIDEO") "" else imageBase64String,
                                    duration = durationSecs.toIntOrNull() ?: 5,
                                    redirectUrl = redirectLink,
                                    size = sizeStr,
                                    type = typeStr
                                )
                                title = ""
                                imageBase64String = ""
                                redirectLink = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مزامنة ونشر اللافتة الإعلانية فوراً 🚀")
                    }
                }
            }
        }

        item {
            Text("🗂️ اللافتات والحملات الإعلانية النشطة حالياً بالفئات:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        items(list) { b ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(b.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("النوع: ${b.type} | المساحة: ${b.size} | الوقت: ${b.durationSeconds} ثواني", fontSize = 11.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { viewModel.deleteBanner(b) }) {
                        Text("🗑️", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun TabComplaintsMonitor(viewModel: AppViewModel, complaints: List<Complaint>) {
    if (complaints.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("✅ لا توجد شكاوى أو بلاغات مسجلة ضد شركاء الدليل.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(complaints) { c ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("👤 الشاكي: ${c.userPhone.ifBlank { "مجهول" }}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            IconButton(onClick = { viewModel.deleteComplaint(c) }) {
                                Text("🗑️ إغلاق البلاغ")
                            }
                        }
                        Text("🛠️ ضد المهني: ${c.providerName} (رقم # ${c.providerId})", fontSize = 12.sp)
                        Text("📜 تفاصيل المخالفة: ${c.details}", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun TabModeratorsManagement(viewModel: AppViewModel, moderators: List<Moderator>) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isNewModCanEditCats by remember { mutableStateOf(true) }
    var isNewModCanDelProvs by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🛡️ إضافة مشرف فرعي وتحديد صلاحياته اللحظية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم المشرف") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور") }, modifier = Modifier.fillMaxWidth())

                    // Real-time precise switches for permissions toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صلاحية تعديل وإضافة الأقسام والخدمات:", fontSize = 12.sp)
                        Switch(checked = isNewModCanEditCats, onCheckedChange = { isNewModCanEditCats = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صلاحية حذف مقدمي الخدمات في الدليل اليمن:", fontSize = 12.sp)
                        Switch(checked = isNewModCanDelProvs, onCheckedChange = { isNewModCanDelProvs = it })
                    }

                    Button(
                        onClick = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                viewModel.addModerator(
                                    username = username.trim(),
                                    passwordHex = password.trim(),
                                    permissions = "CUSTOM",
                                    canEditCategories = isNewModCanEditCats,
                                    canDeleteProviders = isNewModCanDelProvs
                                )
                                username = ""
                                password = ""
                                isNewModCanEditCats = true
                                isNewModCanDelProvs = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إنشاء حساب المشرف ومزامنته 🔐")
                    }
                }
            }
        }

        item {
            Text("🗂️ المشرفون المسجلون حالياً بالمنصة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        items(moderators) { m ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("👤 المشرف: ${m.username}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            var newPassInput by remember { mutableStateOf(m.passwordHex) }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newPassInput,
                                    onValueChange = { newPassInput = it },
                                    label = { Text("تغيير كلمة المرور", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1.3f),
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        if (newPassInput.isNotBlank()) {
                                            viewModel.updateModerator(m.copy(passwordHex = newPassInput.trim()))
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(35.dp)
                                ) {
                                    Text("تحديث 🔐", fontSize = 10.sp)
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                if (m.username == "WAM2026") {
                                    // Protect super admin
                                } else {
                                    viewModel.deleteModerator(m)
                                }
                            },
                            enabled = m.username != "WAM2026"
                        ) {
                            Text("🗑️", color = if (m.username == "WAM2026") Color.Gray else MaterialTheme.colorScheme.error)
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Real-time direct privileges toggle control switches directly inside the list item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تعديل الأقسام:", fontSize = 11.sp)
                        Checkbox(
                            checked = m.canEditCategories,
                            onCheckedChange = {
                                viewModel.updateModerator(m.copy(canEditCategories = it))
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("حذف المهنيين:", fontSize = 11.sp)
                        Checkbox(
                            checked = m.canDeleteProviders,
                            onCheckedChange = {
                                viewModel.updateModerator(m.copy(canDeleteProviders = it))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabThemeIconConfiguration(viewModel: AppViewModel, settings: AppSettings, context: Context) {
    var selectedTheme by remember { mutableStateOf(settings.activeTheme) }
    var selectedFontColor by remember { mutableStateOf(settings.fontColor) }
    
    // Icon configurations
    var customAiIconBase64String by remember { mutableStateOf(settings.customAiIconBase64) }
    var selectedAiEffect by remember { mutableStateOf(settings.customAiIconEffect) }
    var customChatIconBase64String by remember { mutableStateOf(settings.customChatIconBase64) }
    var selectedChatEffect by remember { mutableStateOf(settings.customChatIconEffect) }
    var customAboutIconBase64String by remember { mutableStateOf(settings.customAboutIconBase64) }
    var selectedAboutEffect by remember { mutableStateOf(settings.customAboutIconEffect) }

    // Floating Icon Sizes (as Percentages: 30f to 150f)
    var aiIconSz by remember { mutableStateOf(settings.aiIconSize.toString()) }
    var chatIconSz by remember { mutableStateOf(settings.chatIconSize.toString()) }
    var aboutIconSz by remember { mutableStateOf(settings.aboutIconSize.toString()) }

    // Hiding/Deleting switches
    var isAiIconHidden by remember { mutableStateOf(settings.isAiIconHidden) }
    var isChatIconHidden by remember { mutableStateOf(settings.isChatIconHidden) }
    var isAboutIconHidden by remember { mutableStateOf(settings.isAboutIconHidden) }

    // Floating sequence ordering: e.g. "AI,CHAT,ABOUT"
    var iconOrderStr by remember { mutableStateOf(settings.iconOrder) }

    // About App dynamic contents
    var aboutTitleStr by remember { mutableStateOf(settings.aboutTitle) }
    var aboutSubtitleStr by remember { mutableStateOf(settings.aboutSubtitle) }
    var aboutDetailsStr by remember { mutableStateOf(settings.aboutDetails) }
    var aboutImageBase64String by remember { mutableStateOf(settings.aboutImageBase64) }
    var isAboutTextDeletedOption by remember { mutableStateOf(settings.isAboutContentTextDeleted) }
    var isAboutImageReplacesContentOption by remember { mutableStateOf(settings.isAboutImageReplacesContent) }

    // Welcome properties
    var welcomeTextStr by remember { mutableStateOf(settings.welcomeText) }
    var welcomeImgBase64 by remember { mutableStateOf(settings.welcomeImageBase64) }
    var isWelcomeImgActive by remember { mutableStateOf(settings.isWelcomeImageActive) }
    var welcomeTxtSz by remember { mutableStateOf(settings.welcomeTextSize.toString()) }
    var welcomeTxtPos by remember { mutableStateOf(settings.welcomeTextPosition) }

    var appIconSz by remember { mutableStateOf(settings.appIconSize.toString()) }
    var footerImageBase64 by remember { mutableStateOf(settings.footerBackgroundImageBase64) }

    val aiIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    customAiIconBase64String = compressAndResizeImageAdmin(bytes)
                    Toast.makeText(context, "🤖 تم تعيين صورة المساعد الذكي المخصصة بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تحديد الملف", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val chatIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    customChatIconBase64String = compressAndResizeImageAdmin(bytes)
                    Toast.makeText(context, "💬 تم تعيين صورة أيقونة الدردشة الفورية المخصصة!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تعيين الصورة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val aboutIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    customAboutIconBase64String = compressAndResizeImageAdmin(bytes)
                    Toast.makeText(context, "ℹ️ تم تحميل وتعيين أيقونة حول التطبيق المخصصة!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تحديد الملف", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val aboutPageCardImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    aboutImageBase64String = compressAndResizeImageAdmin(bytes)
                    Toast.makeText(context, "🖼️ تم رفع وتعيين صورة تعبيرية لصفحة حول التطبيق!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تحديد صورة حول التطبيق", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val footerBgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    footerImageBase64 = compressAndResizeImageAdmin(bytes)
                    Toast.makeText(context, "🖼️ تم تغيير خلفية تذييل التطبيق من الاستوديو بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تعيين خلفية التذييل", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val welcomeImgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    welcomeImgBase64 = compressAndResizeImageAdmin(bytes)
                    Toast.makeText(context, "🖼️ تم اختيار صورة الترحيب البديلة بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تعيين صورة الترحيب", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Theme Colors
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌌 تحديد طابع وألوان ومظهر التطبيق العام والمزامنة اللحظية:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { selectedTheme = "COSMIC_SLATE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedTheme == "COSMIC_SLATE") CosmicSlatePrimary else Color.DarkGray)
                        ) {
                            Text("كوزميك سيلفر 🌌", fontSize = 11.sp, color = Color.Black)
                        }

                        Button(
                            onClick = { selectedTheme = "CHARCOAL_GOLD" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedTheme == "CHARCOAL_GOLD") CharcoalGoldPrimary else Color.DarkGray)
                        ) {
                            Text("الذهبي الفاخر ✨", fontSize = 11.sp, color = Color.Black)
                        }

                        Button(
                            onClick = { selectedTheme = "ROYAL_EMERALD" },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedTheme == "ROYAL_EMERALD") RoyalEmeraldPrimary else Color.DarkGray)
                        ) {
                            Text("الزمردي الراقي 🟢", fontSize = 11.sp, color = Color.Black)
                        }
                    }

                    Text("🖋️ تحديد لون خطوط التطبيق والكتابة في الحقول والمدخلات:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { selectedFontColor = "BRIGHT_WHITE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedFontColor == "BRIGHT_WHITE") Color.White else Color.DarkGray)
                        ) {
                            Text("الأبيض الناصع ◽", fontSize = 10.sp, color = Color.Black)
                        }

                        Button(
                            onClick = { selectedFontColor = "LIGHT_GOLD" },
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedFontColor == "LIGHT_GOLD") LightGoldColor else Color.DarkGray)
                        ) {
                            Text("الذهبي الفاتح 🟡", fontSize = 10.sp, color = Color.Black)
                        }

                        Button(
                            onClick = { selectedFontColor = "VIBRANT_SILVER" },
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedFontColor == "VIBRANT_SILVER") VibrantSilverColor else Color.DarkGray)
                        ) {
                            Text("الفضي المتوهج ◽", fontSize = 10.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Floating Icons Order, Sizes and Hide/Delete properties (AI, CHAT, ABOUT)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚙️ تخصيص الأزرار والأيقونات العائمة المعلقة للجمهور:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CharcoalGoldPrimary)

                    OutlinedTextField(
                        value = iconOrderStr,
                        onValueChange = { iconOrderStr = it },
                        label = { Text("ترتيب ظهور الأيقونات (مفصولة بفاصلة)") },
                        placeholder = { Text("مثال: AI,CHAT,ABOUT") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("ملاحظة: يمكنك إخفاء أيقونة بحذف كلمتها تماماً من السلسلة أعلاه مثل: 'AI,CHAT' لإلغاء أيقونة حول التطبيق.", fontSize = 9.sp, color = Color.LightGray)

                    // Resizes Form Controls
                    Text("📐 التحكم الفردي بالحجم والمقاس (النسبة المئوية 30 إلى 120):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = aiIconSz, onValueChange = { aiIconSz = it }, label = { Text("حجم المساعد (AI)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = chatIconSz, onValueChange = { chatIconSz = it }, label = { Text("حجم الدردشة (CHAT)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = aboutIconSz, onValueChange = { aboutIconSz = it }, label = { Text("حجم حول (ABOUT)") }, modifier = Modifier.weight(1.1f))
                    }

                    // Visibility toggles
                    Text("👁️ التحكم بالظهور المباشر والإخفاء الكلي:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isAiIconHidden, onCheckedChange = { isAiIconHidden = it })
                            Text("إخفاء أيقونة مساعد الذكاء الاصطناعي كلياً", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isChatIconHidden, onCheckedChange = { isChatIconHidden = it })
                            Text("إخفاء أيقونة المحادثة الفورية المعلقة", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isAboutIconHidden, onCheckedChange = { isAboutIconHidden = it })
                            Text("إخفاء أيقونة حول التطبيق وتفاصيله العائمة", fontSize = 11.sp)
                        }
                    }

                    // Upload Base64 custom icon design buttons
                    Text("🛡️ تخصيص وتعيين صور الأيقونات من الاستوديو:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { aiIconLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Text("صورة الذكاء", fontSize = 10.sp)
                        }
                        Button(onClick = { chatIconLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Text("صورة الشات", fontSize = 10.sp)
                        }
                        Button(onClick = { aboutIconLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Text("صورة حول", fontSize = 10.sp)
                        }
                    }

                    // Effects Configurations
                    Text("✨ تحديد تأثير الحركة والتوهج لكل أيقونة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("تأثير المساعد (AI):", fontSize = 10.sp)
                            Row {
                                FilterChip(selected = selectedAiEffect == "NONE", onClick = { selectedAiEffect = "NONE" }, label = { Text("عادي") })
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(selected = selectedAiEffect == "GLOW", onClick = { selectedAiEffect = "GLOW" }, label = { Text("توهج") })
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(selected = selectedAiEffect == "ROTATE", onClick = { selectedAiEffect = "ROTATE" }, label = { Text("دوران") })
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("تأثير الشات (CHAT):", fontSize = 10.sp)
                            Row {
                                FilterChip(selected = selectedChatEffect == "NONE", onClick = { selectedChatEffect = "NONE" }, label = { Text("عادي") })
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(selected = selectedChatEffect == "GLOW", onClick = { selectedChatEffect = "GLOW" }, label = { Text("توهج") })
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(selected = selectedChatEffect == "ROTATE", onClick = { selectedChatEffect = "ROTATE" }, label = { Text("دوران") })
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("تأثير حول (ABOUT):", fontSize = 10.sp)
                            Row {
                                FilterChip(selected = selectedAboutEffect == "NONE", onClick = { selectedAboutEffect = "NONE" }, label = { Text("عادي") })
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(selected = selectedAboutEffect == "GLOW", onClick = { selectedAboutEffect = "GLOW" }, label = { Text("توهج") })
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(selected = selectedAboutEffect == "ROTATE", onClick = { selectedAboutEffect = "ROTATE" }, label = { Text("دوران") })
                            }
                        }
                    }
                }
            }
        }

        // Custom about app page content and image replacement configurations
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ℹ️ تخصيص وتعديل معلومات صفحة 'حول التطبيق':", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CharcoalGoldPrimary)

                    OutlinedTextField(
                        value = aboutTitleStr,
                        onValueChange = { aboutTitleStr = it },
                        label = { Text("العنوان الرئيسي لصفحة حول التطبيق") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = aboutSubtitleStr,
                        onValueChange = { aboutSubtitleStr = it },
                        label = { Text("العنوان الفرعي أو رقم الإصدار") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = aboutDetailsStr,
                        onValueChange = { aboutDetailsStr = it },
                        label = { Text("تفاصيل ومعلومات الاستخدام والاتصال باليمن") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Text("🖼️ تعيين صورة تعبيرية/شعار لصفحة حول التطبيق:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { aboutPageCardImageLauncher.launch("image/*") }) {
                            Text("اختر الصورة من هاتفك 🖼️")
                        }
                        if (aboutImageBase64String.isNotBlank()) {
                            Text("صورة مخصصة نشطة 🟢", fontSize = 10.sp, color = Color.Green)
                        }
                    }

                    Text("⚙️ دمج وتحكم كامل بالبيانات الورقية أو عرض الصور:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isAboutTextDeletedOption, onCheckedChange = { isAboutTextDeletedOption = it })
                            Text("حذف/إلغاء المربعات النصية التفصيلية لصفحة حول كلياً", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isAboutImageReplacesContentOption, onCheckedChange = { isAboutImageReplacesContentOption = it })
                            Text("استبدال الصفحة كاملة بالصورة المرفوعة فقط", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Welcome controls Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("👋 إعدادات رسالة الترحيب / صورة الترحيب بالدليل:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل صورة الترحيب بدلاً من النص:", fontSize = 12.sp)
                        Switch(checked = isWelcomeImgActive, onCheckedChange = { isWelcomeImgActive = it })
                    }

                    if (isWelcomeImgActive) {
                        Button(onClick = { welcomeImgLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                            Text("اختر صورة الترحيب من استوديو الهاتف 🖼️")
                        }
                        if (welcomeImgBase64.isNotBlank()) {
                            Text("تم تحديد مظهر صورة الترحيب بنجاح وسيتم حفظها بقاعدة البيانات.", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        OutlinedTextField(
                            value = welcomeTextStr,
                            onValueChange = { welcomeTextStr = it },
                            label = { Text("نص رسالة الترحيب العلوية") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = welcomeTxtSz,
                            onValueChange = { welcomeTxtSz = it },
                            label = { Text("حجم الخط") },
                            modifier = Modifier.weight(1f)
                        )
                        Column(modifier = Modifier.weight(2.3f)) {
                            Text("مكان ظهور رسالة الترحيب:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = welcomeTxtPos == "TOP",
                                    onClick = { welcomeTxtPos = "TOP" },
                                    label = { Text("قمة ▴", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = welcomeTxtPos == "MIDDLE",
                                    onClick = { welcomeTxtPos = "MIDDLE" },
                                    label = { Text("وسط ◂", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = welcomeTxtPos == "BOTTOM",
                                    onClick = { welcomeTxtPos = "BOTTOM" },
                                    label = { Text("تذييل ▾", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Footer background change from gallery
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎨 مظهر تذييل شريط التطبيق وتخصيصه:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Button(onClick = { footerBgLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("رفع وتغيير صورة خلفية التذييل من الاستوديو 🖼️")
                    }
                    if (footerImageBase64.isNotBlank()) {
                        Text("تم تعيين صورة تذييل مخصصة وسيتم تفعيلها فوراً بالحزمة!", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Save Button with direct mapping sync
        item {
            Button(
                onClick = {
                    viewModel.saveAppSettingsDirect(
                        settings.copy(
                            activeTheme = selectedTheme,
                            fontColor = selectedFontColor,
                            customAiIconBase64 = customAiIconBase64String,
                            customAiIconEffect = selectedAiEffect,
                            customChatIconBase64 = customChatIconBase64String,
                            customChatIconEffect = selectedChatEffect,
                            customAboutIconBase64 = customAboutIconBase64String,
                            customAboutIconEffect = selectedAboutEffect,
                            aiIconSize = aiIconSz.toFloatOrNull() ?: settings.aiIconSize,
                            chatIconSize = chatIconSz.toFloatOrNull() ?: settings.chatIconSize,
                            aboutIconSize = aboutIconSz.toFloatOrNull() ?: settings.aboutIconSize,
                            isAiIconHidden = isAiIconHidden,
                            isChatIconHidden = isChatIconHidden,
                            isAboutIconHidden = isAboutIconHidden,
                            iconOrder = iconOrderStr,
                            aboutTitle = aboutTitleStr,
                            aboutSubtitle = aboutSubtitleStr,
                            aboutDetails = aboutDetailsStr,
                            aboutImageBase64 = aboutImageBase64String,
                            isAboutContentTextDeleted = isAboutTextDeletedOption,
                            isAboutImageReplacesContent = isAboutImageReplacesContentOption,
                            footerBackgroundImageBase64 = footerImageBase64,
                            welcomeText = welcomeTextStr,
                            welcomeImageBase64 = welcomeImgBase64,
                            isWelcomeImageActive = isWelcomeImgActive,
                            welcomeTextSize = welcomeTxtSz.toFloatOrNull() ?: settings.welcomeTextSize,
                            welcomeTextPosition = welcomeTxtPos
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تطبيق وحفظ إعدادات المظهر الفوري 🎨✨")
            }
        }
    }
}

@Composable
fun TabDatabasePrivacyBackups(viewModel: AppViewModel, settings: AppSettings, context: Context) {
    var globalChatToggle by remember { mutableStateOf(settings.isChatEnabledGlobal) }
    var disabledMsg by remember { mutableStateOf(settings.chatDisabledMessage) }
    var disabledListStr by remember { mutableStateOf(settings.disabledChatProviderIds) }
    var blockVisitorsChat by remember { mutableStateOf(settings.preventVisitorsChat) }
    var blockProvidersChat by remember { mutableStateOf(settings.preventProvidersChat) }
    var blockedUserPhonesStr by remember { mutableStateOf(settings.blockedChatUserPhones) }
    
    // Live update app version URL for DownloadManager
    var appUrlStr by remember { mutableStateOf(settings.latestVersionUrl) }
    var appVerCode by remember { mutableStateOf(settings.latestVersionCode.toString()) }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live Chat activation / deactivation management for all or specific providers
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💬 التحكم بميزة المحادثة الفورية والتعطيل اللحظي للتواصل:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ميزة المحادثة الفورية نشطة عامة:", fontSize = 12.sp)
                        Switch(checked = globalChatToggle, onCheckedChange = { globalChatToggle = it })
                    }

                    OutlinedTextField(
                        value = disabledMsg,
                        onValueChange = { disabledMsg = it },
                        label = { Text("رسالة تظهر للجمهور عند الإيقاف") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = disabledListStr,
                        onValueChange = { disabledListStr = it },
                        label = { Text("أرقام معينة للمنع (مثال: 1,3,4 مع فصله بفواصل)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("منع الزوار (غير المشتركين) من الدردشة:", fontSize = 12.sp)
                        Switch(checked = blockVisitorsChat, onCheckedChange = { blockVisitorsChat = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("منع مقدمي الخدمات من إجراء أو الرد على المحادثات:", fontSize = 12.sp)
                        Switch(checked = blockProvidersChat, onCheckedChange = { blockProvidersChat = it })
                    }

                    OutlinedTextField(
                        value = blockedUserPhonesStr,
                        onValueChange = { blockedUserPhonesStr = it },
                        label = { Text("هواتف الزوار أو مقدمي الخدمة المحظورين (مفصولة بفواصل)") },
                        placeholder = { Text("مثال: 777644670,7112233") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.saveAppSettingsDirect(
                                settings.copy(
                                    isChatEnabledGlobal = globalChatToggle,
                                    chatDisabledMessage = disabledMsg,
                                    disabledChatProviderIds = disabledListStr,
                                    preventVisitorsChat = blockVisitorsChat,
                                    preventProvidersChat = blockProvidersChat,
                                    blockedChatUserPhones = blockedUserPhonesStr
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مزامنة وحفظ قرارات الإيقاف الفورية")
                    }
                }
            }
        }

        // Live App Versioning & Download URL Update
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔄 إعدادات الترقية والارتباط المباشر بالخادم (In-App Update):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = appUrlStr,
                        onValueChange = { appUrlStr = it },
                        label = { Text("رابط تحميل حزمة التطبيق APK من ميديا فاير أو غيره") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = appVerCode,
                        onValueChange = { appVerCode = it },
                        label = { Text("رمز الاصدار بالخادم للمقارنة الفورية (مثال: 2)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.saveAppSettingsDirect(
                                settings.copy(
                                    latestVersionUrl = appUrlStr,
                                    latestVersionCode = appVerCode.toIntOrNull() ?: 2
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مزامنة ونشر ترقية النظام للجمهور")
                    }
                }
            }
        }

        // Data Management option, wipe chat records for security, or restore db backup
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🧼 خصوصية المستخدمين وسجلات النظافة الفورية:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    Button(
                        onClick = { viewModel.clearAllChatRecords() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🧼 حذف كافة سجلات محادثات الأعضاء نهائياً")
                    }
                }
            }
        }

        // CSV Backup and Restore Clipboard Integration (allows backup transfer directly via simple base64 CSV)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💾 نظام النسخ الاحتياطي اللحظي لقاعدة البيانات ودليل اليمن:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("يمكنك تحويل قاعدة البيانات بالكامل إلى نص منسق وحفظه أو نسخه، واستعادته بأي وقت أو هاتف بدون فقدان معلوماتك.", fontSize = 11.sp, color = Color.Gray)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.backupDatabaseState()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("توليد النسخة للحافظة 📋")
                        }

                        Button(
                            onClick = {
                                val clipData = clipboard.primaryClip
                                if (clipData != null && clipData.itemCount > 0) {
                                    val text = clipData.getItemAt(0).text.toString()
                                    if (text.isNotBlank()) {
                                        coroutineScope.launch {
                                            val ok = viewModel.restoreDatabase(text)
                                            if (ok) {
                                                Toast.makeText(context, "✅ تم استيراد واستعادة قاعدة البيانات والمهنيين والمشرفين بنجاح!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "❌ صيغة ملف التصدير غير صحيحة", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "الحافظة فارغة! انسخ نص النسخ الاحتياطي أولاً.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("استعادة من الحافظة 📥")
                        }
                    }

                    if (viewModel.backupStringState.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("yemen_db_backup", viewModel.backupStringState))
                                Toast.makeText(context, "📋 تم نسخ كود النسخة الاحتياطية بنجاح! احفظه بأمان.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("نسخ الكود المتولد الآن ✂️")
                        }
                    }
                }
            }
        }
    }
}
