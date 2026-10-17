package cl.duoc.museo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.museo.data.model.CartItem
import cl.duoc.museo.data.model.CatalogItem
import cl.duoc.museo.data.model.Favorite
import cl.duoc.museo.data.model.Museum
import cl.duoc.museo.data.model.NotificationItem
import cl.duoc.museo.data.model.Order
import cl.duoc.museo.data.model.User

@Database(
    entities = [
        User::class,
        CatalogItem::class,
        Museum::class,
        Favorite::class,
        CartItem::class,
        Order::class,
        NotificationItem::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoleConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun catalogDao(): CatalogDao
    abstract fun museumDao(): MuseumDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `catalog_items` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `price` REAL NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `availability` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `rating` REAL NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "museo_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}