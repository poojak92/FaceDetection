package id.singd.android.signdsdk.pluginchooser

import id.singd.android.signdsdk.core.IPlugin
import id.singd.android.signdsdk.core.IPluginCommand
import org.kodein.di.Kodein
import kotlin.collections.ArrayList

internal class InOrderChooser() : IPluginChooser {

    override lateinit var diContainer: Kodein
    private val mRing = ArrayList<IPluginCommand>()
    private var mPosition = 0

    override fun hasNext(): Boolean {
        return mPosition < mRing.size
    }

    override fun next(): IPlugin {
        if (mPosition == mRing.size) throw Throwable("No plugin available!")
        val plugin = mRing[mPosition]
        mPosition++
        return plugin.createInstance(diContainer)
    }

    override fun registerPlugin(pluginCommand: IPluginCommand) {
        if (this.mPosition == 0) {
            mRing.add(pluginCommand)
        }
    }

    override fun reset() {
        mPosition = 0
    }
}