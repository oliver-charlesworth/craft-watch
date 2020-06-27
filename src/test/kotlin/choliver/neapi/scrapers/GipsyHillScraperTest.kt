package choliver.neapi.scrapers

import choliver.neapi.Scraper.Item
import choliver.neapi.byName
import choliver.neapi.executeScraper
import choliver.neapi.noDesc
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI

class GipsyHillScraperTest {
  companion object {
    private val ITEMS = executeScraper(GipsyHillScraper())
  }

  @Test
  fun `finds all the beers`() {
    assertEquals(21, ITEMS.size)
  }

  @Test
  fun `extracts beer details`() {
    assertEquals(
      Item(
        name = "Carver",
        summary = "Micro IPA",
        perItemPrice = 2.20,
        abv = 2.8,
        sizeMl = 330,
        available = true,
        thumbnailUrl = URI("https://i1.wp.com/gipsyhillbrew.com/wp-content/uploads/2018/11/CARVER.png?resize=300%2C300&ssl=1")
      ),
      ITEMS.byName("Carver").noDesc()
    )
  }

  @Test
  fun `identifies mixed cases`() {
    assertTrue(ITEMS.byName("DJ BBQ Box").mixed)
  }

  @Test
  fun `ignores abv for multi-pack`() {
    assertNull(ITEMS.byName("DJ BBQ Box").abv)
  }

  @Test
  fun `normalises price for multi-pack`() {
    assertEquals(2.33, ITEMS.byName("Core Range 24 Pack").perItemPrice)
  }

  @Test
  fun `extracts description`() {
    assertNotNull(ITEMS.byName("Carver").desc)
  }
}

