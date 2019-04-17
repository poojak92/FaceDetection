package id.singd.android.signdsdk.core

import id.singd.android.signdsdk.core.annotations.Predecessors
import id.singd.android.signdsdk.core.annotations.Provides
import org.kodein.di.Kodein
import kotlin.reflect.KClass

class KPluginCommand(private val plugin: KClass<*>, private val creatorDelegate: (Kodein) -> IPlugin) :
    IPluginCommand {


    override fun createInstance(container: Kodein): IPlugin {
        return creatorDelegate(container)
    }

    override fun getProvides(): Array<Property> {
        return (plugin.annotations.firstOrNull() { it is Provides } as Provides? )?.provides ?: emptyArray()
    }

    override fun getPreconditions(): Array<Property> {
        return (plugin.annotations.firstOrNull() { it is Predecessors } as Predecessors? )?.predecessors ?: emptyArray()
    }
}