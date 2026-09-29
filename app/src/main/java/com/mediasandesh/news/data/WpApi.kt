package com.mediasandesh.news.data

import retrofit2.http.GET
import retrofit2.http.Query

interface WpApi {
    @GET("wp/v2/posts")
    suspend fun posts(
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1,
        @Query("categories") category: Int? = null,
        @Query("search") search: String? = null,
        @Query("exclude") exclude: Int? = null,
        @Query("_embed") embed: Int = 1
    ): List<WpPost>

    @GET("wp/v2/posts")
    suspend fun post(
        @Query("include") include: Int,
        @Query("_embed") embed: Int = 1
    ): List<WpPost>

    @GET("wp/v2/posts")
    suspend fun recent(
        @Query("per_page") perPage: Int = 50,
        @Query("_fields") fields: String = "id,date,categories"
    ): List<WpPost>

    @GET("wp/v2/categories")
    suspend fun categories(
        @Query("per_page") perPage: Int = 100,
        @Query("orderby") orderBy: String = "count",
        @Query("order") order: String = "desc",
        @Query("hide_empty") hideEmpty: Boolean = true
    ): List<WpCategory>

    @GET("wp/v2/pages/4411")
    suspend fun youtubePage(): WpPage
}

data class WpPage(val content: Rendered = Rendered())
