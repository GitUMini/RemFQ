package ki.zq.remfp.fragment

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ki.zq.remfp.R
import ki.zq.remfp.adapter.HistoryAdapter
import ki.zq.remfp.bean.RealBean
import ki.zq.remfp.databinding.FragmentHistoryBinding
import ki.zq.remfp.db.RealBeanDB
import ki.zq.remfp.db.RealBeanDao
import ki.zq.remfp.diffutil.HistoryDFC
import ki.zq.remfp.model.HistoryViewModel
import ki.zq.remfp.util.ExcelUtils
import ki.zq.remfp.util.FixLayoutManager
import java.io.File

class HistoryFragment : Fragment() {
    private var fileName: String? = null
    private val columnName = arrayOf(
        "序号", "报销人", "受票单位", "发票代码", "发票号码", "开票日期", "发票名目",
        "开票企业识别号", "开票企业名称", "金额", "税额", "金额合计", "单据编号", "费用说明"
    )

    private lateinit var realBeanDao: RealBeanDao
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var defaultSharedPreferences: SharedPreferences

    private val historyViewModel: HistoryViewModel by viewModels()
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater)

        historyViewModel.apply {
            deleteLiveData.observe(viewLifecycleOwner) {
                if (it > 0) show("删除成功")
            }
        }
        initViews()
        initListener()
        return binding.root
    }

    private fun initListener() {
        binding.btnHistoryOperationOutput.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireContext()).create()
            builder.setCancelable(false)
            builder.setTitle("导出数据？")
            builder.setButton(DialogInterface.BUTTON_POSITIVE, "确定") { p0, _ ->
                try {
                    if (getRecordData().isEmpty()) {
                        show("无法获取将要导出的数据，请检查！")
                    } else {
                        exportExcel()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                p0.dismiss()
            }
            builder.setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { p0, _ ->
                p0?.dismiss()
            }
            builder.show()
        }
        binding.btnHistoryOperationShare.setOnClickListener {
            share2Wechat()
        }
    }

    @SuppressLint("InflateParams")
    private fun initViews() {
        realBeanDao = RealBeanDB.getDatabase(requireContext()).ocrDao()
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val emptyView = LayoutInflater.from(requireContext()).inflate(R.layout.item_empty, null)

        historyAdapter = HistoryAdapter()
        historyAdapter.apply {
            setDiffCallback(HistoryDFC())
            setHasStableIds(true)
            isUseEmpty = true
            setEmptyView(emptyView)
            setOnItemClickListener { adapter, _, position ->
                val clickBean = adapter.data[position] as RealBean
                val builder = MaterialAlertDialogBuilder(requireContext()).setItems(
                    arrayOf(
                        "详细信息",
                        "删除发票"
                    )
                ) { dialog, p1 ->
                    when (p1) {
                        0 -> {
                            dialog.dismiss()
                            val builder = MaterialAlertDialogBuilder(requireContext())
                                .setTitle("详细信息")
                                .setCancelable(false)
                                .setMessage(getBeanDetail(clickBean)).create()

                            builder.setButton(
                                DialogInterface.BUTTON_POSITIVE,
                                "确定"
                            ) { dialog1, _ -> dialog1?.dismiss() }
                            builder.show()
                        }

                        else -> {
                            historyViewModel.deleteBean(clickBean)
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

        binding.historyRecyclerView.apply {
            layoutManager = FixLayoutManager(requireContext())
            adapter = historyAdapter
        }

        realBeanDao.getAllBeans().observe(viewLifecycleOwner) {
            historyAdapter.setDiffNewData(it)
        }
    }

    private fun share2Wechat() {
        val file = File(requireActivity().getExternalFilesDir(null), "taizhang.xlsx")
        if (!file.exists()) {
            show("台账文件不存在，请先导出再分享！")
        } else {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "ki.zq.remfp.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.setPackage("com.tencent.mm")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "分享台账文件"))
        }
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
            } else show("导出失败，请检查！")
        }
    }

    //导出前，将数据集合 转化成ArrayList<ArrayList<String>>
    private fun getRecordData(): ArrayList<ArrayList<String>> {
        val fapiaoList: ArrayList<ArrayList<String>> = ArrayList()
        val baseBeanList = historyAdapter.data
        baseBeanList.apply {
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

    private fun getBeanDetail(realBean: RealBean): String {
        val detailBuilder: StringBuilder = StringBuilder()
        realBean.apply {
            detailBuilder.append("报销人: $fpToPerson\r\n")
                .append("受票单位: $fpToCompany\r\n")
                .append("发票代码: $fpCode\r\n")
                .append("发票号码: $fpNumber\r\n")
                .append("开票日期: " + fpDate!! + "\r\n")
                .append("发票名目: $fpThing\r\n")
                .append("开票企业识别号: $fpFromCompanyCode\r\n")
                .append("开票企业名称: $fpFromCompanyName\r\n")
                .append("金额: $fpMoney\r\n")
                .append("税额: $fpTax\r\n")
                .append("合计: $fpAll")
        }
        return detailBuilder.toString()
    }

    private fun getFPDocNumber(): String? {
        return defaultSharedPreferences.getString("FPDocNumber", "单据编号")
    }

    private fun getFPCoastFor(): String? {
        return defaultSharedPreferences.getString("FPCoastFor", "费用说明")
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