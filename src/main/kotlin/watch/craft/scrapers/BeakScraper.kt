package watch.craft.scrapers

import org.jsoup.nodes.Document
import watch.craft.*
import watch.craft.Scraper.IndexEntry
import watch.craft.Scraper.ScrapedItem
import java.net.URI

class BeakScraper : Scraper {
  override val name = "Beak"
  override val rootUrls = listOf(URI("https://beakbrewery.com/collections/beer"))

  override fun scrapeIndex(root: Document) = root
    .selectMultipleFrom(".collection .product_thumb")
    .map { el ->
      val a = el.selectFrom("a")
      val rawName = a.textFrom("p")

      IndexEntry(rawName, a.hrefFrom()) { doc ->
        val desc = doc.selectFrom(".product_description")
        val allTheText = "${rawName}\n${desc.text()}"

        ScrapedItem(
          name = rawName.split(" ")[0].toTitleCase(),
          summary = null,
          desc = desc.normaliseParagraphsFrom(),
          sizeMl = allTheText.extract(ML_REGEX, ignoreCase = true)[1].toInt(),
          abv = allTheText.extract(ABV_REGEX)[1].toDouble(),
          available = !a.text().contains("Sold Out", ignoreCase = true),
          numItems = allTheText.maybeExtract(NUM_ITEMS_REGEX, ignoreCase = true)?.get(1)?.toInt() ?: 1,
          price = a.priceFrom(".price"),
          thumbnailUrl = a.srcFrom("img")
        )
      }
    }

  companion object {
    const val ML_REGEX = "(\\d+)\\s*ml"
    const val ABV_REGEX = "(\\d+(\\.\\d+)?)\\s*%"
    const val NUM_ITEMS_REGEX = "(\\d+)\\s*x"
  }
}
