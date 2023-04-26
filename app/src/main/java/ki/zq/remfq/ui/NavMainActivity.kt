package ki.zq.remfq.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ki.zq.remfq.adapter.ViewPager2Adapter
import ki.zq.remfq.databinding.ActivityNavMainBinding
import ki.zq.remfq.fragment.HistoryFragment
import ki.zq.remfq.fragment.ScanFragment
import ki.zq.remfq.fragment.SettingsFragment
import ki.zq.remfq.util.BnvMediator

class NavMainActivity : AppCompatActivity() {

    private var _binding: ActivityNavMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityNavMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragments = mapOf(
            A to ScanFragment.newInstance(),
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