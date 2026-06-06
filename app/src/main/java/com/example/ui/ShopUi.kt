package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.TextStyle
import java.net.URLEncoder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.CartItem
import com.example.data.Product
import com.example.data.ProductCatalog
import com.example.data.User
import com.example.data.SavedDesign
import com.example.data.OrderHistory
import com.example.viewmodel.ShopViewModel

// ---- Theme Colors (Custom Dark Tech Theme) ----
val SlateBg = Color(0xFF0F172A)      // Deep Dark Slate
val SlateSurface = Color(0xFF1E293B) // Card Surface Slate
val SkyAccent = Color(0xFF38BDF8)    // Main blue accent
val NeonGreen = Color(0xFF22C55E)    // WhatsApp/Signal Green
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val BorderSlate = Color(0xFF334155)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShopApp(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val clicksHistory by viewModel.whatsappClicks.collectAsStateWithLifecycle()

    // Listen to notification flows from ViewModel
    LaunchedEffect(Unit) {
        viewModel.notificationFlow.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = SlateBg,
        topBar = {
            ShopTopBar(
                viewModel = viewModel,
                cartCount = cartItems.sumOf { it.quantity }
            )
        },
        bottomBar = {
            ShopBottomBar(
                activeTab = viewModel.activeTab,
                onTabSelected = { viewModel.activeTab = it },
                cartCount = cartItems.sumOf { it.quantity }
            )
        },
        floatingActionButton = {
            if (viewModel.activeTab == "shop") {
                FloatingWhatsAppWidget {
                    // Triggers Direct Contact Chat Widget (Prompt 12)
                    val supportUrl = viewModel.buildWhatsAppUrl(
                        productTitle = "Support Client Cam's",
                        variant = "Assistance",
                        priceFormatted = "Gratuit",
                        customText = "Je souhaite une assistance de personnalisation !",
                        hasLogo = false
                    )
                    openUrl(context, supportUrl)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (viewModel.activeTab) {
                "shop" -> ShopShowcaseScreen(viewModel)
                "cart" -> ShopCartScreen(viewModel, cartItems)
                "analytics" -> AnalyticsScreen(viewModel, clicksHistory)
                "dev-suite" -> DevSuiteScreen(viewModel)
                "profile" -> ClientProfileScreen(viewModel)
            }

            // Universal Interceptor Upsell Dialog (Prompt 13)
            if (viewModel.showUpsellDialog) {
                UpsellDialog(
                    upsellProduct = viewModel.activeUpsellProduct,
                    onAccept = { viewModel.acceptUpsell() },
                    onDecline = { viewModel.declineUpsell() }
                )
            }
        }
    }
}

// ---- CUSTOM COMPONENTS ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopTopBar(viewModel: ShopViewModel, cartCount: Int) {
    var showCurrencyMenu by mutableStateOf(false)

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SlateSurface,
            titleContentColor = TextPrimary
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(SkyAccent, NeonGreen)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "C",
                        color = SlateBg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Cam's Customizer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Shopify Prototyping Platform",
                        fontSize = 10.sp,
                        color = SkyAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        actions = {
            // Currency Switcher Trigger Component (Prompt 11)
            Box {
                Button(
                    onClick = { showCurrencyMenu = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateBg,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "${viewModel.currentCurrency.symbol} (${viewModel.currentCurrency.code})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Currency",
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showCurrencyMenu,
                    onDismissRequest = { showCurrencyMenu = false },
                    modifier = Modifier.background(SlateSurface)
                ) {
                    viewModel.currencies.forEach { currency ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${currency.symbol} - ${currency.code} (Rate: ${currency.rate})",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                viewModel.selectCurrency(currency)
                                showCurrencyMenu = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ShopBottomBar(activeTab: String, onTabSelected: (String) -> Unit, cartCount: Int) {
    NavigationBar(
        containerColor = SlateSurface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = activeTab == "shop",
            onClick = { onTabSelected("shop") },
            label = { Text("Boutique", fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Boutique") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SlateBg,
                selectedTextColor = SkyAccent,
                indicatorColor = SkyAccent,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = activeTab == "cart",
            onClick = { onTabSelected("cart") },
            label = { Text("Panier", fontWeight = FontWeight.SemiBold) },
            icon = {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(containerColor = SkyAccent) {
                                Text(cartCount.toString(), color = SlateBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Panier")
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SlateBg,
                selectedTextColor = SkyAccent,
                indicatorColor = SkyAccent,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = activeTab == "analytics",
            onClick = { onTabSelected("analytics") },
            label = { Text("Analyses", fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Webhook Analytics") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SlateBg,
                selectedTextColor = SkyAccent,
                indicatorColor = SkyAccent,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = activeTab == "dev-suite",
            onClick = { onTabSelected("dev-suite") },
            label = { Text("Developer", fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.Default.Code, contentDescription = "SDK Dev Solutions") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SlateBg,
                selectedTextColor = SkyAccent,
                indicatorColor = SkyAccent,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = activeTab == "profile",
            onClick = { onTabSelected("profile") },
            label = { Text("Profil", fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Compte Client") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SlateBg,
                selectedTextColor = SkyAccent,
                indicatorColor = SkyAccent,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}

// ---- SHOP SHOWCASE SCREEN ----

@Composable
fun ShopShowcaseScreen(viewModel: ShopViewModel) {
    var expandedItemForCustomize by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Horizontal Filter Categories Layout
        val categories = listOf("Tous", "Mode", "Sport", "Maison", "Accessoires")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = viewModel.selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedCategory = category },
                    label = { Text(category, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SkyAccent,
                        selectedLabelColor = SlateBg,
                        containerColor = SlateSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = BorderSlate,
                        selectedBorderColor = SkyAccent
                    )
                )
            }
        }

        // Subtitle Info
        Text(
            text = "Cliquez sur un produit pour configurer son slogan et son logo interactif.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Products Catalog Grid (Dark Tech Theme Layout - Prompt 3)
        val filteredProducts = remember(viewModel.selectedCategory) {
            if (viewModel.selectedCategory == "Tous") {
                ProductCatalog.items
            } else {
                ProductCatalog.items.filter { it.category == viewModel.selectedCategory }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 145.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredProducts) { product ->
                ProductCard(
                    product = product,
                    currencySymbol = viewModel.currentCurrency.symbol,
                    currencyRate = viewModel.currentCurrency.rate,
                    onClick = {
                        viewModel.selectProduct(product)
                        expandedItemForCustomize = product
                    }
                )
            }
        }
    }

    // Dynamic Sheet / Modal to Customise Selected Product (Prompt 1, 2, 8, 9)
    expandedItemForCustomize?.let { product ->
        CustomisationDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { expandedItemForCustomize = null }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    currencySymbol: String,
    currencyRate: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Elegant Image Loader with status indicator
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            // Category Badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SlateBg.copy(alpha = 0.8f))
                    .border(0.5.dp, SkyAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = product.category,
                    color = SkyAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = product.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = String.format(
                        "%.1f - %.1f %s",
                        product.minPrice * currencyRate,
                        product.maxPrice * currencyRate,
                        currencySymbol
                    ),
                    color = SkyAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SkyAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Personaliser",
                        tint = SkyAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ---- PRODUCT CUSTOMISATION SYSTEM DIALOG ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomisationDialog(
    product: Product,
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header (Back press)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = SkyAccent
                        )
                    }
                    Text(
                        text = "Configuration Cam's",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(48.dp)) // balances lay
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mini Item View
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateBg)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.title,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(product.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Base : ${String.format("%.2f %s", product.minPrice * viewModel.currentCurrency.rate, viewModel.currentCurrency.symbol)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Variant Selector
                Text("Sélectionner la variante :", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    product.variants.forEach { v ->
                        val isSel = viewModel.selectedVariant == v
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) SkyAccent else SlateBg)
                                .border(1.dp, if (isSel) SkyAccent else BorderSlate, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectedVariant = v }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = v,
                                color = if (isSel) SlateBg else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TEXT BOX MANDATORY INPUT FIELD (Prompt 1 & Prompt 8 Validation)
                Text(
                    text = "Slogan Personnalisé (Obligatoire) *",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = viewModel.customText,
                    onValueChange = { viewModel.onCustomTextChange(it) },
                    placeholder = { Text("Tapez votre texte unique ici...", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SlateBg,
                        unfocusedContainerColor = SlateBg,
                        focusedBorderColor = if (viewModel.customTextError != null) Color.Red else SkyAccent,
                        unfocusedBorderColor = if (viewModel.customTextError != null) Color.Red.copy(alpha = 0.7f) else BorderSlate,
                    ),
                    textStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                )
                // Red Error warning dynamically displayed (Prompt 8)
                viewModel.customTextError?.let { err ->
                    Text(
                        text = err,
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.0.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // FILE LOGO UPLOADER BOX SIMULATOR (Prompt 1)
                Text(
                    text = "Image / Logo à imprimer (Optionnel)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!viewModel.hasLogoImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateBg)
                            .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                            .clickable {
                                // Simulates Selecting/Uploading a logotype file transparently
                                viewModel.simulateLogoUpload("mon_logo_custom.png")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload Logo", tint = SkyAccent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Importer une Image", color = SkyAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("PNG, JPG (Simulation de téléchargeur)", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateBg)
                            .border(1.dp, SkyAccent, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Uploaded", tint = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(viewModel.customLogoName ?: "fichier_upload.png", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Image liée aux line items Shopify", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.removeLogo() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // DYNAMIC REAL-TIME PRICE ESTIMATOR BOX (Prompt 2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(SlateBg, SlateBg.copy(alpha = 0.5f))
                            )
                        )
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Prix de Base :", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                String.format("%.2f %s", product.minPrice * viewModel.currentCurrency.rate, viewModel.currentCurrency.symbol),
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (viewModel.customText.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("+ Ajout Slogan :", color = TextSecondary, fontSize = 13.sp)
                                val sloganCost = ((product.maxPrice - product.minPrice) / 2.0)
                                Text(
                                    String.format("+ %.2f %s", sloganCost * viewModel.currentCurrency.rate, viewModel.currentCurrency.symbol),
                                    color = SkyAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (viewModel.hasLogoImage) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("+ Logo brodé/imprimé :", color = TextSecondary, fontSize = 13.sp)
                                val logoCost = ((product.maxPrice - product.minPrice) / 2.0)
                                Text(
                                    String.format("+ %.2f %s", logoCost * viewModel.currentCurrency.rate, viewModel.currentCurrency.symbol),
                                    color = SkyAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderSlate)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Estimé :", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = viewModel.getFormattedPrice(product, viewModel.customText.isNotEmpty(), viewModel.hasLogoImage),
                                color = SkyAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ACTIONS BUTTON ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Alternative direct order by WhatsApp Button (Prompt 4 & Prompt 10 Webhook Click Logger)
                    Button(
                        onClick = {
                            if (viewModel.customText.isEmpty()) {
                                Toast.makeText(context, "Saisissez un slogan avant d'envoyer !", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val calculatedPriceFormatted = viewModel.getFormattedPrice(product, viewModel.customText.isNotEmpty(), viewModel.hasLogoImage)
                            val waUrl = viewModel.buildWhatsAppUrl(
                                productTitle = product.title,
                                variant = viewModel.selectedVariant,
                                priceFormatted = calculatedPriceFormatted,
                                customText = viewModel.customText,
                                hasLogo = viewModel.hasLogoImage
                            )
                            
                            // 1. Logs metrics inside analytical Database (Prompt 10)
                            viewModel.handleWhatsAppRoutingClick(
                                productId = product.id,
                                productTitle = product.title,
                                priceEur = viewModel.calculateCurrentPrice(product, viewModel.customText.isNotEmpty(), viewModel.hasLogoImage)
                            )
                            
                            // 2. Open external WhatsApp App
                            openUrl(context, waUrl)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "WhatsApp", tint = SlateBg)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", color = SlateBg, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }

                    // Standard Ajax Add to Cart button (Prompt 5)
                    Button(
                        onClick = {
                            viewModel.handleAddToCartClick()
                            if (viewModel.customText.isNotEmpty() && viewModel.customTextError == null) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ajouter au Panier", color = SlateBg, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.handleSaveCurrentDesign { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SkyAccent),
                    border = BorderStroke(1.dp, SkyAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Enregistrer le Design", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sauvegarder ce Design", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // PAYMENT REASSURANCE BLOCK (Prompt 9 SSL certificates snippet)
                SSLReassuranceWidget()
            }
        }
    }
}

// ---- SHOPPING CART SCREEN ----

@Composable
fun ShopCartScreen(viewModel: ShopViewModel, cartItems: List<CartItem>) {
    val context = LocalContext.current
    var isCheckoutCssSimulatorExpanded by remember { mutableStateOf(false) }

    if (cartItems.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(SlateSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.RemoveShoppingCart,
                    contentDescription = "Panier Vide",
                    tint = TextSecondary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Votre panier Cam's est vide", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Explorez le catalogue et configurez vos premiers vêtements ou de superbes accessoires.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.activeTab = "shop" },
                colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Visiter la Boutique", color = SlateBg, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Votre Panier", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                TextButton(onClick = { viewModel.clearCart() }) {
                    Text("Tout Vider", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cart Items List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cartItems.forEach { item ->
                    CartItemRow(item = item, viewModel = viewModel, onRemove = { viewModel.removeCartItem(item) })
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                SSLReassuranceWidget()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Totals Card
            val subtotalEur = cartItems.sumOf { it.computedPrice * it.quantity }
            val totalInCurrency = subtotalEur * viewModel.currentCurrency.rate

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderSlate),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sous-total :", color = TextSecondary, fontSize = 14.sp)
                        Text(
                            String.format("%.2f %s", totalInCurrency, viewModel.currentCurrency.symbol),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Livraison estimée :", color = TextSecondary, fontSize = 14.sp)
                        Text("OFFERTE", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Général :", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            String.format("%.2f %s", totalInCurrency, viewModel.currentCurrency.symbol),
                            color = SkyAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Option 1: Checkout with style injected screen (Prompt 15)
                        Button(
                            onClick = { isCheckoutCssSimulatorExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                        ) {
                            Text("Commander (Stripe SSL)", color = SlateBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Option 2: WhatsApp checkout
                        Button(
                            onClick = {
                                // Compile total bill list and route via WhatsApp API
                                val formattedProductsList = cartItems.joinToString("\n") {
                                    "- ${it.productTitle} x${it.quantity} (${it.variant}) [Slogan: ${it.customText}]"
                                }
                                val rawTotalStr = String.format("%.2f %s", totalInCurrency, viewModel.currentCurrency.symbol)
                                val msg = """
                                    Bonjour Cam's ! Je souhaite commander ce panier groupé :
                                    
                                    🧾 Détails du panier :
                                    $formattedProductsList
                                    
                                    💰 Total Général : $rawTotalStr
                                    
                                    Merci d'enregistrer ma commande groupée !
                                """.trimIndent()

                                val encodedMsg = try {
                                    URLEncoder.encode(msg, "UTF-8")
                                } catch (e: Exception) {
                                    msg.replace(" ", "%20")
                                }

                                // Logs click analytics
                                cartItems.forEach { item ->
                                    viewModel.handleWhatsAppRoutingClick(
                                        productId = item.productId,
                                        productTitle = item.productTitle,
                                        priceEur = item.computedPrice
                                    )
                                }

                                openUrl(context, "https://wa.me/33612345678?text=$encodedMsg")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Order on WA", tint = SlateBg)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", color = SlateBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Modern Checkout UI Dark Tech Style Injection Panel Screen (Prompt 15)
    if (isCheckoutCssSimulatorExpanded) {
        CheckoutCssSimulationScreen(
            viewModel = viewModel,
            cartItems = cartItems,
            onDismiss = { isCheckoutCssSimulatorExpanded = false }
        )
    }
}

@Composable
fun CartItemRow(item: CartItem, viewModel: ShopViewModel, onRemove: () -> Unit) {
    val rate = viewModel.currentCurrency.rate
    val symbol = viewModel.currentCurrency.symbol

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, BorderSlate),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Find Product Image link
            val itemCatalogUrl = remember(item.productId) {
                ProductCatalog.items.find { it.id == item.productId }?.imageUrl ?: ""
            }

            AsyncImage(
                model = itemCatalogUrl,
                contentDescription = item.productTitle,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.productTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Variante : ${item.variant}", color = SkyAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "Slogan: \"${item.customText}\"",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.logoPath != null) {
                    Text(
                        "🖼️ Logo : ${item.logoPath}",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer", tint = Color.Red.copy(alpha = 0.8f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = String.format("%.2f %s", item.computedPrice * rate, symbol),
                    color = SkyAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ---- WEBHOOK CLICK TRACKER ANALYTICS SCREEN ----

@Composable
fun AnalyticsScreen(viewModel: ShopViewModel, clicks: List<com.example.data.WhatsAppClick>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Webhook Analytics (Node.js)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "Visualisation du point de terminaison Express /api/webhook/whatsapp-click (Prompt 10)",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Total Traffic Metrics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, BorderSlate),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Métriques de conversion globales", color = SkyAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Clics WhatsApp", color = TextSecondary, fontSize = 12.sp)
                        Text(clicks.size.toString(), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Valeur Estimée Client", color = TextSecondary, fontSize = 12.sp)
                        val totalInEur = clicks.sumOf { it.finalPrice }
                        Text(
                            viewModel.formatDirectPrice(totalInEur),
                            color = NeonGreen,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Serveur d'écoute Node.js en cours d'exécution sur le port 3000.",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateBg)
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Click traffic distribution list
        Text("Volume d'intention d'achat par Produit", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (clicks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucune donnée enregistrée.\nConfigurez un vêtement et cliquez sur le bouton 'WhatsApp' pour émettre un clic webhook.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Group and count
            val grouped = remember(clicks) {
                clicks.groupBy { it.productId }.mapValues { entry ->
                    entry.value.size
                }.toList().sortedByDescending { it.second }
            }

            val maxClicks = grouped.maxOfOrNull { it.second } ?: 1

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderSlate),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    grouped.forEach { (prodId, count) ->
                        // Match Name
                        val productTitle = remember(prodId) {
                            ProductCatalog.items.find { it.id == prodId }?.title ?: prodId
                        }
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = productTitle,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$count clics",
                                    color = SkyAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Styled progress bar representing percentages
                            val percentage = count.toFloat() / maxClicks.toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(SlateBg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(percentage)
                                        .clip(CircleShape)
                                        .background(Brush.horizontalGradient(listOf(SkyAccent, NeonGreen)))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- INTERACTIVE DEV SDK CODESUITES SCREEN ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevSuiteScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedSolutionIndex by remember { mutableIntStateOf(1) }
    var selectedDropdownProduct by remember { mutableStateOf(ProductCatalog.items[0]) }
    var showProductMenu by mutableStateOf(false)
    var visualStateTab by remember { mutableStateOf(0) } // 0 = code, 1 = visual preview (for email/badges/JSON-LD)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Cam's Developer Console", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "Code snippet generator of the 15 coding prompts for Cam's Customizer",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Drops select product
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Simuler avec :", color = TextSecondary, fontSize = 12.sp)
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { showProductMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateSurface, contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedDropdownProduct.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                DropdownMenu(
                    expanded = showProductMenu,
                    onDismissRequest = { showProductMenu = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(SlateSurface)
                ) {
                    ProductCatalog.items.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.title, color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                selectedDropdownProduct = p
                                showProductMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Dropdown Prompt selection ScrollableRow
        ScrollableTabRow(
            selectedTabIndex = selectedSolutionIndex - 1,
            containerColor = SlateBg,
            contentColor = SkyAccent,
            edgePadding = 0.dp
        ) {
            for (i in 1..11) {
                Tab(
                    selected = selectedSolutionIndex == i,
                    onClick = {
                        selectedSolutionIndex = i
                        visualStateTab = 0 // reset to code view
                    },
                    text = {
                        Text(
                            text = "Prompt $i",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Code solutions viewer card header (Copy option)
        val fullSourceCode = viewModel.getSolutionCode(selectedSolutionIndex, selectedDropdownProduct)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = viewModel.getSolutionTitle(selectedSolutionIndex),
                color = SkyAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            
            // Sub-tabs if visual is possible (for Email Confirmation, JSON-LD, CSS Style rules)
            val supportsVisual = selectedSolutionIndex in listOf(6, 7, 9, 11)
            if (supportsVisual) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurface)
                        .padding(2.dp)
                ) {
                    Text(
                        "Code",
                        color = if (visualStateTab == 0) SlateBg else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (visualStateTab == 0) SkyAccent else Color.Transparent)
                            .clickable { visualStateTab = 0 }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Text(
                        "Rendu",
                        color = if (visualStateTab == 1) SlateBg else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (visualStateTab == 1) SkyAccent else Color.Transparent)
                            .clickable { visualStateTab = 1 }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(fullSourceCode))
                    Toast.makeText(context, "Code copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = SkyAccent)
            }
        }

        // Display Area Terminal
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, BorderSlate),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (visualStateTab == 0) {
                    // Code Raw Editor Highlight Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = fullSourceCode,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    // Styled visual preview emulated window
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedSolutionIndex) {
                            6 -> EmailPreviewPanel(selectedDropdownProduct, viewModel.customText.ifEmpty { "Cam's Rider" })
                            7 -> JSONLDPreviewPanel(fullSourceCode)
                            9 -> SSLReassuranceWidget()
                            11 -> Box(modifier = Modifier.padding(16.dp)) { CheckoutButtonSimulationWidget(selectedDropdownProduct, viewModel) }
                        }
                    }
                }
            }
        }
    }
}

// ---- HELPER RENDERING SUBCOMPONENTS ----

@Composable
fun SSLReassuranceWidget() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SlateBg)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = "SSL Verified", tint = SkyAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "🔒 SÉCURITÉ DE TRANSACTION SSL 256-BIT CERTIFIÉE",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emulate Cards icons gracefully using Material Icons
            CardIconSymbol("Visa", SkyAccent)
            CardIconSymbol("Mastercard", Color.Red)
            CardIconSymbol("Stripe", SkyAccent)
            CardIconSymbol("G-Pay", Color.White)
        }
    }
}

@Composable
fun CardIconSymbol(name: String, tintColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(BorderSlate)
            .border(0.5.dp, TextSecondary, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name, color = tintColor, fontWeight = FontWeight.Black, fontSize = 10.sp)
    }
}

@Composable
fun EmailPreviewPanel(product: Product, customText: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateBg),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderSlate),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Brand Logo Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "CAM'S CUSTOMS",
                    color = SkyAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Confirmation de commande #CMS-9843",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate)

            Text("Bonjour F. M. Camara,", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Votre paiement chez Cam's a été approuvé avec succès. Nos ateliers de sérigraphie ont enclenché l'impression de votre pièce personnalisée avec les repères suivants :",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info summary table frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SlateSurface)
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Article", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Total", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = BorderSlate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(product.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Slogan : \"$customText\"", color = SkyAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("Option(s) : Standard", color = TextSecondary, fontSize = 10.sp)
                        }
                        Text(
                            String.format("%.2f €", product.maxPrice),
                            color = SkyAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text("Suivre la fabrication", color = SlateBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun JSONLDPreviewPanel(jsonString: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(SlateBg)
            .padding(12.dp)
    ) {
        Text(
            "Visualisation du graphe SEO Schema.org",
            color = SkyAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Text(
            "Format JSON-LD compatible Google Rich Results",
            color = TextSecondary,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = jsonString,
            color = NeonGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
fun CheckoutButtonSimulationWidget(product: Product, viewModel: ShopViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateSurface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SIMULATION DE CHECKOUT",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = SkyAccent), // Customized vibrant blue button style (Prompt 15)
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "PAYER ${viewModel.getFormattedPrice(product, true, false).uppercase()}",
                color = SlateBg,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Arrière-plans et typographie harmonisés suite aux Style Injections CSS.",
            color = TextSecondary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ---- CUSTOM PULSING FLOATING WHATSAPP CHAT BUTTON ----

@Composable
fun FloatingWhatsAppWidget(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    FloatingActionButton(
        onClick = onClick,
        containerColor = NeonGreen,
        contentColor = SlateBg,
        shape = CircleShape,
        modifier = Modifier
            .scale(pulseScale)
            .size(56.dp)
            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = "WhatsApp Chat Widget",
            modifier = Modifier.size(28.dp),
            tint = SlateBg
        )
    }
}

// --- UNIVERSAL UPSELL INTERCEPTOR POPUP DIALOG ---

@Composable
fun UpsellDialog(
    upsellProduct: Product?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    if (upsellProduct == null) return

    Dialog(onDismissRequest = onDecline) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, SkyAccent)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(SkyAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = "Offre Spéciale", tint = SkyAccent, modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "🔥 Offre Spéciale Fit Pack !",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Assortissez votre tapis de yoga avec notre bouteille en inox isotherme gravée pour seulement 18,00 € (au lieu de 28,00 €) !",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Product mini card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateBg)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = upsellProduct.imageUrl,
                        contentDescription = upsellProduct.title,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(upsellProduct.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row {
                            Text("18,00 €", color = SkyAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("28,00 €", color = TextSecondary, fontSize = 11.sp, style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action choices
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Oui, ajouter le pack !", color = SlateBg, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDecline) {
                    Text("Non merci, juste le tapis", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- EXTENSION INTEGRATIONS ---

@Composable
fun CheckoutCssSimulationScreen(
    viewModel: ShopViewModel,
    cartItems: List<CartItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val subtotal = cartItems.sumOf { it.computedPrice * it.quantity }
    val convertedTotalStr = viewModel.formatDirectPrice(subtotal)
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateBg),
            border = BorderStroke(1.dp, SkyAccent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Simulation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Checkout Customizer", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    "Simulation d'injection de scripts de Checkout pour Cam's (Prompt 15)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cart Recap in checkout
                Text("VOTRE COMMANDE", color = SkyAccent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.productTitle} x${item.quantity}", color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(String.format("%.2f %s", item.computedPrice * viewModel.currentCurrency.rate, viewModel.currentCurrency.symbol), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderSlate)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Général", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(convertedTotalStr, color = SkyAccent, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Input card payment fields - Styled Injected Dark Tech (Prompt 15)
                Text("INFORMATIONS DE PAIEMENT SÉCURISÉ", color = SkyAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Card input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurface)
                        .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("••••  ••••  ••••  4242", color = TextPrimary, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                        Text("12 / 29", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                // Styled CSS Checkout Button (Prompt 15 style target check)
                Button(
                    onClick = {
                        val authUser = viewModel.currentUser
                        if (authUser != null) {
                            viewModel.handlePlaceOrderFromCart(cartItems) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "Succès : Paiement simulé & Commande validée !", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Succès : Paiement simulé sécurisé ! (Pour lier cette commande à un compte historique, connectez-vous d'abord à votre Profil)", Toast.LENGTH_LONG).show()
                            viewModel.clearCart()
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyAccent), // Vibrant blue buttons
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Payer $convertedTotalStr", color = SlateBg, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Merchant helper
                Text(
                    text = "💡 Ce Checkout simule l'application des règles CSS Shopify Plus de Cam's (arrières-plans sombres, boutons de validation bleus, style harmonisé).",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Utility tools for URL and contexts
private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir l'application !", Toast.LENGTH_SHORT).show()
    }
}

// Client Profile Screen containing account registration, logins, historic orders, and saved designs
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val user = viewModel.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (user == null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Mon Espace Client",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Suivez vos commandes et retrouvez vos créations sauvegardées.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                var isLoginTab by remember { mutableStateOf(true) }

                // Segmented tab switch style
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurface)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isLoginTab) SkyAccent else Color.Transparent)
                            .clickable { isLoginTab = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Connexion",
                            color = if (isLoginTab) SlateBg else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isLoginTab) SkyAccent else Color.Transparent)
                            .clickable { isLoginTab = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Inscription",
                            color = if (!isLoginTab) SlateBg else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoginTab) {
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, BorderSlate)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Connexion sécurisée", color = SkyAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Adresse E-mail", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SkyAccent,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedLabelColor = SkyAccent,
                                    unfocusedLabelColor = TextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Mot de passe", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SkyAccent,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedLabelColor = SkyAccent,
                                    unfocusedLabelColor = TextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    viewModel.handleLogIn(email, password) { succ, msg ->
                                        if (!succ) {
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SE CONNECTER", color = SlateBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var fullName by remember { mutableStateOf("") }
                    var phone by remember { mutableStateOf("") }
                    var address by remember { mutableStateOf("") }
                    val avatars = listOf("Aventurier", "Créateur", "Minimaliste", "Artiste", "Slasher")
                    var selectedAvatar by remember { mutableStateOf(avatars[0]) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, BorderSlate)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Nouveau compte", color = SkyAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Nom complet *", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SkyAccent,
                                    unfocusedBorderColor = BorderSlate
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("E-mail *", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SkyAccent,
                                    unfocusedBorderColor = BorderSlate
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Mot de passe *", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SkyAccent,
                                    unfocusedBorderColor = BorderSlate
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Numéro de téléphone", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SkyAccent,
                                    unfocusedBorderColor = BorderSlate
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Adresse de Livraison", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = SkyAccent,
                                    unfocusedBorderColor = BorderSlate
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Style d'avatar :", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                avatars.forEach { av ->
                                    val isSel = selectedAvatar == av
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) SkyAccent else SlateBg)
                                            .border(1.dp, if (isSel) SkyAccent else BorderSlate, RoundedCornerShape(8.dp))
                                            .clickable { selectedAvatar = av }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = av,
                                            color = if (isSel) SlateBg else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    viewModel.handleSignUp(email, password, fullName, phone, address, selectedAvatar) { succ, msg ->
                                        if (!succ) {
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("CRÉER MON COMPTE", color = SlateBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Profile display
            var isEditing by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar graphic display
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(SkyAccent, NeonGreen)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.fullName.take(1).uppercase(),
                                color = SlateBg,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.fullName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(user.email, color = TextSecondary, fontSize = 13.sp)
                            Text("Avatar : ${user.avatarStyle}", color = SkyAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier le Profil", tint = SkyAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (user.phone.isNotEmpty() || user.address.isNotEmpty()) {
                        HorizontalDivider(color = BorderSlate, modifier = Modifier.padding(vertical = 8.dp))
                        if (user.phone.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = "Téléphone", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(user.phone, color = TextPrimary, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (user.address.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Home, contentDescription = "Adresse", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(user.address, color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.handleLogOut() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Déconnexion", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                var editFullName by remember { mutableStateOf(user.fullName) }
                var editPhone by remember { mutableStateOf(user.phone) }
                var editAddress by remember { mutableStateOf(user.address) }
                val avatars = listOf("Aventurier", "Créateur", "Minimaliste", "Artiste", "Slasher")
                var editAvatar by remember { mutableStateOf(user.avatarStyle) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, SkyAccent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Modifier mes informations", color = SkyAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editFullName,
                            onValueChange = { editFullName = it },
                            label = { Text("Nom complet", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = SkyAccent,
                                unfocusedBorderColor = BorderSlate
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Téléphone", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = SkyAccent,
                                unfocusedBorderColor = BorderSlate
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editAddress,
                            onValueChange = { editAddress = it },
                            label = { Text("Adresse", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = SkyAccent,
                                unfocusedBorderColor = BorderSlate
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Style d'avatar :", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            avatars.forEach { av ->
                                val isSel = editAvatar == av
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) SkyAccent else SlateBg)
                                        .border(1.dp, if (isSel) SkyAccent else BorderSlate, RoundedCornerShape(8.dp))
                                        .clickable { editAvatar = av }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = av,
                                        color = if (isSel) SlateBg else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Annuler", color = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    viewModel.handleUpdateProfile(editFullName, editPhone, editAddress, editAvatar) { succ, msg ->
                                        if (succ) {
                                            isEditing = false
                                        } else {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ENREGISTRER", color = SlateBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Saved Designs
            Text("Mes Personnalisations Sauvegardées", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.savedDesignsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateSurface)
                        .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Aucun design", tint = TextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aucun design personnalisé sauvegardé.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Créez une pièce unique en boutique et cliquez sur 'Sauvegarder ce Design' !",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.savedDesignsList.forEach { design ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            border = BorderStroke(1.dp, BorderSlate)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(design.productTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Variante : ${design.variant}", color = SkyAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Slogan : \"${design.customText}\"", color = TextPrimary, fontSize = 12.sp, style = TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                                        if (design.customLogoName != null) {
                                            Text("Logo : ${design.customLogoName}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.handleRemoveDesign(design.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.handleLoadDesignInCustomizer(design) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Load", tint = SlateBg, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Charger dans l'Atelier", color = SlateBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Order History
            Text("Historique de mes Commandes", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.orderHistoryList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateSurface)
                        .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Vous n'avez pas encore passé de commande.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    viewModel.orderHistoryList.forEach { order ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            border = BorderStroke(1.dp, BorderSlate)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Commande #${1000 + order.id}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date(order.orderDate))
                                        Text(dateStr, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonGreen.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = order.status.uppercase(),
                                            color = NeonGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                HorizontalDivider(color = BorderSlate, modifier = Modifier.padding(vertical = 10.dp))
                                Text("Articles :", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(order.itemsSummary, color = TextPrimary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Montant total :", color = TextSecondary, fontSize = 12.sp)
                                    Text(
                                        text = viewModel.formatDirectPrice(order.totalAmount),
                                        color = SkyAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
