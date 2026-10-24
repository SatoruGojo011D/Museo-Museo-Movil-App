package cl.duoc.museo.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanCodeResolverTest {

    @Test
    fun resolvesRegisteredMuseumAndProductCodes() {
        val museum = ScanCodeResolver.resolve("MUS_01")
        val product = ScanCodeResolver.resolve("CAT_03")

        assertEquals(ScannedContentKind.MUSEUM, museum?.kind)
        assertEquals("mus_01", museum?.id)
        assertEquals(ScannedContentKind.PRODUCT, product?.kind)
        assertEquals("prod_3", product?.id)
    }

    @Test
    fun resolvesRegisteredCodeFromUrlPath() {
        val content = ScanCodeResolver.resolve("https://museo.example/catalogo/CAT_02?source=qr")

        assertEquals("prod_2", content?.id)
    }

    @Test
    fun resolvesVideoJsonAndKeepsItsSafeUrl() {
        val content = ScanCodeResolver.resolve(
            """{"titulo":"Video de ejemplo","descripcion":"Descripción del video","url":"https://www.youtube.com/shorts/LcrPS1-jacs"}""",
        )

        assertEquals(ScannedContentKind.VIDEO, content?.kind)
        assertEquals("Video de ejemplo", content?.title)
        assertEquals("Descripción del video", content?.description)
        assertEquals("https://www.youtube.com/shorts/LcrPS1-jacs", content?.externalUrl)
    }

    @Test
    fun rejectsVideoJsonWithUnsafeOrInvalidUrls() {
        val unsafe = """{"titulo":"Video","descripcion":"Descripción","url":"javascript:alert(1)"}"""
        val malformed = """{"titulo":"Video","descripcion":"Descripción","url":"https://bad url"}"""

        assertNull(ScanCodeResolver.resolve(unsafe))
        assertNull(ScanCodeResolver.resolve(malformed))
    }

    @Test
    fun rejectsUnknownAndEmptyCodes() {
        assertNull(ScanCodeResolver.resolve("UNKNOWN_123"))
        assertNull(ScanCodeResolver.resolve("  "))
    }
}
