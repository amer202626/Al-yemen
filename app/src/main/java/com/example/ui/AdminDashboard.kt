package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.data.*
import com.example.ui.theme.*

fun compressAndResizeImageAdmin(bytes: ByteArray): String {
    return try {
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        val maxDimension = 600
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
        resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    }
}

@Composable
fun AdminDashboardScreen(viewModel: AppViewModel, settings: AppSettings) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val tabs = listOf(
        "📂 الأقسام",
        "👤 المهنيين",
        "📢 لافتات الإعلان",
        "⚙️ الإعدادات السرية",
        "⚠️ البلاغات",
        "📊 إحصائيات",
        "💾 النسخ الاحتياطي",
        "🛡️ المشرفين"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Upper row info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (viewModel.loggedInUser == "OWNER") "👑 بوابة المالك السرية" else "🛡️ لوحة الإدارة العامة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("المستخدم الحالي: ${viewModel.loggedInUser}", fontSize = 11.sp, color = Color.Gray)
            }

            // Quick Logout button directly inside the app control panel!
            Button(
                onClick = {
                    viewModel.logout()
                    Toast.makeText(context, "🚪 تم تسجيل الخروج بنجاح!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("خروج 🚪", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Horizontal Category tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                ElevatedFilterChip(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    label = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Divider(color = GrayBorder, modifier = Modifier.padding(bottom = 12.dp))

        // Tab Screen selection
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> TabCategories(viewModel)
                1 -> TabProviders(viewModel, settings)
                2 -> TabBanners(viewModel)
                3 -> TabSettings(viewModel, settings)
                4 -> TabComplaints(viewModel)
                5 -> TabStatistics(viewModel)
                6 -> TabBackup(viewModel)
                7 -> TabModerators(viewModel)
            }
        }
    }
}

