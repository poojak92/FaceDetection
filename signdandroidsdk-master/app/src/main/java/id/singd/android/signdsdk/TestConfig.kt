package id.singd.android.signdsdk

import id.singd.android.mitek.MitekPlugin
import id.singd.android.signdsdk.commands.IMessenger
import id.singd.android.signdsdk.core.IConfigurator
import id.singd.android.signdsdk.core.ILogger
import id.singd.android.signdsdk.core.IPluginCommand
import id.singd.android.signdsdk.core.KPluginCommand
//import id.singd.android.signdsdk.demoplugin.DemoPlugin
import id.singd.android.signdsdk.logging.AndroidLogger
import id.singd.android.signdsdk.messenger.Messenger
import id.singd.android.signdsdk.pluginchooser.IPluginChooser
import id.singd.android.signdsdk.pluginchooser.InOrderChooser
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.kodein.di.newInstance

internal class TestConfig : IConfigurator {
    override fun createDiSetUp(): Kodein {
        return Kodein {
            bind<ILogger>() with singleton { AndroidLogger() }
            bind<IMessenger>() with singleton { Messenger(instance()) }
            bind<IPluginChooser>() with singleton { InOrderChooser() }
        }
    }

    override fun getPluginCommands(): Array<IPluginCommand> {
        return arrayOf(
            /*KPluginCommand(DemoPlugin::class) { container ->
                (container.direct.newInstance {
                    DemoPlugin(
                        instance()
                    )
                })
            },*/
            KPluginCommand(MitekPlugin::class) { container ->
                (container.direct.newInstance {
                    MitekPlugin(
                        instance()
                    )
                })
            }
        )
    }
}