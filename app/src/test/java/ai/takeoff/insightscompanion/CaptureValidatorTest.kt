package ai.takeoff.insightscompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureValidatorTest {
    @Test fun acceptsSummaryWithDistributionAnchorAndThreeMetrics() {
        val r = CaptureValidator.validate(
            "Reel\ninsights\nSummary\nViews\n2668\nViewers\n2131\nAverage watch time\n26s\nFollows\n10",
            mapOf("views" to 2668.0, "viewers" to 2131.0, "avg_watch_time_seconds" to 26.0, "follows" to 10.0),
        )
        assertTrue(r.accepted)
        assertEquals("summary", r.pageHint)
    }

    @Test fun acceptsSummaryWhenSmallHeaderIsMissed() {
        val r = CaptureValidator.validate(
            "Views 2668\nViewers 2131\nAverage watch time 26s\nFollows 10",
            mapOf("views" to 2668.0, "viewers" to 2131.0, "avg_watch_time_seconds" to 26.0, "follows" to 10.0),
        )
        assertTrue(r.accepted)
        assertEquals("summary", r.pageHint)
    }

    @Test fun acceptsImpactRatesWithoutViews() {
        val r = CaptureValidator.validate(
            "Reel insights\nWhat impacts your views\nSkip rate 21.0%\nShare rate 0.2%\nLike rate 2.1%\nSave rate 0.4%",
            mapOf("skip_rate" to .21, "share_rate" to .002, "like_rate" to .021, "save_rate" to .004),
        )
        assertTrue(r.accepted)
        assertEquals("impact_rates", r.pageHint)
    }

    @Test fun acceptsSourcesWithoutViews() {
        val r = CaptureValidator.validate(
            "Reel insights\nTop sources of views\nReels tab 70.5%\nExplore 21.8%\nFeed 4.9%",
            mapOf("source_reels_tab_rate" to .705, "source_explore_rate" to .218, "source_feed_rate" to .049),
        )
        assertTrue(r.accepted)
        assertEquals("sources", r.pageHint)
    }

    @Test fun acceptsEngagementWithoutViews() {
        val r = CaptureValidator.validate(
            "Reel insights\nActions after viewing\nProfile visits 40\nFollows 10\nInteractions\nLikes 46\nComments 2\nReposts 1\nShares 4\nSaves 8",
            mapOf("profile_visits" to 40.0, "follows" to 10.0, "likes" to 46.0, "comments" to 2.0, "reposts" to 1.0, "shares" to 4.0, "saves" to 8.0),
        )
        assertTrue(r.accepted)
        assertEquals("engagement", r.pageHint)
    }

    @Test fun acceptsAudienceWithoutViews() {
        val r = CaptureValidator.validate(
            "Reel insights\nWho viewed your reel\nFollowers 5.9%\nNon-followers 94.1%\nAudience details\n25-34 52.1%\n35-44 14.6%",
            mapOf("audience_followers_rate" to .059, "audience_nonfollowers_rate" to .941, "age_25_34_rate" to .521, "age_35_44_rate" to .146),
        )
        assertTrue(r.accepted)
        assertEquals("audience", r.pageHint)
    }

    @Test fun doesNotForceOverviewRuleOnScrolledOverviewSections() {
        val r = CaptureValidator.validate(
            "Reel insights\nOverview Engagement Audience\nWhat impacts your views\nSkip rate 21%\nShare rate .2%\nLike rate 2.1%",
            mapOf("skip_rate" to .21, "share_rate" to .002, "like_rate" to .021),
        )
        assertTrue(r.accepted)
        assertEquals("impact_rates", r.pageHint)
    }

    @Test fun rejectsGraphOnlyRetentionInsteadOfFabricatingGroundTruth() {
        val r = CaptureValidator.validate(
            "Reel insights\nHow long people watched your reel\n100% 50% 0% 0:00 0:57",
            emptyMap(),
        )
        assertFalse(r.accepted)
        assertEquals("retention", r.pageHint)
        assertEquals("retention_chart_has_no_labeled_metrics", r.reason)
    }

    @Test fun rejectsUnrelatedScreenEvenIfNumbersWereParsed() {
        val r = CaptureValidator.validate(
            "TakeOff Insights\nConnection\nReady",
            mapOf("views" to 2611.0, "follows" to 10.0, "likes" to 46.0),
        )
        assertFalse(r.accepted)
    }

    @Test fun rejectsWeakMetricCoincidenceWithoutInsightsContext() {
        val r = CaptureValidator.validate(
            "Dashboard\nViews 2611\nLikes 46",
            mapOf("views" to 2611.0, "likes" to 46.0),
        )
        assertFalse(r.accepted)
    }
}
