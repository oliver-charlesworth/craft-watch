package watch.craft.scrapers

import watch.craft.*
import watch.craft.Scraper.Job.Leaf
import watch.craft.Scraper.ScrapedItem
import java.net.URI

class GipsyHillScraper : Scraper {
  override val brewery = Brewery(
    shortName = "Gipsy Hill",
    name = "Gipsy Hill Brewing",
    location = "Gispy Hill, London",
    websiteUrl = URI("https://gipsyhillbrew.com/")
  )

  override val jobs = forRootUrls(ROOT_URL) { root ->
    root
      .selectMultipleFrom(".product")
      .map { el ->
        val a = el.selectFrom(".woocommerce-LoopProduct-link")
        val rawName = a.textFrom(".woocommerce-loop-product__title")

        Leaf(rawName, a.hrefFrom()) { doc ->
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
            sizeMl = rawSummary.maybe { sizeMlFrom() },
            numItems = numCans,
            price = el.ownTextFrom(".woocommerce-Price-amount").toDouble()
          )
        }
      }
  }

  companion object {
    private val ROOT_URL = URI("https://gipsyhillbrew.com")
  }
}
