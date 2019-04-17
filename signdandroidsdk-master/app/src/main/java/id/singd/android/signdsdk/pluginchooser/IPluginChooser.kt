package id.singd.android.signdsdk.pluginchooser

import id.singd.android.signdsdk.core.IPlugin
import id.singd.android.signdsdk.core.IPluginCommand
import org.kodein.di.Kodein

internal interface IPluginChooser : Iterator<IPlugin> {

    var diContainer: Kodein
    fun registerPlugin(pluginCommand: IPluginCommand)
    fun reset()
}