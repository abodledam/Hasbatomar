package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HasadDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: HasadDatabase? = null

        fun getDatabase(context: Context): HasadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HasadDatabase::class.java,
                    "hasad_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.transactionDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: TransactionDao) {
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L

            dao.insertTransaction(
                TransactionEntity(
                    type = "INCOME",
                    amount = 12500.0,
                    title = "راتب العمل الأساسي",
                    category = "راتب شهري",
                    paymentMethod = "BANK",
                    dateMillis = now - (2 * dayMillis),
                    notes = "تحويل الراتب الشهري عن طريق البنك"
                )
            )

            dao.insertTransaction(
                TransactionEntity(
                    type = "EXPENSE",
                    amount = 1450.0,
                    title = "مشتريات السوبرماركت الأسبوعية",
                    category = "طعام ومشتريات",
                    paymentMethod = "WALLET",
                    dateMillis = now - (1 * dayMillis),
                    notes = "دفع عبر محفظة إلكترونية"
                )
            )

            dao.insertTransaction(
                TransactionEntity(
                    type = "EXPENSE",
                    amount = 350.0,
                    title = "فاتورة الكهرباء لشهر سبتمبر",
                    category = "فواتير وخدمات",
                    paymentMethod = "CASH",
                    dateMillis = now,
                    notes = "سداد نقدي نقداً للمحصل"
                )
            )

            dao.insertTransaction(
                TransactionEntity(
                    type = "TRANSFER",
                    amount = 2000.0,
                    title = "سحب نقدي من الحساب البنكي",
                    category = "تحويل بين الحسابات",
                    paymentMethod = "BANK",
                    targetPaymentMethod = "CASH",
                    dateMillis = now - (1 * dayMillis),
                    notes = "سحب كاش من ماكينة الصراف الآلي"
                )
            )
        }
    }
}
