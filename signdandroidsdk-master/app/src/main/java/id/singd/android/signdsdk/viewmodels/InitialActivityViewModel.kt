package id.singd.android.signdsdk.viewmodels

import android.arch.lifecycle.ViewModel
import id.singd.android.signdsdk.commands.IMessenger
import id.singd.android.signdsdk.core.ILogger

class InitialActivityViewModel : ViewModel() {

    lateinit var messenger: IMessenger
    lateinit var logger: ILogger

}