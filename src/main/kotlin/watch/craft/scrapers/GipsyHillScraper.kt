package watch.craft.scrapers

import org.jsoup.nodes.Document
import watch.craft.*
import watch.craft.Scraper.IndexEntry
import watch.craft.Scraper.ScrapedItem
import java.net.URI

class GipsyHillScraper : Scraper {
  override val name = "Gipsy Hill"
  override val rootUrls = listOf(URI("https://gipsyhillbrew.com"))

  override fun scrapeIndex(root: Document) = root
    .selectMultipleFrom(".product")
    .map { el ->
      val a = el.selectFrom(".woocommerce-LoopProduct-link")
      val rawName = a.textFrom(".woocommerce-loop-product__title")

      IndexEntry(rawName, a.hrefFrom()) { doc ->
        val rawSummary = doc.textFrom(".summary")
        val numCans = with(doc.maybeSelectMultipleFrom(".woosb-title-inner")) {
          if (isEmpty()) {
            1
          } else {
            map { it.extractFrom(regex = "(\\d+) ×")[1].toInt() }.sum()
          }
        }
        val style = rawSummary.maybeExtract("Style: (.*) ABV")?.get(1)
        val mixed = style in listOf("Various", "Mixed")

        val name = rawName.replace(" \\(.*\\)$".toRegex(), "")

        ScrapedItem(
          thumbnailUrl = a.srcFrom(".attachment-woocommerce_thumbnail"),
          name = name,
          summary = if (mixed) null else style,
          desc = doc.maybeWholeTextFrom(".description"),
          mixed = mixed,
          available = true, // TODO
          abv = if (mixed) null else rawSummary.maybeExtract("ABV: (\\d+(\\.\\d+)?)")?.get(1)?.toDouble(),
          sizeMl = rawSummary.maybeExtract("(\\d+)ml")?.get(1)?.toInt(),
          numItems = numCans,
          price = el.ownTextFrom(".woocommerce-Price-amount").toDouble()
        )
      }
    }
}
