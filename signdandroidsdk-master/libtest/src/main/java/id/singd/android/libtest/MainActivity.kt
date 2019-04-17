package id.singd.android.libtest

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import id.singd.android.signdsdk.HttpMethods
import id.singd.android.signdsdk.HttpRequest
import id.singd.android.signdsdk.HttpResponse
import id.singd.android.signdsdk.SigndSdk
import id.singd.android.signdsdk.core.IHostAppCallback
import id.singd.android.signdsdk.core.SigndTrustManager
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class MainActivity : AppCompatActivity(), IHostAppCallback  {

    private lateinit var recycler: RecyclerView
    var mApiKey: String = ""

    private val list = emptyList<String>().toMutableList()

    override fun onResult(result: String) {
        val json = JSONObject(result)
        for (key in json.keys()){
            list.add("$key -> ${json.getString(key)}")
        }
    }

    override fun performHttpCall(request: HttpRequest): Callable<HttpResponse>{
        return Callable {
            val trustManager = SigndTrustManager.getInstance(this)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
            val sslSocketFactory = sslContext.getSocketFactory()
            val client = OkHttpClient().newBuilder()
                    .connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(1, TimeUnit.MINUTES)
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .build()
            val headers = request.singleValueHeaders.toMutableMap()
            headers.putAll(request.multiValueHeaders.mapValues { it ->
                it.value.joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]"
                )
            })
            lateinit var response : Response
            if(request.method == HttpMethods.GET) {
                //response = khttp.get(request.url, headers = headers)
            } else if (request.method == HttpMethods.POST) {
                val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), request.body)
                val req = Request.Builder()
                        .url(request.url)
                        .post(body)
                        .build()
                response = client.newCall(req).execute()
                //response = khttp.post(request.url, headers = headers, json = request.body)
            } else if (request.method == HttpMethods.PUT){
                //response = khttp.put(request.url, headers = headers, json = request.body)
            } else {
                //response = khttp.delete(request.url, headers = headers)
            }

            if(response.body() != null)
                HttpResponse(body = response.body()!!.string())
            else
                HttpResponse()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler = findViewById<RecyclerView>(R.id.recyclerView).apply{
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        val go = findViewById<Button>(R.id.btn_go)
        go.setOnClickListener {
            mApiKey = "cGF0cmljazpiODM0NTA2NS01MTI1LTRjYjItOGYyNi1kMTU4ZDY3OGZiZDI="
            val intent = SigndSdk.Builder(this).setApiKey(mApiKey).setHostAppCallback(this).build()
         //   val mIntent = Intent(this, MainActivity::class.java)
          //  val intent = SigndSdk.Builder(mIntent).setApiKey("").setHostAppCallback(this).build()

            startActivityForResult(intent, 123)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 123 && resultCode == Activity.RESULT_OK){
            Log.d("lib==MAINACTIVITY: ", data?.getStringExtra("result"))
            //val json = JSONObject(data.getStringExtra("result"))
            // TODO show in recycler view
            //recycler.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list)
        }
    }

}
