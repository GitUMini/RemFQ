package ki.zq.remfp.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ki.zq.remfp.compose.CPScan
import ki.zq.remfp.databinding.FragmentScanBinding
import ki.zq.remfp.enums.EnumSaveFlag.*
import ki.zq.remfp.model.ScanViewModel

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private val scanViewModel by viewModels<ScanViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater)

        binding.scanComposeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(
                    viewLifecycleOwner
                )
            )
            setContent {
                CPScan.CPScanResult(model = scanViewModel)
            }
        }
        scanViewModel.saveFlagLiveData.observe(viewLifecycleOwner) {
            when (it) {
                FLAG_ADD_SUCCESS -> {
                    show("保存成功")
                }
                FLAG_ADD_FAILURE -> {
                    show("保存失败")}
                FLAG_IS_EXIST -> {
                    show("发票已存在")}
                else -> {
                    show("结果未知")
                }
            }
        }
        return binding.root
    }

    private fun show(info: String) {
        Toast.makeText(requireContext(), info, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            requireActivity().finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ScanFragment()
    }
}