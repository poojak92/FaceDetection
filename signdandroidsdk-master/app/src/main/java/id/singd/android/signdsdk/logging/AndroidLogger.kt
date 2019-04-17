package id.singd.android.signdsdk.logging

import android.util.Log
import id.singd.android.signdsdk.core.ILogger

class AndroidLogger : ILogger {
    override fun debug(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun verbose(tag: String, msg: String) {
        Log.v(tag, msg)
    }

    override fun error(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    override fun error(tag: String, error: Throwable) {
        Log.e(tag, error.javaClass.simpleName, error)
    }

    override fun warning(tag: String, msg: String) {
        Log.w(tag, msg)
    }
}