package id.singd.android.signdsdk

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.util.JsonReader
import id.singd.android.signdsdk.commands.ExecuteOnSigndCallbackCommand
import id.singd.android.signdsdk.commands.IMessenger
import id.singd.android.signdsdk.core.IHostAppCallback
import id.singd.android.signdsdk.core.IPluginCommand
import id.singd.android.signdsdk.core.ISigndCallback
import id.singd.android.signdsdk.core.Property
import id.singd.android.signdsdk.messenger.ExecuteOnInitActivityCommand
import id.singd.android.signdsdk.pluginchooser.IPluginChooser
import kotlinx.coroutines.experimental.async
import org.json.JSONException
import org.json.JSONObject
import org.kodein.di.Kodein
import java.util.concurrent.*
import kotlin.collections.HashMap

/**
 * This class manages the selection of new plugins, stores the results.
 * It also handles the communication with the host app.
 */
internal class PluginContainer(
    private val pluginChooser: IPluginChooser,
    private val messenger: IMessenger
) : ISigndCallback {

    private val executorService = Executors.newCachedThreadPool()

    /**
     * Callback to the host app
     */
    internal var hostAppCallback: IHostAppCallback? = null

    /**
     * Callback to the host app
     */
    internal var apiKey: String? = ""

    /**
     * cache for results
     */
    private val result: MutableMap<Property, Any> = HashMap()

    /**
     * Property to access the DIContainer, which is responsible to provide instances of dependencies
     */
    internal var diContainer: Kodein
        get() {
            return pluginChooser.diContainer
        }
        set(value) {
            pluginChooser.diContainer = value
        }

    /**
     * inital function to register for commands
     */
    init {
        messenger.register(ExecuteOnSigndCallbackCommand::class.qualifiedName.orEmpty(), this)
        result[Property.TransactionTimestamp] = System.currentTimeMillis().toString()
        result[Property.TransactionId] = "-1"
    }

    /**
     * function used to register functions at for later processing
     */
    internal fun registerPlugin(pluginCommand: IPluginCommand) {
        pluginChooser.registerPlugin(pluginCommand)
    }

    override fun performHttpCall(request: HttpRequest): Future<HttpResponse> {
        val requestWithAuth = HttpRequest(
            request.url,
            request.method,
            request.isHttps,
            request.body,
            request.singleValueHeaders.toMutableMap().apply {
                put(
                    "Authorization",
                    "Basic $apiKey"
                )
                put("Content-Type", "application/json")

            },
            request.multiValueHeaders
        )
        val future = if (hostAppCallback != null)
            (hostAppCallback as IHostAppCallback).performHttpCall(requestWithAuth)
        else
            Callable { return@Callable HttpResponse() }
        return executorService.submit(future)

    }

    override fun nextPlugin() {
        if(result.containsKey(Property.TransactionId) && result[Property.TransactionId] == "-1"){
            val req = HttpRequest(
                "https://signd.io:4000/api/v1/onboard/sessions",
                HttpMethods.POST,
                singleValueHeaders = hashMapOf(
                    "Authorization" to "Basic $apiKey",
                    "Content-Type" to "application/json"
                )
            )
            val resp = performHttpCall(req).get()
            try {
                val json = JSONObject(resp.body)
                if (json.has("id")) {
                    result[Property.TransactionId] = json.get("id")
                }
            } catch (e: JSONException){

            }

        }
        when {
            result[Property.TransactionId] == "-1" -> messenger.postCommand(ExecuteOnInitActivityCommand {
                if (it is Activity) {
                    pluginChooser.reset()
                    val js = JSONObject()
                    js.put("error", "Authorization failed")
                    if (hostAppCallback != null) {
                        (hostAppCallback as IHostAppCallback).onResult(js.toString())
                    }

                    it.setResult(RESULT_CANCELED)
                    it.finish()
                }
            })
            pluginChooser.hasNext() -> {
                val plugin = pluginChooser.next()
                plugin.registrationStep(result)
            }
            else -> {
                val req = HttpRequest(
                    "https://signd.io:4000/api/v1/onboard/sessions/${result[Property.TransactionId]}/complete",
                    HttpMethods.PUT,
                    singleValueHeaders = hashMapOf(
                        "Authorization" to "Basic $apiKey"
                    )
                )
                performHttpCall(req).get()
                messenger.postCommand(ExecuteOnInitActivityCommand {
                    if (it is Activity) {
                        pluginChooser.reset()

                        //val intent = it.intent

                        val js = JSONObject()
                        result.forEach { entry -> js.put(entry.key.name, entry.value) }
                        if (hostAppCallback != null) {
                            (hostAppCallback as IHostAppCallback).onResult(js.toString())
                        }
                        //intent.putExtra("result", js.toString())
                        result[Property.TransactionId] = "-1"
                        it.setResult(RESULT_OK)
                        it.finish()
                    }
                })
            }
        }
    }

    override fun updateProperties(properties: Map<Property, Any>) {
        for (pair in properties) {
            result[pair.key] = pair.value
        }
    }

}