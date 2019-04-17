package id.singd.android.signdsdk.core

import id.singd.android.signdsdk.HttpRequest
import id.singd.android.signdsdk.HttpResponse
import java.util.concurrent.Callable

/**
 * Interface to
 */
interface IHostAppCallback {
    fun performHttpCall(request: HttpRequest) : Callable<HttpResponse>
    fun onResult(result: String)
}