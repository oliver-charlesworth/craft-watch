package watch.craft.executor

import mu.KotlinLogging
import watch.craft.*
import watch.craft.executor.ScraperExecutor.Result
import watch.craft.storage.CachingGetter
import java.time.Clock

class Executor(
  private val getter: CachingGetter,
  private val clock: Clock = Clock.systemUTC()
) {
  private val logger = KotlinLogging.logger {}

  fun scrape(vararg scrapers: Scraper): Inventory {
    val items = scrapers
      .flatMap {
        ScraperExecutor(getter, it)
          .execute()
          .normalise()
      }
      .bestPricedItems()
      .also { it.logStats() }

    return Inventory(
      metadata = Metadata(
        capturedAt = clock.instant()
      ),
      items = items
    )
  }

  private fun List<Result>.normalise() = mapNotNull {
    try {
      it.normalise().addCategories()
    } catch (e: InvalidItemException) {
      logger.warn("[${it.brewery}] Invalid item [${it.entry.rawName}]", e)
      null
    } catch (e: Exception) {
      logger.warn("[${it.brewery}] Unexpected error while validating [${it.entry.rawName}]", e)
      null
    }
  }

  private fun List<Item>.bestPricedItems() = groupBy { ItemGroupFields(it.brewery, it.name, it.keg) }
    .map { (key, group) ->
      if (group.size > 1) {
        logger.info("[${key.brewery}] Eliminating ${group.size - 1} more expensive item(s) for [${key.name}]")
      }
      group.minBy { it.perItemPrice }!!
    }

  private fun List<Item>.logStats() {
    groupBy { it.brewery }.forEach { (key, group) -> logger.info("Scraped (${key}): ${group.size}") }
    logger.info("Scraped (TOTAL): ${size}")
  }

  private data class ItemGroupFields(
    val brewery: String,
    val name: String,
    val keg: Boolean
  )
}
