package com.example.data

import kotlinx.coroutines.flow.Flow

class ShopRepository(private val shopDao: ShopDao) {
    val cartItems: Flow<List<CartItem>> = shopDao.getCartItems()
    val whatsappClicks: Flow<List<WhatsAppClick>> = shopDao.getWhatsAppClicks()

    suspend fun addCartItem(item: CartItem) {
        shopDao.insertCartItem(item)
    }

    suspend fun updateCartItem(item: CartItem) {
        shopDao.updateCartItem(item)
    }

    suspend fun removeCartItem(id: Int) {
        shopDao.deleteCartItem(id)
    }

    suspend fun clearCart() {
        shopDao.clearCart()
    }

    suspend fun trackWhatsAppClick(click: WhatsAppClick) {
        shopDao.insertWhatsAppClick(click)
    }

    // --- User Actions ---
    suspend fun getUserByEmail(email: String): User? {
        return shopDao.getUserByEmail(email)
    }

    suspend fun getUserById(id: Int): User? {
        return shopDao.getUserById(id)
    }

    suspend fun registerUser(user: User): Long {
        return shopDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        shopDao.updateUser(user)
    }

    // --- Saved Design Actions ---
    fun getSavedDesignsForUser(userId: Int): Flow<List<SavedDesign>> {
        return shopDao.getSavedDesignsForUser(userId)
    }

    suspend fun saveDesign(design: SavedDesign) {
        shopDao.insertSavedDesign(design)
    }

    suspend fun removeSavedDesign(id: Int) {
        shopDao.deleteSavedDesign(id)
    }

    // --- Order History Actions ---
    fun getOrderHistoryForUser(userId: Int): Flow<List<OrderHistory>> {
        return shopDao.getOrderHistoryForUser(userId)
    }

    suspend fun addOrderHistory(order: OrderHistory) {
        shopDao.insertOrderHistory(order)
    }
}
