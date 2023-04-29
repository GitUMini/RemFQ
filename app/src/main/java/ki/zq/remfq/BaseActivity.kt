package ki.zq.remfq

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

abstract class BaseActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    fun logd(info: String) {
        Log.d("MyTag", info)
    }

    fun loge(info: String) {
        Log.e("MyTag", info)
    }

    fun runOnIODispatcher(method:()->Unit){
        launch {
            withContext(Dispatchers.IO){
                method()
            }
        }
    }

    fun runOnMainDispatcher(method:()->Unit){
        launch {
            withContext(Dispatchers.Main){
                method()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}