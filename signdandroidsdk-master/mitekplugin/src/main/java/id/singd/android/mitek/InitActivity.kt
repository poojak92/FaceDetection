package id.singd.android.mitek

import android.app.Activity
import android.app.FragmentTransaction
import android.app.ProgressDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import com.miteksystems.facialcapture.science.api.params.FacialCaptureApi
import com.miteksystems.facialcapture.workflow.FacialCaptureWorkflowActivity
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowParameters
import com.miteksystems.misnap.analyzer.MiSnapAnalyzerResult
import com.miteksystems.misnap.misnapworkflow.MiSnapWorkflowActivity
import com.miteksystems.misnap.params.CameraApi
import com.miteksystems.misnap.params.CreditCardApi
import com.miteksystems.misnap.params.MiSnapApi
import com.miteksystems.misnap.params.MiSnapApi.*
import id.singd.android.signdsdk.HttpMethods
import id.singd.android.signdsdk.HttpRequest
import id.singd.android.signdsdk.HttpResponse
import id.singd.android.signdsdk.commands.ExecuteOnSigndCallbackCommand
import id.singd.android.signdsdk.core.ISigndCallback
import id.singd.android.signdsdk.core.Property
import id.singd.android.signdsdk.core.Property.*
import id.singd.android.signdsdk.factories.KodeinViewModelFactory
import kotlinx.coroutines.experimental.async
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class InitActivity : AppCompatActivity() {

    //private val LICENSE_KEY = "{\"signature\":\"k2vUXI52CpH1Ptz8qmlxH9I/HJamtaldoJryJw/VdWXJ9J8BA5dG2XsDZ6R70Js7UORcLoyigrxD6Hhhov5+x4qU2tPk7V31uiUqR9He84m7xC00osiXwcnaRyY88DVGv8mBDFwWbd892s1qBrAuZpUTN/BvB8h6Hv9o2dqblIQGt3uvofU7W7EdO/GBjMYy1CK8JFksFRDef2oeNa8K/O39EcmR/TcSOt6wxs2CVAJnuXt/BZenNHzk4nN6gx5gkxvGaC1AkeoyOOFUdTJzf2/KxZvsaZiujT8JrpQvdrGaQkhzL/i7UdTD4IAWfN2zJVIJUyLcGEMg8F85+eFyQA==\",\"organization\":\"Daon\",\"signed\":{\"features\":[\"ALL\"],\"expiry\":\"2018-12-21 00:00:00\",\"applicationIdentifier\":\"id.singd.android.libtest\"},\"version\":\"2.1\"}"

    private val LICENSE_KEY =
        "{\"signature\":\"e1G7aJRqQRzqHW\\/BsY++ao2QNFVV7ymPlVJTCONMsXZLuw4tUx5NfZgRkCj3mX9+7gmXyHT46Lcv3gJLasihyJUw6gDEw9nghYrB5v6ShR0+jia8Nu1egC8hXiil1vlaOFYcFbgWk35SV0J9XqjTzXHwTFnnH44KlSh5yb\\/SJQh3EfZMFY3bLtn+b8dVCiXf2vVYJdo6iYznd0P7KbKniqhgl9bY\\/ZGaoFcKLmcAEpVqd+XGwi8IVPLVwGkXjBwaEWb42zSFB+y4hNHXHaNo43OrpfJip7pjVKqDubrXNZmi6EOStZQTDlauH3flmTtaW1wEc3H6DLQatpB0L4QB\\/w==\",\"organization\":\"Daon\",\"signed\":{\"features\":[\"ALL\"],\"expiry\":\"2019-12-18 00:00:00\",\"applicationIdentifier\":\"id.singd.android.libtest\"},\"version\":\"2.1\"}"

    private val A1_LICENSE_KEY =
        "{\"signature\":\"dY38sXX1oHWoH\\/PNyOB6UXw8Jc3GAashnO3579BcHOgJ1JCuqi4BoLD0r8lNaEgDFt0TOn3WfE+MgCZ23d1rzOWh1VvDee+idm2siibp0pNi0\\/KrqDjB06K8HSfI6cP0WIVsVdv3rXBUox9XEPwsayXOpOA8s6Otx9fT4hqwRCCHLdHIXBuaVK1\\/1mUz+ofomSeyuR\\/npwOdE3x+Q0gnn0BCrMKNmd0SUAc2Z3pKx6\\/jDAKLkdqy\\/DorU2d0mleJMy4yBNUMQnhs1TZw8+SsZj8N5Fd\\/w8l8MXCI6sVG1olM5m7e3xDTMuyq4EbSBRqheAjm4Te07qzHkGPo\\/mmm2A==\",\"organization\":\"Daon\",\"signed\":{\"features\":[\"ALL\"],\"expiry\":\"2019-11-29 00:00:00\",\"applicationIdentifier\":\"group.a1.android.a1paket\"},\"version\":\"2.1\"}"

    private var mIdImage: ByteArray? = null

    private var mId2Image: ByteArray? = null

    private lateinit var mTransactionId: String

    private lateinit var mSpinner: Spinner

    private var mNextScan: String? = null

    private val mMapping = hashMapOf<String, Any>(
        "Fields" to hashMapOf(
            //"AUTHORITY" to Authority,
            "BIRTHDATE" to Birthday,
            //"BIRTHPLACE" to Birthplace,
            //"CITY_ADDRESS" to City,
            //"DOC_NUMBER" to DocNumber,
            //"EXPEDITOR" to Expeditor,
            //"EXPIRY" to Expiry,
            //"ID_NUMBER" to IDNumber,
            //"MRZ" to MRZ,
            //"NATIONALITY" to Nationality,
            "NAME" to FirstName,
            //"SEX" to Gender,
            //"STATE_ADDRESS" to State,
            //"STREET_ADDRESS" to Street,
            "SURNAME" to LastName,
            "TEST_GLOBAL_AUTHENTICITY_RATIO" to IDAuthenticityRatio,
            "TEST_GLOBAL_AUTHENTICITY_VALUE" to IsIDValid,
            "TEST_FACE_RECOGNITION_RATIO" to FaceMatchRatio,
            "TEST_FACE_RECOGNITION_VALUE" to IsFaceMatch
        ),
        //"Image1cut" to IDImageSide1,
        //"Image2cut" to IDImageSide2,
        //"ImagePhoto" to FaceImage,
        //"ImageSignature" to SignImage,
        "Result" to GlobalResult
    )

    private lateinit var viewModel: InitActivityViewModel

    private lateinit var mBtnGo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init)

        mTransactionId = intent.getStringExtra("transactionId")

        mSpinner = findViewById(R.id.id_spinner)
        mBtnGo = findViewById(R.id.gobutton)

        mSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf(
                SelectorItems(PARAMETER_DOCTYPE_PASSPORT, getString(R.string.id_type_passport)),
                SelectorItems(
                    PARAMETER_DOCTYPE_DRIVER_LICENSE,
                    getString(R.string.id_type_driver_licence)
                ),
                SelectorItems(PARAMETER_DOCTYPE_ID_CARD_FRONT, getString(R.string.id_type_id_card))
            )
        )

        viewModel = ViewModelProviders.of(this, KodeinViewModelFactory.getInstance())
            .get(InitActivityViewModel::class.java)

        mBtnGo.setOnClickListener {
            mBtnGo.isEnabled = false
            var jjs: JSONObject? = null
            try {
                val doc = (mSpinner.selectedItem as SelectorItems).item
                if (MiSnapApi.PARAMETER_DOCTYPE_ID_CARD_FRONT == doc) {
                    mNextScan = MiSnapApi.PARAMETER_DOCTYPE_ID_CARD_BACK
                } else {
                    mNextScan = null
                }
                jjs = JSONObject()
                jjs.put(CameraApi.MiSnapAllowScreenshots, 1)
                jjs.put(
                    MiSnapApi.MiSnapDocumentType,
                    doc
                )
                jjs.put(MiSnapApi.MiSnapOrientation, 1)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val intentMiSnap = Intent(this, MiSnapWorkflowActivity::class.java)
            intentMiSnap.putExtra(JOB_SETTINGS, jjs!!.toString())
            startActivityForResult(intentMiSnap, MiSnapApi.RESULT_PICTURE_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (MiSnapApi.RESULT_PICTURE_CODE == requestCode) {
            processIdImageResult(resultCode, data)
        } else if (requestCode == 1994) {
            processFaceImageResult(resultCode, data)
        }
    }

    private fun processIdImageResult(resultCode: Int, data: Intent?) {
        if (Activity.RESULT_OK == resultCode) {
            if (data != null) {
                val extras = data.extras
                val miSnapResultCode = extras?.getString(RESULT_CODE)
                when (miSnapResultCode) {
                    // MiSnap check capture
                    RESULT_SUCCESS_VIDEO, RESULT_SUCCESS_STILL -> {
                        Log.i(
                            InitActivity::class.simpleName,
                            "MIBI: " + extras.getString(RESULT_MIBI_DATA)!!
                        )

                        // Image returned successfully
                        if (mIdImage == null)
                            mIdImage = data.getByteArrayExtra(RESULT_PICTURE_DATA)
                        else
                            mId2Image = data.getByteArrayExtra(RESULT_PICTURE_DATA)

                        val warnings = extras.getStringArrayList(RESULT_WARNINGS)
                        if (warnings != null && !warnings.isEmpty()) {
                            var message = "WARNINGS:"
                            if (warnings.contains(MiSnapAnalyzerResult.FrameChecks.WRONG_DOCUMENT.name)) {
                                message += "\nWrong document detected"
                            }
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    }

                    // Card.io credit card capture
                    RESULT_SUCCESS_CREDIT_CARD -> {
                        val creditCardInfo = StringBuilder()
                        creditCardInfo.append(
                            "Redacted number: " + extras.getString(
                                CreditCardApi.CREDIT_CARD_REDACTED_NUMBER
                            ) + "\n"
                        )
                        // Never log a raw card number. Avoid displaying it.
                        creditCardInfo.append("Card number: *HIDDEN*\n") // + extras.getString(MiSnapApi.CREDIT_CARD_NUMBER));
                        creditCardInfo.append("Formatted number: *HIDDEN*\n") //+ extras.getString(MiSnapApi.CREDIT_CARD_FORMATTED_NUMBER));
                        creditCardInfo.append("Card type: " + extras.getString(CreditCardApi.CREDIT_CARD_TYPE) + "\n")
                        creditCardInfo.append("CVV: " + extras.getInt(CreditCardApi.CREDIT_CARD_CVV) + "\n")
                        creditCardInfo.append("Expiration month: " + extras.getInt(CreditCardApi.CREDIT_CARD_EXPIRY_MONTH) + "\n")
                        creditCardInfo.append("Expiration year: " + extras.getInt(CreditCardApi.CREDIT_CARD_EXPIRY_YEAR) + "\n")
                        AlertDialog.Builder(this).setTitle("Card.io Data")
                            .setMessage(creditCardInfo.toString()).show()
                        Log.i(InitActivity::class.simpleName, creditCardInfo.toString())
                    }

                }
                if (mNextScan == null) {
                    launchFaceImageProcess()
                } else {
                    var jjs: JSONObject? = null
                    try {
                        val doc = mNextScan
                        mNextScan = null
                        jjs = JSONObject()
                        jjs.put(CameraApi.MiSnapAllowScreenshots, 1)
                        jjs.put(
                            MiSnapDocumentType,
                            doc
                        )
                        //jjs.put("MAX_TIMEOUTS_BEFORE_FAILOVER", Int.MAX_VALUE)
                        jjs.put(MiSnapOrientation, 1)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    val intentMiSnap = Intent(this, MiSnapWorkflowActivity::class.java)
                    intentMiSnap.putExtra(JOB_SETTINGS, jjs!!.toString())
                    startActivityForResult(intentMiSnap, RESULT_PICTURE_CODE)
                }
            } else {
                // Image canceled, stop
                Toast.makeText(this, "MiSnap canceled", Toast.LENGTH_SHORT).show()
                mBtnGo.isEnabled = true
            }
        } else if (Activity.RESULT_CANCELED == resultCode) {
            // Camera not working or not available, stop
            Toast.makeText(this, "Operation canceled!!!", Toast.LENGTH_SHORT).show()
            if (data != null && data.extras != null) {
                val extras = data.extras
                val miSnapResultCode = extras.getString(RESULT_CODE)
                if (!miSnapResultCode!!.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Shutdown reason: $miSnapResultCode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            mBtnGo.isEnabled = true

        }
    }

    private fun launchFaceImageProcess() {
        // Add in parameter info for MiSnap
        val overrides = ParameterOverrides(this)
        val paramMap = overrides.load()
        val jjs = JSONObject()
        try {

            // Add FacialCapture-specific parameters from the Settings Activity, stored in shared preferences
            // NOTE: If you do not set these, the optimized defaults for this SDK version will be used.
            // NOTE: Do not set these unless you are purposefully overriding the defaults!
            for (param in paramMap.entries) {
                jjs.put(param.key, param.value)
            }
            jjs.put(CameraApi.MiSnapAllowScreenshots, 1)
            Log.d("Signd", "Application id: " + applicationContext.packageName)
            if (applicationContext.packageName.equals("group.a1.android.a1paket")) {
                Log.d("Signd", "A1 key used")
                jjs.put(FacialCaptureApi.FacialCaptureLicenseKey, A1_LICENSE_KEY)
            } else {
                Log.d("Signd", "Signd key used")
                jjs.put(FacialCaptureApi.FacialCaptureLicenseKey, LICENSE_KEY)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }


        val jjsWorkflow = JSONObject()
        try {
            // Optionally add in customizable runtime settings for the FacialCapture workflow.
            // NOTE: These don't go into the JOB_SETTINGS because they are for your app, not for core FacialCapture.
            jjsWorkflow.put(CameraApi.MiSnapAllowScreenshots, 1)
            jjsWorkflow.put(
                FacialCaptureWorkflowParameters.FACIALCAPTURE_WORKFLOW_MESSAGE_DELAY,
                500
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }


        val intentFacialCapture =
            Intent(this, FacialCaptureWorkflowActivity::class.java)
        intentFacialCapture.putExtra(JOB_SETTINGS, jjs.toString())
        intentFacialCapture.putExtra(
            FacialCaptureWorkflowParameters.EXTRA_WORKFLOW_PARAMETERS,
            jjsWorkflow.toString()
        )
        startActivityForResult(intentFacialCapture, 1994)
    }

    private fun processFaceImageResult(resultCode: Int, data: Intent?) {
        if (Activity.RESULT_OK == resultCode) {
            if (data != null) {
                val encodedIdImage = Base64.encodeToString(mIdImage, Base64.NO_WRAP)
                val faceImage = data.getByteArrayExtra(RESULT_PICTURE_DATA)
                val encodedFaceImage = Base64.encodeToString(faceImage, Base64.NO_WRAP)

                mIdImage = null


                val url = "https://signd.io:4000/api/v1/onboard/sessions/$mTransactionId/identify"

                val payload = JSONObject()
                payload.put("image_doc", encodedIdImage)
                payload.put("image_face", encodedFaceImage)

                if (mId2Image != null) {
                    val encodedId2Image = Base64.encodeToString(mId2Image, Base64.DEFAULT)
                    payload.put("image_doc_back", encodedId2Image)
                    mId2Image = null
                }

                val dialog = ProgressDialog.show(
                    this,
                    "Please wait",
                    ""
                )//this.getString(R.string.loading_msg))
                dialog.show()

                viewModel.messenger.postCommand(ExecuteOnSigndCallbackCommand {
                    if (it is ISigndCallback) {
                        val request =
                            HttpRequest(
                                url,
                                isHttps = true,
                                body = payload.toString(),
                                method = HttpMethods.POST
                            )
                        var response: HttpResponse? = null
                        var wasTimeout = false
                        try {
                            response = it.performHttpCall(request).get(20, TimeUnit.SECONDS)
                        } catch (e: TimeoutException) {
                            wasTimeout = true
                            viewModel.resultDict[GlobalResult] = "Pending"
                        } catch (e: Exception) {

                        }

                        if (!wasTimeout && response != null && response.body.isNotEmpty()) {
                            var json: JSONObject? = null
                            try {
                                json = JSONObject(response.body)
                            } catch (e: JSONException) {
                                Log.e(
                                    this@InitActivity.javaClass.canonicalName,
                                    "JSON Convert failed!",
                                    e
                                )
                            }

                            if (json == null) {
                                viewModel.resultDict[GlobalResult] = "Fail"
                            } else {

                                if (json.has("Fields") && json["Fields"] is JSONObject) {
                                    parseResponse(json, viewModel.resultDict, mMapping)
                                    if (viewModel.resultDict.containsKey(IsIDValid) && viewModel.resultDict.containsKey(
                                            IsFaceMatch
                                        )
                                    ) {
                                        if ((viewModel.resultDict[IsIDValid].toString().equals("OK") ||
                                                    viewModel.resultDict[IsIDValid].toString().equals("DOUBTFUL")) &&
                                            (viewModel.resultDict[IsFaceMatch].toString().equals("OK") ||
                                                    viewModel.resultDict[IsFaceMatch].toString().equals("DOUBTFUL"))
                                        ) {
                                            viewModel.resultDict[GlobalResult] = "OK"
                                        } else {
                                            viewModel.resultDict[GlobalResult] = "Fail"
                                        }
                                    } else {
                                        viewModel.resultDict[GlobalResult] = "Fail"
                                    }
                                } else {
                                    viewModel.resultDict[GlobalResult] = "Fail"
                                }
                            }
                        } else if (!wasTimeout) {
                            viewModel.resultDict[GlobalResult] = "Fail"
                        }
                        this@InitActivity.runOnUiThread {
                            dialog.dismiss()
                            if (viewModel.resultDict[GlobalResult]!!.equals("OK") || viewModel.retries == 0) {
                                it.updateProperties(viewModel.resultDict)
                                setResult(Activity.RESULT_OK, this@InitActivity.intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@InitActivity,
                                    getString(R.string.msg_try_again),
                                    Toast.LENGTH_SHORT
                                ).show()
                                mBtnGo.isEnabled = true
                            }
                        }
                    }
                })

            } else {
                // Image canceled, stop
                Toast.makeText(this, "MiSnap canceled", Toast.LENGTH_SHORT).show()
                mBtnGo.isEnabled = true
            }
        } else if (Activity.RESULT_CANCELED == resultCode) {
            // Camera not working or not available, stop
            Toast.makeText(this, "MiSnap aborted", Toast.LENGTH_SHORT).show()
            val msg = data?.getStringExtra(RESULT_CODE)
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            mBtnGo.isEnabled = true
        }
    }

    private fun parseResponse(
        json: JSONObject,
        result: MutableMap<Property, Any>,
        mapping: Map<String, *>
    ) {
        for ((key, value) in mapping) {
            if (value is Map<*, *> && json.has(key) && json[key] is JSONObject) {
                parseResponse(
                    json.getJSONObject(key),
                    result,
                    value.filterKeys { it is String } as Map<String, *>
                )
            } else if (json.has(key) && value is Property) {
                if (json[key] is JSONArray) {
                    result[value] = arrayOf(json.getJSONArray(key))
                } else {
                    result[value] = json.get(key)
                }
            }
        }
    }

}
