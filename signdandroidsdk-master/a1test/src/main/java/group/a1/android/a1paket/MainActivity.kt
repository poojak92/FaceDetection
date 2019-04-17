package group.a1.android.a1paket

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Button
import id.singd.android.signdsdk.HttpMethods
import id.singd.android.signdsdk.HttpRequest
import id.singd.android.signdsdk.HttpResponse
import org.json.JSONObject
import java.util.concurrent.Callable
import id.singd.android.signdsdk.SigndSdk
import id.singd.android.signdsdk.core.IHostAppCallback
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder




class MainActivity : AppCompatActivity(), IHostAppCallback {

    private lateinit var recycler: RecyclerView

    private val list = emptyList<String>().toMutableList()

    override fun onResult(result: String) {
        val json = JSONObject(result)
        for (key in json.keys()) {
            list.add("$key -> ${json.getString(key)}")
        }
    }

    override fun performHttpCall(request: HttpRequest): Callable<HttpResponse> {
        return Callable {
            val connection = URL(request.url).openConnection() as HttpURLConnection;
            connection.connectTimeout = 30000 //30 Seconds
            connection.readTimeout = 30000 //30 Seconds
            request.singleValueHeaders.entries.forEach{ connection.setRequestProperty(it.key, it.value) }
            request.multiValueHeaders.entries.forEach{ it.value.forEach { value -> connection.setRequestProperty(it.key, value) } }
            connection.requestMethod = request.method.name
            lateinit var response: HttpResponse
            try {
                if (request.method == HttpMethods.POST) {
                    if(!request.body.isEmpty()){
                        connection.doOutput = true;
                        val byteContent = request.body.toByteArray()
                        connection.setFixedLengthStreamingMode(byteContent.count())
                        val out = BufferedOutputStream(connection.getOutputStream())
                        out.write(byteContent)
                    }
                } else if (request.method == HttpMethods.PUT) {
                    if(!request.body.isEmpty()){
                        connection.doOutput = true;
                        val byteContent = request.body.toByteArray()
                        connection.setFixedLengthStreamingMode(byteContent.count())
                        val out = BufferedOutputStream(connection.getOutputStream())
                        out.write(byteContent)
                    }
                }

                val result = mutableListOf<Byte>()
                try {
                    val instream = BufferedInputStream(connection.inputStream)
                    for(bytesRead in instream.iterator()) {
                        result.add(bytesRead)
                    }
                } catch (e: IOException){
                    val instream = BufferedInputStream(connection.errorStream)
                    for(bytesRead in instream.iterator()) {
                        result.add(bytesRead)
                    }
                }

                response = HttpResponse(body = String(result.toByteArray()), headers = connection.headerFields)
            } catch (e: Exception){
                response = HttpResponse(error = e)
            }finally {
                connection.disconnect()
            }
            return@Callable response
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler = findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        val go = findViewById<Button>(R.id.btn_go)
        go.setOnClickListener {
          //  val mIntent = Intent(this, MainActivity::class.java)
          //  val intent = SigndSdk.Builder(mIntent).setApiKey("").setHostAppCallback(this).build()

            val intent = SigndSdk.Builder(this).setApiKey("").setHostAppCallback(this).build()
            startActivityForResult(intent, 123)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
        //    val json = JSONObject(data?.getStringExtra("result"))
          //  val contactUri = data?.getData()
          // Log.d("MainActivity", contactUri.toString())

            // TODO show in recycler view
           // recycler.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list)
        }
    }
}
