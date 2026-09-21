package co.archer.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ArcherSDKTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    LastCrashStore.init(context)
    ArcherLocalCrypto.init(context)
    LastCrashStore.clear()
  }

  @After
  fun tearDown() {
    LastCrashStore.clear()
  }

  @Test
  fun bufferBeforeStart() {
    Archer.configure(
      context,
      ArcherConfiguration(apiKey = "arch_test_buffer", environment = ArcherEnvironment.DEVELOPMENT),
    )
    assertFalse(Archer.isStarted())
    Archer.log("hello", LogLevel.INFO)
    Archer.analytics("sample_opened")
    Archer.recordCrashEvent(reason = "test_crash")
    assertEquals(3, Archer.pendingCount())
  }

  @Test
  fun lastCrashWritePeekConsume() {
    LastCrashStore.clear()
    val payload = LastCrashPayload(
      reason = "unit_last_crash",
      stackTrace = "frame0\nframe1",
      fatal = true,
      threadName = "main",
      signal = "SIGABRT",
      metadata = mapOf("capturePath" to "unit"),
    )
    LastCrashStore.write(payload)
    val peeked = LastCrashStore.peek()
    assertEquals("unit_last_crash", peeked?.reason)
    assertEquals("SIGABRT", peeked?.signal)
    assertEquals("frame0\nframe1", peeked?.stackTrace)

    val consumed = LastCrashStore.consume()
    assertEquals("unit_last_crash", consumed?.reason)
    assertNull(LastCrashStore.peek())
  }

  @Test
  fun injectTestLastCrashThenReport() {
    LastCrashStore.clear()
    Archer.configure(
      context,
      ArcherConfiguration.build("arch_test_inject", ArcherEnvironment.DEVELOPMENT) { s ->
        s.crashes.reportLastCrashOnStart = true
      },
    )
    val before = Archer.pendingCount()
    Archer.injectTestLastCrash(
      reason = "injected_crash",
      stackTrace = "a\nb",
      signal = "SIGSEGV",
      metadata = mapOf("k" to "v"),
    )
    assertNotNull(LastCrashStore.peek())
    Archer.reportPersistedLastCrashIfPresent()
    assertNull(LastCrashStore.peek())
    assertEquals(before + 1, Archer.pendingCount())
  }

  @Test
  fun crashDisabledDropsEnqueue() {
    Archer.configure(
      context,
      ArcherConfiguration.build("arch_test_crash_off", ArcherEnvironment.DEVELOPMENT) { s ->
        s.crashes.enabled = false
      },
    )
    val before = Archer.pendingCount()
    Archer.recordCrashEvent(reason = "should_drop")
    assertEquals(before, Archer.pendingCount())
  }
}
