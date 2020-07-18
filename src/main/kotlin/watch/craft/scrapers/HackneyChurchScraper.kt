package watch.craft.scrapers

import watch.craft.Offer
import watch.craft.Scraper
import watch.craft.Scraper.Job.Leaf
import watch.craft.Scraper.ScrapedItem
import watch.craft.dsl.*
import java.net.URI

class HackneyChurchScraper : Scraper {
  override val jobs = forRootUrls(ROOT_URL) { root ->
    root
      .selectMultipleFrom("#Collection .hcbc-collection-grid-item")
      .map { el ->
        val rawName = el.textFrom(".grid-view-item__title")

        Leaf(rawName, el.urlFrom("a.grid-view-item__link")) { doc ->
          val price = el.selectFrom(".price")
          val desc = doc.formattedTextFrom(".hcbc-product-description")
          val allQuantities = desc.collectFromLines { quantityFrom() }
          val distinctSizes = desc.collectFromLines { sizeMlFrom() }.distinct()

          ScrapedItem(
            name = rawName,
            summary = null,
            desc = desc,
            mixed = allQuantities.size > 1,
            abv = null,
            available = !price.containsMatchFrom(regex = "sold out"),
            offers = setOf(
              Offer(
                quantity = allQuantities.sum(),
                totalPrice = price.priceFrom(),
                sizeMl = distinctSizes.singleOrNull()
              )
            ),
            thumbnailUrl = el.urlFrom("img.grid-view-item__image")
          )
        }
      }
  }

  companion object {
    private val ROOT_URL = URI("https://hackneychurchbrew.co/collections/beers")
  }
}
