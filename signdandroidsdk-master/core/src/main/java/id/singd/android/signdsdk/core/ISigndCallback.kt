package id.singd.android.signdsdk.core

import id.singd.android.signdsdk.HttpRequest
import id.singd.android.signdsdk.HttpResponse
import java.util.concurrent.Future

interface ISigndCallback {
    fun performHttpCall(request: HttpRequest): Future<HttpResponse>
    fun nextPlugin()
    fun updateProperties(properties: Map<Property, Any>)
    //fun showFragment(@IdRes idRes: Int, fragment: Fragment, tag: String?)

}