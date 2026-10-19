package com.gastrack.app.core.network

import com.gastrack.app.core.common.Resource
import com.gastrack.app.data.models.ApiErrorDto
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

suspend fun <T> networkCall(json: Json, block: suspend () -> T): Resource<T> = withContext(Dispatchers.IO) {
    try { Resource.Success(block()) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: MissingSessionException) { Resource.Error(error.message.orEmpty(), 401) }
    catch (error: HttpException) {
        val message = error.response()?.errorBody()?.use { body ->
            runCatching { json.decodeFromString<ApiErrorDto>(body.string()).error }.getOrNull()
        }
        Resource.Error(if (error.code() == 401) "Your session is missing or expired. Please sign in again." else message ?: "Request failed (${error.code()}).", error.code(), error.code() in listOf(408, 429, 502, 503, 504))
    }
    catch (error: SocketTimeoutException) { Resource.Error("The server took too long. Check the current record before retrying a transaction.", retryable = true) }
    catch (error: IOException) { Resource.Error("Unable to connect. Check your connection and try again.", retryable = true) }
    catch (error: SerializationException) { Resource.Error("The server returned an unexpected response.") }
    catch (error: NoSuchElementException) { Resource.Error("The server response is missing required data.") }
    catch (error: ArithmeticException) { Resource.Error("The server returned an invalid numeric value.") }
    catch (error: IllegalStateException) { Resource.Error("The server returned an unexpected response.") }
    catch (error: IllegalArgumentException) { Resource.Error(error.message ?: "Invalid data.") }
}
