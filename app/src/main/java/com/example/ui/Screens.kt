package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult

data class SearchSuggestion(
    val text: String,
    val subtext: String,
    val icon: String,
    val type: String, // "NAME", "CATEGORY", "LOCATION"
    val value: String = "",
    val id: Int? = null
)

// --- 1. HOME SCREEN ---
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    settings: AppSettings,
    categoriesList: List<Category>,
    providersList: List<ServiceProvider>,
    bannersList: List<Banner>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Banner timer index state
    var currentBannerIndex by remember { mutableStateOf(0) }
    
    // Local filters drawer state
    var showAdvancedFilters by remember { mutableStateOf(false) }

    // Start auto banner transition
    if (bannersList.isNotEmpty()) {
        LaunchedEffect(key1 = currentBannerIndex, key2 = bannersList.size) {
            val currentBanner = bannersList.getOrNull(currentBannerIndex % bannersList.size)
            val duration = (currentBanner?.durationSeconds ?: 5) * 1000L
            delay(duration)
            currentBannerIndex = (currentBannerIndex + 1) % bannersList.size
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Maintenance Banner Warning (Owner can see this) ---
        if (settings.isMaintenanceMode) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🛠️ وضع الصيانة نشط حالياً",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "تم إيقاف التسجيلات الجديدة مؤقتاً بواسطة الإدارة.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- 1. ACTIVE BANNERS SECTION ---
        if (bannersList.isNotEmpty()) {
            item {
                val banner = bannersList[currentBannerIndex % bannersList.size]
                val cardHeight = when (banner.bannerSize.uppercase()) {
                    "SMALL" -> 80.dp
                    "LARGE" -> 180.dp
                    else -> 120.dp
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .clickable {
                            if (banner.targetUrl.isNotBlank()) {
                                Toast
                                    .makeText(
                                        context,
                                        "🔗 جاري التوجيه إلى الرابط: ${banner.targetUrl}",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (banner.type == "IMAGE" && banner.content.length > 50) {
                            // Render Base64 image
                            ProviderImage(banner.content, modifier = Modifier.fillMaxSize())
                            // Tint/overlay layer
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            )
                        } else if (banner.type == "VIDEO") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray)
                            )
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("▶️", fontSize = 28.sp)
                                Text("مقطع فيديو ترويجي نشط 🎬", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (banner.type != "VIDEO") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = if (banner.type == "IMAGE") "🖼️ إعلان مصور" else "📢 إعلان ممول",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (banner.type == "IMAGE") Color.White else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (banner.type == "IMAGE" && banner.content.length > 50) "اضغط للتوجيه والاطلاع بخصومات حصرية" else banner.content,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. SEARCH BAR & QUICK FILTERS ACCORDION ---
        item {
            val suggestions = remember(viewModel.searchQuery, providersList, categoriesList) {
                if (viewModel.searchQuery.isBlank()) emptyList<SearchSuggestion>()
                else {
                    val query = viewModel.searchQuery.trim().lowercase()
                    val list = mutableListOf<SearchSuggestion>()
                    
                    // 1. Matches by Provider Name (limit to 3)
                    val nameMatches = providersList.filter { it.name.lowercase().contains(query) }.take(3)
                    for (m in nameMatches) {
                        list.add(SearchSuggestion(text = m.name, subtext = "اسم مهني", icon = "👤", type = "NAME", value = m.name))
                    }
                    
                    // 2. Matches by Category / Specialization (limit to 3)
                    val catMatches = categoriesList.filter { it.nameAr.lowercase().contains(query) || it.nameEn.lowercase().contains(query) }.take(3)
                    for (c in catMatches) {
                        list.add(SearchSuggestion(text = c.nameAr, subtext = "تخصص / قسم", icon = "📂", type = "CATEGORY", id = c.id))
                    }
                    
                    // 3. Matches by Location / Geographic (limit to 3)
                    val allNeighborhoods = providersList.map { it.neighborhood.trim() }.filter { it.isNotBlank() }.distinct()
                    val locMatches = allNeighborhoods.filter { it.lowercase().contains(query) }.take(3)
                    for (l in locMatches) {
                        list.add(SearchSuggestion(text = l, subtext = "موقع جغرافي / منطقة", icon = "📍", type = "LOCATION", value = l))
                    }
                    
                    list
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GrayBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.searchQuery = it },
                            placeholder = { Text("🔎 ابحث بالاسم، التخصص أو المحل...", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = parseHexColor(settings.fontColor, Color.White),
                                unfocusedTextColor = parseHexColor(settings.fontColor, Color.White)
                            )
                        )

                        // Voice Search Toggle
                        IconButton(
                            onClick = {
                                viewModel.isVoiceSearchActive = !viewModel.isVoiceSearchActive
                                if (viewModel.isVoiceSearchActive) {
                                    viewModel.searchQuery = "ماهر محمد"
                                    Toast.makeText(context, "🎤 تم تمثيل البحث الصوتي: 'ماهر محمد'", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.searchQuery = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (viewModel.isVoiceSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(text = "🎙️", fontSize = 20.sp)
                        }

                        // Advanced Filter Toggle
                        IconButton(
                            onClick = { showAdvancedFilters = !showAdvancedFilters }
                        ) {
                            Text(text = if (showAdvancedFilters) "🔼" else "🔽", fontSize = 20.sp)
                        }
                    }

                    // Interactive search suggestions dropdown box
                    if (suggestions.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Text(
                                    text = "💡 نتائج مقترحة متطابقة فورياً:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                for (sug in suggestions) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                when (sug.type) {
                                                    "NAME" -> {
                                                        viewModel.searchQuery = sug.value
                                                    }
                                                    "CATEGORY" -> {
                                                        viewModel.filterCategoryId = sug.id
                                                        viewModel.searchQuery = "" // clear to open category list
                                                        Toast.makeText(context, "📂 تصفية حسب تخصص: ${sug.text}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    "LOCATION" -> {
                                                        viewModel.filterRegion = sug.value
                                                        viewModel.searchQuery = "" // clear to view active region
                                                        Toast.makeText(context, "📍 تصفية حسب موقع: ${sug.text}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(sug.icon, fontSize = 16.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sug.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(sug.subtext, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text("⏎ تطبيق", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Light)
                                    }
                                }
                            }
                        }
                    }

                    // Collapsible Advanced Filter panel
                    AnimatedVisibility(visible = showAdvancedFilters) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Divider(color = GrayBorder)
                            
                            // Region filter list
                            Text("📍 تصفية حسب المنطقة الحالية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                val locations = listOf("صنعاء القديمة", "جولة الرويشان", "كريتر", "حي القطيع", "المسبح", "شارع جمال", "الحديدة")
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (viewModel.filterRegion == "") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.filterRegion = "" }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("الكل", color = Color.White, fontSize = 12.sp)
                                }
                                for (loc in locations) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (viewModel.filterRegion == loc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewModel.filterRegion = loc }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(loc, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Star rating filter
                            Text("⭐ الحد الأدنى للتقييم:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (r in 0..5) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 50.dp, height = 32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (viewModel.filterRating == r) Color(0xFFFFCC00) else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewModel.filterRating = r }
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(if (r == 0) "الكل" else "★ $r", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Radius Distance filter
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🌐 نطاق تصفية الخريطة: ${viewModel.searchRadiusKm.toInt()} كم", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("الأقصى: ${settings.isRadiusSearchMaxLimited} كم", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = viewModel.searchRadiusKm,
                                onValueChange = { viewModel.searchRadiusKm = it },
                                valueRange = 1f..settings.isRadiusSearchMaxLimited.toFloat()
                            )

                            // Clear filters button
                            Button(
                                onClick = {
                                    viewModel.searchQuery = ""
                                    viewModel.filterRegion = ""
                                    viewModel.filterCategoryId = null
                                    viewModel.filterRating = 0
                                    viewModel.searchRadiusKm = 10f
                                    viewModel.isVoiceSearchActive = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🧹 إعادة ضبط كافة الفلاتر")
                            }
                        }
                    }
                }
            }
        }

        // --- 3. CATEGORIES HORIZONTAL GRID ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "📂 تصفح حسب مهنة الخدمة الرئيسي:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Render category options
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    // All category filter option
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.filterCategoryId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .size(width = 110.dp, height = 75.dp)
                            .clickable { viewModel.filterCategoryId = null }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("📂", fontSize = 24.sp)
                            Text("كل المهن", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    for (cat in categoriesList) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (viewModel.filterCategoryId == cat.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .size(width = 115.dp, height = 75.dp)
                                .clickable { viewModel.filterCategoryId = cat.id }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            ) {
                                Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                                    ProviderImage(
                                        imgStr = cat.imageBase64,
                                        modifier = Modifier.fillMaxSize(),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 22.sp)
                                    )
                                }
                                Text(
                                    text = if (settings.activeLanguage == "AR") cat.nameAr else cat.nameEn,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. RECOMMENDED SECTION (موصى بهم) ---
        val recommendedList = providersList.filter { it.isRecommended }
        if (recommendedList.isNotEmpty() && viewModel.filterCategoryId == null && viewModel.searchQuery.isBlank()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("⭐ متميز وموصى به من المالك:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFFCC00))
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        for (p in recommendedList) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFCC00)),
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable {
                                        viewModel.selectedProviderId = p.id
                                        viewModel.currentScreen = "PROVIDER_DETAIL"
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ProviderImage(p.profileImageBase64, modifier = Modifier.fillMaxSize(), textStyle = LocalTextStyle.current.copy(fontSize = 18.sp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text(p.neighborhood, fontSize = 11.sp, maxLines = 1)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("★ ${p.averageRating} ", color = Color(0xFFFFD700), fontSize = 11.sp)
                                            if (p.isVerified) {
                                                Text(" ✔️ موثق ", color = BlueVerified, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. RESULTS LISTING (ORDERED AND FILTERED) ---
        item {
            Text(
                text = "📁 قائمة الوجوه والكوادر المهنية المتوفرة:",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (providersList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭 لا يوجد أي مقدمي خدمات مسجلين بعد.", textAlign = TextAlign.Center)
                        Text("استخدم 👤 لتقديم أول طلب انضمام!", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            items(providersList) { p ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Data Saving Mode effect (disable dynamic scaling, lower transparency layers)
                            alpha = if (settings.isDataSavingMode) 0.85f else 1.0f
                        }
                        .clickable {
                            viewModel.selectedProviderId = p.id
                            viewModel.currentScreen = "PROVIDER_DETAIL"
                        },
                    border = BorderStroke(
                        1.dp, 
                        if (p.isPinned) Color(0xFFFFCC00) else if (p.hasMonthlySubscription) MaterialTheme.colorScheme.primary else GrayBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Profile picture or placeholder emoji
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            ProviderImage(p.profileImageBase64, modifier = Modifier.fillMaxSize(), textStyle = LocalTextStyle.current.copy(fontSize = 24.sp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = p.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (p.isVerified) {
                                    Text("✔️", color = BlueVerified, fontSize = 12.sp) // Verified blue checkmark
                                }
                                if (p.isPinned) {
                                    Text("📌 تثبيت", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            val categoryName = categoriesList.find { it.id == p.mainCategoryId }?.nameAr ?: "تخصص عام"
                            Text(text = "التخصص: $categoryName", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = "العنوان: ${p.neighborhood} - ${p.address}", fontSize = 11.sp, color = Color.Gray)
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("★ ${p.averageRating}", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("(${p.ratingCount} تقييم)", color = Color.Gray, fontSize = 10.sp)
                                
                                if (p.hasMonthlySubscription) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFE65100))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("عضوية ذهبية ⭐", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderImage(imgStr: String, modifier: Modifier = Modifier, textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current) {
    val bitmap = remember(imgStr) {
        if (imgStr.length > 50) { // Base64 data usually is quite long
            try {
                val cleanStr = if (imgStr.contains("base64,")) imgStr.substringAfter("base64,") else imgStr
                val decodedBytes = android.util.Base64.decode(cleanStr, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Profile Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = if (imgStr.isBlank()) "👨‍💼" else imgStr,
                style = textStyle,
                textAlign = TextAlign.Center
            )
        }
    }
}

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
    // Compress with high quality but low size (65-75% is optimal)
    resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
    val byteArray = outputStream.toByteArray()
    return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
}

// --- 2. REGISTRATION FORM (👤) ---
@Composable
fun RegisterScreen(viewModel: AppViewModel, settings: AppSettings, categoriesList: List<Category>) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<Int?>(categoriesList.firstOrNull { it.parentId == null }?.id) }
    var selectedSubCatId by remember { mutableStateOf<Int?>(null) }
    var locationGps by remember { mutableStateOf("") }
    
    // Captured images representation as simulation strings
    var profileImage by remember { mutableStateOf("👨‍🔧") }
    var idCardImage by remember { mutableStateOf("📝") }
    
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var activeImageField by remember { mutableStateOf("PROFILE") } // "PROFILE" or "IDCARD"
    
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val base64 = compressAndResizeImage(bitmap)
            if (activeImageField == "PROFILE") {
                profileImage = base64
            } else {
                idCardImage = base64
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val base64 = compressAndResizeImage(bitmap)
                    if (activeImageField == "PROFILE") {
                        profileImage = base64
                    } else {
                        idCardImage = base64
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تحميل الصورة من المعرض", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (settings.isMaintenanceMode) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🛠️ نعتذر منكم !", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Text(
                    text = "التطبيق حالياً في وضع صيانة لرفع الخوادم وتعديل لافتات الإعلان، يرجى المحاولة لاحقاً.",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "👤 تقديم استمارة الانضمام كمهني:",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "أدخل بياناتك وسيتم تفعيل حسابك مباشرة بعد مراجعة المشرف الرئيسي.",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Haza list fields
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name
                Text("الاسم الكامل الثلاثي (إجباري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("مثال: ماهر محمد طاهر") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parseHexColor(settings.fontColor, Color.White),
                        unfocusedTextColor = parseHexColor(settings.fontColor, Color.White)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Mobile
                Text("رقم الهاتف / فعال واتساب (إجباري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("مثال: 777644670") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parseHexColor(settings.fontColor, Color.White),
                        unfocusedTextColor = parseHexColor(settings.fontColor, Color.White)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Main Category
                var mainCategoryExpanded by remember { mutableStateOf(false) }
                val mainCategory = categoriesList.find { it.id == selectedCatId }
                
                Text("القسم المهني الرئيسي (إجباري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { mainCategoryExpanded = true }
                        .padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(mainCategory?.let { "${it.imageBase64} ${it.nameAr}" } ?: "اختر القسم المهني الرئيسي...", fontWeight = FontWeight.Bold)
                        Text("▼")
                    }
                    DropdownMenu(
                        expanded = mainCategoryExpanded,
                        onDismissRequest = { mainCategoryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        categoriesList.filter { it.parentId == null }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.imageBase64}  ${cat.nameAr}", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedCatId = cat.id
                                    selectedSubCatId = null
                                    mainCategoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sub Category / Specific service type
                val subCategories = categoriesList.filter { it.parentId == selectedCatId }
                var subCategoryExpanded by remember { mutableStateOf(false) }
                val subCategory = subCategories.find { it.id == selectedSubCatId } ?: subCategories.firstOrNull()
                
                Text("نوع الخدمة بالتحديد / قسم فرعي (إجباري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { subCategoryExpanded = true }
                        .padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(subCategory?.let { "${it.imageBase64} ${it.nameAr}" } ?: "اختر الخدمة بالتفصيل...", fontWeight = FontWeight.Bold)
                        Text("▼")
                    }
                    DropdownMenu(
                        expanded = subCategoryExpanded,
                        onDismissRequest = { subCategoryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        if (subCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("تأكيد القسم العام التلقائي") },
                                onClick = { subCategoryExpanded = false }
                            )
                        } else {
                            subCategories.forEach { subCat ->
                                DropdownMenuItem(
                                    text = { Text("${subCat.imageBase64}  ${subCat.nameAr}", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedSubCatId = subCat.id
                                        subCategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Work Address
                Text("مكان وعنوان مركز/محل العمل الحالي (إجباري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("مثال: صنعاء - شارع الجزائر جوار مركز الاتصالات") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parseHexColor(settings.fontColor, Color.White),
                        unfocusedTextColor = parseHexColor(settings.fontColor, Color.White)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Neighborhood
                Text("منطقة الدائرة السكنية الحالية (إجباري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = neighborhood,
                    onValueChange = { neighborhood = it },
                    placeholder = { Text("مثال: صنعاء القديمة / حي الروضة") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parseHexColor(settings.fontColor, Color.White),
                        unfocusedTextColor = parseHexColor(settings.fontColor, Color.White)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // GPS Location
                Text("إحداثيات وموقع الخريطة (اختياري / انقر للتوليد التلقائي):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = locationGps,
                    onValueChange = { locationGps = it },
                    placeholder = { Text("مثال: 15.3526, 44.2074") },
                    trailingIcon = {
                        IconButton(onClick = { locationGps = "15.34${(10..99).random()}, 44.20${(10..99).random()}" }) {
                            Text("📍")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parseHexColor(settings.fontColor, Color.White),
                        unfocusedTextColor = parseHexColor(settings.fontColor, Color.White)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Pick profile image
                Text("تحميل الصورة الشخصية السيلفي (إجباري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        ProviderImage(profileImage, modifier = Modifier.fillMaxSize(), textStyle = LocalTextStyle.current.copy(fontSize = 36.sp))
                    }
                    Button(
                        onClick = {
                            activeImageField = "PROFILE"
                            showImagePickerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("📸 اخر صورة أو اختيار من البوم الكاميرا")
                    }
                }

                // Pick identity card (optional)
                Text("تحميل صورة بطاقة الهوية الوطنية (اختياري):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        ProviderImage(idCardImage, modifier = Modifier.fillMaxSize(), textStyle = LocalTextStyle.current.copy(fontSize = 32.sp))
                    }
                    Button(
                        onClick = {
                            activeImageField = "IDCARD"
                            showImagePickerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("📁 تحميل الهوية الشخصية")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit button
                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank() || address.isBlank() || neighborhood.isBlank() || selectedCatId == null) {
                            Toast.makeText(context, "⚠️ الرجاء ملء كافة الحقول الإجبارية أولاً!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.registerPendingProvider(
                                name = name,
                                phone = phone,
                                catId = selectedSubCatId ?: selectedCatId!!,
                                address = address,
                                neighborhood = neighborhood,
                                profileImgBase64 = profileImage,
                                idCardImgBase64 = idCardImage
                            )
                            Toast.makeText(context, "🌟 تم تقديم طلب الانضمام للمراجعة الفورية بنجاح!", Toast.LENGTH_LONG).show()
                            viewModel.currentScreen = "HOME"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تقديم طلب الانضمام للمراجعة الفورية 🚀", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // Interactive Predefined Image Selection Picker (100% stable Camera & Gallery simulation)
    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text(if (activeImageField == "PROFILE") "📸 اختر الصورة الشخصية " else "📁 اختر صورة الهوية والمستندات") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر طريقة تحميل الصورة من هاتفك مباشرة للتحقق أو اختر تفعيل رمز تجريبي سريع:", fontSize = 12.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                cameraLauncher.launch(null)
                                showImagePickerDialog = false
                            }
                        ) {
                            Text("📷 الكاميرا حياً")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                galleryLauncher.launch("image/*")
                                showImagePickerDialog = false
                            }
                        ) {
                            Text("🖼️ الاستوديو")
                        }
                    }

                    Divider()
                    Text("أو انقر على رمز مقترح وسنتعامل معه فوراً:", fontSize = 11.sp, color = Color.Gray)

                    val picks = if (activeImageField == "PROFILE") {
                        listOf("👨‍🔧", "🍲", "📱", "🧵", "👩‍🎓", "🚗", "🏠", "💼")
                    } else {
                        listOf("📝", "🆔", "💳", "📄", "📜")
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        for (emoji in picks) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (activeImageField == "PROFILE") {
                                            profileImage = emoji
                                        } else {
                                            idCardImage = emoji
                                        }
                                        showImagePickerDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 32.sp)
                            }
                        }
                    }
                    
                    Text("أو ارفق رابط صورة كاربونية مباشرة:", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = if (activeImageField == "PROFILE") profileImage else idCardImage,
                        onValueChange = {
                            if (activeImageField == "PROFILE") profileImage = it else idCardImage = it
                        },
                        label = { Text("رمز أو رابط الصورة الكاربونية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showImagePickerDialog = false }) {
                    Text("حفظ الاختيار")
                }
            }
        )
    }
}

// --- 3. LOGIN SCREEN ---
@Composable
fun LoginScreen(viewModel: AppViewModel, settings: AppSettings) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, GrayBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔐 بوابة الدخول (المشرفين والمالك)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("اسم المستخدم:") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parseHexColor(settings.fontColor, Color.White),
                        unfocusedTextColor = parseHexColor(settings.fontColor, Color.White)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("كلمة المرور:") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parseHexColor(settings.fontColor, Color.White),
                        unfocusedTextColor = parseHexColor(settings.fontColor, Color.White)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Remember me checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = viewModel.saveLoginState,
                            onCheckedChange = { viewModel.toggleSaveLogin(it) }
                        )
                        Text("حفظ تسجيل الدخول وتذكر عودتي", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        val success = viewModel.login(user, pass)
                        if (!success) {
                            Toast.makeText(context, "❌ كلمة المرور أو اسم المستخدم غير صحيح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تسجيل دخول المشرف 🗝️", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "ملاحظة: هذا الباب محمي بقائمة الأجهزة المصرحة والتحقق الثنائي.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- 4. ABOUT & HELP SCREEN (ℹ️) ---
@Composable
fun AboutScreen(viewModel: AppViewModel, settings: AppSettings) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🇾🇪", fontSize = 52.sp)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, GrayBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "معلومات عامة عن الدليل الحالي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Divider(color = GrayBorder)
                    
                    Text(text = "اسم التطبيق المفعل: ${settings.appName}", fontWeight = FontWeight.Bold)
                    Text(text = "إصدار التشغيل: v2.5-Premium", color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "نص رسالة الترحيب: \"${settings.welcomeMessage}\"",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📞 مركز الدعم الفني والمشاركة الرسمي:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    // Support numbers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboard.setText(AnnotatedString(settings.supportPhone))
                                Toast.makeText(context, "📋 تم نسخ رقم الدعم الفني", Toast.LENGTH_SHORT).show()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("رقم الاتصال المباشر:")
                        Text(settings.supportPhone, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboard.setText(AnnotatedString(settings.supportWhatsapp))
                                Toast.makeText(context, "📋 تم نسخ رقم الواتساب", Toast.LENGTH_SHORT).show()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("رقم الواتساب الفعال:")
                        Text(settings.supportWhatsapp, fontWeight = FontWeight.Bold, color = Color(0xFF25D366))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboard.setText(AnnotatedString(settings.supportEmail))
                                Toast.makeText(context, "📋 تم نسخ إيميل الدعم الفني", Toast.LENGTH_SHORT).show()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("البريد الإلكتروني المخدم:")
                        Text(settings.supportEmail, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.currentScreen = "HOME" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🏠 العودة إلى الصفحة الرئيسية")
            }
        }
    }
}

// --- 5. PROVIDER DETAILS SCREEN & RESERVATIONS ---
@Composable
fun ProviderDetailScreen(viewModel: AppViewModel, settings: AppSettings, providerId: Int) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var ratingSelected by remember { mutableStateOf(5) }
    var complaintText by remember { mutableStateOf("") }
    var showComplaintDialog by remember { mutableStateOf(false) }
    
    // Quick booking state
    val scope = rememberCoroutineScope()

    val providerState = viewModel.allProviders.collectAsState().value.find { it.id == providerId } ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper card details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    1.dp,
                    if (providerState.isPinned) Color(0xFFFFCC00) else if (providerState.hasMonthlySubscription) MaterialTheme.colorScheme.primary else GrayBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        ProviderImage(providerState.profileImageBase64, modifier = Modifier.fillMaxSize(), textStyle = LocalTextStyle.current.copy(fontSize = 36.sp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(providerState.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (providerState.isVerified) {
                            Text("✔️ موثق", color = BlueVerified, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (providerState.isPinned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFD700))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("📌 مهني مثبت في الصدارة", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("العنوان الحالي: ${providerState.neighborhood} - ${providerState.address}", fontSize = 12.sp, color = Color.Gray)
                    Text("الهاتف: ${providerState.phone}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    // Share Profile logic
                    Button(
                        onClick = {
                            val shareText = "دليل اليمن للخدمات 🇾🇪\n" +
                                    "انضم للخدمة والمهني: ${providerState.name}\n" +
                                    "الهاتف: ${providerState.phone}\n" +
                                    "الموقع: ${providerState.address}\n" +
                                    "تطبيق دليل اليمن المحمل: https://wam.ye/yemen-applet"
                            clipboard.setText(AnnotatedString(shareText))
                            Toast.makeText(context, "📋 تم نسخ بطاقة المشاركة الذكية!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("🔗 مشاركة بيانات مقدم الخدمة ورابط التحميل")
                    }
                }
            }
        }

        // Ratings & review star widget
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⭐ قيّم الخدمة الحالية ومشاركتها:", fontWeight = FontWeight.Bold)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { ratingSelected = i }
                            ) {
                                Text(
                                    text = if (i <= ratingSelected) "★" else "☆",
                                    color = Color(0xFFFFD700),
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.addRatingToProvider(providerState.id, ratingSelected)
                            viewModel.awardLoyaltyPoints(15) // earn points
                            Toast.makeText(context, "⭐ تم تقديم تقييمك وحصدت 15 نقطة ولاء!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إرسال التقييم الآن")
                    }
                }
            }
        }

        // Communication buttons & Reservation booking logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // Log interaction
                        viewModel.logInteraction(providerState.id, providerState.name)
                        Toast.makeText(context, "📞 جاري الاتصال المباشر بالمهني: ${providerState.phone}", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📞 اتصال مباشر")
                }

                Button(
                    onClick = {
                        if (settings.isGuestBrowsingEnabled && !viewModel.isLoggedIn) {
                            Toast.makeText(context, "🚫 يرجى تسجيل الدخول أولاً للدردشة التفاعلية!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.currentScreen = "CHAT_ROOM"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💬 دردشة وتواصل")
                }
            }
        }

        // File complaint
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFC62828)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showComplaintDialog = true }
            ) {
                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "⚠️ الإبلاغ عن مقدم الخدمة هذا للمشرفين",
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showComplaintDialog) {
        AlertDialog(
            onDismissRequest = { showComplaintDialog = false },
            title = { Text("⚠️ تقديم بلاغ ضد مقدم الخدمة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل تفاصيل الشكوى أو ممارسات غير قانونية لمراجعتها فوراً:")
                    OutlinedTextField(
                        value = complaintText,
                        onValueChange = { complaintText = it },
                        placeholder = { Text("مثال: عدم الالتزام بالوقت المتفق عليه أو المبالغة بالسعر") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (complaintText.isNotBlank()) {
                            viewModel.fileComplaint(providerState.id, providerState.name, "مستعمل مجهول", complaintText)
                            Toast.makeText(context, "✓ تم تقديم البلاغ وسيفحصه المشرفين فوراً", Toast.LENGTH_LONG).show()
                            showComplaintDialog = false
                            complaintText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("إرسال البلاغ فوراً")
                }
            },
            dismissButton = {
                TextButton(onClick = { showComplaintDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

// --- 6. USER HISTORY & BOOKINGS ("طلبات الخدمة السابقة") ---
@Composable
fun PreviousRequestsScreen(viewModel: AppViewModel, settings: AppSettings) {
    val previousLogs by viewModel.previousInteractions.collectAsState()
    val allProvs by viewModel.allProviders.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "📋 طلبات الخدمة السابقة والتواصل:",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "تعرض هذه الصفحة المهنيين ومزودي الخدمات الزراعية الذين قمت بالتواصل معهم مسبقاً لمتابعة الحالة.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        if (previousLogs.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "📭 لم تتواصل مع أي مهني حتى الآن.",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            items(previousLogs) { log ->
                val p = allProvs.find { it.id == log.first }
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            ProviderImage(p?.profileImageBase64 ?: "👨‍🔧", modifier = Modifier.fillMaxSize(), textStyle = LocalTextStyle.current.copy(fontSize = 18.sp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(p?.name ?: "مهني يمني", fontWeight = FontWeight.Bold)
                            Text("رقم الاتصال: ${p?.phone ?: "لا يوجد"}", fontSize = 11.sp, color = Color.Gray)
                            Text("طريقة التواصل: ${log.second}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("مكتمل ✔️", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = { viewModel.currentScreen = "HOME" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🏠 عودة للرئيسية")
            }
        }
    }
}

// --- 7. CHAT CHANNELS SCREEN ---
@Composable
fun ChatScreen(viewModel: AppViewModel, settings: AppSettings) {
    val messagesList by viewModel.chatMessages.collectAsState()
    var messageField by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(key1 = messagesList.size) {
        if (messagesList.isNotEmpty()) {
            listState.animateScrollToItem(messagesList.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💬 دردشة فورية مع الدعم والإدارة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(
                onClick = { viewModel.clearChatHistory() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("🧹 مسح المحادثة", fontSize = 10.sp)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (messagesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("💡 ابدأ الدردشة بإرسال أول استفسار للإدارة هنا.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messagesList) { msg ->
                        val isMe = msg.senderId == "USER"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isMe) 12.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 12.dp
                                ),
                                modifier = Modifier.widthIn(max = 250.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = msg.senderName, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 10.sp,
                                        color = if (isMe) Color.Black else MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = msg.messageText, fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = messageField,
                onValueChange = { messageField = it },
                placeholder = { Text("أكتب رسالتك للمشرفين هنا...") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = parseHexColor(settings.fontColor, Color.White),
                    unfocusedTextColor = parseHexColor(settings.fontColor, Color.White)
                ),
                singleLine = true
            )
            Button(
                onClick = {
                    if (messageField.isNotBlank()) {
                        viewModel.sendChatMessage(messageField, "USER", "ADMIN")
                        messageField = ""
                    }
                }
            ) {
                Text("إرسال")
            }
        }
    }
}
