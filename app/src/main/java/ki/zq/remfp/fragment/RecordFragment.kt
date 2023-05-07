package ki.zq.remfp.fragment

import android.content.ContentResolver
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.JsonSyntaxException
import ki.zq.remfp.R
import ki.zq.remfp.bean.RealBean
import ki.zq.remfp.databinding.FragmentRecordBinding
import ki.zq.remfp.enums.EnumSaveFlag.FLAG_ADD_FAILURE
import ki.zq.remfp.enums.EnumSaveFlag.FLAG_ADD_SUCCESS
import ki.zq.remfp.enums.EnumSaveFlag.FLAG_IS_EXIST
import ki.zq.remfp.model.ScanViewModel
import ki.zq.remfp.ocr.HttpUtil
import ki.zq.remfp.util.BaseUtil
import ki.zq.remfp.util.BaseUtil.encode64
import ki.zq.remfp.util.BeanUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class RecordFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    private val scanViewModel by viewModels<ScanViewModel>()

    private var beanList: MutableList<RealBean> = arrayListOf()
    private lateinit var textInputLayouts: MutableList<TextInputLayout>

    private val selected64s: MutableList<String> = arrayListOf()

    private var scanFlag = false

    private val launcherForSingleSelect =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                // 获取 ContentResolver
                val contentResolver: ContentResolver = requireActivity().contentResolver
                // 将 URI 转换为 Bitmap，并将其编码为 Base64 字符串
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    selected64s.add(baos.toByteArray().encode64())
                }
            }
            getResultFromNet()
            setEditAble(selected64s.isNotEmpty())
        }
    private val launcherForMultipleSelect =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            for (i in uris.indices) {
                // 获取 ContentResolver
                val contentResolver: ContentResolver = requireActivity().contentResolver
                // 将 URI 转换为 Bitmap，并将其编码为 Base64 字符串
                val inputStream = contentResolver.openInputStream(uris[i])
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    selected64s.add(baos.toByteArray().encode64())
                }
            }
            getResultFromNet()
            setEditAble(selected64s.isNotEmpty())
            binding.btnRecordScanNext.visibility = View.VISIBLE
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater)

        scanViewModel.saveFlagLiveData.observe(viewLifecycleOwner) {
            when (it) {
                FLAG_ADD_SUCCESS -> {
                    show("保存成功")
                }

                FLAG_ADD_FAILURE -> {
                    show("保存失败")
                }

                FLAG_IS_EXIST -> {
                    show("发票已存在")
                }

                else -> {
                    show("结果未知")
                }
            }
        }
        initViews()
        binding.tvRecordScanSingle.setOnClickListener(this)
        binding.tvRecordScanMultiple.setOnClickListener(this)
        binding.tvRecordScanSave.setOnClickListener(this)
        binding.tvRecordScanClear.setOnClickListener(this)
        scanViewModel.currentBeanLiveData.observe(viewLifecycleOwner) {
            for (i in textInputLayouts.indices) {
                textInputLayouts[i].editText?.setText(it.toStringList()[i])
            }
        }
        return binding.root
    }

    private fun initViews() {
        textInputLayouts = arrayListOf(
            binding.tilToPerson,
            binding.tilToCompany,
            binding.tilFpCode,
            binding.tilFpNumber,
            binding.tilFpDate,
            binding.tilFpThing,
            binding.tilFromCompanyCode,
            binding.tilFromCompanyName,
            binding.tilFpMoney,
            binding.tilFpTax,
            binding.tilFpAll,
        )
        setEditAble(false)
    }

    private fun getFinalResult(ins: String): RealBean? {
        var resultBean: RealBean? = null
        try {
            resultBean =
                BeanUtils.switchOCRBean2RealBean(ins, BaseUtil.getFPToPerson(requireContext()))
            scanViewModel.setCurrentRealBean(resultBean)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            run {
                show("扫描失败！")
            }
        }
        return resultBean
    }

    private fun clearAllText() {
        if (scanFlag) {
            textInputLayouts.forEach {
                it.editText?.setText("")
            }
            setEditAble(false)
            scanFlag = false
        } else {
            run {
                show("没有任何内容！")
            }
        }
    }

    private fun setEditAble(enable: Boolean) {
        textInputLayouts.forEach {
            it.isEnabled = enable
        }
    }

    private fun saveEdit() {
        val hasEditResult = arrayListOf<String>()
        textInputLayouts.forEach {
            hasEditResult.add(it.editText?.text.toString())
        }
        scanViewModel.setCurrentRealBean(
            RealBean().apply {
                fpToPerson = hasEditResult[0]
                fpToCompany = hasEditResult[1]
                fpCode = hasEditResult[2]
                fpNumber = hasEditResult[3]
                fpDate = hasEditResult[4]
                fpThing = hasEditResult[5]
                fpFromCompanyCode = hasEditResult[6]
                fpFromCompanyName = hasEditResult[7]
                fpMoney = hasEditResult[8]
                fpTax = hasEditResult[9]
                fpAll = hasEditResult[10]
            }
        )
    }

    private fun getResultFromNet() {
        requireActivity().lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val size = selected64s.size
                if (selected64s.isNotEmpty()) {
                    for (i in selected64s.indices) {
                        HttpUtil.multipleInvoice(selected64s[i])?.also { result ->
                            withContext(Dispatchers.Main) {
                                if (result.isEmpty()) {
                                    scanFlag = false
                                } else {
                                    val resultBean = getFinalResult(result)
                                    if (resultBean != null) {
                                        scanFlag = true
                                        beanList.add(BeanUtils.listResult2RealBean(resultBean.toStringList()))
                                        if (size == 1)
                                            run {
                                                show("扫描成功！")
                                            }
                                        else
                                            run {
                                                show("第 ${(i + 1)} 张扫描成功！")
                                            }
                                    } else {
                                        if (size == 1)
                                            run {
                                                show("扫描失败！")
                                            }
                                        else
                                            run {
                                                show("第 ${(i + 1)} 张扫描失败！")
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    if (beanList.size == 1) scanViewModel.setCurrentRealBean(beanList[0])
                    else {
                        scanViewModel.setCurrentRealBeanList(beanList)
                        scanViewModel.setCurrentRealBean(beanList[0])
                    }
                }
            }
        }
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
        fun newInstance() = RecordFragment()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_record_scan_single -> {
                launcherForSingleSelect.launch("image/*")
            }

            R.id.tv_record_scan_multiple -> {
                launcherForMultipleSelect.launch("image/*")
            }

            R.id.tv_record_scan_save -> {
                if (scanFlag) {
                    saveEdit()
                    val currentRealBean = scanViewModel.currentBeanLiveData
                    currentRealBean.value?.also { realBean ->
                        BaseUtil.checkFPData(realBean).apply {
                            if (isNotEmpty()) {
                                show("请检查《$this》是否正确！")
                            } else {
                                val builder = MaterialAlertDialogBuilder(requireContext()).create()
                                builder.apply {
                                    setCancelable(false)
                                    setTitle("保存")
                                    setMessage("确认数据无误？")
                                    setButton(DialogInterface.BUTTON_POSITIVE, "确定") { p0, _ ->
                                        requireActivity().lifecycleScope.launch {
                                            withContext(Dispatchers.IO) {
                                                scanViewModel.saveToDb()
                                            }
                                        }
                                        p0.dismiss()
                                    }
                                    setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { p0, _ ->
                                        show("取消！")
                                        p0?.dismiss()
                                    }
                                }
                                builder.show()
                            }
                        }
                    }
                } else {
                    show("请扫描后再保存！")
                }
            }

            R.id.tv_record_scan_clear -> {
                if (scanFlag) {
                    clearAllText()
                    scanFlag = false
                } else
                    show("当前已无数据")
            }
        }
    }
}