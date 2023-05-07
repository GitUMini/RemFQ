package ki.zq.remfp.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import ki.zq.remfp.BuildConfig
import ki.zq.remfp.compose.CPHistory.CpHistoryItem
import ki.zq.remfp.databinding.FragmentHistoryBinding
import ki.zq.remfp.model.HistoryViewModel
import ki.zq.remfp.util.BaseUtil
import ki.zq.remfp.util.ExcelUtils
import java.io.File

class HistoryFragment : Fragment() {
    private var fileName: String? = null
    private val columnName = arrayOf(
        "序号", "报销人", "受票单位", "发票代码", "发票号码", "开票日期", "发票名目",
        "开票企业识别号", "开票企业名称", "金额", "税额", "金额合计", "单据编号", "费用说明"
    )

    private val historyViewModel: HistoryViewModel by viewModels()
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater)
        binding.historyComposeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(
                    viewLifecycleOwner
                )
            )
            setContent {
                CpHistoryItem(historyViewModel = historyViewModel)
            }
        }

        historyViewModel.apply {
            deleteLiveData.observe(viewLifecycleOwner) {
                if (it > 0) show("删除成功")
            }
        }
        binding.fabSearchOutput.setOnClickListener {
            try {
                if (getRecordData().isEmpty()) {
                    show("无法获取将要导出的数据，请检查！")
                } else {
                    exportExcel()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return binding.root
    }

    private fun share2Wechat(){
        val file = File(requireActivity().getExternalFilesDir(null), "taizhang.xlsx")
        val uri = FileProvider.getUriForFile(requireContext(), "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.setPackage("com.tencent.mm")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "分享台账文件"))
    }

    //导出excel
    private fun exportExcel() {
        val path = requireActivity().getExternalFilesDir(null)
        val file = File(path, "taizhang.xlsx")
        if (!file.exists()) {
            file.createNewFile()
        }
        fileName = path?.absolutePath + "/" + file.name
        ExcelUtils.initExcel(fileName!!, columnName)
        ExcelUtils.writeObjListToExcel(getRecordData(), fileName!!).apply {
            if (this) {
                show("导出成功！")
                share2Wechat()
            } else show("导出失败，请检查！")
        }
    }

    //导出前，将数据集合 转化成ArrayList<ArrayList<String>>
    private fun getRecordData(): ArrayList<ArrayList<String>> {
        val fapiaoList: ArrayList<ArrayList<String>> = ArrayList()
        val baseBeanList = historyViewModel.allBeansLiveData.value
        baseBeanList?.apply {
            for (i in baseBeanList.indices) {
                val realBean = baseBeanList[i]
                val beanList = ArrayList<String>()
                beanList.add(i.toString())
                beanList.add(realBean.fpToPerson!!)
                beanList.add(realBean.fpToCompany!!)
                beanList.add(realBean.fpCode!!)
                beanList.add(realBean.fpNumber!!)
                beanList.add(realBean.fpDate!!)
                beanList.add(realBean.fpThing!!)
                beanList.add(realBean.fpFromCompanyCode!!)
                beanList.add(realBean.fpFromCompanyName!!)
                beanList.add(realBean.fpMoney!!)
                beanList.add(realBean.fpTax!!)
                beanList.add(realBean.fpAll!!)
                beanList.add(getFPDocNumber()!!)
                beanList.add(getFPCoastFor()!!)
                fapiaoList.add(beanList)
            }
        }
        return fapiaoList
    }

    private fun getFPDocNumber(): String? {
        val shp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return shp.getString("FPDocNumber", "单据编号")
    }

    private fun getFPCoastFor(): String? {
        val shp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return shp.getString("FPCoastFor", "费用说明")
    }

    private fun show(info: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), info, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            requireActivity().finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmStatic
        fun newInstance() = HistoryFragment()
    }
}