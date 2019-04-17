package id.singd.android.signdsdk.demoplugin

import android.app.Activity
import android.content.Intent
import id.singd.android.signdsdk.commands.ExecuteOnActivityCommand
import id.singd.android.signdsdk.commands.IMessenger
import id.singd.android.signdsdk.core.IPlugin
import id.singd.android.signdsdk.core.ISigndCallback
import id.singd.android.signdsdk.core.Property
import id.singd.android.signdsdk.core.annotations.Predecessors
import id.singd.android.signdsdk.core.annotations.Provides
import id.singd.android.signdsdk.demoplugin.views.DemoPluginActivity
import java.util.*

@Provides([Property.FirstName, Property.LastName])
class DemoPlugin(override val messenger: IMessenger) : IPlugin {

    override fun registrationStep(properties: Map<Property, Any>) {
        messenger.postCommand(ExecuteOnActivityCommand{
            if(it is Activity){
                val intent = Intent(it, DemoPluginActivity::class.java)
                it.startActivityForResult(intent, 12)
            }
        })
    }
}