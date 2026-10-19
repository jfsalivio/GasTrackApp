package com.gastrack.app.core.di

import com.gastrack.app.BuildConfig
import com.gastrack.app.core.network.AuthInterceptor
import com.gastrack.app.data.api.GasTrackApi
import com.gastrack.app.data.repository.*
import com.gastrack.app.domain.repository.*
import dagger.*
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun tokens(store: SessionStore): com.gastrack.app.core.network.TokenStorage = store
    @Provides @Singleton fun json() = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
    @Provides @Singleton fun client(auth: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Headers/body logging would disclose passwords, reset tokens and customer data.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder().addInterceptor(auth)
            .addInterceptor { chain ->
                if (chain.request().url.encodedPath.startsWith("/auth/")) chain.proceed(chain.request())
                else logging.intercept(chain)
            }
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false).followRedirects(false).build()
    }
    @Provides @Singleton fun api(client: OkHttpClient, json: Json): GasTrackApi {
        val url = BuildConfig.API_BASE_URL
        require(url.isNotBlank() && (BuildConfig.DEBUG || url.startsWith("https://"))) { "Configure the API base URL" }
        return Retrofit.Builder().baseUrl(url.trimEnd('/') + "/").client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(GasTrackApi::class.java)
    }
    @Provides @Singleton fun auth(impl: AuthRepositoryImpl): AuthRepository = impl
    @Provides @Singleton fun operations(impl: OperationsRepositoryImpl): OperationsRepository = impl
    @Provides @Singleton fun management(impl: ManagementRepositoryImpl): ManagementRepository = impl
    @Provides @Singleton fun fulfillment(impl: FulfillmentRepositoryImpl): FulfillmentRepository = impl
}
