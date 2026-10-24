package cl.duoc.museo.data.repository

import android.util.Log
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.CartItem
import cl.duoc.museo.data.model.CatalogItem
import cl.duoc.museo.data.model.CheckoutReceipt
import cl.duoc.museo.data.model.Favorite
import cl.duoc.museo.data.model.Museum
import cl.duoc.museo.data.model.NotificationItem
import cl.duoc.museo.data.model.Order
import cl.duoc.museo.data.model.Product
import cl.duoc.museo.data.model.ReceiptLine
import cl.duoc.museo.data.model.User
import cl.duoc.museo.data.remote.ApiClient
import cl.duoc.museo.utils.TaxCalculator
import androidx.room.withTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MuseumRepository(private val db: AppDatabase) {

    suspend fun getAllCatalogItems(): List<CatalogItem> = db.catalogDao().getAllItems()

    suspend fun cacheCatalogItems(items: List<CatalogItem>) {
        db.catalogDao().insertItems(items)
    }

    suspend fun saveCatalogItem(item: CatalogItem) {
        db.catalogDao().insertItem(item)
    }

    suspend fun deactivateCatalogItem(itemId: String) {
        db.catalogDao().deactivateItem(itemId)
    }

    suspend fun getCatalogItem(itemId: String): CatalogItem? =
        db.catalogDao().getItemById(itemId)

    // --- MUSEUMS ---
    fun getMuseumsFlow(): Flow<List<Museum>> = db.museumDao().getAllMuseums()

    suspend fun refreshMuseums() {
        try {
            val response = ApiClient.apiService.getVirtualMuseums()
            if (response.isSuccessful) {
                response.body()?.let { museums ->
                    db.museumDao().insertAll(museums)
                }
            } else {
                Log.w("MuseumRepository", "La API de museos respondió HTTP ${response.code()}.")
            }
        } catch (e: Exception) {
            Log.w("MuseumRepository", "No se pudo sincronizar el catálogo de museos.", e)
        }
    }

    suspend fun getMuseumById(id: String): Museum? {
        return db.museumDao().getMuseumById(id)
    }

    // --- PRODUCTS ---
    suspend fun getProducts(): List<Product> {
        return try {
            val response = ApiClient.apiService.getProducts()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.w("MuseumRepository", "La API de productos respondió HTTP ${response.code()}.")
                emptyList()
            }
        } catch (e: Exception) {
            Log.w("MuseumRepository", "No se pudo cargar el catálogo remoto de productos.", e)
            emptyList()
        }
    }

    // --- FAVORITES (Per User) ---
    fun getFavoritesForUser(userId: Long): Flow<List<Favorite>> =
        db.favoriteDao().getFavoritesForUser(userId)

    suspend fun toggleFavorite(userId: Long, museum: Museum) {
        val isFav = db.favoriteDao().isFavorite(userId, museum.id)
        if (isFav) {
            db.favoriteDao().deleteFavorite(userId, museum.id)
        } else {
            db.favoriteDao().insertFavorite(
                Favorite(
                    userId = userId,
                    museumId = museum.id,
                    museumName = museum.name,
                    museumCountry = museum.country,
                    imageUrl = museum.imageUrl,
                    classification = museum.classification,
                ),
            )
        }
    }

    suspend fun isFavorite(userId: Long, museumId: String): Boolean =
        db.favoriteDao().isFavorite(userId, museumId)

    // --- CART (Per User) ---
    fun getCartItemsForUser(userId: Long): Flow<List<CartItem>> =
        db.cartDao().getCartItemsForUser(userId)

    suspend fun addToCart(userId: Long, product: Product, quantity: Int) {
        val existingItem = db.cartDao().getCartItemByProduct(userId, product.id)
        if (existingItem != null) {
            db.cartDao().updateQuantity(existingItem.id, existingItem.quantity + quantity)
        } else {
            db.cartDao().insertCartItem(CartItem(
                userId = userId,
                productId = product.id,
                productName = product.name,
                price = product.price,
                quantity = quantity,
                imageUrl = product.imageUrl,
            ))
        }
    }

    suspend fun updateCartQuantity(itemId: Long, quantity: Int) {
        if (quantity <= 0) {
            db.cartDao().deleteCartItem(itemId)
        } else {
            db.cartDao().updateQuantity(itemId, quantity)
        }
    }

    suspend fun removeFromCart(itemId: Long) {
        db.cartDao().deleteCartItem(itemId)
    }

    // --- ORDERS / HISTORIAL ---
    fun getOrdersForUser(userId: Long): Flow<List<Order>> =
        db.orderDao().getOrdersForUser(userId)

    fun getAllOrders(): Flow<List<Order>> =
        db.orderDao().getAllOrders()

    suspend fun updateOrderStatus(orderId: Long, newStatus: String) {
        db.orderDao().updateOrderStatus(orderId, newStatus)
    }

    suspend fun checkout(
        userId: Long,
        country: String,
        shippingType: String,
    ): CheckoutReceipt? = db.withTransaction {
        val cartList = db.cartDao().getCartItemsForUser(userId).first()
        if (cartList.isEmpty()) return@withTransaction null

        val subtotal = cartList.sumOf { it.price * it.quantity }
        val isNational = country.equals("Chile", ignoreCase = true)
        val isExpress = shippingType.contains("Expreso", ignoreCase = true)
        val shippingCost = when {
            isNational && isExpress -> 18.0
            isNational -> 12.0
            isExpress -> 38.0
            else -> 25.0
        }
        val totalCount = cartList.sumOf { it.quantity }
        val vat = TaxCalculator.calculateVat(subtotal)
        val totalAmount = subtotal + vat + shippingCost
        val orderDate = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-CL")).format(Date())

        val orderId = db.orderDao().insertOrder(
            Order(
                userId = userId,
                date = orderDate,
                productCount = totalCount,
                totalAmount = totalAmount,
                status = "Pendiente",
                country = country,
                shippingType = shippingType,
            ),
        )
        db.cartDao().clearCartForUser(userId)
        db.notificationDao().insertNotification(
            NotificationItem(
                userId = userId,
                title = "¡Solicitud recibida!",
                message = "Tu pedido por US$ ${totalAmount.toInt()} a $country está pendiente de confirmación.",
                category = "Pedidos",
                timestamp = "Ahora",
            ),
        )

        CheckoutReceipt(
            orderId = orderId,
            date = orderDate,
            items = cartList.map { item ->
                ReceiptLine(
                    name = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.price,
                    lineTotal = item.price * item.quantity,
                )
            },
            subtotal = subtotal,
            vat = vat,
            shippingCost = shippingCost,
            country = country,
            shippingType = shippingType,
            total = totalAmount,
        )
    }

    // --- NOTIFICATIONS ---
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationItem>> =
        db.notificationDao().getNotificationsForUser(userId)

    suspend fun markNotificationAsRead(id: Long) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun addNotification(notification: NotificationItem) {
        db.notificationDao().insertNotification(notification)
    }

    // --- USER MANAGEMENT ---
    suspend fun registerUser(user: User): Long {
        return db.userDao().insertUser(user)
    }

    suspend fun updateUser(user: User) {
        db.userDao().updateUser(user)
    }

    suspend fun getUserByEmail(email: String): User? {
        return db.userDao().getUserByEmail(email)
    }

    suspend fun getUserById(userId: Long): User? {
        return db.userDao().getUserById(userId)
    }
}