package ki.zq.remfq.fragment

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ki.zq.remfq.R
import ki.zq.remfq.adapter.HistoryAdapter
import ki.zq.remfq.bean.RealBean
import ki.zq.remfq.databinding.FragmentHistoryBinding
import ki.zq.remfq.diffutil.HistoryDFC
import ki.zq.remfq.model.HistoryViewModel
import ki.zq.remfq.util.BaseUtil
import ki.zq.remfq.util.ExcelUtils
import ki.zq.remfq.util.FixLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HistoryFragment : Fragment() {
    private val name = arrayOf(
        "序号", "报销人", "受票单位", "发票代码", "发票号码", "开票日期", "发票名目",
        "开票企业识别号", "开票企业名称", "金额", "税额", "金额合计"
    )
    private lateinit var fapiaoList: ArrayList<ArrayList<String>>
    private var fileName: String? = null

    private lateinit var historyAdapter: HistoryAdapter

    private lateinit var historyViewModel: HistoryViewModel

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // _binding = FragmentHistoryBinding.inflate(layoutInflater)
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_history, container, false)
        historyViewModel = ViewModelProvider(requireActivity()).get(HistoryViewModel::class.java)
        initViews()
        return binding.root
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    private fun initViews() {
        val emptyView = LayoutInflater.from(requireContext()).inflate(R.layout.item_empty, null)
        val path = requireActivity().getExternalFilesDir(null)?.absolutePath
        path?.let {
            val realPath = path.replace("/storage/emulated/0", "") + "/"
            binding.tvSavePath.text = "导出位置 : $realPath"
        }

        binding.fabSearchOutput.setOnClickListener {
            if (historyAdapter.data.size != 0) {
                val builder = MaterialAlertDialogBuilder(requireContext()).create()
                builder.setCancelable(false)
                builder.setMessage("导出数据？")
                builder.setButton(DialogInterface.BUTTON_POSITIVE, "确定") { p0, _ ->
                    requireActivity().lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            exportExcel()
                        }
                    }
                    p0.dismiss()
                }
                builder.setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { p0, _ ->
                    p0?.dismiss()
                }
                builder.show()
            } else
                show("无历史数据，无法导出！", true)
        }

        historyAdapter = HistoryAdapter()
        historyAdapter.apply {
            setDiffCallback(HistoryDFC())
            setHasStableIds(true)
            isUseEmpty = true
            setEmptyView(emptyView)
            setOnItemClickListener { adapter, _, position ->
                val clickedItem = adapter.data[position] as RealBean
                val builder = MaterialAlertDialogBuilder(requireContext()).setItems(
                    arrayOf(
                        "详细信息",
                        "删除发票"
                    )
                ) { _, p1 ->
                    when (p1) {
                        0 -> {
                            getBeanDetail(clickedItem)
                        }
                        else -> {
                            deleteBean(clickedItem)
                        }
                    }
                }.setTitle("请选择操作").setCancelable(false).create()
                builder.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    "取消"
                ) { p0, _ -> p0?.dismiss() }
                builder.show()
            }
        }

        binding.recvSearchResult.apply {
            layoutManager = FixLayoutManager(requireContext())
            adapter = historyAdapter
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                historyViewModel.getAllBeans()?.collect {
                    withContext(Dispatchers.Main) {
                        historyAdapter.setList(it)
                    }
                }
            }
        }
    }

    private fun getBeanDetail(realBean: RealBean) {
        val detailBuilder: StringBuilder = StringBuilder()
        realBean.apply {
            detailBuilder.append("报销人: $fpToPerson\r\n")
                .append("受票单位: $fpToCompany\r\n")
                .append("发票代码: $fpCode\r\n")
                .append("发票号码: $fpNumber\r\n")
                .append("开票日期: " + BaseUtil.longToString(fpDate!!) + "\r\n")
                .append("发票名目: $fpThing\r\n")
                .append("开票企业识别号: $fpFromCompanyCode\r\n")
                .append("开票企业名称: $fpFromCompanyName\r\n")
                .append("金额: $fpMoney\r\n")
                .append("税额: $fpTax\r\n")
                .append("合计: $fpAll")
        }
        val builder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.MaterialAlertDialog_Title_Center
        ).create()
        builder.setTitle("详情")
        builder.setCancelable(false)
        builder.setMessage(detailBuilder.toString())
        builder.setButton(DialogInterface.BUTTON_POSITIVE, "确定") { p0, _ -> p0.dismiss() }
        builder.show()
    }

    private fun deleteBean(realBean: RealBean) {
        val builder = MaterialAlertDialogBuilder(requireContext()).create()
        builder.setCancelable(false)
        builder.setMessage("删除本条发票记录？")
        builder.setButton(DialogInterface.BUTTON_POSITIVE, "确定") { p0, _ ->
            lifecycleScope.launch {
                val flag = withContext(Dispatchers.IO) {
                    historyViewModel.deleteBean(realBean)
                }
                if (flag != 1) {
                    show("删除成功！", true)
                    p0?.dismiss()
                }
            }
        }
        builder.setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { p0, _ ->
            show("取消！", true)
            p0?.dismiss()
        }
        builder.show()
    }

    private fun show(info: String, flag: Boolean) {
        requireActivity().runOnUiThread {
            if (flag)
                Toast.makeText(requireContext(), info, Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(requireContext(), info, Toast.LENGTH_LONG).show()
        }
    }

    //导出excel
    private fun exportExcel() {
        val path = requireActivity().getExternalFilesDir(null)
        val file = File(path, "taizhang.xls")
        if (!file.exists()) {
            file.createNewFile()
        }
        fileName = path?.absolutePath + "/" + file.name
        ExcelUtils.initExcel(fileName!!, name)
        val result = ExcelUtils.writeObjListToExcel(getRecordData(), fileName!!)
        if (result) {
            show("导出成功！", false)
        }
    }

    //导出前，将数据集合 转化成ArrayList<ArrayList<String>>
    private fun getRecordData(): ArrayList<ArrayList<String>> {
        fapiaoList = ArrayList()
        for (i in historyAdapter.data.indices) {
            val realBean = historyAdapter.data[i]
            val beanList = ArrayList<String>()
            beanList.add(i.toString())
            beanList.add(realBean.fpToPerson!!)
            beanList.add(realBean.fpToCompany!!)
            beanList.add(realBean.fpCode!!)
            beanList.add(realBean.fpNumber!!)
            beanList.add(BaseUtil.longToString(realBean.fpDate!!))
            beanList.add(realBean.fpThing!!)
            beanList.add(realBean.fpFromCompanyCode!!)
            beanList.add(realBean.fpFromCompanyName!!)
            beanList.add(realBean.fpMoney!!)
            beanList.add(realBean.fpTax!!)
            beanList.add(realBean.fpAll!!)
            fapiaoList.add(beanList)
        }
        return fapiaoList
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