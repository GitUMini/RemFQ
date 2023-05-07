package ki.zq.remfp.ui

import android.os.Bundle
import android.widget.Toast
import ki.zq.remfp.BaseActivity
import ki.zq.remfp.adapter.ViewPager2Adapter
import ki.zq.remfp.databinding.ActivityNavMainBinding
import ki.zq.remfp.fragment.HistoryFragment
import ki.zq.remfp.fragment.RecordFragment
import ki.zq.remfp.fragment.ScanFragment
import ki.zq.remfp.fragment.SettingsFragment
import ki.zq.remfp.ocr.HttpUtil
import ki.zq.remfp.util.BaseUtil
import ki.zq.remfp.util.BnvMediator

class NavMainActivity : BaseActivity() {

    private var _binding: ActivityNavMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityNavMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragments = mapOf(
            A to RecordFragment.newInstance(),
            B to HistoryFragment.newInstance(),
            C to SettingsFragment.newInstance()
        )

        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.title = "台账助手"

        binding.viewPager2.apply {
            adapter = ViewPager2Adapter(this@NavMainActivity, fragments)
            BnvMediator(binding.bnv, this) { bnv, vp2 ->
                vp2.isUserInputEnabled = true  //false:ViewPager2不能滑动
                bnv.itemIconTintList = null  //显示BottomNavigationView的图标
            }.attach()
        }

        runOnIODispatcher {
            BaseUtil.isOutDate().apply {
                if (this) {
                    runOnMainDispatcher {
                        Toast.makeText(applicationContext, "Token过期，已重新获取！", Toast.LENGTH_SHORT).show()
                    }
                    HttpUtil.getAccessToken()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val A: Int = 0
        private const val B: Int = 1
        private const val C: Int = 2
    }
}