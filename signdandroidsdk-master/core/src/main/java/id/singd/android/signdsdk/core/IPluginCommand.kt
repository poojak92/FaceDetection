package id.singd.android.signdsdk.core

import org.kodein.di.Kodein

interface IPluginCommand {

    fun createInstance(container: Kodein) : IPlugin

    fun getProvides() : Array<Property>

    fun getPreconditions() : Array<Property>
}