package id.singd.android.signdsdk.core

import org.kodein.di.Kodein

interface IConfigurator {
    fun createDiSetUp() : Kodein
    fun getPluginCommands() : Array<IPluginCommand>
}