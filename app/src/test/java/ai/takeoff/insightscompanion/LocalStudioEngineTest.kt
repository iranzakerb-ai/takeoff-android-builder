package ai.takeoff.insightscompanion

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalStudioEngineTest {

    @Test
    fun testAiVideoCinematicPackageContract() {
        val pkg = LocalStudioEngine.generateAiVideoPackage(
            niche = "کلینیک زیبایی",
            description = "جوانسازی پوست و تزریق تخصصی ژل و بوتاکس",
            mode = "cinematic",
            audience = "خانم‌های ۳۰ تا ۵۵ سال",
            offer = "مشاوره رایگان و پکیج VIP جوانسازی",
            constraints = "فضای مطب لوکس، نورپردازی ملایم",
            actorCount = 2
        )

        assertTrue(pkg.getBoolean("ok"))
        assertEquals("cinematic", pkg.getString("mode"))
        assertTrue(pkg.getString("scenario_id").startsWith("TKF-AI-"))

        val charSheets = pkg.getJSONArray("character_sheets")
        assertEquals(2, charSheets.length())
        for (i in 0 until charSheets.length()) {
            val charObj = charSheets.getJSONObject(i)
            assertTrue(charObj.getString("name").isNotBlank())
            assertTrue(charObj.getString("role").isNotBlank())
            assertTrue(charObj.getString("omni_prompt").contains("photorealistic"))
        }

        val scenes = pkg.getJSONArray("scenes")
        assertEquals(5, scenes.length())
        val allowedDurations = setOf(4, 6, 8, 10)
        for (i in 0 until scenes.length()) {
            val scene = scenes.getJSONObject(i)
            assertEquals(i + 1, scene.getInt("scene_number"))
            assertTrue(allowedDurations.contains(scene.getInt("duration_seconds")))
            assertTrue(scene.getString("hook_type").isNotBlank())
            assertTrue(scene.getString("camera").isNotBlank())
            assertTrue(scene.getString("omni_prompt").contains("Cinematic video shot"))
        }

        val bundle = pkg.getString("copy_ready_bundle")
        assertTrue(bundle.contains("Character Sheet"))
        assertTrue(bundle.contains("Flow.labs Presets"))
        assertFalse(pkg.has("pdf_path"))
    }

    @Test
    fun testAiVideoViral10sPackageContract() {
        val pkg = LocalStudioEngine.generateAiVideoPackage(
            niche = "صافکاری PDR",
            description = "صافکاری پیشرفته بدون رنگ و بدون برش قطعات",
            mode = "viral_10s",
            audience = "مالکان خودروهای لوکس",
            offer = "ترمیم فرورفتگی در کمتر از یک ساعت",
            constraints = "کارگاه تخصصی، نور متمرکز PDR",
            actorCount = 1
        )

        assertTrue(pkg.getBoolean("ok"))
        assertEquals("viral_10s", pkg.getString("mode"))
        assertEquals(1, pkg.getInt("total_scenes"))
        assertEquals(10, pkg.getInt("total_duration_seconds"))

        val charSheets = pkg.getJSONArray("character_sheets")
        assertEquals(0, charSheets.length())

        val scenes = pkg.getJSONArray("scenes")
        assertEquals(1, scenes.length())
        val scene = scenes.getJSONObject(0)
        assertEquals(1, scene.getInt("scene_number"))
        assertEquals(10, scene.getInt("duration_seconds"))
        assertTrue(scene.getString("camera").contains("iPhone 15 Pro"))

        val prompt = scene.getString("omni_prompt")
        assertTrue(prompt.contains("Hyper-realistic raw smartphone footage"))
        assertTrue(prompt.contains("captured on iPhone 15 Pro"))
        assertFalse(pkg.has("pdf_path"))
    }

    @Test
    fun testScenarioPackageSmart10ScenariosContract() {
        val pkg = LocalStudioEngine.generateScenarioPackage(
            niche = "کافه رستوران",
            description = "سرو قهوه تخصصی و صبحانه‌های جذاب ارگانیک",
            mode = "smart",
            audience = "جوانان و خانواده‌ها",
            offer = "برانچ آخر هفته با تخفیف ویژه",
            constraints = "فضای دنج و باز",
            actorCount = 2
        )

        assertTrue(pkg.getBoolean("ok"))
        assertEquals("smart", pkg.getString("mode"))
        val scenarios = pkg.getJSONArray("scenarios")
        assertEquals(10, scenarios.length())

        for (i in 0 until scenarios.length()) {
            val sc = scenarios.getJSONObject(i)
            assertEquals(i + 1, sc.getInt("id"))
            assertTrue(sc.getString("title").isNotBlank())
            assertTrue(sc.getString("hook_spoken").isNotBlank())
            assertTrue(sc.getString("hook_visual").isNotBlank())
            assertTrue(sc.getString("cta").isNotBlank())

            val scenes = sc.getJSONArray("scenes")
            assertTrue(scenes.length() in 4..12)
        }
    }

    @Test
    fun testScenarioPackageShort15sContract() {
        val pkg = LocalStudioEngine.generateScenarioPackage(
            niche = "املاک لوکس",
            description = "مشاوره و خرید آپارتمان‌های مدرن",
            mode = "short_15s",
            audience = "سرمایه‌گذاران ملک",
            offer = "فرصت پیش‌خرید پنت‌هاوس",
            constraints = "پنت‌هاوس با ویو شهر",
            actorCount = 1
        )

        assertTrue(pkg.getBoolean("ok"))
        assertEquals("short_15s", pkg.getString("mode"))
        val scenarios = pkg.getJSONArray("scenarios")
        assertEquals(10, scenarios.length())

        for (i in 0 until scenarios.length()) {
            val sc = scenarios.getJSONObject(i)
            assertEquals(i + 1, sc.getInt("id"))
            val scenes = sc.getJSONArray("scenes")
            assertEquals(1, scenes.length())
            val scene = scenes.getJSONObject(0)
            assertEquals(15, scene.getInt("duration_seconds"))
        }
    }
}
