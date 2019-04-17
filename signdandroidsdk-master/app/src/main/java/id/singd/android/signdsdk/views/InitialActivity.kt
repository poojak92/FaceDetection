package id.singd.android.signdsdk.views

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import id.singd.android.signdsdk.R
import id.singd.android.signdsdk.commands.ExecuteOnActivityCommand
import id.singd.android.signdsdk.commands.ExecuteOnSigndCallbackCommand
import id.singd.android.signdsdk.commands.ICommand
import id.singd.android.signdsdk.core.ISigndCallback
import id.singd.android.signdsdk.factories.KodeinViewModelFactory
import id.singd.android.signdsdk.messenger.ExecuteOnInitActivityCommand
import id.singd.android.signdsdk.viewmodels.InitialActivityViewModel

class InitialActivity : AppCompatActivity() {

    private lateinit var viewModel: InitialActivityViewModel

    private var commandBuffer: ICommand? = ExecuteOnSigndCallbackCommand {
        if (it is ISigndCallback) {
            it.nextPlugin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)
        /*supportFragmentManager.beginTransaction()
            .replace(R.id.action_bar_container, null, null)
            .addToBackStack(null)
            .commit()*/
        viewModel = ViewModelProviders.of(this, KodeinViewModelFactory.getInstance())
            .get(InitialActivityViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        viewModel.messenger.register(ExecuteOnActivityCommand::class.qualifiedName.orEmpty(), this)
        viewModel.messenger.register(
            ExecuteOnInitActivityCommand::class.qualifiedName.orEmpty(),
            this
        )
        if (commandBuffer != null) {
            val command = commandBuffer
            commandBuffer = null
            viewModel.messenger.postCommand(command!!)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.messenger.deregister(
            ExecuteOnActivityCommand::class.qualifiedName.orEmpty(),
            this
        )
        viewModel.messenger.deregister(
            ExecuteOnInitActivityCommand::class.qualifiedName.orEmpty(),
            this
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        commandBuffer = ExecuteOnSigndCallbackCommand {
            if (it is ISigndCallback) {
                it.nextPlugin()
            }
        }
    }
}
