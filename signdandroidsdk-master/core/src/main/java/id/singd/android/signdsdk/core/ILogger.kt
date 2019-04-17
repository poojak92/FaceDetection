package id.singd.android.signdsdk.core

interface ILogger {

    fun debug(tag: String, msg: String)
    fun verbose(tag: String, msg: String)
    fun error(tag: String, msg: String)
    fun error(tag: String, error: Throwable)
    fun warning(tag: String, msg: String)
}