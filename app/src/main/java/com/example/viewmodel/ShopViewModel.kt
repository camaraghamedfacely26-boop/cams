package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder

data class CurrencyOption(val code: String, val symbol: String, val rate: Double)

class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val shopDao = AppDatabase.getDatabase(application).shopDao()
    private val repository = ShopRepository(shopDao)

    // Reactive states from DB
    val cartItems: StateFlow<List<CartItem>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val whatsappClicks: StateFlow<List<WhatsAppClick>> = repository.whatsappClicks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Configuration States
    val currencies = listOf(
        CurrencyOption("EUR", "€", 1.00),
        CurrencyOption("USD", "$", 1.08),
        CurrencyOption("GBP", "£", 0.84),
        CurrencyOption("CAD", "C$", 1.48)
    )

    var currentCurrency by mutableStateOf(currencies[0])
        private set

    var selectedCategory by mutableStateOf("Tous")
        set

    // Selected product state for details/customization
    var selectedProduct: Product? by mutableStateOf(null)
        private set

    // Active item customization state
    var customText by mutableStateOf("")
        private set
    var customTextError by mutableStateOf<String?>(null)
        private set
    var hasLogoImage by mutableStateOf(false)
        private set
    var customLogoName by mutableStateOf<String?>(null)
        private set
    var selectedVariant by mutableStateOf("")

    // Dialog & Interceptor flow
    var activeUpsellProduct: Product? by mutableStateOf(null)
        private set
    var showUpsellDialog by mutableStateOf(false)

    // Selected navigation tab
    var activeTab by mutableStateOf("shop") // shop, cart, analytics, dev-suite

    // Notification banner
    private val _notificationFlow = MutableSharedFlow<String>()
    val notificationFlow: SharedFlow<String> = _notificationFlow

    init {
        // Initialize default variant when product is loaded
        selectedProduct = ProductCatalog.items[0]
        selectedVariant = ProductCatalog.items[0].variants[0]
    }

    fun selectProduct(product: Product) {
        selectedProduct = product
        selectedVariant = product.variants.firstOrNull() ?: "Standard"
        customText = ""
        customTextError = null
        hasLogoImage = false
        customLogoName = null
    }

    fun selectCurrency(option: CurrencyOption) {
        currentCurrency = option
    }

    // Dynamic Price Calculator (Prompt 2)
    fun calculateCurrentPrice(product: Product, hasText: Boolean, hasLogo: Boolean): Double {
        val delta = (product.maxPrice - product.minPrice) / 2.0
        var total = product.minPrice
        if (hasText) total += delta
        if (hasLogo) total += delta
        return total
    }

    fun getFormattedPrice(product: Product, hasText: Boolean, hasLogo: Boolean): String {
        val nativePrice = calculateCurrentPrice(product, hasText, hasLogo)
        val convertedPrice = nativePrice * currentCurrency.rate
        return String.format("%.2f %s", convertedPrice, currentCurrency.symbol)
    }

    fun getFormattedPriceDouble(product: Product, hasText: Boolean, hasLogo: Boolean): Double {
        return calculateCurrentPrice(product, hasText, hasLogo) * currentCurrency.rate
    }

    fun formatDirectPrice(amountInEur: Double): String {
        return String.format("%.2f %s", amountInEur * currentCurrency.rate, currentCurrency.symbol)
    }

    // Customization text input listener with strict validation (Prompt 8)
    fun onCustomTextChange(text: String) {
        customText = text
        validateCustomText(text)
    }

    private fun validateCustomText(text: String): Boolean {
        if (text.isEmpty()) {
            customTextError = "La personnalisation textuelle est requise pour passer commande."
            return false
        }
        
        // Strict protection against dangerous characters / XSS
        val xssKeywords = listOf("<script>", "</script>", "javascript:", "onload=", "onerror=", "alert(")
        if (text.contains("<") || text.contains(">") || xssKeywords.any { text.lowercase().contains(it) }) {
            customTextError = "Caractères ou balises non autorisés (sécurité XSS)."
            return false
        }

        // Limit checking for clothing/accessories (as per Prompt 8 limit description)
        val isClothing = selectedProduct?.category in listOf("Mode", "Sport")
        if (isClothing && text.length > 50) {
            customTextError = "Saisie limitée à 50 caractères maximum pour les vêtements."
            return false
        } else if (text.length > 80) {
            customTextError = "Saisie limitée à 80 caractères maximum."
            return false
        }

        customTextError = null
        return true
    }

    // Logo image picker mock
    fun simulateLogoUpload(fileName: String) {
        hasLogoImage = true
        customLogoName = fileName
        triggerNotification("Image $fileName téléchargée avec succès !")
    }

    fun removeLogo() {
        hasLogoImage = false
        customLogoName = null
    }

    // Cart API with dynamic upsell (Prompt 5 & Prompt 13)
    fun handleAddToCartClick() {
        val product = selectedProduct ?: return
        
        // Perform field validation (Saisie obligatoire check)
        if (!validateCustomText(customText)) {
            triggerNotification("Erreur de validation : " + (customTextError ?: "Saisie invalide"))
            return
        }

        // Check for Upsell possibility: If client adds a Yoga Mat, propose Reusable Water Bottle (Prompt 13)
        if (product.id == "tapis-yoga") {
            val bottleProduct = ProductCatalog.items.find { it.id == "bouteille" }
            if (bottleProduct != null) {
                activeUpsellProduct = bottleProduct
                showUpsellDialog = true
                return // Pause normal insertion, wait for upsell response
            }
        }

        executeAddToCart(product, customText, hasLogoImage)
    }

    fun acceptUpsell() {
        val upsellProd = activeUpsellProduct ?: return
        showUpsellDialog = false
        
        viewModelScope.launch {
            // First add the primary item (Yoga Mat)
            selectedProduct?.let { executeAddToCart(it, customText, hasLogoImage) }
            
            // Then quick-add the upsell item (Bottle) at a special pack bundle price
            val discountPriceEur = 18.0 // Promo price for bundle
            val upsellCartItem = CartItem(
                productId = upsellProd.id,
                productTitle = "${upsellProd.title} (Promo Yoga Pack)",
                variant = "500ml",
                customText = "Yoga Companion",
                logoPath = if (hasLogoImage) "parent_logo_sync.png" else null,
                computedPrice = discountPriceEur,
                quantity = 1
            )
            repository.addCartItem(upsellCartItem)
            triggerNotification("Tapis + Bouteille isotherme (€18) ajoutés avec succès !")
            activeUpsellProduct = null
        }
    }

    fun declineUpsell() {
        showUpsellDialog = false
        selectedProduct?.let { executeAddToCart(it, customText, hasLogoImage) }
        activeUpsellProduct = null
    }

    private fun executeAddToCart(product: Product, txt: String, img: Boolean) {
        val finalPriceEur = calculateCurrentPrice(product, txt.isNotEmpty(), img)
        val cartItem = CartItem(
            productId = product.id,
            productTitle = product.title,
            variant = selectedVariant,
            customText = txt,
            logoPath = if (img) (customLogoName ?: "uploaded_image.png") else null,
            computedPrice = finalPriceEur,
            quantity = 1
        )
        viewModelScope.launch {
            repository.addCartItem(cartItem)
            triggerNotification("${product.title} ajouté au panier sans recharger la page !")
        }
    }

    fun removeCartItem(item: CartItem) {
        viewModelScope.launch {
            repository.removeCartItem(item.id)
            triggerNotification("Produit retiré du panier.")
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
            triggerNotification("Panier vidé.")
        }
    }

    // WhatsApp Routing API (Prompt 4, Prompt 10 & Prompt 12)
    fun buildWhatsAppUrl(productTitle: String, variant: String, priceFormatted: String, customText: String, hasLogo: Boolean): String {
        val message = """
            Bonjour Cam's Support ! Je souhaite commander la pièce personnalisée suivante :
            🛍️ Produit : $productTitle
            📐 Option/Variante : $variant
            ✍️ Personnalisation texte : ${if (customText.isNotEmpty()) customText else "Aucune"}
            🖼️ Logo/Image : ${if (hasLogo) "Oui (téléchargé)" else "Non"}
            🧾 Prix final estimé : $priceFormatted
            
            Merci de valider les détails techniques de ma commande !
        """.trimIndent()
        
        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            message.replace(" ", "%20") // Fallback
        }
        
        return "https://wa.me/33612345678?text=$encodedMessage"
    }

    fun handleWhatsAppRoutingClick(productId: String, productTitle: String, priceEur: Double) {
        viewModelScope.launch {
            // Simulated Server-Side Webhook click tracking prior to redirecting
            val click = WhatsAppClick(
                productId = productId,
                productTitle = productTitle,
                finalPrice = priceEur,
                currency = currentCurrency.symbol
            )
            repository.trackWhatsAppClick(click)
            triggerNotification("Intégration WhatsApp : Clic enregistré en bdd (Webhook Node.js) !")
        }
    }

    private fun triggerNotification(message: String) {
        viewModelScope.launch {
            _notificationFlow.emit(message)
        }
    }

    // --- USER AUTHENTICATION STATE & LOGIC ---
    var currentUser by mutableStateOf<User?>(null)
        private set

    var savedDesignsList by mutableStateOf<List<SavedDesign>>(emptyList())
        private set

    var orderHistoryList by mutableStateOf<List<OrderHistory>>(emptyList())
        private set

    private var designsJob: kotlinx.coroutines.Job? = null
    private var ordersJob: kotlinx.coroutines.Job? = null

    fun setUserSession(user: User?) {
        currentUser = user
        designsJob?.cancel()
        ordersJob?.cancel()
        if (user != null) {
            designsJob = viewModelScope.launch {
                repository.getSavedDesignsForUser(user.id).collect {
                    savedDesignsList = it
                }
            }
            ordersJob = viewModelScope.launch {
                repository.getOrderHistoryForUser(user.id).collect {
                    orderHistoryList = it
                }
            }
        } else {
            savedDesignsList = emptyList()
            orderHistoryList = emptyList()
        }
    }

    fun handleSignUp(
        email: String,
        passwordRaw: String,
        fullName: String,
        phone: String,
        address: String,
        avatarStyle: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (email.isEmpty() || passwordRaw.isEmpty() || fullName.isEmpty()) {
            onResult(false, "Veuillez remplir tous les champs obligatoires (*) !")
            return
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$".toRegex()
        if (!email.matches(emailRegex)) {
            onResult(false, "Format d'adresse e-mail invalide !")
            return
        }
        viewModelScope.launch {
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                onResult(false, "Un compte avec cet e-mail existe déjà !")
                return@launch
            }
            val newUser = User(
                email = email,
                passwordHash = passwordRaw,
                fullName = fullName,
                phone = phone,
                address = address,
                avatarStyle = avatarStyle
            )
            val newId = repository.registerUser(newUser)
            val registeredUser = newUser.copy(id = newId.toInt())
            setUserSession(registeredUser)
            triggerNotification("Inscription réussie ! Bienvenue ${registeredUser.fullName}.")
            onResult(true, "Bienvenue !")
        }
    }

    fun handleLogIn(email: String, passwordRaw: String, onResult: (Boolean, String) -> Unit) {
        if (email.isEmpty() || passwordRaw.isEmpty()) {
            onResult(false, "Veuillez remplir l'e-mail et le mot de passe !")
            return
        }
        viewModelScope.launch {
            val existing = repository.getUserByEmail(email)
            if (existing == null || existing.passwordHash != passwordRaw) {
                onResult(false, "Adresse e-mail ou mot de passe incorrect !")
                return@launch
            }
            setUserSession(existing)
            triggerNotification("Ravi de vous revoir, ${existing.fullName} !")
            onResult(true, "Connecté !")
        }
    }

    fun handleLogOut() {
        val oldName = currentUser?.fullName ?: ""
        setUserSession(null)
        triggerNotification("Déconnexion réussie. Au revoir, $oldName !")
    }

    fun handleUpdateProfile(fullName: String, phone: String, address: String, avatarStyle: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUser
        if (user == null) {
            onResult(false, "Veuillez d'abord vous connecter !")
            return
        }
        if (fullName.isEmpty()) {
            onResult(false, "Le nom complet est obligatoire !")
            return
        }
        viewModelScope.launch {
            val updatedUser = user.copy(
                fullName = fullName,
                phone = phone,
                address = address,
                avatarStyle = avatarStyle
            )
            repository.updateUser(updatedUser)
            setUserSession(updatedUser)
            triggerNotification("Profil mis à jour !")
            onResult(true, "Profil mis à jour !")
        }
    }

    fun handleSaveCurrentDesign(onResult: (Boolean, String) -> Unit) {
        val user = currentUser
        if (user == null) {
            onResult(false, "Veuillez vous connecter pour sauvegarder vos personnalisations !")
            return
        }
        val product = selectedProduct
        if (product == null) {
            onResult(false, "Aucun produit sélectionné à personnaliser !")
            return
        }
        if (customText.isEmpty()) {
            onResult(false, "Veuillez entrer un slogan avant de sauvegarder !")
            return
        }
        viewModelScope.launch {
            val design = SavedDesign(
                userId = user.id,
                productId = product.id,
                productTitle = product.title,
                customText = customText,
                customLogoName = if (hasLogoImage) customLogoName else null,
                variant = selectedVariant
            )
            repository.saveDesign(design)
            triggerNotification("Design personnalisé pour ${product.title} enregistré avec succès !")
            onResult(true, "Design enregistré !")
        }
    }

    fun handleRemoveDesign(designId: Int) {
        viewModelScope.launch {
            repository.removeSavedDesign(designId)
            triggerNotification("Design supprimé de votre profil.")
        }
    }

    fun handleLoadDesignInCustomizer(design: SavedDesign) {
        val matchingProduct = ProductCatalog.items.find { it.id == design.productId }
        if (matchingProduct != null) {
            selectedProduct = matchingProduct
            selectedVariant = design.variant
            customText = design.customText
            customTextError = null
            if (design.customLogoName != null) {
                hasLogoImage = true
                customLogoName = design.customLogoName
            } else {
                hasLogoImage = false
                customLogoName = null
            }
            activeTab = "shop"
            triggerNotification("Design ${matchingProduct.title} reappliqué dans l'éditeur !")
        } else {
            triggerNotification("Le produit de ce design n'est plus disponible.")
        }
    }

    fun handlePlaceOrderFromCart(items: List<CartItem>, onResult: (Boolean, String) -> Unit) {
        val user = currentUser
        if (user == null) {
            onResult(false, "Veuillez vous connecter pour enregistrer votre commande sur votre profil !")
            return
        }
        if (items.isEmpty()) {
            onResult(false, "Votre panier est vide !")
            return
        }
        val subtotal = items.sumOf { it.computedPrice * it.quantity }
        val itemsStr = items.joinToString(", ") { "${it.quantity}x ${it.productTitle} (${it.variant})" }
        viewModelScope.launch {
            val order = OrderHistory(
                userId = user.id,
                itemsSummary = itemsStr,
                totalAmount = subtotal,
                status = "En préparation"
            )
            repository.addOrderHistory(order)
            repository.clearCart()
            triggerNotification("Commande simulée validée et liée à votre profil !")
            onResult(true, "Commande réussie !")
        }
    }

    // Dynamic developer content solvers for display in Dev tab (Prompts 1 - 15)
    fun getSolutionTitle(id: Int): String {
        return when (id) {
            1 -> "1. Structure Liquid Options (Product Form)"
            2 -> "2. Calculateur de prix dynamique JavaScript"
            3 -> "3. Catalogue CSS Responsive Grid (Dark Tech)"
            4 -> "4. Script Routeur d'API d'achat WhatsApp"
            5 -> "5. Script AJAX Cart Fetch Pipeline API"
            6 -> "6. Template HTML Confirmation Email"
            7 -> "7. SEO JSON-LD Schema (Google Structured)"
            8 -> "8. Validateur de formulaires JavaScript"
            9 -> "9. Snippet Liquid des Badges de Paiement"
            10 -> "10. Webhook Node.js / Express Tracker"
            11 -> "11. Console CSS Style checkout Shopify"
            else -> ""
        }
    }

    // Code builders matching user's specific prompts (showing real functional solutions)
    fun getSolutionCode(id: Int, product: Product? = null): String {
        val prod = product ?: selectedProduct ?: ProductCatalog.items[0]
        val currentPriceFormatted = getFormattedPrice(prod, customText.isNotEmpty(), hasLogoImage)
        val minPriceStr = String.format("%.2f€", prod.minPrice)
        val maxPriceStr = String.format("%.2f€", prod.maxPrice)

        return when (id) {
            1 -> """
                <!-- product-form.liquid for ${prod.title} -->
                <div class="cms-customization-options-container" style="margin-bottom: 20px;">
                  
                  <!-- 1. Mandatory Text Box (Line Item Property) -->
                  <div class="cms-option-field">
                    <label for="cms-custom-text" class="cms-label" style="display:block; margin: 8px 0; color: #f8fafc; font-weight:600;">
                      Saisissez votre personnalisation <span class="required" style="color:red;">*</span>
                    </label>
                    <input type="text"
                           id="cms-custom-text"
                           name="properties[Slogan personnalisé]"
                           required
                           maxlength="50"
                           placeholder="Ex: Cam's Spirit"
                           style="width:100%; padding: 12px; background:#1e293b; color:#fff; border:1px solid #38bdf8; border-radius:6px;"
                           data-min-price="${prod.minPrice}"
                           data-max-price="${prod.maxPrice}">
                  </div>
                  
                  <!-- 2. Logo / File Uploader (Line Item Property) -->
                  <div class="cms-option-field" style="margin-top: 15px;">
                    <label for="cms-custom-logo" class="cms-label" style="display:block; margin: 8px 0; color: #f8fafc; font-weight:600;">
                      Importez votre logo ou image (PNG, JPG)
                    </label>
                    <input type="file"
                           id="cms-custom-logo"
                           name="properties[Fichier logo]"
                           accept="image/*"
                           style="width:100%; padding: 8px; color: #94a3b8;">
                  </div>
                </div>
            """.trimIndent()

            2 -> """
                /**
                 * Dynamic Price Calculator (Pure Vanilla JS)
                 * Shopify Product Page Customizer Engine
                 */
                (function() {
                  const inputTxt = document.getElementById('cms-custom-text');
                  const inputLogo = document.getElementById('cms-custom-logo');
                  const priceDisplay = document.querySelector('.product-single__price');
                  
                  if (!inputTxt || !priceDisplay) return;
                  
                  const minPrice = parseFloat(inputTxt.dataset.minPrice) || ${prod.minPrice};
                  const maxPrice = parseFloat(inputTxt.dataset.maxPrice) || ${prod.maxPrice};
                  const delta = (maxPrice - minPrice) / 2.0;

                  function updateDynamicPrice() {
                    let currentComputedPrice = minPrice;
                    
                    if (inputTxt.value.trim() !== '') {
                      currentComputedPrice += delta;
                    }
                    if (inputLogo && inputLogo.files && inputLogo.files.length > 0) {
                      currentComputedPrice += delta;
                    }
                    
                    // Format dynamically according to Shopify locales
                    priceDisplay.innerHTML = new Intl.NumberFormat('fr-FR', { 
                      style: 'currency', currency: 'EUR' 
                    }).format(currentComputedPrice);
                  }
                  
                  inputTxt.addEventListener('input', updateDynamicPrice);
                  if (inputLogo) inputLogo.addEventListener('change', updateDynamicPrice);
                })();
            """.trimIndent()

            3 -> """
                /* Dark Tech Responsive Grid System for Cam's Catalogue */
                .cams-catalogue-grid {
                  display: grid;
                  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
                  gap: 24px;
                  padding: 20px;
                  background-color: #0f172a; /* Slate Dark background */
                }
                
                .cams-product-card {
                  background: #1e293b; /* Dark slate container */
                  border: 1px solid #334155;
                  border-radius: 12px;
                  overflow: hidden;
                  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), 
                              border-color 0.3s ease;
                }
                
                .cams-product-card:hover {
                  transform: translateY(-8px);
                  border-color: #38bdf8; /* Vibrant Blue accent hover */
                  box-shadow: 0 10px 25px -5px rgba(56, 189, 248, 0.15);
                }
            """.trimIndent()

            4 -> """
                /**
                 * WhatsApp API Routing Alternative Order Script
                 */
                function initiateWhatsAppOrder() {
                  const title = "${prod.title}";
                  const currentPrice = "${currentPriceFormatted}";
                  const textVal = document.getElementById('cms-custom-text')?.value || "";
                  const hasLogoFile = document.getElementById('cms-custom-logo')?.files?.length > 0;
                  
                  const message = "Bonjour Cam's ! Je souhaite commander : \n" +
                                  "🛍️ Produit : " + title + "\n" +
                                  "✍️ Custom Slogan : " + (textVal || "Aucune") + "\n" +
                                  "🖼️ Logo Uploadé : " + (hasLogoFile ? "Oui" : "Non") + "\n" +
                                  "🧾 Prix final estimé : " + currentPrice;
                                  
                  const apiEndpoint = "https://wa.me/33612345678?text=" + encodeURIComponent(message);
                  window.open(apiEndpoint, '_blank');
                }
            """.trimIndent()

            5 -> """
                /**
                 * AJAX Cart fetch Pipeline API (Shopify cart/add.js)
                 * Non-reloading Custom Cart injection (Prompt 5)
                 */
                async function addCustomProductToCart() {
                  const inputTxt = document.getElementById('cms-custom-text');
                  const customLogo = document.getElementById('cms-custom-logo');
                  
                  const slogan = inputTxt ? inputTxt.value : '';
                  if (!slogan) {
                    alert("La personnalisation est obligatoire !");
                    return;
                  }
                  
                  let formData = new FormData();
                  formData.append('id', '${prod.id}'); // Shopify Variant ID
                  formData.append('quantity', '1');
                  formData.append('properties[Slogan personnalisé]', slogan);
                  if (customLogo && customLogo.files[0]) {
                    formData.append('properties[Fichier logo]', customLogo.files[0]);
                  }
                  
                  try {
                    const response = await fetch('/cart/add.js', {
                      method: 'POST',
                      body: formData
                    });
                    
                    if (!response.ok) throw new Error("Rupture de stock / Echec AJAX.");
                    const cartItem = await response.json();
                    
                    // Update header bubble count
                    const cartResponse = await fetch('/cart.js');
                    const cart = await cartResponse.json();
                    document.querySelector('.cart-count-bubble').textContent = cart.item_count;
                    
                  } catch (error) {
                    console.error("Erreur de panier AJAX : ", error);
                  }
                }
            """.trimIndent()

            6 -> """
                <!-- Shopify Order Confirmation Email Template HTML -->
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml" style="background-color: #0f172a;">
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                  <title>Confirmation de Commande - Cam's</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; height: 100% !important; background-color: #0f172a; margin: 0; padding: 40px;">
                  <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; padding: 30px;">
                    <tr>
                      <td align="center" style="padding-bottom: 20px; border-bottom: 1px solid #334155;">
                        <h1 style="color: #38bdf8; margin: 0; font-size: 28px; letter-spacing: 1px;">CAM'S CUSTOM</h1>
                        <p style="color: #94a3b8; font-size:14px;">Votre pièce unique en cours de création</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding: 25px 0;">
                        <p style="color: #f8fafc; font-size: 16px;">Merci pour votre achat !</p>
                        <p style="color: #94a3b8; font-size: 14px; line-height: 1.6;">Nos ateliers d'impression ont reçu votre fichier de personnalisation. Voici le récapitulatif technique :</p>
                        
                        <!-- Product Table -->
                        <table width="100%" style="margin-top: 20px; border-collapse: collapse;">
                          <tr style="border-bottom: 1px solid #334155;">
                            <th align="left" style="color: #f8fafc; padding: 12px 0;">Article</th>
                            <th align="right" style="color: #f8fafc; padding: 12px 0;">Prix</th>
                          </tr>
                          <tr>
                            <td style="padding: 15px 0;">
                              <div style="color: #f8fafc; font-weight: 600; font-size: 15px;">${prod.title}</div>
                              <div style="color: #38bdf8; font-size: 13px; margin-top: 4px;">Slogan: "${customText.ifEmpty { "Cam's Rider" }}"</div>
                              <div style="color: #94a3b8; font-size: 12px;">Logo : ${if (hasLogoImage) "Fichier Image joint" else "Aucun logo"}</div>
                            </td>
                            <td align="right" style="color: #38bdf8; font-weight: bold; font-size: 15px;">${currentPriceFormatted}</td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
            """.trimIndent()

            7 -> """
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org",
                  "@type": "Product",
                  "name": "${prod.title}",
                  "image": ["${prod.imageUrl}"],
                  "description": "${prod.description}",
                  "category": "${prod.category}",
                  "brand": {
                    "@type": "Brand",
                    "name": "Cam's"
                  },
                  "offers": {
                    "@type": "AggregateOffer",
                    "priceCurrency": "EUR",
                    "lowPrice": "${prod.minPrice}",
                    "highPrice": "${prod.maxPrice}",
                    "offerCount": "2",
                    "priceValuableUntil": "2027-12-31"
                  }
                }
                </script>
            """.trimIndent()

            8 -> """
                /**
                 * Javascript Form Security and Length Validator (Prompt 8)
                 * Blocks dangerous XSS and limits text characters
                 */
                function validateCamsCustomForm() {
                  const inputTxt = document.getElementById("cms-custom-text");
                  const errorLabel = document.getElementById("cms-error-msg");
                  
                  if (!inputTxt) return true;
                  
                  const value = inputTxt.value.trim();
                  
                  // Rule 1: Mandatory assessment
                  if(value === "") {
                    displayError("La personnalisation textuelle est requise !");
                    return false;
                  }
                  
                  // Rule 2: XSS security filtering
                  const unsafePattern = /[<>\/()[\]{}|&*$]/g;
                  if(unsafePattern.test(value) || value.toLowerCase().includes("script")) {
                    displayError("Caractères interdits par mesures de sécurité XSS !");
                    return false;
                  }
                  
                  // Rule 3: 50 characters limits for clothing/apparel
                  const category = "${prod.category}";
                  if((category === "Mode" || category === "Sport") && value.length > 50) {
                    displayError("Saisie limitée à 50 caractères pour les vêtements.");
                    return false;
                  }
                  
                  clearError();
                  return true;
                  
                  function displayError(msg) {
                    errorLabel.style.display = "block";
                    errorLabel.textContent = msg;
                    errorLabel.style.color = "red";
                    inputTxt.style.borderColor = "red";
                  }
                  
                  function clearError() {
                    errorLabel.style.display = "none";
                    inputTxt.style.borderColor = "#38bdf8";
                  }
                }
            """.trimIndent()

            9 -> """
                <!-- checkout-reassurance-badges.liquid (Prompt 9) -->
                <div class="cms-reassurance-container" style="border-top: 1px solid #334155; padding-top: 20px; margin-top: 20px; text-align: center;">
                  <p class="ssl-text" style="color: #94a3b8; font-size: 12px; margin-bottom: 12px; font-weight:500;">
                    🔒 Connexion cryptée SSL 256-bits. Transaction 100% sécurisée.
                  </p>
                  
                  <div class="payment-logos-row" style="display: flex; justify-content: center; gap: 15px;">
                    <!-- Native Shopify SVG Payment Filter tags -->
                    {{ 'visa' | payment_type_svg_tag: class: 'payment-icon', style: 'height:24px;' }}
                    {{ 'master' | payment_type_svg_tag: class: 'payment-icon', style: 'height:24px;' }}
                    {{ 'shopify_pay' | payment_type_svg_tag: class: 'payment-icon', style: 'height:24px;' }}
                    {{ 'apple_pay' | payment_type_svg_tag: class: 'payment-icon', style: 'height:24px;' }}
                    {{ 'google_pay' | payment_type_svg_tag: class: 'payment-icon', style: 'height:24px;' }}
                  </div>
                </div>
            """.trimIndent()

            10 -> """
                /**
                 * Node.js Webhook Express Server (Prompt 10)
                 * Logs WhatsApp Order button click metrics
                 */
                const express = require('express');
                const fs = require('fs');
                const app = express();
                app.use(express.json());

                // Endpoint triggered on WhatsApp redirection
                app.post('/api/webhook/whatsapp-click', (req, res) => {
                  const { productId, productTitle, amount, date } = req.body;
                  
                  if (!productId) {
                    return res.status(400).json({ error: "Missing productId" });
                  }

                  const clickLog = {
                    productId,
                    productTitle,
                    amount: amount || 0,
                    timestamp: date || new Date().toISOString()
                  };

                  // Read existing local logs and push
                  fs.readFile('./whatsapp-traffic-logs.json', 'utf8', (err, data) => {
                    let logs = [];
                    if (!err && data) {
                      try { logs = JSON.parse(data); } catch(e) {}
                    }
                    
                    logs.push(clickLog);
                    
                    // Store analytics
                    fs.writeFile('./whatsapp-traffic-logs.json', JSON.stringify(logs, null, 2), (writeErr) => {
                      if (writeErr) return res.status(500).json({ status: "fail" });
                      res.status(200).json({ status: "success", saved: clickLog });
                    });
                  });
                });

                app.listen(3000, () => console.log("Cam's webhook server tracker running on port 3000"));
            """.trimIndent()

            11 -> """
                /* Checkout Shopify Style Customization Injection Rules (Prompt 15) */
                
                /* Body & Slate Dark background settings */
                .checkout-body {
                  background-color: #0f172a !important;
                  color: #f1f5f9 !important;
                  font-family: 'Space Grotesk', system-ui, sans-serif !important;
                }
                
                /* High contrast vibrant blue verification inputs */
                .input-placeholder, .field__input {
                  background-color: #1e293b !important;
                  color: #fff !important;
                  border: 1px solid #334155 !important;
                  border-radius: 8px !important;
                }
                
                .field__input:focus {
                  border-color: #38bdf8 !important;
                  box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.25) !important;
                }
                
                /* Checkout Validation Blue Button */
                .btn, #continue_button, .step__footer__continue-btn {
                  background-color: #38bdf8 !important;
                  color: #0f172a !important;
                  font-weight: 700 !important;
                  text-transform: uppercase !important;
                  letter-spacing: 0.5px !important;
                  border-radius: 8px !important;
                  padding: 16px 24px !important;
                  transition: background-color 0.2s ease !important;
                }
                
                .btn:hover {
                  background-color: #0ea5e9 !important;
                }
            """.trimIndent()

            else -> ""
        }
    }
}
