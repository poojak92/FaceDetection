package id.singd.android.signdsdk

import java.lang.Exception

data class HttpResponse (var headers: Map<String, List<String>> = emptyMap(),
                         var body : String = "",
                         var error : Exception? = null) {
}