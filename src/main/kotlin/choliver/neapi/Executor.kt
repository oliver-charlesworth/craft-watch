package choliver.neapi

import choliver.neapi.Scraper.IndexEntry
import choliver.neapi.Scraper.Result
import mu.KotlinLogging

class Executor(getter: HttpGetter) {
  private val jsonGetter = JsonGetter(getter)
  private val logger = KotlinLogging.logger {}

  fun scrapeAll(vararg scrapers: Scraper) = Inventory(
    items = scrapers.flatMap { ScraperExecutor(it).execute() }
  )

  inner class ScraperExecutor(private val scraper: Scraper) {
    private val brewery = scraper.name

    fun execute(): List<Item> {
      logger.info("[${brewery}] Scraping brewery")

      return scrapeIndexSafely(scraper)
        .mapNotNull { scrapeItem(it) }
        .bestPricedItems()
    }

    private fun scrapeItem(entry: IndexEntry): Item? {
      logger.info("[${brewery}] Scraping item: ${entry.rawName}")

      return when (val result = scrapeItemSafely(entry)) {
        is Result.Item -> result.normalise(brewery, entry.url)
        is Result.Skipped -> {
          logger.info("[${brewery}] Skipping item because: ${result.reason}")
          null
        }
      }
    }

    private fun scrapeIndexSafely(scraper: Scraper): List<IndexEntry> = try {
      scraper.scrapeIndex(jsonGetter.request(scraper.rootUrl))
    } catch (e: Exception) {
      logger.error("[${brewery}] Error scraping brewery", e)
      emptyList()
    }

    private fun scrapeItemSafely(entry: IndexEntry) = try {
      entry.scrapeItem(jsonGetter.request(entry.url))
    } catch (e: Exception) {
      logger.error("[${brewery}] Error scraping item: ${entry.rawName}", e)
      Result.Skipped("Error scraping item")
    }

    private fun List<Item>.bestPricedItems() = groupBy { it.name to it.summary }
      .map { (key, group) ->
        if (group.size > 1) {
          logger.info("[${brewery}] Eliminating ${group.size - 1} more expensive item(s) for: ${key}")
        }
        group.minBy { it.perItemPrice }!!
      }
  }
}
