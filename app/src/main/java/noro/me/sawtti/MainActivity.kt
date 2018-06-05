package noro.me.sawtti

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import io.realm.Realm
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel
    private lateinit var listenButton: ImageButton
    private lateinit var fingerPrintBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Realm.init(applicationContext);
        //Log.d("SAWTTI", Realm.getDefaultInstance().path)

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE), 0)

        } else {
            Log.d("SAWTTI", "Permission Granted")
        }

        AndroidFFMPEGLocator(applicationContext);
        viewModel = ViewModel()
        listenButton = findViewById(R.id.listenButton)
        fingerPrintBtn = findViewById(R.id.fingerPrintsBtn)

        listenButton.setOnClickListener {
            if (viewModel.isListening) {
                viewModel.stopMic()
            } else {
                viewModel.listenToMic()
            }

        }

        fingerPrintBtn.setOnClickListener {
            val externalSDPath = Environment.getExternalStorageDirectory().absolutePath

            val url = Uri.parse(externalSDPath  + "/music/closer5.wav");
            Log.d("SAWTTI", url.path)
            viewModel.fingerPrint(url, "tone400", "test", null)
        }

    }

    fun askPermissio(thisActivity: Activity) {

    }

}
