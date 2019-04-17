package id.singd.android.signdsdk.demoplugin.views

import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import id.singd.android.signdsdk.commands.ExecuteOnSigndCallbackCommand
import id.singd.android.signdsdk.core.ISigndCallback
import id.singd.android.signdsdk.core.Property
import id.singd.android.signdsdk.demoplugin.R
import id.singd.android.signdsdk.demoplugin.viewmodels.DemoPluginViewModel
import id.singd.android.signdsdk.factories.KodeinViewModelFactory
import java.util.*

class DemoPluginActivity : AppCompatActivity() {

    private lateinit var btnSubmit : Button

    private lateinit var txtName : EditText

    private lateinit var txtLastname : EditText

    private lateinit var viewModel: DemoPluginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_plugin)
        btnSubmit = findViewById(R.id.btnSubmit)
        txtName = findViewById(R.id.txtName)
        txtLastname = findViewById(R.id.txtLastname)
        viewModel = ViewModelProviders.of(this, KodeinViewModelFactory.getInstance()).get(DemoPluginViewModel::class.java)

        btnSubmit.setOnClickListener{
            val dict = Hashtable<Property, Any>()
            dict[Property.FirstName] = txtName.text
            dict[Property.LastName] = txtLastname.text
            viewModel.messenger.postCommand(ExecuteOnSigndCallbackCommand{
                if(it is ISigndCallback){
                    it.updateProperties(dict)
                }
            })
            setResult(android.app.Activity.RESULT_OK)
            finish()
        }
    }


}
