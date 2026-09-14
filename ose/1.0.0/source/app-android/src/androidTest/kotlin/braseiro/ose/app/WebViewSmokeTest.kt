package braseiro.ose.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebViewSmokeTest {
    @Test
    fun localBundleLoadsAndBridgeReceivesViewState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            var ready: String? = null

            while (System.nanoTime() < deadlineNanos && ready != "\"true\"") {
                ready = evaluateJavascript(
                    scenario,
                    "document.body && document.body.dataset.ready"
                )
                if (ready != "\"true\"") Thread.sleep(100)
            }

            assertEquals("\"true\"", ready)

            var finalUrl: String? = null
            scenario.onActivity { activity ->
                finalUrl = activity.webViewForTest().url
            }
            assertNotNull(finalUrl)
            assertTrue(finalUrl!!.startsWith(MainActivity.LOCAL_APP_ORIGIN))
            assertFalse(finalUrl!!.startsWith("file:///android_asset/"))

            var bridgeMessage: BridgeEnvelopeMetadata? = null
            while (System.nanoTime() < deadlineNanos && bridgeMessage == null) {
                scenario.onActivity { activity ->
                    bridgeMessage = activity.lastBridgeMessage
                }
                if (bridgeMessage == null) Thread.sleep(100)
            }

            assertEquals(BridgeEnvelopeMetadata(1, "ViewState"), bridgeMessage)
        }
    }

    private fun evaluateJavascript(
        scenario: ActivityScenario<MainActivity>,
        script: String
    ): String? {
        val latch = CountDownLatch(1)
        var result: String? = null
        scenario.onActivity { activity ->
            activity.webViewForTest().evaluateJavascript(script) { value ->
                result = value
                latch.countDown()
            }
        }
        assertTrue("evaluateJavascript callback timed out", latch.await(2, TimeUnit.SECONDS))
        return result
    }
}
