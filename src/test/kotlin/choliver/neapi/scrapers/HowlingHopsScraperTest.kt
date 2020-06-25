package choliver.neapi.scrapers

import choliver.neapi.ScrapedItem
import choliver.neapi.executeScraper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI

class HowlingHopsScraperTest {
  companion object {
    private val ITEMS = executeScraper(HowlingHopsScraper())
  }

  @Test
  fun `finds all the beers`() {
    assertEquals(13, ITEMS.size)
  }

  @Test
  fun `extracts sale price not original price`() {
    assertEquals(
      ScrapedItem(
        name = "Push Push",
        summary = "DDH Pale Ale",
        perItemPrice = 4.00,
        abv = 5.8,
        sizeMl = 440,
        available = true,
        thumbnailUrl = URI("https://www.howlinghops.co.uk/wp-content/uploads/2020/06/push-push-440ml-324x324.png")
      ),
      ITEMS.first { it.name == "Push Push" }
    )
  }

  @Test
  fun `finds best prices for beer`() {
    // There are two raw prices available for this beer
    assertEquals(
      listOf(2.75),
      ITEMS
        .filter { it.name == "Passionfruit Gose" }
        .map { it.perItemPrice }
    )
  }

  @Test
  fun `identifies out-of-stock items`() {
    assertFalse(
      ITEMS.first { it.name == "NEW 12 Beer Mega Pack" }.available
    )
  }

  @Test
  fun `doesn't extract apparel`() {
    assertTrue(
      ITEMS.none { it.name.contains("various colours") }
    )
  }
}

