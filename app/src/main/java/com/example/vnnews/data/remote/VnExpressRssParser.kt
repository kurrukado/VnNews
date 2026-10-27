package com.example.vnnews.data.remote

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

object VnExpressRssParser {
    fun parse(inputStream: InputStream): List<RssItemDto> {
        val items = mutableListOf<RssItemDto>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var title = ""
        var link = ""
        var pubDate = ""
        var guid = ""
        var description = ""
        var enclosureUrl: String? = null
        var inItem = false
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("item", ignoreCase = true)) {
                        inItem = true
                        title = ""
                        link = ""
                        pubDate = ""
                        guid = ""
                        description = ""
                        enclosureUrl = null
                    } else if (inItem && currentTag.equals("enclosure", ignoreCase = true)) {
                        val url = parser.getAttributeValue(null, "url")
                        if (!url.isNullOrBlank()) {
                            enclosureUrl = url
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        val text = parser.text ?: ""
                        when (currentTag.lowercase()) {
                            "title" -> title += text
                            "link" -> link += text
                            "pubdate" -> pubDate += text
                            "guid" -> guid += text
                            "description" -> description += text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true) && inItem) {
                        if (title.isNotBlank() && link.isNotBlank()) {
                            items.add(
                                RssItemDto(
                                    title = title.trim(),
                                    pubDate = pubDate.trim(),
                                    link = link.trim(),
                                    guid = guid.trim().ifBlank { link.trim() },
                                    thumbnail = enclosureUrl,
                                    description = description.trim(),
                                    enclosure = enclosureUrl?.let { EnclosureDto(it) }
                                )
                            )
                        }
                        inItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return items
    }
}
