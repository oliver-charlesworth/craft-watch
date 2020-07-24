package watch.craft

import org.jsoup.nodes.Document
import java.net.URI

data class ScraperEntry(
  val scraper: Scraper,
  val brewery: Brewery
)

interface Scraper {
  val jobs: List<Job>

  sealed class Work<R> {
    data class JsonWork<R>(val work: (data: Any) -> R) : Work<R>()
    data class HtmlWork<R>(val work: (data: Document) -> R) : Work<R>()
  }

  sealed class Job {
    open val name: String? = null
    abstract val url: URI

    data class More(
      override val url: URI,
      val work: Work<List<Job>>
    ) : Job()

    data class Leaf(
      override val name: String,
      override val url: URI,
      val work: Work<ScrapedItem>
    ) : Job()
  }

  data class ScrapedItem(
    val name: String,
    val summary: String? = null,
    val desc: String? = null,
    val mixed: Boolean = false,
    val offers: Set<Offer> = emptySet(),
    val abv: Double? = null,
    val available: Boolean,
    val thumbnailUrl: URI
  )
}
