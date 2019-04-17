package id.singd.android.signdsdk

import android.app.Activity
import android.content.Intent
import id.singd.android.signdsdk.core.IConfigurator
import id.singd.android.signdsdk.core.IHostAppCallback
import id.singd.android.signdsdk.factories.KodeinViewModelFactory
import id.singd.android.signdsdk.views.InitialActivity
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.kodein.di.newInstance

class SigndSdk internal constructor(
    configuration: IConfigurator = TestConfig()
) {

    private val pluginContainer: PluginContainer

    init {
        val diConfig = configuration.createDiSetUp()
        pluginContainer = diConfig.direct.newInstance { PluginContainer(instance(), instance()) }
        pluginContainer.diContainer = diConfig
        KodeinViewModelFactory.getInstance(diConfig, diConfig.direct.instance())
        configuration.getPluginCommands().forEach { pluginContainer.registerPlugin(it) }
    }

    private object Holder {
        val INSTANCE = SigndSdk()
    }

    companion object {
        private val instance: SigndSdk by lazy { Holder.INSTANCE }
    }

    class Builder(context: Activity) {
        private val mIntent = Intent(context, InitialActivity::class.java)
        private var mApiKey: String = ""

        fun setApiKey(apiKey: String): Builder {
            mApiKey = apiKey
            return this
        }

        fun setHostAppCallback(callback: IHostAppCallback): Builder {
            instance.pluginContainer.hostAppCallback = callback
            return this
        }

        fun build(): Intent {
            if (instance.pluginContainer.hostAppCallback == null) {
                throw Exception("HostAppCallback has to be specified")
            }
            if (mApiKey.isEmpty()) {
                mApiKey = "cGF0cmljazpiODM0NTA2NS01MTI1LTRjYjItOGYyNi1kMTU4ZDY3OGZiZDI="
            }
            instance.pluginContainer.apiKey = mApiKey
            return mIntent
        }
    }

}