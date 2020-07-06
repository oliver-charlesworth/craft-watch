package watch.craft.executor

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.jsoup.Jsoup
import watch.craft.FatalScraperException
import watch.craft.NonFatalScraperException
import watch.craft.Scraper
import watch.craft.Scraper.IndexEntry
import watch.craft.Scraper.ScrapedItem
import watch.craft.SkipItemException
import watch.craft.storage.CachingGetter
import java.net.URI

class ScraperAdapter(
  private val getter: CachingGetter,
  private val scraper: Scraper,
  private val rateLimitPeriodMillis: Int = 10
) {
  data class Result(
    val breweryName: String,
    val entry: IndexEntry,
    val item: ScrapedItem
  )

  private val logger = KotlinLogging.logger {}
  private val breweryName = scraper.brewery.shortName

  suspend fun execute() = coroutineScope {
    scraper.rootUrls
      .map { rootUrl ->
        async {
          onIoThread { scrapeIndexSafely(rootUrl) }
            .mapIndexed { idx, entry ->
              async {
                delay(idx * rateLimitPeriodMillis.toLong())
                onIoThread { scrapeItemSafely(entry)?.let { Result(breweryName, entry, it) } }
              }
            }
            .mapNotNull { it.await() }
        }
      }
      .flatMap { it.await() }
  }

  private fun scrapeIndexSafely(url: URI): List<IndexEntry> {
    logger.info("[${breweryName}] Scraping index: ${url}")
    return try {
      scraper.scrapeIndex(request(url))
    } catch (e: NonFatalScraperException) {
      logger.warn("[${breweryName}] Error while scraping brewery", e)
      emptyList()
    } catch (e: FatalScraperException) {
      logger.error("[${breweryName}] Fatal error while scraping brewery", e)
      throw e
    } catch (e: Exception) {
      logger.warn("[${breweryName}] Unexpected error while scraping brewery", e)
      emptyList()
    }
  }

  private fun scrapeItemSafely(entry: IndexEntry): ScrapedItem? {
    logger.info("[${breweryName}] Scraping [${entry.rawName}]")
    return try {
      entry.scrapeItem(request(entry.url))
    } catch (e: SkipItemException) {
      logger.info("[${breweryName}] Skipping [${entry.rawName}] because: ${e.message}")
      null
    } catch (e: NonFatalScraperException) {
      logger.warn("[${breweryName}] Error while scraping [${entry.rawName}]", e)
      null
    } catch (e: FatalScraperException) {
      logger.error("[${breweryName}] Error while scraping [${entry.rawName}]", e)
      throw e
    } catch (e: Exception) {
      logger.warn("[${breweryName}] Unexpected error while scraping [${entry.rawName}]", e)
      null
    }
  }

  private fun request(url: URI) = try {
    Jsoup.parse(String(getter.request(url)), url.toString())!!
  } catch (e: Exception) {
    throw FatalScraperException("Could not read page: ${url}", e)
  }
}
