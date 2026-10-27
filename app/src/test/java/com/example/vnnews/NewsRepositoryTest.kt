package com.example.vnnews

import com.example.vnnews.data.remote.EnclosureDto
import com.example.vnnews.data.remote.RssItemDto
import com.example.vnnews.data.repository.NetworkNewsRepository
import com.example.vnnews.fake.FakeArticleDao
import com.example.vnnews.fake.FakeNewsApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [NetworkNewsRepository] and [RssItemDto] mapping logic
 * (Unit 5: Retrofit 2 + rss2json, Coil fallback extraction; Unit 6: Room bookmarking).
 */
class NewsRepositoryTest {

    private lateinit var fakeApiService: FakeNewsApiService
    private lateinit var fakeDao: FakeArticleDao
    private lateinit var repository: NetworkNewsRepository

    private val sampleDto = RssItemDto(
        title = "Tin tuc thoi su",
        pubDate = "Sun, 27 Oct 2026 12:00:00 +0700",
        link = "https://vnexpress.net/thoi-su/tin-moi-12345.html",
        thumbnail = "https://example.com/thumb.jpg",
        description = "<p>Mo ta ngan gon ve tin tuc</p>"
    )

    @Before
    fun setUp() {
        fakeApiService = FakeNewsApiService()
        fakeDao = FakeArticleDao()
        repository = NetworkNewsRepository(fakeApiService, fakeDao)
    }

    // ==========================================
    // Repository Categories Tests
    // ==========================================

    @Test
    fun getCategories_returnsFiveRequiredVnExpressCategories() {
        val categories = repository.getCategories()

        assertEquals(5, categories.size)
        val ids = categories.map { it.id }
        assertTrue(ids.contains("tin-moi-nhat"))
        assertTrue(ids.contains("so-hoa"))
        assertTrue(ids.contains("kinh-doanh"))
        assertTrue(ids.contains("the-thao"))
        assertTrue(ids.contains("giai-tri"))

        // All category URLs must point to valid VnExpress RSS endpoints
        categories.forEach { category ->
            assertTrue(
                "Category URL should point to vnexpress.net RSS",
                category.rssUrl.startsWith("https://vnexpress.net/rss/")
            )
        }
    }

    // ==========================================
    // Repository Network Fetching Tests
    // ==========================================

    @Test
    fun getNews_fetchesFeedFromApiServiceAndReturnsItems() = runTest {
        fakeApiService.responseItems = listOf(sampleDto)

        val items = repository.getNews("https://vnexpress.net/rss/tin-moi-nhat.rss")

        assertEquals(1, items.size)
        assertEquals(sampleDto.title, items[0].title)
        assertEquals(sampleDto.link, items[0].link)
    }

    // ==========================================
    // Repository Bookmark Toggling Tests
    // ==========================================

    @Test
    fun toggleBookmark_whenArticleNotBookmarked_insertsNewEntityWithBookmarkTrue() = runTest {
        repository.toggleBookmark(sampleDto)

        val bookmarkedArticles = repository.getBookmarkedArticles().first()
        assertEquals(1, bookmarkedArticles.size)
        val entity = bookmarkedArticles[0]
        assertEquals(sampleDto.link, entity.link)
        assertEquals(sampleDto.title, entity.title)
        assertEquals("Mo ta ngan gon ve tin tuc", entity.description)
        assertEquals(sampleDto.thumbnail, entity.imageUrl)
        assertTrue(entity.isBookmarked)
    }

    @Test
    fun toggleBookmark_whenArticleAlreadyBookmarked_updatesStatusToFalse() = runTest {
        // First toggle: bookmarks the article
        repository.toggleBookmark(sampleDto)
        var bookmarkedArticles = repository.getBookmarkedArticles().first()
        assertEquals(1, bookmarkedArticles.size)

        // Second toggle: unbookmarks the article
        repository.toggleBookmark(sampleDto)
        bookmarkedArticles = repository.getBookmarkedArticles().first()
        assertTrue("Bookmarked list should be empty after toggling off", bookmarkedArticles.isEmpty())
    }

    @Test
    fun removeBookmark_updatesBookmarkStatusToFalse() = runTest {
        repository.toggleBookmark(sampleDto)
        assertEquals(1, repository.getBookmarkedArticles().first().size)

        repository.removeBookmark(sampleDto.link)
        val bookmarkedArticles = repository.getBookmarkedArticles().first()
        assertTrue(bookmarkedArticles.isEmpty())
    }

