package android.text

import org.jsoup.Jsoup

object Html {
    fun fromHtml(source: String): CharSequence = Jsoup.parse(source).text()
    fun fromHtml(source: String, flags: Int): CharSequence = Jsoup.parse(source).text()
}
