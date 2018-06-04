package noro.me.sawtti

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
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

            val url = Uri.parse(externalSDPath  + "/music/tone400.wav");
            Log.d("SAWTTI", url.path)
            viewModel.fingerPrint(url, "tone400", "test", null)
        }

    }

}
