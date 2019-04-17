package id.singd.android.mitek

import android.arch.lifecycle.ViewModel
import id.singd.android.signdsdk.commands.IMessenger
import id.singd.android.signdsdk.core.Property
import java.util.*

class InitActivityViewModel : ViewModel() {

    internal var retries = 0

    internal lateinit var messenger: IMessenger

    internal val resultDict = Hashtable<Property, Any>()
}