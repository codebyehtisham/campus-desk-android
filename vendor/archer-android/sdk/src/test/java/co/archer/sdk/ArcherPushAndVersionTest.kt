package co.archer.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/** Pure JVM tests — no Robolectric / Android runtime required. */
class ArcherPushAndVersionTest {
  @Test
  fun pushEnvelopeParse() {
    val bad = ArcherPushPayloadParser.parse(mapOf("messageId" to UUID.randomUUID().toString()))
    assertNull(bad)

    val id = UUID.randomUUID().toString()
    val good = ArcherPushPayloadParser.parse(
      mapOf(
        "archer" to mapOf(
          "messageId" to id,
          "deepLink" to "myapp://home",
          "data" to mapOf("a" to "1"),
        ),
      ),
    )
    assertEquals(id, good?.messageId)
    assertEquals("myapp://home", good?.deepLink)
    assertEquals("1", good?.data?.get("a"))

    val js = ArcherPushPayloadParser.parse(
      mapOf(
        "archer" to mapOf(
          "messageId" to id,
          "deepLink" to "javascript:alert(1)",
        ),
      ),
    )
    assertNull(js?.deepLink)
  }

  @Test
  fun versionIs100() {
    assertEquals("1.0.1", ArcherSDKVersion.CURRENT)
    val err = ArcherError.Rejected(
      "SDK_OUTDATED",
      "SDK 0.1.0 is outdated; update to 0.2.0",
    )
    assertEquals("SDK_OUTDATED", err.code)
    assertTrue(err.message.contains("outdated"))
    assertFalse(err.message.contains("arch_"))
  }
}
