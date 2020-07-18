package watch.craft.scrapers

import watch.craft.Offer
import watch.craft.Scraper
import watch.craft.Scraper.Job.Leaf
import watch.craft.Scraper.ScrapedItem
import watch.craft.SkipItemException
import watch.craft.dsl.*
import java.net.URI

class PollysScraper : Scraper {
  override val jobs = forRootUrls(ROOT_URL) { root ->
    root
      .selectMultipleFrom(".product")
      .map { el ->
        val rawName = el.textFrom(".woocommerce-loop-product__title")
        val a = el.selectFrom(".woocommerce-loop-product__link")

        Leaf(rawName, a.urlFrom()) { doc ->
          if (rawName.containsMatch("mix")) {
            throw SkipItemException("Don't know how to deal with mixed packs")
          }

          val parts = rawName.split(" – ")

          ScrapedItem(
            name = parts[0],
            summary = parts[1],
            desc = doc.formattedTextFrom("#tab-description"),
            mixed = false,
            abv = rawName.abvFrom(),
            available = ".out-of-stock" !in doc,
            offers = setOf(
              Offer(
                totalPrice = doc.priceFrom("#main .woocommerce-Price-amount"),
                sizeMl = POLLYS_CAN_SIZE_ML
              )
            ),
            thumbnailUrl = a.urlFrom("img")
          )
        }
      }
  }

  companion object {
    private val ROOT_URL = URI("https://shop.pollysbrew.co/")

    private const val POLLYS_CAN_SIZE_ML = 440
  }
}
