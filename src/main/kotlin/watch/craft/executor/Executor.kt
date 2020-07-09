package watch.craft.executor

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import watch.craft.*
import watch.craft.enrichers.Categoriser
import watch.craft.enrichers.Newalyser
import watch.craft.executor.ScraperAdapter.Result
import watch.craft.network.Retriever
import java.time.Clock
import java.time.Instant

class Executor(
  private val results: ResultsManager,
  private val createRetriever: (name: String) -> Retriever,
  private val clock: Clock = Clock.systemUTC()
) {
  private val logger = KotlinLogging.logger {}

  fun scrape(scrapers: Collection<Scraper>): Inventory {
    val now = clock.instant()

    return scrapers
      .execute()
      .normalise()
      .toInventory(scrapers, now)
      .enrichWith(Categoriser(CATEGORY_KEYWORDS))
      .enrichWith(Newalyser(results, now))
      .sort()
      .bestPricedItems()
      .also { it.logStats() }
  }

  private fun Collection<Scraper>.execute() = runBlocking {
    this@execute
      .map { async { it.execute() } }
      .flatMap { it.await() }
      .toSet()  // To make clear that order is not important
  }

  private suspend fun Scraper.execute() = createRetriever(brewery.shortName).use {
    ScraperAdapter(it, this).execute()
  }

  private fun Collection<Result>.normalise() = mapNotNull {
    try {
      it.normalise()
    } catch (e: InvalidItemException) {
      logger.warn("[${it.breweryName}] Invalid item [${it.rawName}]", e)
      null
    } catch (e: Exception) {
      logger.warn("[${it.breweryName}] Unexpected error while validating [${it.rawName}]", e)
      null
    }
  }

  private fun Collection<Item>.toInventory(scrapers: Collection<Scraper>, now: Instant) = Inventory(
    metadata = Metadata(capturedAt = now),
    categories = CATEGORY_KEYWORDS.keys.toList(),
    breweries = scrapers.map { it.brewery },
    items = toList()
  )

  private fun Inventory.enrichWith(enricher: Enricher) = copy(
    items = items.map(enricher::enrich),
    breweries = breweries.map(enricher::enrich)
  )

  private fun Inventory.sort() = copy(items = items.sortedWith(
    compareBy(
      { it.brewery },
      { it.name },
      { it.available },
      { it.onlyOffer().sizeMl },
      { it.onlyOffer().keg },
      { it.onlyOffer().quantity }
    )
  ))

  private fun Inventory.bestPricedItems() = copy(items = items.groupBy { ItemGroupFields(it.brewery, it.name, it.onlyOffer().keg) }
    .map { (key, group) ->
      if (group.size > 1) {
        logger.info("[${key.brewery}] Eliminating ${group.size - 1} more expensive item(s) for [${key.name}]")
      }
      group.minBy { it.onlyOffer().run { totalPrice / quantity } }!!
    }
  )

  private fun Inventory.logStats() {
    items.groupBy { it.brewery }
      .forEach { (key, group) -> logger.info("Scraped (${key}): ${group.size}") }
    logger.info("Scraped (TOTAL): ${items.size}")
  }

  private fun Item.onlyOffer() = offers.single()

  private data class ItemGroupFields(
    val brewery: String,
    val name: String,
    val keg: Boolean
  )
}
