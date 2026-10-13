package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.HasadDatabase
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("حصاد", appName)
  }

  @Test
  fun `database inserts and calculates offline transactions`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = HasadDatabase.getDatabase(context)
    val dao = database.transactionDao()

    val testTx = TransactionEntity(
      type = "EXPENSE",
      amount = 450.0,
      title = "فاتورة إنترنت",
      category = "فواتير وخدمات",
      paymentMethod = "WALLET",
      dateMillis = System.currentTimeMillis()
    )

    val insertedId = dao.insertTransaction(testTx)
    assertTrue(insertedId > 0)

    val list = dao.getAllTransactions().first()
    assertNotNull(list.find { it.title == "فاتورة إنترنت" })
  }

  @Test
  fun `launch MainActivity test`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(controller.get())
  }
}
