package id.singd.android.signdsdk

import java.util.*

data class HttpRequest(var url: String,
                       var method: HttpMethods = HttpMethods.GET,
                       var isHttps : Boolean = true,
                       var body : String = "",
                       var singleValueHeaders : Map<String, String> = emptyMap(),
                       var multiValueHeaders: Map<String, List<String>> = emptyMap()
                       ) {
}