package cl.duoc.museo.utils

enum class ScannedContentKind {
    MUSEUM,
    PRODUCT,
}

data class ScannedContent(
    val id: String,
    val kind: ScannedContentKind,
    val type: String,
    val title: String,
    val author: String,
    val description: String,
)

object ScanCodeResolver {
    private val contentByCode = buildMap {
        fun museum(vararg codes: String, id: String, title: String, location: String) {
            val item = ScannedContent(
                id = id,
                kind = ScannedContentKind.MUSEUM,
                type = "Museo Virtual",
                title = title,
                author = location,
                description = "Explora las colecciones y la visita virtual de $title.",
            )
            codes.forEach { put(it, item) }
        }

        fun product(
            vararg codes: String,
            id: String,
            type: String,
            title: String,
            description: String,
        ) {
            val item = ScannedContent(
                id = id,
                kind = ScannedContentKind.PRODUCT,
                type = type,
                title = title,
                author = "Museo de los Museos",
                description = description,
            )
            codes.forEach { put(it, item) }
        }

        museum(
            "MUSEO_COLONIAL_01", "MUS_01", "MUS_001", "MUS_1", "MUSEUM_1",
            id = "mus_01",
            title = "Museo Nacional de Arte Colonial",
            location = "Bogotá, Colombia",
        )
        museum(
            "MUS_02", "MUS_002", "MUS_2", "MUSEUM_2",
            id = "mus_2",
            title = "Museo Frida Kahlo Virtual",
            location = "Ciudad de México, México",
        )
        museum(
            "MUS_03", "MUS_003", "MUS_3", "MUSEUM_3",
            id = "mus_3",
            title = "Museo Oro del Banco de la República",
            location = "Bogotá, Colombia",
        )
        museum(
            "MUS_04", "MUS_004", "MUS_4", "MUSEUM_4",
            id = "mus_4",
            title = "Museo Histórico Nacional de Chile",
            location = "Santiago, Chile",
        )
        museum(
            "MUS_05", "MUS_005", "MUS_5", "MUSEUM_5",
            id = "mus_5",
            title = "Museu de Arte de São Paulo",
            location = "São Paulo, Brasil",
        )
        product(
            "CAT_01", "PROD_1", "ITEM_1",
            id = "prod_1",
            type = "Curso",
            title = "Curso: Museología e Innovación Digital",
            description = "Solicitud de información del curso de museología digital.",
        )
        product(
            "CAT_02", "PUB_01", "BOOK_01", "PROD_2", "ITEM_2",
            id = "prod_2",
            type = "Publicación",
            title = "Libro: El Arte Barroco Virreinal",
            description = "Estudio del arte barroco en el patrimonio iberoamericano.",
        )
        product(
            "CAT_03", "PROD_3", "ITEM_3",
            id = "prod_3",
            type = "Producto",
            title = "Kit Didáctico: Réplicas Arqueológicas",
            description = "Materiales educativos para explorar piezas arqueológicas.",
        )
        product(
            "CAT_04", "PROD_4", "ITEM_4",
            id = "prod_4",
            type = "Curso",
            title = "Diplomado: Curaduría de Arte Iberoamericano",
            description = "Programa de formación profesional en curaduría.",
        )
        product(
            "CAT_05", "PUB_02", "BOOK_02",
            id = "cat_05",
            type = "Publicación",
            title = "Catálogo Conmemorativo Soumaya 3D",
            description = "Catálogo digital de obras y colecciones museológicas.",
        )
        product(
            "CAT_06", "PUB_03", "BOOK_03",
            id = "cat_06",
            type = "Publicación",
            title = "Guía Técnica de Digitalización",
            description = "Manual para digitalizar colecciones y archivos museológicos.",
        )
    }

    fun resolve(rawCode: String): ScannedContent? {
        val payload = rawCode.trim()
            .substringBefore('#')
            .substringBefore('?')
            .substringAfterLast('/')
            .trim()
            .uppercase()
        return contentByCode[payload]
    }
}
