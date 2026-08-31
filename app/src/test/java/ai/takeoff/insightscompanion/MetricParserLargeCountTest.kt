package ai.takeoff.insightscompanion

import org.junit.Assert.assertEquals
import org.junit.Test

class MetricParserLargeCountTest {
    @Test fun parsesEnglishCompactMillions() {
        assertEquals(1_200_000.0, MetricParser.parseCompactNumberForTest("1.2M")!!, 0.1)
    }

    @Test fun parsesPersianMillionAndBillionWords() {
        assertEquals(1_200_000.0, MetricParser.parseCompactNumberForTest("۱٫۲ میلیون")!!, 0.1)
        assertEquals(2_500_000_000.0, MetricParser.parseCompactNumberForTest("۲٫۵ میلیارد")!!, 0.1)
        assertEquals(987_000.0, MetricParser.parseCompactNumberForTest("۹۸۷ هزار")!!, 0.1)
    }

    @Test fun preservesFullIntegerCountsAboveMillion() {
        assertEquals(1_234_567.0, MetricParser.parseCompactNumberForTest("1,234,567")!!, 0.1)
    }
}