    @Test
    fun isBookmarkedFlow_emitsAccurateBookmarkStatus() = runTest {
        val flow = repository.isBookmarkedFlow(sampleDto.link)
        assertFalse(flow.first())

        repository.toggleBookmark(sampleDto)
        assertTrue(flow.first())

        repository.toggleBookmark(sampleDto)
        assertFalse(flow.first())
    }

    // ==========================================
    // RssItemDto Fallback Image Extraction Tests
    // ==========================================

    @Test
    fun getImageUrl_withThumbnailPresent_returnsThumbnail() {
        val dto = RssItemDto(
            title = "Title",
            link = "https://example.com/1",
            thumbnail = "https://example.com/thumbnail.jpg",
            enclosure = EnclosureDto("https://example.com/enclosure.jpg"),
            description = """<img src="https://example.com/desc.jpg">"""
        )
        assertEquals("https://example.com/thumbnail.jpg", dto.getImageUrl())
    }

    @Test
    fun getImageUrl_withThumbnailNull_andEnclosurePresent_returnsEnclosureLink() {
        val dto = RssItemDto(
            title = "Title",
            link = "https://example.com/2",
            thumbnail = null,
            enclosure = EnclosureDto("https://example.com/enclosure.jpg"),
            description = """<img src="https://example.com/desc.jpg">"""
        )
        assertEquals("https://example.com/enclosure.jpg", dto.getImageUrl())
    }

    @Test
    fun getImageUrl_withThumbnailAndEnclosureNull_extractsImgSrcFromDescription() {
        val dto = RssItemDto(
            title = "Title",
            link = "https://example.com/3",
            thumbnail = null,
            enclosure = null,
            description = """<p><a href="https://vnexpress.net"><img src="https://i1-vnexpress.vnecdn.net/sample.jpg" /></a>Nội dung bài viết</p>"""
        )
        assertEquals("https://i1-vnexpress.vnecdn.net/sample.jpg", dto.getImageUrl())
    }

    @Test
    fun getImageUrl_withSingleQuotedImgSrcInDescription_extractsUrlCorrectly() {
        val dto = RssItemDto(
            title = "Title",
            link = "https://example.com/4",
            thumbnail = null,
            enclosure = null,
            description = """<div><img src='https://example.com/single_quote.png'>Chi tiet tin</div>"""
        )
        assertEquals("https://example.com/single_quote.png", dto.getImageUrl())
    }

    @Test
    fun getImageUrl_withNoImagesInAnyField_returnsNull() {
        val dto = RssItemDto(
            title = "Title",
            link = "https://example.com/5",
            thumbnail = null,
            enclosure = null,
            description = "Chỉ là đoạn văn bản thuần túy không chứa thẻ hình ảnh nào cả."
        )
        assertNull(dto.getImageUrl())
    }

    // ==========================================
    // RssItemDto Description HTML Sanitization Tests
    // ==========================================

    @Test
    fun getCleanDescription_stripsAllHtmlTagsAndTrimsWhitespace() {
        val htmlDesc = """
            <p>
                <a href="https://vnexpress.net"><img src="https://example.com/pic.jpg"></a>
                Tập đoàn công nghệ <b>FPT</b> công bố lợi nhuận <i>kỷ lục</i> trong quý 3.
            </p>
        """.trimIndent()

        val dto = RssItemDto(
            title = "Title",
            link = "https://example.com/6",
            description = htmlDesc
        )

        val cleanDesc = dto.getCleanDescription()
        assertFalse(cleanDesc.contains("<p>"))
        assertFalse(cleanDesc.contains("<b>"))
        assertFalse(cleanDesc.contains("<i>"))
        assertFalse(cleanDesc.contains("<img"))
        assertTrue(cleanDesc.contains("Tập đoàn công nghệ FPT công bố lợi nhuận kỷ lục trong quý 3."))
    }

    @Test
    fun getCleanDescription_withNullDescription_returnsEmptyString() {
        val dto = RssItemDto(
            title = "Title",
            link = "https://example.com/7",
            description = null
        )
        assertEquals("", dto.getCleanDescription())
    }
}
