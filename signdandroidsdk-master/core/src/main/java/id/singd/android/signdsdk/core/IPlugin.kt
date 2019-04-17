package id.singd.android.signdsdk.core

import id.singd.android.signdsdk.commands.IMessenger
import java.util.*

interface IPlugin {
    /*val predecessors: Array<IPlugin>
    val neededProperties: Array<Property>
    val providedProperties: Array<Property>*/
    val messenger: IMessenger
    fun registrationStep(properties: Map<Property, Any>)

}