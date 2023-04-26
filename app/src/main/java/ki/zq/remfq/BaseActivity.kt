package ki.zq.remfq

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class BaseActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    fun initActionBar(actionBar: ActionBar?, title: String) {
        actionBar?.title = title
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val upArrow =
            ResourcesCompat.getDrawable(resources, R.drawable.abc_ic_ab_back_material, null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            upArrow?.colorFilter = BlendModeColorFilter(Color.WHITE, BlendMode.SRC_ATOP)
        } else {
            @Suppress("DEPRECATION")
            upArrow?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        actionBar?.setHomeAsUpIndicator(upArrow)
    }

    fun logd(info: String) {
        Log.d("MyTag", info)
    }

    fun loge(info: String) {
        Log.e("MyTag", info)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}