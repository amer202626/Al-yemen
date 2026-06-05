package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

// --- Helper for automatic image compression and resizing ---
fun compressAndResizeImage(bitmap: android.graphics.Bitmap): String {
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
    return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
}

data class SearchSuggestion(
    val text: String,
    val subtext: String,
    val icon: String,
    val type: String, // "NAME", "CATEGORY", "LOCATION"
    val value: String = "",
    val id: Int? = null
)

// --- Helper Composable for Base64 or Text Icons ---
@Composable
fun ProviderImage(imgStr: String, modifier: Modifier = Modifier, textStyle: TextStyle = LocalTextStyle.current) {
    if (imgStr.startsWith("base64,") || imgStr.length > 50) {
        val cleanStr = if (imgStr.contains("base64,")) imgStr.substringAfter("base64,") else imgStr
        val decodedBytes = try {
            android.util.Base64.decode(cleanStr, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
        if (decodedBytes != null) {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "صورة مقدم الخدمة",
                    modifier = modifier,
                    contentScale = ContentScale.Crop
                )
                return
            }
        }
    }
    // Fallback emoji
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val displayChar = if (imgStr.isNotBlank() && imgStr.length <= 4) imgStr else "⚙️"
        Text(displayChar, style = textStyle, textAlign = TextAlign.Center)
    }
}

