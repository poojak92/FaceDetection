package id.singd.android.mitek

import android.app.Activity
import android.content.Intent
import id.singd.android.signdsdk.commands.ExecuteOnActivityCommand
import id.singd.android.signdsdk.commands.IMessenger
import id.singd.android.signdsdk.core.IPlugin
import id.singd.android.signdsdk.core.Property

class MitekPlugin(override val messenger: IMessenger) : IPlugin {

    override fun registrationStep(properties: Map<Property, Any>) {
        messenger.postCommand(ExecuteOnActivityCommand {
            if (it is Activity) {
                val intent = Intent(it, InitActivity::class.java)
                if (properties.containsKey(Property.TransactionId))
                    intent.putExtra("transactionId", properties[Property.TransactionId] as String)
                it.startActivityForResult(intent, 12)
            }
        })
    }
}