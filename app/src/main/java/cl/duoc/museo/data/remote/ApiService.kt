package cl.duoc.museo.data.remote

import cl.duoc.museo.data.model.Museum
import cl.duoc.museo.data.model.Product
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("api/v1/museums")
    suspend fun getVirtualMuseums(): Response<List<Museum>>

    @GET("api/v1/museums/{id}")
    suspend fun getMuseumById(@Path("id") id: String): Response<Museum>

    @GET("api/v1/products")
    suspend fun getProducts(): Response<List<Product>>
}