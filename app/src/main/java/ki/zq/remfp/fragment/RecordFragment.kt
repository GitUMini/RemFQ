package ki.zq.remfp.fragment

import android.content.ContentResolver
import android.content.Context
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
import ki.zq.remfp.util.BaseUtils
import ki.zq.remfp.util.BaseUtils.encode64
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
    private var nextIndex = 0

    private lateinit var mContext: Context

    private val launcherForSingleSelect =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                // 获取 ContentResolver
                val contentResolver: ContentResolver = requireActivity().contentResolver
                // 将 URI 转换为 Bitmap，并将其编码为 Base64 字符串
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bitmap = resizeBitmap(BitmapFactory.decodeStream(inputStream))
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    selected64s.add(baos.toByteArray().encode64())
                }
                getResultFromNet()
                setEditAble(selected64s.isNotEmpty())
            } else {
                show("没有选择任何发票图片！")
            }
        }
    private val launcherForMultipleSelect =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                for (i in uris.indices) {
                    // 获取 ContentResolver
                    val contentResolver: ContentResolver = requireActivity().contentResolver
                    // 将 URI 转换为 Bitmap，并将其编码为 Base64 字符串
                    val inputStream = contentResolver.openInputStream(uris[i])
                    if (inputStream != null) {
                        val bitmap = resizeBitmap(BitmapFactory.decodeStream(inputStream))
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        baos.toByteArray().encode64().apply {
                            if (selected64s.contains(this)) {
                                show("存在相同发票，已过滤！")
                            } else {
                                selected64s.add(this)
                            }
                        }
                    }
                }
                getResultFromNet()
                setEditAble(selected64s.isNotEmpty())
                binding.recordLlcOperation.visibility =
                    if (selected64s.size > 1) View.VISIBLE else View.GONE
            } else {
                show("没有选择任何发票图片！")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater)
        mContext = requireContext().applicationContext

        initViews()
        initListener()
        initLiveDataObserver()
        return binding.root
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        return if ((bitmap.width > 4096) or (bitmap.height > 4096)) {
            Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
        } else
            bitmap
    }

    private fun initLiveDataObserver() {
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
        scanViewModel.currentBeanLiveData.observe(viewLifecycleOwner) {
            for (i in textInputLayouts.indices) {
                textInputLayouts[i].editText?.setText(it.toStringList()[i])
            }
        }
    }

    private fun initListener() {
        binding.tvRecordScanSingle.setOnClickListener(this)
        binding.tvRecordScanMultiple.setOnClickListener(this)
        binding.tvRecordScanSave.setOnClickListener(this)
        binding.tvRecordScanClear.setOnClickListener(this)
        binding.btnRecordScanPrevious.setOnClickListener(this)
        binding.btnRecordScanNext.setOnClickListener(this)
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
                BeanUtils.switchOCRBean2RealBean(ins, BaseUtils.getFPToPerson(requireContext()))
            scanViewModel.setCurrentRealBean(resultBean)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            run {
                show("扫描失败！")
            }
        }
        return resultBean
    }

    private fun clearAll() {
        if (scanFlag) {
            textInputLayouts.forEach {
                it.editText?.setText("")
            }
            setEditAble(false)
            selected64s.clear()
            beanList.clear()
            binding.recordLlcOperation.visibility = View.GONE
            scanFlag = false
        } else {
            run {
                show("当前没有任何内容！")
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
                        scanViewModel.setCurrentRealBean(beanList[nextIndex])
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
                binding.recordLlcOperation.visibility = View.GONE
                selected64s.clear()
                if (scanFlag) {
                    clearAll()
                }
                launcherForSingleSelect.launch("image/*")
            }

            R.id.tv_record_scan_multiple -> {
                selected64s.clear()
                if (scanFlag) {
                    clearAll()
                }
                launcherForMultipleSelect.launch("image/*")
            }

            R.id.tv_record_scan_save -> {
                if (scanFlag) {
                    saveEdit()
                    val currentRealBean = scanViewModel.currentBeanLiveData
                    currentRealBean.value?.also { realBean ->
                        BaseUtils.checkFPData(realBean).apply {
                            if (isNotEmpty()) {
                                show("请检查《$this》是否正确！")
                            } else {
                                val builder = MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("即将保存，确认数据无误？")
                                    .setCancelable(false)
                                    .create()
                                builder.setButton(
                                    DialogInterface.BUTTON_POSITIVE,
                                    "确定"
                                ) { dialog, _ ->
                                    lifecycleScope.launch {
                                        withContext(Dispatchers.IO) {
                                            scanViewModel.saveToDb()
                                        }
                                    }
                                    dialog?.dismiss()
                                }
                                builder.setButton(
                                    DialogInterface.BUTTON_NEGATIVE,
                                    "取消"
                                ) { dialog, _ ->
                                    show("取消保存！")
                                    dialog?.dismiss()
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
                clearAll()
            }

            R.id.btn_record_scan_previous -> {
                if (nextIndex > 0) {
                    nextIndex--
                    scanViewModel.setCurrentRealBean(beanList[nextIndex])
                    show("当前${(nextIndex + 1)}张发票")
                } else {
                    show("已经是第一张了！")
                }
            }

            R.id.btn_record_scan_next -> {
                if (nextIndex < beanList.size - 1) {
                    nextIndex++
                    scanViewModel.setCurrentRealBean(beanList[nextIndex])
                    show("当前${(nextIndex + 1)}张发票")
                } else {
                    show("已经是最后一张了！")
                }
            }
        }
    }
}