// --- 1. HOME SCREEN ---
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    settings: AppSettings,
    categoriesList: List<Category>,
    providersList: List<ServiceProvider>,
    allBannersList: List<Banner>,
    onSelectProvider: (ServiceProvider) -> Unit
) {
    val context = LocalContext.current
    var showAdvancedFilters by remember { mutableStateOf(false) }

    // Text color mapping according to admin choices
    val txtColor = when (settings.fontColor) {
        "LIGHT_GOLD" -> LightGoldColor
        "VIBRANT_SILVER" -> VibrantSilverColor
        else -> BrightWhiteColor
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- AD BANNER SLIDER ---
        if (allBannersList.isNotEmpty()) {
            item {
                Text(
                    text = "⭐ عروض إعلانية مميزة",
                    style = MaterialTheme.typography.titleMedium,
                    color = txtColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                // Custom automatic carousel slider for multiple banners
                var activeIndex by remember { mutableStateOf(0) }
                val activeBanner = allBannersList.getOrNull(activeIndex % allBannersList.size)
                
                LaunchedEffect(activeBanner) {
                    val sec = activeBanner?.durationSeconds ?: 5
                    delay(sec * 1000L)
                    activeIndex++
                }

                if (activeBanner != null) {
                    val bannerHeight = when (activeBanner.size) {
                        "SMALL" -> 85.dp
                        "LARGE" -> 160.dp
                        else -> 120.dp
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight)
                            .clickable {
                                if (activeBanner.redirectUrl.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeBanner.redirectUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "الرابط غير صالح للتوجيه", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Render loading image or video placeholder
                            if (activeBanner.type == "VIDEO") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🎬 إعلان فيديو ترويجي نشط", color = Color.White, fontSize = 14.sp)
                                        Text(activeBanner.title, color = CharcoalGoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("اضغط للتشغيل أو التنقل", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            } else {
                                ProviderImage(
                                    imgStr = activeBanner.content,
                                    modifier = Modifier.fillMaxSize(),
                                    textStyle = TextStyle(fontSize = 32.sp)
                                )
                                // Title Overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = activeBanner.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SEARCH INPUT BOX & SUGGESTIONS ---
        item {
            val suggestions = remember(viewModel.searchQuery, providersList, categoriesList) {
                if (viewModel.searchQuery.isBlank()) emptyList<SearchSuggestion>()
                else {
                    val query = viewModel.searchQuery.trim().lowercase()
                    val list = mutableListOf<SearchSuggestion>()
                    // Filter matching name
                    val nameM = providersList.filter { it.name.lowercase().contains(query) }.take(3)
                    for (n in nameM) {
                        list.add(SearchSuggestion(n.name, "اسم مهني متطابق", "👤", "NAME", value = n.name))
                    }
                    // Filter matching category
                    val catM = categoriesList.filter { it.nameAr.lowercase().contains(query) || it.nameEn.lowercase().contains(query) }.take(3)
                    for (c in catM) {
                        list.add(SearchSuggestion(c.nameAr, "تخصص / مهنة", "📂", "CATEGORY", id = c.id))
                    }
                    // Filter matching location
                    val locs = providersList.map { it.neighborhood.trim() }.filter { it.isNotBlank() }.distinct()
                    val locM = locs.filter { it.lowercase().contains(query) }.take(3)
                    for (l in locM) {
                        list.add(SearchSuggestion(l, "حي / موقع جغرافي", "📍", "LOCATION", value = l))
                    }
                    list
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔎 بحث متقدم بالاسم، التخصص، أو الحي",
                        color = txtColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثال: سباك، حدة، ماهر...") },
                        singleLine = true,
                        leadingIcon = { Text("🔎", modifier = Modifier.padding(horizontal = 6.dp)) },
                        trailingIcon = {
                            if (viewModel.searchQuery.isNotBlank() || viewModel.filterCategoryId != null || viewModel.filterRegion != null) {
                                TextButton(onClick = {
                                    viewModel.searchQuery = ""
                                    viewModel.filterCategoryId = null
                                    viewModel.filterRegion = null
                                }) {
                                    Text("تصفير ✖", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )

                    // Autocomplete Suggestions Box
                    if (suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "💡 مقترحات مطابقة فورياً:",
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                for (s in suggestions) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                when (s.type) {
                                                    "NAME" -> {
                                                        viewModel.searchQuery = s.value
                                                    }

                                                    "CATEGORY" -> {
                                                        viewModel.filterCategoryId = s.id
                                                    }

                                                    "LOCATION" -> {
                                                        viewModel.filterRegion = s.value
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(s.icon, modifier = Modifier.padding(end = 8.dp))
                                        Column {
                                            Text(s.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(s.subtext, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { showAdvancedFilters = !showAdvancedFilters }) {
                        Text(if (showAdvancedFilters) "إخفاء الفلاتر الإضافية ▴" else "تصفية مخصصة حسب الموقع والمنطقة ▾")
                    }

                    if (showAdvancedFilters) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val regions = providersList.map { it.neighborhood }.filter { it.isNotBlank() }.distinct()
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(regions) { r ->
                                FilterChip(
                                    selected = viewModel.filterRegion == r,
                                    onClick = {
                                        viewModel.filterRegion = if (viewModel.filterRegion == r) null else r
                                    },
                                    label = { Text("📍 $r") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- CATEGORIES LIST ---
        item {
            Text(
                text = "📂 التخصصات المهنية المتوفرة",
                style = MaterialTheme.typography.titleMedium,
                color = txtColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    FilterChip(
                        selected = viewModel.filterCategoryId == null,
                        onClick = { viewModel.filterCategoryId = null },
                        label = { Text("الكل 🌐") }
                    )
                }
                items(categoriesList.filter { it.parentId == null }) { cat ->
                    FilterChip(
                        selected = viewModel.filterCategoryId == cat.id,
                        onClick = { viewModel.filterCategoryId = cat.id },
                        label = { Text("${cat.imageBase64} ${cat.nameAr}") }
                    )
                }
            }
        }

        // --- SERVICES PROVIDERS RESULTS ---
        val filteredList = providersList.filter { p ->
            val mainCat = categoriesList.find { it.id == p.mainCategoryId }
            val subCat = categoriesList.find { it.id == p.subCategoryId }
            val matchesQuery = viewModel.searchQuery.isBlank() ||
                    p.name.contains(viewModel.searchQuery, ignoreCase = true) ||
                    p.neighborhood.contains(viewModel.searchQuery, ignoreCase = true) ||
                    p.workAddress.contains(viewModel.searchQuery, ignoreCase = true) ||
                    (mainCat != null && (mainCat.nameAr.contains(viewModel.searchQuery, ignoreCase = true) || mainCat.nameEn.contains(viewModel.searchQuery, ignoreCase = true))) ||
                    (subCat != null && (subCat.nameAr.contains(viewModel.searchQuery, ignoreCase = true) || subCat.nameEn.contains(viewModel.searchQuery, ignoreCase = true)))

            val matchesCategory = viewModel.filterCategoryId == null || p.mainCategoryId == viewModel.filterCategoryId || p.subCategoryId == viewModel.filterCategoryId
            val matchesRegion = viewModel.filterRegion == null || p.neighborhood == viewModel.filterRegion

            matchesQuery && matchesCategory && matchesRegion && !p.isPending && !p.isBlocked
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🤷‍♂️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا توجد نتائج مطابقة لبحثك في اليمن حالياً.",
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredList) { p ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProvider(p) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            ProviderImage(p.profileImageBase64, modifier = Modifier.fillMaxSize(), textStyle = TextStyle(fontSize = 28.sp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = p.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = txtColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (p.isPremium) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("⭐ مميز", fontSize = 9.sp, color = CharcoalGoldPrimary, fontWeight = FontWeight.Black, modifier = Modifier.background(Color.Black).padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                text = "📍 ${p.neighborhood}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            val catName = categoriesList.find { it.id == p.mainCategoryId }?.nameAr ?: "تخصص عام"
                            Text(
                                text = "📂 التخصص: $catName",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⭐", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(p.rating.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("(${p.ratingCount} تقييم)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// --- 2. REGISTRATION FORM (👤) ---
@Composable
fun RegisterScreen(viewModel: AppViewModel, settings: AppSettings, categoriesList: List<Category>) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedMainCategory by remember { mutableStateOf<Category?>(null) }
    var profileImage by remember { mutableStateOf("") }
    var idCardImage by remember { mutableStateOf("") }

    var expandedDropdown by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val base64 = compressAndResizeImage(bitmap)
            profileImage = base64
            Toast.makeText(context, "📸 تم التقاط ومسح الصورة الشخصية وتصغيرها آلياً!", Toast.LENGTH_SHORT).show()
        }
    }

    val idLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val base64 = compressAndResizeImage(bitmap)
            idCardImage = base64
            Toast.makeText(context, "📸 تم التقاط وتصغير صورة الهوية الشخصية!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "✍️ تسجيل مهني جديد بالمنصة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "يرجى تعبئة جميع الحقول وإرفاق صور واضحة لضمان موافقة المشرفين السريعة على طلبك.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل ثنائياً أو ثلاثياً") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (الواتساب أو الإتصال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = neighborhood,
                    onValueChange = { neighborhood = it },
                    label = { Text("الحي أو المديرية (مثال: السبعين، حدة...)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("عنوان العمل التفصيلي") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedMainCategory?.let { "${it.imageBase64} ${it.nameAr}" } ?: "اختر التخصص المهني الأساسي...")
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        for (cat in categoriesList.filter { it.parentId == null }) {
                            DropdownMenuItem(
                                text = { Text("${cat.imageBase64} ${cat.nameAr}") },
                                onClick = {
                                    selectedMainCategory = cat
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Captured Images section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📷 الصور المرفقة للتوثيق (صورة بروفايل وبطاقة الهوية):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Button(onClick = { cameraLauncher.launch(null) }) {
                                Text("التقاط البروفايل")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (profileImage.isNotBlank()) {
                                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))) {
                                    ProviderImage(profileImage, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Button(onClick = { idLauncher.launch(null) }) {
                                Text("التقاط الهوية")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (idCardImage.isNotBlank()) {
                                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))) {
                                    ProviderImage(idCardImage, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }

        // Submission controller
        item {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || selectedMainCategory == null) {
                        Toast.makeText(context, "الرجاء كتابة الاسم، الهاتف والتخصص أولاً!", Toast.LENGTH_SHORT).show()
                    } else {
                        val p = ServiceProvider(
                            name = name,
                            phoneNumber = phone,
                            neighborhood = neighborhood,
                            workAddress = address,
                            mainCategoryId = selectedMainCategory!!.id,
                            isPending = true,
                            profileImageBase64 = profileImage.ifBlank { "👨‍🔧" },
                            idCardImageBase64 = idCardImage
                        )
                        viewModel.registerProvider(p)
                        Toast.makeText(context, "✅ تم إرسال طلبك بنجاح! سينظر فيه المشرفون فوراً.", Toast.LENGTH_LONG).show()
                        name = ""
                        phone = ""
                        neighborhood = ""
                        address = ""
                        selectedMainCategory = null
                        profileImage = ""
                        idCardImage = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("إرسال طلب الانضمام للدليل المستمر 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- 3. PROVIDER DETAILS SCREEN & GOOGLE MAPS & CHAT LINK ---
@Composable
fun ProviderDetailScreen(
    p: ServiceProvider,
    viewModel: AppViewModel,
    settings: AppSettings,
    categoriesList: List<Category>,
    onBack: () -> Unit,
    onOpenChatWithProvider: (ServiceProvider) -> Unit
) {
    val context = LocalContext.current
    var complainText by remember { mutableStateOf("") }
    var complainPhone by remember { mutableStateOf("") }
    var ratingChosen by remember { mutableStateOf(5) }
    var showComplainDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Button & Name header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("«", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "بروفايل مقدم الخدمة",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }

        // Profile Picture Card details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        ProviderImage(p.profileImageBase64, modifier = Modifier.fillMaxSize(), textStyle = TextStyle(fontSize = 42.sp))
                    }

                    Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ ${p.rating}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalGoldPrimary)
                        Text(" (${p.ratingCount} تقييم في المنصة)", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
                    }

                    val cat = categoriesList.find { it.id == p.mainCategoryId }?.nameAr ?: "صيانة عامة"
                    Text("التخصص: $cat", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Contacts, Address & Call link
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📍 السكن: ${p.neighborhood}", fontSize = 13.sp)
                    Text("🏠 العنوان التفصيلي: ${p.workAddress}", fontSize = 13.sp)
                    Text("📞 الهاتف: ${p.phoneNumber}", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Regular Phone Call with logging
                        Button(
                            onClick = {
                                viewModel.incrementCallCounter()
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${p.phoneNumber}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📱 اتصال هاتفي")
                        }

                        // WhatsApp Link directly
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${p.phoneNumber}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Text("💬 واتساب مباشر")
                        }
                    }

                    // --- CHAT WITH USERS INSIDE APP ---
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Verify if chat is globally or per-provider disabled
                    val disabledIdsList = settings.disabledChatProviderIds.split(",").map { it.trim() }
                    val isChatOffline = !settings.isChatEnabledGlobal || disabledIdsList.contains(p.id.toString())

                    if (isChatOffline) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("🛑 خدمة الدردشة الفورية معطلة مؤقتاً:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                                Text(settings.chatDisabledMessage, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    } else {
                        Button(
                            onClick = { onOpenChatWithProvider(p) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("💬 محادثة فورية مدمجة بالتطبيق الآمن", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- MAPS INTEGRATION SECTION ---
        if (settings.isMapEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📍 خريطة توجيه موقع مقدم الخدمة الجغرافي:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        
                        // Beautiful fully custom canvas depiction of a clean map outline in Yemen
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2B3A4A))
                        ) {
                            // Draw simulated topological Yemen Grid
                            val w = size.width
                            val h = size.height
                            // Draw grid lines
                            for (i in 0..10) {
                                val x = w * (i.toFloat() / 10f)
                                drawLine(Color.Gray.copy(alpha = 0.15f), start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, h))
                                val y = h * (i.toFloat() / 10f)
                                drawLine(Color.Gray.copy(alpha = 0.15f), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(w, y))
                            }
                            // Styled compass node
                            drawCircle(Color(0xFF10B981).copy(alpha = 0.4f), radius = 24.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w / 2, h / 2))
                            drawCircle(Color(0xFF10B981), radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w / 2, h / 2))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الإحداثيات التقريبية: رصد حي السكن", fontSize = 11.sp, color = Color.Gray)
                            Button(
                                onClick = {
                                    // Fire ACTION_VIEW directly to Google Maps navigation route directions
                                    val geoUri = "geo:${p.latitude},${p.longitude}?q=${Uri.encode(p.name)}"
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        // fallback to normal browser url directions
                                        val webUri = "https://www.google.com/maps/dir/?api=1&destination=${p.latitude},${p.longitude}"
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri))
                                        context.startActivity(webIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("🌐 زر الاتجاهات (فتح الخريطة)")
                            }
                        }
                    }
                }
            }
        }

        // Rating & Reporting feedback action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val updated = p.copy(
                            rating = ((p.rating * p.ratingCount) + ratingChosen) / (p.ratingCount + 1),
                            ratingCount = p.ratingCount + 1
                        )
                        viewModel.updateProvider(updated)
                        Toast.makeText(context, "✅ تم تسجيل تقييمك ($ratingChosen نجوم) بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("⭐ قيّم بـ $ratingChosen نجوم")
                }

                Slider(
                    value = ratingChosen.toFloat(),
                    onValueChange = { ratingChosen = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { showComplainDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⚠️ بلاغ")
                }
            }
        }
    }

    // Complain popup alert panel
    if (showComplainDialog) {
        AlertDialog(
            onDismissRequest = { showComplainDialog = false },
            title = { Text("تقديم بلاغ ضد مقدم الخدمة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرجاء ذكر سبب مخالفته للمعايير أو سبب الإشعار:")
                    OutlinedTextField(
                        value = complainPhone,
                        onValueChange = { complainPhone = it },
                        label = { Text("رقم هاتفك للتواصل") }
                    )
                    OutlinedTextField(
                        value = complainText,
                        onValueChange = { complainText = it },
                        label = { Text("تفاصيل الشكوى") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (complainText.isNotBlank()) {
                        viewModel.addComplaint(complainPhone, p.id, p.name, complainText)
                        Toast.makeText(context, "✅ تم إرسال البلاغ لمراجعة الإدارة.", Toast.LENGTH_SHORT).show()
                        showComplainDialog = false
                    }
                }) {
                    Text("إرسال الشكوى")
                }
            },
            dismissButton = {
                TextButton(onClick = { showComplainDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// --- 4. INTEGRATED REAL-TIME CHAT SCREEN ---
@Composable
fun LiveChatScreen(viewModel: AppViewModel, receiver: ServiceProvider, onBack: () -> Unit) {
    val messages by viewModel.chatMessages.collectAsState()
    var inputMsg by remember { mutableStateOf("") }
    val listState = rememberScrollState()

    // Filter messages for current user/receiver dialogue channel
    val activeChatList = messages.filter {
        (it.senderId == "USER" && it.receiverId == "PROVIDER_${receiver.id}") ||
                (it.senderId == "PROVIDER_${receiver.id}" && it.receiverId == "USER")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("«", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(36.dp).clip(CircleShape)) {
                ProviderImage(receiver.profileImageBase64, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(receiver.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text("محادثة فورية مباشرة آمنة تدار بالكامل محلياً", fontSize = 10.sp, color = Color.Gray)
            }
        }

        // Dialogue Box Scroll
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeChatList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("👋 ابدأ المحادثة برحابة وسرعة الآن!", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 40.dp))
                    }
                } else {
                    for (m in activeChatList) {
                        val isUser = m.senderId == "USER"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 0.dp,
                                            bottomEnd = if (isUser) 0.dp else 12.dp
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .widthIn(max = 240.dp)
                            ) {
                                Text(
                                    text = m.message,
                                    color = if (isUser) Color.Black else Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input keyboard bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputMsg,
                onValueChange = { inputMsg = it },
                placeholder = { Text("اكتب رسالتك لـ ${receiver.name}...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (inputMsg.isNotBlank()) {
                        viewModel.sendChatMessage("USER", "PROVIDER_${receiver.id}", inputMsg.trim())
                        inputMsg = ""
                        // Trigger simulated fast provider answers
                        val triggerText = when (activeChatList.size) {
                            0 -> "أهلاً بك يا غالي! تفضل، كيف يمكنني خدمتك في تخصصي اليوم؟"
                            1 -> "سأتواصل معك فوراً، يمكنك أيضاً الاتصال بي على رقمي للتفاهم السريع."
                            else -> "تمام جداً يسعدني ويشرفني خدمتك!"
                        }
                        // Delayed response simulation
                        try {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                viewModel.sendChatMessage("PROVIDER_${receiver.id}", "USER", triggerText)
                            }, 1200)
                        } catch (e: Exception) {
                            // Handler backup
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("إرسال")
            }
        }
    }
}
