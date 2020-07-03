package watch.craft

import watch.craft.Scraper.ScrapedItem
import watch.craft.executor.ScraperExecutor

private const val GOLDEN_DATE = "2020-06-30"

fun executeScraper(scraper: Scraper, dateString: String? = GOLDEN_DATE): List<ScrapedItem> {
  val setup = Setup(dateString)
  return ScraperExecutor(setup.getter, scraper).execute().map { it.item }
}

fun List<ScrapedItem>.byName(name: String) = first { it.name == name }

fun ScrapedItem.noDesc() = copy(desc = null)    // Makes it easier to test item equality