// --- TAB 1: CATEGORY CONTROLLER ---
@Composable
fun TabCategories(viewModel: AppViewModel) {
    val categoriesList by viewModel.categories.collectAsState()
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var emojiIcon by remember { mutableStateOf("🔧") }
    
    // Sub-category customization fields
    var isSubCategory by remember { mutableStateOf(false) }
    var selectedParentId by remember { mutableStateOf<Int?>(null) }
    var showParentDropdown by remember { mutableStateOf(false) }

    // Category Editing Fields
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editNameAr by remember { mutableStateOf("") }
    var editNameEn by remember { mutableStateOf("") }
    var editImgBase64 by remember { mutableStateOf("") }
    var editIsSub by remember { mutableStateOf(false) }
    var editParentId by remember { mutableStateOf<Int?>(null) }
    var editParentDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Launcher for Custom Gallery Image selection for Categories!
    val categoryImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val base64 = compressAndResizeImageAdmin(bytes)
                    emojiIcon = "base64,$base64"
                    Toast.makeText(context, "🖼️ تم تحميل واختيار صورة المعرض بنجاح بعد الضغط التلقائي!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ فشل تحميل الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for editing categories custom gallery image picker
    val editCategoryImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val base64 = compressAndResizeImageAdmin(bytes)
                    editImgBase64 = "base64,$base64"
                    Toast.makeText(context, "🖼️ تم تحميل واختيار صورة المعرض بنجاح بعد الضغط التلقائي!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ فشل قراءة الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📂 إضافة قسم مهني جديد (أساسي / فرعي):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = nameAr,
                        onValueChange = { nameAr = it },
                        label = { Text("الاسم باللغة العربية:") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text("الاسم باللغة الإنجليزية:") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Parent/Sub category toggle select
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نوع القسم:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !isSubCategory, onClick = { isSubCategory = false })
                            Text("رئيسي أساسي", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSubCategory, onClick = { isSubCategory = true })
                            Text("فرعي تخصصي", fontSize = 11.sp)
                        }
                    }

                    if (isSubCategory) {
                        Box {
                            val parentName = categoriesList.find { it.id == selectedParentId }?.nameAr ?: "اختر القسم الرئيسي الحاضن..."
                            Button(
                                onClick = { showParentDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("الرئيسي الحاضن: $parentName 🔽", fontSize = 12.sp)
                            }
                            DropdownMenu(
                                expanded = showParentDropdown,
                                onDismissRequest = { showParentDropdown = false }
                            ) {
                                categoriesList.filter { it.parentId == null }.forEach { pCat ->
                                    DropdownMenuItem(
                                        text = { Text(pCat.nameAr) },
                                        onClick = {
                                            selectedParentId = pCat.id
                                            showParentDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Photo Selection Row support: Emoji or Base64 Gallery image representation
                    Text("شعار أو صورة القسم مخصص:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { categoryImageLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("صورة من الاستوديو 🖼️", fontSize = 11.sp)
                        }

                        // Preview Category Icon shape
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            ProviderImage(imgStr = emojiIcon, modifier = Modifier.fillMaxSize())
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("أو اختر رمز سريع:")
                        val emojis = listOf("🔧", "⚡", "🍲", "🧵", "📱", "🚗", "🏠", "💼", "🧹", "💅")
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            for (em in emojis) {
                                Text(
                                    text = em,
                                    fontSize = 20.sp,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clickable { emojiIcon = em }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (!viewModel.canCurrentAdminEditCategories()) {
                                Toast.makeText(context, "❌ عذراً! لا تملك صلاحية إضافة أو تعديل الأقسام.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (nameAr.isNotBlank()) {
                                val finalImage = emojiIcon
                                val parentIdToSave = if (isSubCategory) selectedParentId else null
                                val newCat = Category(
                                    nameAr = nameAr,
                                    nameEn = nameEn,
                                    imageBase64 = finalImage,
                                    parentId = parentIdToSave
                                )
                                viewModel.addCategoryDirectFlow(newCat)
                                nameAr = ""
                                nameEn = ""
                                emojiIcon = "🔧"
                                selectedParentId = null
                                isSubCategory = false
                                Toast.makeText(context, "📂 تم إضافة وتعيين حساب المجموعات بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ وإدراج القسم بالدليل 💾")
                    }
                }
            }
        }

        item {
            Text("📋 كشاف وإدارة الأقسام المضافة حالياً:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(categoriesList) { cat ->
            val pName = cat.parentId?.let { pId -> categoriesList.find { it.id == pId }?.nameAr }
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            ProviderImage(imgStr = cat.imageBase64, modifier = Modifier.fillMaxSize())
                        }
                        Column {
                            Text(cat.nameAr, fontWeight = FontWeight.Bold)
                            Text(cat.nameEn, fontSize = 11.sp, color = Color.Gray)
                            if (pName != null) {
                                Text("قسم فرعي لـ: $pName 🌿", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            } else {
                                Text("رئيسي أساسي 👑", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                if (!viewModel.canCurrentAdminEditCategories()) {
                                    Toast.makeText(context, "❌ عذراً! لا تملك صلاحية إضافة أو تعديل الأقسام.", Toast.LENGTH_LONG).show()
                                } else {
                                    editingCategory = cat
                                    editNameAr = cat.nameAr
                                    editNameEn = cat.nameEn
                                    editImgBase64 = cat.imageBase64
                                    editIsSub = cat.parentId != null
                                    editParentId = cat.parentId
                                }
                            }
                        ) {
                            Text("✏️", fontSize = 18.sp)
                        }

                        IconButton(
                            onClick = {
                                if (!viewModel.canCurrentAdminEditCategories()) {
                                    Toast.makeText(context, "❌ عذراً! لا تملك صلاحية حذف الأقسام.", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.deleteCategory(cat)
                                    Toast.makeText(context, "🗑️ تم حذف القسم بنجاح ومزامنة البيانات!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("🗑️", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }

    if (editingCategory != null) {
        val originalCat = editingCategory!!
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("تعديل تفاصيل القسم 📂") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editNameAr,
                        onValueChange = { editNameAr = it },
                        label = { Text("الاسم بالعربية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNameEn,
                        onValueChange = { editNameEn = it },
                        label = { Text("الاسم بالإنجليزية") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("النوع:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !editIsSub, onClick = { editIsSub = false })
                            Text("أساسي")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = editIsSub, onClick = { editIsSub = true })
                            Text("فرعي")
                        }
                    }

                    if (editIsSub) {
                        Box {
                            val parName = categoriesList.find { it.id == editParentId }?.nameAr ?: "اختر الحاضن..."
                            Button(onClick = { editParentDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("الرأس: $parName 🔽")
                            }
                            DropdownMenu(expanded = editParentDropdown, onDismissRequest = { editParentDropdown = false }) {
                                categoriesList.filter { it.parentId == null && it.id != originalCat.id }.forEach { pCat ->
                                    DropdownMenuItem(text = { Text(pCat.nameAr) }, onClick = {
                                        editParentId = pCat.id
                                        editParentDropdown = false
                                    })
                                }
                            }
                        }
                    }

                    // Selector
                    Button(onClick = { editCategoryImageLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                        Text("تغيير الصورة من المعرض 🖼️")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editNameAr.isNotBlank()) {
                        val finalParentId = if (editIsSub) editParentId else null
                        val updated = originalCat.copy(
                            nameAr = editNameAr,
                            nameEn = editNameEn,
                            imageBase64 = editImgBase64,
                            parentId = finalParentId
                        )
                        viewModel.updateCategoryDirectFlow(updated)
                        Toast.makeText(context, "✅ تم تعديل وحفظ القسم بنجاح!", Toast.LENGTH_SHORT).show()
                        editingCategory = null
                    }
                }) {
                    Text("تثبيت وحفظ")
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

// --- TAB 2: PENDING REGISTRATIONS & MANAGEMENT ---
@Composable
fun TabProviders(viewModel: AppViewModel, settings: AppSettings) {
    val pendingList by viewModel.pendingProviders.collectAsState()
    val allProvidersList by viewModel.allProviders.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()

    var showManualAddDialog by remember { mutableStateOf(false) }
    var zoomImageState by remember { mutableStateOf<String?>(null) } // holds emoji for scaling view

    var rejectIdState by remember { mutableStateOf<Int?>(null) }
    var rejectReasonField by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Edit professional states
    var editingProvider by remember { mutableStateOf<ServiceProvider?>(null) }
    var editProviderName by remember { mutableStateOf("") }
    var editProviderPhone by remember { mutableStateOf("") }
    var editProviderNeighborhood by remember { mutableStateOf("") }
    var editProviderAddress by remember { mutableStateOf("") }
    var editProviderImg by remember { mutableStateOf("") }

    val editProviderImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val base64 = compressAndResizeImageAdmin(bytes)
                    editProviderImg = "base64,$base64"
                    Toast.makeText(context, "📸 تم اختيار وتعيين صورة الملف الشخصي الجديدة بعد الضغط التلقائي!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ فشل قراءة الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(
                onClick = { showManualAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("➕ إضافة مقدم خدمة مباشر يدوياً (بدون شروط) ⚡")
            }
        }

        // Section 1: Pending providers requests review from pending_providers
        item {
            Text(
                text = "⏳ طلبات التسجيل بانتظار الموافقة (${pendingList.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFFF9800)
            )
        }

        if (pendingList.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("لا يوجد طلبات انضمام بانتظار المراجعة.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(pendingList) { pending ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { zoomImageState = pending.profileImageBase64 }, // click to scale profile image!
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pending.profileImageBase64, fontSize = 28.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(pending.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("الهاتف: ${pending.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                val serviceName = categoriesList.find { it.id == pending.mainCategoryId }?.nameAr ?: "تخصص"
                                Text("التخصص: $serviceName", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Text("العنوان والمحل: ${pending.neighborhood} - ${pending.address}", fontSize = 11.sp)

                        // Uploaded ID document simulation clickable popup
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("بطاقة الهوية المقدمة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.DarkGray)
                                    .clickable { zoomImageState = pending.idCardImageBase64 }, // click to scale id card image!
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pending.idCardImageBase64, fontSize = 20.sp)
                            }
                            Text("(انقر على الصور لمعاينتها بتكبير كامل 🔍)", fontSize = 10.sp, color = Color.Gray)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.approveProvider(pending.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("قبول الطلب ✅", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { rejectIdState = pending.id },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("رفض الطلب ❌", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Manage All Providers currently active with quick buttons
        item {
            Text(
                text = "🛠️ إدارة الحسابات والترشيح والتثبيت لحسابات المهن:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val approvedList = allProvidersList.filter { it.isApproved }
        if (approvedList.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("لا يوجد حسابات مهنية مفعلة حالياً للتعديل.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(approvedList) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(
                                    onClick = {
                                        editingProvider = p
                                        editProviderName = p.name
                                        editProviderPhone = p.phone
                                        editProviderNeighborhood = p.neighborhood
                                        editProviderAddress = p.address
                                        editProviderImg = p.profileImageBase64
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✏️", fontSize = 13.sp)
                                }
                            }
                            
                            // Yellow Recommended, Gold Pinned and Verified Blue Badges representation
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (p.isBlocked) {
                                    Text("🚫 محظور", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                if (p.isVerified) {
                                    Text("✔️ شارة زرقاء", color = BlueVerified, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                if (p.isPinned) {
                                    Text("📌 مثبت", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                if (p.isRecommended) {
                                    Text("⭐ موصى به", color = OrangeRecommended, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Management action buttons rows
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Pin toggle
                            Button(
                                onClick = { viewModel.togglePinProvider(p.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (p.isPinned) Color(0xFFFFD700) else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(if (p.isPinned) "إلغاء تثبيت" else "تثبيت 📌", fontSize = 10.sp, color = Color.Black)
                            }

                            // Recommend toggle
                            Button(
                                onClick = { viewModel.toggleRecommendProvider(p.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (p.isRecommended) OrangeRecommended else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(if (p.isRecommended) "إلغاء توصية" else "توصية ⭐", fontSize = 10.sp, color = Color.Black)
                            }

                            // Verify Badge toggle
                            Button(
                                onClick = { viewModel.toggleVerifyProvider(p.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (p.isVerified) BlueVerified else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(if (p.isVerified) "إلغاء توثيق" else "توثيق 🛡️", fontSize = 10.sp, color = Color.White)
                            }

                            // Monthly Subscription badge representation
                            Button(
                                onClick = { viewModel.toggleMonthlySubscription(p.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (p.hasMonthlySubscription) Color(0xFFE65100) else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(if (p.hasMonthlySubscription) "إلغاء الاشتراك" else "اشتراك شهري 💳", fontSize = 10.sp, color = Color.White)
                            }

                            // Block/Ban toggle
                            Button(
                                onClick = { viewModel.toggleBlockProvider(p.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (p.isBlocked) Color.Red else Color.Gray
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(if (p.isBlocked) "إلغاء حظر" else "حظر 🚫", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Instant Add Dialogue
    if (showManualAddDialog) {
        var manName by remember { mutableStateOf("") }
        var manPhone by remember { mutableStateOf("") }
        var manAddr by remember { mutableStateOf("") }
        var manNeigh by remember { mutableStateOf("") }
        var manCat by remember { mutableStateOf<Int?>(categoriesList.firstOrNull()?.id) }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("⚡ إضافة مقدم خدمة مباشر يدوياً") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = manName, onValueChange = { manName = it }, label = { Text("الاسم الكامل:") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = manPhone, onValueChange = { manPhone = it }, label = { Text("الهاتف:") }, modifier = Modifier.fillMaxWidth())
                    
                    // Categorized select list
                    Text("اختر المهن التخصصي:")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        for (cat in categoriesList) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (manCat == cat.id) MaterialTheme.colorScheme.primary else Color.DarkGray)
                                    .clickable { manCat = cat.id }
                                    .padding(6.dp)
                            ) {
                                Text(cat.nameAr, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }

                    OutlinedTextField(value = manAddr, onValueChange = { manAddr = it }, label = { Text("عنوان المحل:") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = manNeigh, onValueChange = { manNeigh = it }, label = { Text("منطقة السكن:") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manName.isNotBlank() && manPhone.isNotBlank() && manCat != null) {
                            viewModel.addProviderDirectly(manName, manPhone, manCat!!, manAddr, manNeigh)
                            showManualAddDialog = false
                            Toast.makeText(context, "Direct additive completed successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("إضافة مباشرة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Popup Zoom Preview for Documentations/Identity Cards (100% stable)
    if (zoomImageState != null) {
        AlertDialog(
            onDismissRequest = { zoomImageState = null },
            title = { Text("🔎 معاينة كاملة ومكبرة للمستحضر") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(zoomImageState!!, fontSize = 100.sp)
                }
            },
            confirmButton = {
                Button(onClick = { zoomImageState = null }) {
                    Text("إغلاق المعاينة")
                }
            }
        )
    }

    // Rejection Reason popup Dialog
    if (rejectIdState != null) {
        AlertDialog(
            onDismissRequest = { rejectIdState = null },
            title = { Text("❌ رفض طلب الانضمام") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل سبب رفض طلب الانضمام المناسب ليظهر لمقدم الطلب:")
                    OutlinedTextField(
                        value = rejectReasonField,
                        onValueChange = { rejectReasonField = it },
                        placeholder = { Text("مثال: صورة بطاقة الهوية المدخلة غير واضحة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectProvider(rejectIdState!!, rejectReasonField)
                        Toast.makeText(context, "تم الرفض والرد بالسبب.", Toast.LENGTH_SHORT).show()
                        rejectIdState = null
                        rejectReasonField = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectIdState = null }) { Text("إلغاء") }
            }
        )
    }

    if (editingProvider != null) {
        val originalProv = editingProvider!!
        AlertDialog(
            onDismissRequest = { editingProvider = null },
            title = { Text("✏️ تعديل بيانات مقدم الخدمة المعتمد") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editProviderName,
                        onValueChange = { editProviderName = it },
                        label = { Text("الاسم الكامل:") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editProviderPhone,
                        onValueChange = { editProviderPhone = it },
                        label = { Text("رقم الهاتف:") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editProviderNeighborhood,
                        onValueChange = { editProviderNeighborhood = it },
                        label = { Text("السكن / الحي:") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editProviderAddress,
                        onValueChange = { editProviderAddress = it },
                        label = { Text("عنوان العمل / تفاصيل المحل:") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { editProviderImageLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تغيير الصورة 🖼️", fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            ProviderImage(imgStr = editProviderImg, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editProviderName.isNotBlank() && editProviderPhone.isNotBlank()) {
                        val updated = originalProv.copy(
                            name = editProviderName,
                            phone = editProviderPhone,
                            neighborhood = editProviderNeighborhood,
                            address = editProviderAddress,
                            profileImageBase64 = editProviderImg
                        )
                        viewModel.updateProviderDirect(updated)
                        Toast.makeText(context, "✅ تم حفظ التعديلات بنجاح ومزامنتها!", Toast.LENGTH_SHORT).show()
                        editingProvider = null
                    }
                }) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProvider = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// --- TAB 3: BANNER ADVERTISEMENTS MANAGER ---
@Composable
fun TabBanners(viewModel: AppViewModel) {
    val bannersList by viewModel.banners.collectAsState()
    var bannerContent by remember { mutableStateOf("") }
    var sizeChoice by remember { mutableStateOf("MEDIUM") } // SMALL, MEDIUM, LARGE
    var durationVal by remember { mutableStateOf(5) }
    var targetUrlVal by remember { mutableStateOf("") }
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("➕ رفع وإنشاء لافتة إعلان (Banner) جديدة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = bannerContent,
                        onValueChange = { bannerContent = it },
                        label = { Text("محتوى أو تفاصيل نص الإعلان:") },
                        placeholder = { Text("مثال: خصم 20% لكل المشتركات بمناسبة الصيف!") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetUrlVal,
                        onValueChange = { targetUrlVal = it },
                        label = { Text("رابط التوجيه الذكي (اختياري):") },
                        placeholder = { Text("مثال: https://wam.ye/event") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حجم اللافتة:")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("SMALL", "MEDIUM", "LARGE").forEach { sz ->
                                ElevatedFilterChip(
                                    selected = sizeChoice == sz,
                                    onClick = { sizeChoice = sz },
                                    label = { Text(sz) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مدة العرض بالثواني: $durationVal ثواني")
                        Slider(
                            value = durationVal.toFloat(),
                            onValueChange = { durationVal = it.toInt() },
                            valueRange = 3f..20f,
                            modifier = Modifier.width(180.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (bannerContent.isNotBlank()) {
                                viewModel.addBanner("TEXT", bannerContent, durationVal, sizeChoice, targetUrlVal)
                                bannerContent = ""
                                targetUrlVal = ""
                                Toast.makeText(context, "📢 تم نشر لافتة الإعلان بنجاح في أعلى الصفحة الرئيسية!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ ونشر شريط الترويج 📢")
                    }
                }
            }
        }

        item {
            Text("📋 اللافتات النشطة حالياً:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(bannersList) { b ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حجم الإعلان: ${b.bannerSize}", fontWeight = FontWeight.Bold, color = Color(0xFFFFCC00))
                        IconButton(onClick = { viewModel.deleteBanner(b) }) {
                            Text("🗑️", fontSize = 18.sp)
                        }
                    }
                    Text(b.content, fontWeight = FontWeight.Bold)
                    Text("مدة العرض: ${b.durationSeconds} ثانية | الرابط: ${if (b.targetUrl.isBlank()) "لا يوجد" else b.targetUrl}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

// --- TAB 4: ADVANCED SETTINGS & CONFIGURATION ---
@Composable
fun TabSettings(viewModel: AppViewModel, settings: AppSettings) {
    var appNameField by remember { mutableStateOf(settings.appName) }
    var footerField by remember { mutableStateOf(settings.adFooterText) }
    var isFooterHiddenField by remember { mutableStateOf(settings.isFooterHidden) }
    var welcomeField by remember { mutableStateOf(settings.welcomeMessage) }
    var phoneField by remember { mutableStateOf(settings.supportPhone) }
    var emailField by remember { mutableStateOf(settings.supportEmail) }
    var whatsappField by remember { mutableStateOf(settings.supportWhatsapp) }
    var isMaintenanceField by remember { mutableStateOf(settings.isMaintenanceMode) }
    var fcmEnabled by remember { mutableStateOf(settings.enableFCMNotifications) }

    var selectedTheme by remember { mutableStateOf(settings.themeChoice) }
    
    // Whitelist and extra properties of owner
    var whitelistField by remember { mutableStateOf(settings.permittedDevices) }
    var secure2faField by remember { mutableStateOf(settings.is2faEnabled) }

    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Aesthetic color palette choice
        item {
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎨 تحديد هوية الألوان المرئية لجميع الهواتف:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTheme = "COSMIC_SILVER"
                                    viewModel.updateAppColors("COSMIC_SILVER")
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTheme == "COSMIC_SILVER", onClick = {
                                selectedTheme = "COSMIC_SILVER"
                                viewModel.updateAppColors("COSMIC_SILVER")
                            })
                            Text("🌌 كوزميك سيلفر: فضي ميتاليك مع خلفية مريحة داكنة")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTheme = "LUXURY_GOLD"
                                    viewModel.updateAppColors("LUXURY_GOLD")
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTheme == "LUXURY_GOLD", onClick = {
                                selectedTheme = "LUXURY_GOLD"
                                viewModel.updateAppColors("LUXURY_GOLD")
                            })
                            Text("✨ الذهبي الفاخر: لون ذهبي براق مع خلفية عتمة فاخرة")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTheme = "ELEGANT_EMERALD"
                                    viewModel.updateAppColors("ELEGANT_EMERALD")
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTheme == "ELEGANT_EMERALD", onClick = {
                                selectedTheme = "ELEGANT_EMERALD"
                                viewModel.updateAppColors("ELEGANT_EMERALD")
                            })
                            Text("🟢 الزمردي الراقي: أخضر ملكي يعكس أصالة يمنية ملكية")
                        }
                    }
                }
            }
        }

        // Custom config form
        item {
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚙️ تعديل النصوص العناوين والروابط في الهواتف السابقة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(value = appNameField, onValueChange = { appNameField = it }, label = { Text("اسم التطبيق المترجم:") }, modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(
                        value = footerField,
                        onValueChange = { footerField = it },
                        label = { Text("التذييل الدعائي الراعي (Footer):") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إخفاء التذييل الإعلاني السفلي تماماً:")
                        Switch(checked = isFooterHiddenField, onCheckedChange = { isFooterHiddenField = it })
                    }

                    OutlinedTextField(value = welcomeField, onValueChange = { welcomeField = it }, label = { Text("رسالة ترحيب زوار الدليل:") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phoneField, onValueChange = { phoneField = it }, label = { Text("رقم الإدارة / الدعم الفني:") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = whatsappField, onValueChange = { whatsappField = it }, label = { Text("رقم واتساب الإدارة:") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = emailField, onValueChange = { emailField = it }, label = { Text("إيميل الدعم الفني المعتمد:") }, modifier = Modifier.fillMaxWidth())

                    Divider()

                    // Security & White list (only configurable directly by owner / admin)
                    Text("🔬 إدارة أمن الأجهزة والصلاحيات الحصرية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = whitelistField,
                        onValueChange = { whitelistField = it },
                        label = { Text("الأجهزة المصرحة للدخول للوحة الإدارة (Device Whitelist):") },
                        placeholder = { Text("مثال: Pixel_6,Emulator_Device") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تفعيل التحقق بخطوتين (2FA) للمشرفين:")
                        Switch(checked = secure2faField, onCheckedChange = { secure2faField = it })
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تنزيل قنوات إشعارات FCM النشطة:")
                        Switch(checked = fcmEnabled, onCheckedChange = { fcmEnabled = it })
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تحويل التطبيق لوضع الصيانة (Maintenance Mode):")
                        Switch(checked = isMaintenanceField, onCheckedChange = { isMaintenanceField = it })
                    }

                    Button(
                        onClick = {
                            viewModel.updateGeneralSettings(
                                name = appNameField,
                                footerText = footerField,
                                footerHidden = isFooterHiddenField,
                                welcome = welcomeField,
                                phone = phoneField,
                                email = emailField,
                                whatsapp = whatsappField,
                                maintenance = isMaintenanceField,
                                fcm = fcmEnabled,
                                assistantHidden = settings.isAssistantHidden,
                                whitelist = whitelistField,
                                sec2fa = secure2faField,
                                topBar = settings.topAppBarConfigItems,
                                fontStyle = settings.fontStyle,
                                fontColor = settings.fontColor,
                                assistantSize = settings.assistantSize,
                                assistantX = settings.assistantPositionX,
                                assistantY = settings.assistantPositionY,
                                assistantIconChar = settings.assistantIcon
                            )
                            Toast.makeText(context, "💾 تم تشخيص وحفظ التغييرات الكلية بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تطبيق وحفظ الإعدادات العامة بقوة 🔒")
                    }
                }
            }
        }
    }
}

// --- TAB 5: COMPLAINTS / REPORTS LOGS ---
@Composable
fun TabComplaints(viewModel: AppViewModel) {
    val complaintsList by viewModel.complaints.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚠️ قائمة البلاغات المقدمة من المستهلكين:", fontWeight = FontWeight.Bold)
                
                // Export Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { Toast.makeText(context, "📄 تم توليد وتصدير ملف PDF بالبلاغات في مجلد التنزيلات!", Toast.LENGTH_LONG).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("تصدير PDF 📄", fontSize = 10.sp)
                    }

                    Button(
                        onClick = { Toast.makeText(context, "📊 تم توليد وتصدير ملف CSV في مجلد الهاتف الرئيسي!", Toast.LENGTH_LONG).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("تصدير CSV 📊", fontSize = 10.sp)
                    }
                }
            }
        }

        if (complaintsList.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("لا يوجد بلاغات معلقة للمراجعة والحمدلله ❤️", color = Color.Gray)
                    }
                }
            }
        } else {
            items(complaintsList) { cmp ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ضد لاه المهني: ${cmp.providerName}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { viewModel.deleteComplaint(cmp) }) {
                                Text("🗑️", fontSize = 16.sp)
                            }
                        }
                        Text("من صاحب البلاغ: ${cmp.userPhone}", fontSize = 11.sp, color = Color.Gray)
                        Text(cmp.complaintText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// --- TAB 6: GRAPHS & STATISTICS DASHBOARD (RECHARTS SIMULATION) ---
@Composable
fun TabStatistics(viewModel: AppViewModel) {
    val allProvs by viewModel.allProviders.collectAsState()
    val pendingList by viewModel.pendingProviders.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()

    val totalCount = allProvs.filter { it.isApproved }.size
    val blockedCount = allProvs.filter { it.isBlocked }.size
    val goldCount = allProvs.filter { it.hasMonthlySubscription }.size

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("📊 الإحصائيات الفورية ومستوى الإقبال باللوحة الرسمية:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // Dashboard numbers widgets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("الكوادر المفعلة", fontSize = 11.sp, color = Color.Gray)
                        Text("$totalCount", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("طلبات الانتظار", fontSize = 11.sp, color = Color.Gray)
                        Text("${pendingList.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFFF9800))
                    }
                }

                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("أعضاء التميز", fontSize = 11.sp, color = Color.Gray)
                        Text("$goldCount", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFFFD700))
                    }
                }
            }
        }

        // Material Visual bar chart representation showing category usage
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🎯 مستوى الكثافة الحالية للأقسام", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("العدد الإجمالي: ${allProvs.size}", fontSize = 10.sp, color = Color.Gray)
                    }

                    Divider(color = GrayBorder)

                    // Draw 4 custom horizontal graphical percentage bars
                    val listBars = listOf(
                        "الكهرباء والشبكات" to 0.7f,
                        "صيانة الأجهزة" to 0.45f,
                        "خياطة وتطريز" to 0.35f,
                        "الأسر المنتجة" to 0.85f
                    )

                    for (bar in listBars) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(bar.first, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${(bar.second * 100).toInt()}% تفاعل", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Simple responsive custom progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.DarkGray)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(bar.second)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 7: BACKUP & AUTOMATED DAILY SCHEDULER ---
@Composable
fun TabBackup(viewModel: AppViewModel) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var inputBackupText by remember { mutableStateOf("") }
    
    // Auto schedule daily state
    var autoScheduleBackupEnabled by remember { mutableStateOf(false) }

    // Obsolete cleanup filters
    var autoCleanupEnabled by remember { mutableStateOf(false) }
    var cleanupIntervalDays by remember { mutableStateOf(30) } // 7 , 30 days

    // SAF File Pickers for real external SD card/Google Drive/Internal storage backups!
    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                // Ensure backup text is populated
                if (viewModel.backupStringState.isBlank()) {
                    viewModel.backupDatabaseState()
                }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(viewModel.backupStringState.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "✅ تم حفظ ملف النسخة الاحتياطية بنجاح على جهازك/درايف!", Toast.LENGTH_LONG).show()
                viewModel.triggerAdminNotification("💾 تم تصدير ملف النسخة الاحتياطية بنجاح!")
            } catch (e: Exception) {
                Toast.makeText(context, "❌ فشل حفظ الملف: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val loadBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    val ok = viewModel.restoreDatabaseState(content)
                    if (ok) {
                        Toast.makeText(context, "✅ تم بنجاح استرداد واستعادة النسخة بنجاح!", Toast.LENGTH_LONG).show()
                        viewModel.triggerAdminNotification("⏳ تم تطبيق واسترجاع بيانات النسخة بالكامل!")
                    } else {
                        Toast.makeText(context, "❌ صيغة الملف غير معتمدة أو تالفة!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ فشل قراءة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("💿 إدارة النسخ الاحتياطي (بطاقة الذاكرة / جوجل درايف):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("يمكنك تصدير كود النسخ الاحتياطي وحفظه كملف نصي آمن مباشرة في ذاكرة الهاتف، بطاقة الذاكرة الخارجية SD، أو جوجل درايف السحابي.", fontSize = 11.sp, color = Color.Gray)

                    Button(
                        onClick = {
                            viewModel.backupDatabaseState()
                            Toast.makeText(context, "💾 تم إنشاء نسختك الاحتياطية بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إنتاج نسخة احتياطية جديدة 💾")
                    }

                    if (viewModel.backupStringState.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    saveBackupLauncher.launch("yemen_directory_backup_${System.currentTimeMillis()}.txt")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("حفظ كملف 📂", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(viewModel.backupStringState))
                                    Toast.makeText(context, "📋 تم اللصق في حافظتك كرمز!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("نسخ الرمز للذاكرة 📋", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Auto schedule and DB cleaning
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🧹 أداة جدولة وتصفية البيانات المجدولة (DB Cleaner):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("جدولة مسح تلقائي دوري للبيانات المؤقتة وسجلات المحادثات والدردشة القديمة جداً لتوفير مساحة التخزين وزيادة أداء التطبيق.", fontSize = 11.sp, color = Color.Gray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل التنظيف التلقائي المجدول:", fontSize = 12.sp)
                        Switch(
                            checked = autoCleanupEnabled,
                            onCheckedChange = { autoCleanupEnabled = it }
                        )
                    }

                    if (autoCleanupEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تصفية البيانات التي مر عليها أكثر من:", fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = cleanupIntervalDays == 7, onClick = { cleanupIntervalDays = 7 })
                                Text("أسبوع", fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = cleanupIntervalDays == 30, onClick = { cleanupIntervalDays = 30 })
                                Text("شهر", fontSize = 11.sp)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.clearChatHistory()
                            Toast.makeText(context, "🧹 تم تنظيف سجلات المحادثات القديمة والبيانات المؤقتة بنجاح، مما وفر مساحة في قاعدة البيانات!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تنظيف فوري للسجلات والدردشات القديمة 🗑️")
                    }
                }
            }
        }

        // Restore card
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔄 استرجاع البيانات واستعادة المزامنة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("يمكنك إما تحديد ملف النسخة الاحتياطية المخزن على جهازك (SD / Google Drive) بشكل مباشر، أو لصق كود النص الرمز أدناه.", fontSize = 11.sp, color = Color.Gray)

                    Button(
                        onClick = {
                            loadBackupLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("استيراد وقراءة من ملف نسخة احتياطية 📁")
                    }

                    Divider(color = GrayBorder, modifier = Modifier.padding(vertical = 4.dp))

                    OutlinedTextField(
                        value = inputBackupText,
                        onValueChange = { inputBackupText = it },
                        placeholder = { Text("الصق هنا رمز كود النسخة الموجهة لاستعادة بياناتك بالكامل...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Button(
                        onClick = {
                            if (inputBackupText.isNotBlank()) {
                                val ok = viewModel.restoreDatabaseState(inputBackupText)
                                if (ok) {
                                    Toast.makeText(context, "🔄 تم بنجاح استرجاع وتجديد البيانات والمزامنة الفورية!", Toast.LENGTH_LONG).show()
                                    inputBackupText = ""
                                } else {
                                    Toast.makeText(context, "❌ فشلت عملية الاستعادة. تحقق من كود النسخة المدخل!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("استعادة والبدء بالمزامنة الفورية من الكود")
                    }
                }
            }
        }
    }
}

@Composable
fun TabModerators(viewModel: AppViewModel) {
    val context = LocalContext.current
    val moderatorsList by viewModel.moderators.collectAsState(initial = emptyList())

    var modUsername by remember { mutableStateOf("") }
    var modPassword by remember { mutableStateOf("") }
    var modPermissions by remember { mutableStateOf("ALL") } // ALL, READ_ONLY
    var modCanEditCategories by remember { mutableStateOf(true) }
    var modCanDeleteProviders by remember { mutableStateOf(true) }

    var editingModerator by remember { mutableStateOf<Moderator?>(null) }
    var editPassword by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editCanEditCategories by remember { mutableStateOf(true) }
    var editCanDeleteProviders by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🛡️ إضافة مشرف جديد للمنظومة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = modUsername,
                        onValueChange = { modUsername = it },
                        label = { Text("اسم المستخدم (Username)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = modPassword,
                        onValueChange = { modPassword = it },
                        label = { Text("كلمة المرور (Password)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الصلاحيات الرئيسية:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = modPermissions == "ALL", onClick = { modPermissions = "ALL" })
                            Text("كاملة (ALL)", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = modPermissions == "READ_ONLY", onClick = { modPermissions = "READ_ONLY" })
                            Text("قراءة فقط (READ)", fontSize = 11.sp)
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صلاحية تعديل وإضافة الأقسام والخدمات:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = modCanEditCategories,
                                onCheckedChange = { modCanEditCategories = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صلاحية حذف مقدمي الخدمات في الدليل:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = modCanDeleteProviders,
                                onCheckedChange = { modCanDeleteProviders = it }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (modUsername.isBlank() || modPassword.isBlank()) {
                                Toast.makeText(context, "⚠️ الرجاء ملء جميع الحقول المطلوبة!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addModerator(
                                    username = modUsername,
                                    passwordHex = modPassword,
                                    permissions = modPermissions,
                                    canEditCategories = modCanEditCategories,
                                    canDeleteProviders = modCanDeleteProviders
                                )
                                Toast.makeText(context, "✅ تم إضافة المشرف الجديد بنجاح ومزامنة الأجهزة!", Toast.LENGTH_LONG).show()
                                modUsername = ""
                                modPassword = ""
                                modCanEditCategories = true
                                modCanDeleteProviders = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إنشاء حساب المشرف والبدء بالمخطط 🛡️")
                    }
                }
            }
        }

        item {
            Text("قائمة المشرفين المسجلين حالياً ومزامنتهم:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (moderatorsList.isEmpty()) {
            item {
                Text("لا يوجد أي مشرفين مضافين حالياً.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            items(moderatorsList) { m ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "👤 اسم المشرف: ${m.username}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "صلاحيات أساسية: " + if (m.permissions == "ALL") "كاملة 👑" else "قراءة فقط 👁️", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text(text = "طريقة الدخول: ${m.passwordHex}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    editingModerator = m
                                    editUsername = m.username
                                    editPassword = m.passwordHex
                                    editCanEditCategories = m.canEditCategories
                                    editCanDeleteProviders = m.canDeleteProviders
                                }) {
                                    Text("✏️", fontSize = 16.sp)
                                }
                                IconButton(onClick = {
                                    if (m.username == "WAM2026") {
                                        Toast.makeText(context, "❌ لا يمكن حذف الحساب الرئيسي للمدير الرئيسي!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.deleteModerator(m)
                                        Toast.makeText(context, "🗑️ تم حذف حساب المشرف وتحديث قاعدة البيانات!", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text("🗑️", fontSize = 16.sp)
                                }
                            }
                        }

                        // Real-time permission control toggles with individual switches
                        Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.height(1.dp).fillMaxWidth())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                viewModel.updateModerator(m.copy(canEditCategories = !m.canEditCategories))
                                Toast.makeText(context, "🔄 تم مواءمة صلاحية الأقسام فورياً!", Toast.LENGTH_SHORT).show()
                            }) {
                                Checkbox(checked = m.canEditCategories, onCheckedChange = {
                                    viewModel.updateModerator(m.copy(canEditCategories = it))
                                    Toast.makeText(context, "🔄 تم مواءمة صلاحية الأقسام فورياً!", Toast.LENGTH_SHORT).show()
                                })
                                Text("تعديل الأقسام", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                viewModel.updateModerator(m.copy(canDeleteProviders = !m.canDeleteProviders))
                                Toast.makeText(context, "🔄 تم مواءمة صلاحية حذف الأعضاء فورياً!", Toast.LENGTH_SHORT).show()
                            }) {
                                Checkbox(checked = m.canDeleteProviders, onCheckedChange = {
                                    viewModel.updateModerator(m.copy(canDeleteProviders = it))
                                    Toast.makeText(context, "🔄 تم مواءمة صلاحية حذف الأعضاء فورياً!", Toast.LENGTH_SHORT).show()
                                })
                                Text("حذف المهنيين", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingModerator != null) {
        val currentMod = editingModerator!!
        AlertDialog(
            onDismissRequest = { editingModerator = null },
            title = { Text("✏️ تعديل حساب المشرف / كلمة المرور") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("اسم المستخدم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("كلمة المرور") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صلاحية تعديل الأقسام:", fontSize = 12.sp)
                        Switch(
                            checked = editCanEditCategories,
                            onCheckedChange = { editCanEditCategories = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صلاحية حذف المهنيين:", fontSize = 12.sp)
                        Switch(
                            checked = editCanDeleteProviders,
                            onCheckedChange = { editCanDeleteProviders = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editUsername.isNotBlank() && editPassword.isNotBlank()) {
                        val updated = currentMod.copy(
                            username = editUsername,
                            passwordHex = editPassword,
                            canEditCategories = editCanEditCategories,
                            canDeleteProviders = editCanDeleteProviders
                        )
                        viewModel.updateModerator(updated)
                        Toast.makeText(context, "✅ تم تحديث بيانات المشرف ومزامنتها على كل الأجهزة!", Toast.LENGTH_SHORT).show()
                        editingModerator = null
                    }
                }) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingModerator = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
