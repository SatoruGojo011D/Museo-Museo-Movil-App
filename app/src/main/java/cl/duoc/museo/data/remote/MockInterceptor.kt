package cl.duoc.museo.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val uri = chain.request().url.toString()

        val jsonResponse = when {
            uri.contains("api/v1/museums") -> mockMuseumsJson()
            uri.contains("api/v1/products") -> mockProductsJson()
            else -> "[]"
        }

        val mediaType = "application/json".toMediaTypeOrNull()
        val body = jsonResponse.toResponseBody(mediaType)

        return Response.Builder()
            .code(200)
            .message("OK")
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .body(body)
            .addHeader("content-type", "application/json")
            .build()
    }

    private fun mockMuseumsJson(): String {
        return """
            [
                {
                    "id": "mus_1",
                    "name": "Museo del Prado Virtual",
                    "country": "España",
                    "project": "Patrimonio Virreinal y Peninsular",
                    "description": "Una fascinante pinacoteca virtual con las obras de los maestros Velázquez, Goya y El Greco.",
                    "classification": "Arte Clásico / Pintura",
                    "imageUrl": "https://images.unsplash.com/photo-1565008447742-97f6f38c985c",
                    "projectUrl": "https://www.museodelprado.es",
                    "lat": 40.4138,
                    "lng": -3.6921,
                    "isFavorite": false
                },
                {
                    "id": "mus_2",
                    "name": "Museo Frida Kahlo Virtual",
                    "country": "México",
                    "project": "Coyoacán Identidad y Arte",
                    "description": "La Casa Azul muestra la intimidad y la creación de una de las artistas más influyentes de Iberoamérica.",
                    "classification": "Arte Moderno / Bio",
                    "imageUrl": "https://images.unsplash.com/photo-1579783902614-a3fb3927b675",
                    "projectUrl": "https://www.museofridakahlo.org.mx",
                    "lat": 19.3552,
                    "lng": -99.1625,
                    "isFavorite": false
                },
                {
                    "id": "mus_3",
                    "name": "Museo Oro del Banco de la República",
                    "country": "Colombia",
                    "project": "Orfebrería Precolombina",
                    "description": "Alberga una de las colecciones de orfebrería prehispánica más importantes del mundo.",
                    "classification": "Arqueología / Historia",
                    "imageUrl": "https://images.unsplash.com/photo-1582555172866-f73bb12a2ab3",
                    "projectUrl": "https://www.banrepcultural.org/bogota/museo-del-oro",
                    "lat": 4.6017,
                    "lng": -74.0721,
                    "isFavorite": false
                },
                {
                    "id": "mus_4",
                    "name": "Museo Histórico Nacional de Chile",
                    "country": "Chile",
                    "project": "Memoria e Indumentaria",
                    "description": "Ubicado en la Plaza de Armas de Santiago, repasa la historia de Chile desde la época precolombina.",
                    "classification": "Historia / Patrimonio",
                    "imageUrl": "https://images.unsplash.com/photo-1518998053901-5348d3961a04",
                    "projectUrl": "https://www.mhn.gob.cl",
                    "lat": -33.4372,
                    "lng": -70.6506,
                    "isFavorite": false
                },
                {
                    "id": "mus_5",
                    "name": "Museu de Arte de São Paulo (MASP)",
                    "country": "Brasil",
                    "project": "Vanguardia Latinoamericana",
                    "description": "Famoso por su arquitectura icónica en la Avenida Paulista y su impresionante colección de arte occidental.",
                    "classification": "Arte Contemporáneo",
                    "imageUrl": "https://images.unsplash.com/photo-1544531586-fde5298cdd40",
                    "projectUrl": "https://masp.org.br",
                    "lat": -23.5614,
                    "lng": -46.6559,
                    "isFavorite": false
                }
            ]
        """.trimIndent()
    }

    private fun mockProductsJson(): String {
        return """
            [
                {
                    "id": "prod_1",
                    "name": "Curso: Museología e Innovación Digital",
                    "category": "Cursos",
                    "price": 45.00,
                    "imageUrl": "https://images.unsplash.com/photo-1524178232363-1fb2b075b655",
                    "description": "Capacitación interactiva sobre la transformación de los museos físicos a entornos virtuales 3D.",
                    "stock": 15
                },
                {
                    "id": "prod_2",
                    "name": "Libro: El Arte Barroco Virreinal",
                    "category": "Libros",
                    "price": 28.50,
                    "imageUrl": "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c",
                    "description": "Estudio detallado sobre las influencias barrocas en las catedrales y museos iberoamericanos.",
                    "stock": 8
                },
                {
                    "id": "prod_3",
                    "name": "Kit Didáctico: Réplicas Arqueológicas",
                    "category": "Materiales",
                    "price": 32.00,
                    "imageUrl": "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634",
                    "description": "Materiales educativos para la reconstrucción en aula de piezas prehispánicas.",
                    "stock": 20
                },
                {
                    "id": "prod_4",
                    "name": "Diplomado: Curaduría de Arte Iberoamericano",
                    "category": "Cursos",
                    "price": 120.00,
                    "imageUrl": "https://images.unsplash.com/photo-1434030216411-0b793f4b4173",
                    "description": "Programa avanzado para profesionales en gestión y conservación del patrimonio cultural.",
                    "stock": 5
                }
            ]
        """.trimIndent()
    }
}