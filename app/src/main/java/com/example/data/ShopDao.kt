package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    // --- Cart Actions ---
    @Query("SELECT * FROM cart_items ORDER BY timestamp DESC")
    fun getCartItems(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItem)

    @Update
    suspend fun updateCartItem(item: CartItem)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteCartItem(id: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // --- WhatsApp Click Actions ---
    @Query("SELECT * FROM whatsapp_clicks ORDER BY timestamp DESC")
    fun getWhatsAppClicks(): Flow<List<WhatsAppClick>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhatsAppClick(click: WhatsAppClick)

    // --- User Actions ---
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    // --- Saved Designs ---
    @Query("SELECT * FROM saved_designs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSavedDesignsForUser(userId: Int): Flow<List<SavedDesign>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedDesign(design: SavedDesign)

    @Query("DELETE FROM saved_designs WHERE id = :id")
    suspend fun deleteSavedDesign(id: Int)

    // --- Order History ---
    @Query("SELECT * FROM order_history WHERE userId = :userId ORDER BY orderDate DESC")
    fun getOrderHistoryForUser(userId: Int): Flow<List<OrderHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderHistory(order: OrderHistory)
}
