package id.singd.android.signdsdk.demoplugin.viewmodels

import android.arch.lifecycle.ViewModel
import id.singd.android.signdsdk.commands.IMessenger

class DemoPluginViewModel : ViewModel() {

    lateinit var messenger: IMessenger
}