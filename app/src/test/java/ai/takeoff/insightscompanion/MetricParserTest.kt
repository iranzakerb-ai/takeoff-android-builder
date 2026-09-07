package ai.takeoff.insightscompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricParserTest {
    @Test fun parsesEnglishInsights() {
        val text = """
            Views
            12.4K
            Accounts reached
            9,812
            Likes
            431
            Comments
            38
            Shares
            527
            Saves
            183
            Average watch time
            14.8 seconds
            Watch time
            5h 12m 10s
            Follows
            61
        """.trimIndent()
        val m = MetricParser.parse(text)
        assertEquals(12400.0, m["views"]!!, 0.1)
        assertEquals(9812.0, m["reach"]!!, 0.1)
        assertEquals(527.0, m["shares"]!!, 0.1)
        assertEquals(14.8, m["avg_watch_time_seconds"]!!, 0.1)
        assertEquals(18730.0, m["total_watch_time_seconds"]!!, 0.1)
    }

    @Test fun parsesPersianDigitsAndRates() {
        val text = """
            بازدیدها
            ۱۲٬۳۴۵
            اشتراک‌گذاری
            ۵۲۷
            ذخیره‌ها
            ۱۸۳
            میانگین زمان تماشا
            ۱۴٫۸ ثانیه
            نرخ تکمیل
            ۶۱٪
        """.trimIndent()
        val m = MetricParser.parse(text)
        assertEquals(12345.0, m["views"]!!, 0.1)
        assertEquals(527.0, m["shares"]!!, 0.1)
        assertEquals(14.8, m["avg_watch_time_seconds"]!!, 0.1)
        assertEquals(0.61, m["completion_rate"]!!, 0.001)
        assertFalse(m.containsKey("replay_rate"))
    }

    @Test fun parsesCurrentSummary() {
        val text = """
            Reel insights
            Summary
            Views
            2,668
            Viewers
            2,131
            Average watch time
            26s
            Follows
            10
        """.trimIndent()
        val m = MetricParser.parse(text)
        assertEquals(2668.0, m["views"]!!, 0.1)
        assertEquals(2131.0, m["viewers"]!!, 0.1)
        assertEquals(26.0, m["avg_watch_time_seconds"]!!, 0.1)
        assertEquals(10.0, m["follows"]!!, 0.1)
    }

    @Test fun parsesImpactRatesWithoutInventingCounts() {
        val text = """
            What impacts your views
            Skip rate
            21.0%
            Share rate
            0.2%
            Like rate
            2.1%
            Save rate
            0.4%
            Repost rate
            0.0%
            Comment rate
            0.1%
        """.trimIndent()
        val m = MetricParser.parse(text)
        assertEquals(0.21, m["skip_rate"]!!, 0.0001)
        assertEquals(0.002, m["share_rate"]!!, 0.0001)
        assertEquals(0.021, m["like_rate"]!!, 0.0001)
        assertEquals(0.004, m["save_rate"]!!, 0.0001)
        assertEquals(0.0, m["repost_rate"]!!, 0.0001)
        assertEquals(0.001, m["comment_rate"]!!, 0.0001)
        assertFalse(m.containsKey("shares"))
        assertFalse(m.containsKey("likes"))
        assertFalse(m.containsKey("saves"))
        assertFalse(m.containsKey("comments"))
        assertFalse(m.containsKey("reposts"))
    }

    @Test fun parsesTopSources() {
        val text = """
            Top sources of views
            Reels tab
            70.5%
            Explore
            21.8%
            Feed
            4.9%
            Stories
            1.5%
            Profile
            1.1%
        """.trimIndent()
        val m = MetricParser.parse(text)
        assertEquals(0.705, m["source_reels_tab_rate"]!!, 0.0001)
        assertEquals(0.218, m["source_explore_rate"]!!, 0.0001)
        assertEquals(0.049, m["source_feed_rate"]!!, 0.0001)
        assertEquals(0.015, m["source_stories_rate"]!!, 0.0001)
        assertEquals(0.011, m["source_profile_rate"]!!, 0.0001)
    }

    @Test fun parsesEngagementCounts() {
        val text = """
            Actions after viewing
            Profile visits
            40
            Follows
            10
            Interactions
            Likes
            46
            Comments
            2
            Reposts
            1
            Shares
            4
            Saves
            8
        """.trimIndent()
        val m = MetricParser.parse(text)
        assertEquals(40.0, m["profile_visits"]!!, 0.1)
        assertEquals(10.0, m["follows"]!!, 0.1)
        assertEquals(46.0, m["likes"]!!, 0.1)
        assertEquals(2.0, m["comments"]!!, 0.1)
        assertEquals(1.0, m["reposts"]!!, 0.1)
        assertEquals(4.0, m["shares"]!!, 0.1)
        assertEquals(8.0, m["saves"]!!, 0.1)
        assertFalse(m.containsKey("source_profile_rate"))
    }

    @Test fun parsesAudienceAndAgeDistribution() {
        val text = """
            Who viewed your reel
            Followers
            5.9%
            Non-followers
            94.1%
            Audience details
            Age
            13-17
            0.0%
            18-24
            28.3%
            25-34
            52.1%
            35-44
            14.6%
            45-54
            3.2%
            55-64
            1.2%
            65+
            0.5%
        """.trimIndent()
        val m = MetricParser.parse(text)
        assertEquals(0.059, m["audience_followers_rate"]!!, 0.0001)
        assertEquals(0.941, m["audience_nonfollowers_rate"]!!, 0.0001)
        assertEquals(0.0, m["age_13_17_rate"]!!, 0.0001)
        assertEquals(0.283, m["age_18_24_rate"]!!, 0.0001)
        assertEquals(0.521, m["age_25_34_rate"]!!, 0.0001)
        assertEquals(0.146, m["age_35_44_rate"]!!, 0.0001)
        assertEquals(0.032, m["age_45_54_rate"]!!, 0.0001)
        assertEquals(0.012, m["age_55_64_rate"]!!, 0.0001)
        assertEquals(0.005, m["age_65_plus_rate"]!!, 0.0001)
    }

    @Test fun spatialParserPairsTwoColumnSummaryCards() {
        val lines = listOf(
            MetricParser.OcrLine("Views", 50, 500, 170, 535),
            MetricParser.OcrLine("Viewers", 390, 500, 530, 535),
            MetricParser.OcrLine("2,611", 50, 550, 175, 595),
            MetricParser.OcrLine("2,081", 390, 550, 520, 595),
            MetricParser.OcrLine("Average watch time", 50, 650, 275, 690),
            MetricParser.OcrLine("Follows", 390, 650, 500, 690),
            MetricParser.OcrLine("26s", 50, 705, 115, 750),
            MetricParser.OcrLine("10", 390, 705, 440, 750),
        )
        val m = MetricParser.parseStructured(lines)
        assertEquals(2611.0, m["views"]!!, 0.1)
        assertEquals(2081.0, m["viewers"]!!, 0.1)
        assertEquals(26.0, m["avg_watch_time_seconds"]!!, 0.1)
        assertEquals(10.0, m["follows"]!!, 0.1)
    }
}
