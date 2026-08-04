package com.benchmark.androidnative.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {

    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"
    private const val BENCHMARK_API_HOST = "jsonplaceholder.typicode.com"

    /**
     * OkHttp client untuk benchmark HTTP.
     *
     * Jaringan kampus/kantor sering memakai proxy SSL (sertifikat self-signed).
     * Untuk host API penelitian ini saja, sertifikat diterima agar pengujian
     * tetap berjalan — sama seperti [createBenchmarkDio] di Flutter.
     */
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .apply {
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String,
                ) = Unit

                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String,
                ) = Unit

                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            sslSocketFactory(sslContext.socketFactory, trustManager)
            hostnameVerifier(
                HostnameVerifier { hostname, _ ->
                    hostname == BENCHMARK_API_HOST || hostname.endsWith(".typicode.com")
                },
            )
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
