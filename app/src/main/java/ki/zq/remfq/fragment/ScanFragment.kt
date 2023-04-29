package ki.zq.remfq.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.baidu.ocr.sdk.OCR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import ki.zq.remfq.bean.FPBean
import ki.zq.remfq.bean.RealBean
import ki.zq.remfq.databinding.FragmentScanBinding
import ki.zq.remfq.enums.EnumSaveFlag
import ki.zq.remfq.model.ScanViewModel
import ki.zq.remfq.ocr.Base64Util
import ki.zq.remfq.ocr.HttpUtil
import ki.zq.remfq.util.BaseUtil
import ki.zq.remfq.util.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private val scanViewModel by viewModels<ScanViewModel>()
    private lateinit var edtList: MutableList<EditText>
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>

    private var scanFlag = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater)
        edtList = arrayListOf(
            binding.scanTietPerson,
            binding.scanTietToName,
            binding.scanTietFpCode,
            binding.scanTietFpNumber,
            binding.scanTietDate,
            binding.scanTietThing,
            binding.scanTietFromCode,
            binding.scanTietFromName,
            binding.scanTietMoney,
            binding.scanTietTax,
            binding.scanTietAll,
        )

        initView()
        setEditAble(false)
        return binding.root
    }

    @SuppressLint("InflateParams")
    private fun initView() {
        loadingDialog = LoadingDialog(requireContext())
        val launcher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result: Map<String, Boolean> ->
                if (result[Manifest.permission.READ_EXTERNAL_STORAGE] != null && result[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
                    //权限全部获取到之后的动作
                    pickPicture()
                } else {
                    //有权限没有获取到的动作
                    show("有权限被拒绝！")
                }
            }

        // 增值税发票识别
        binding.tvRecordScan.setOnClickListener {
            launcher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }

        binding.tvRecordSave.setOnClickListener {
            if (scanFlag) {
                saveEdit()
                val currentRealBean = scanViewModel.currentBeanLiveData
                currentRealBean.value?.also { realBean ->
                    BaseUtil.checkFPData(realBean).apply {
                        if (length != 0) {
                            show("请检查《$this》是否正确！")
                        } else {
                            val builder = MaterialAlertDialogBuilder(requireContext()).create()
                            builder.apply {
                                setCancelable(false)
                                setTitle("保存")
                                setMessage("确认数据无误？")
                                setButton(DialogInterface.BUTTON_POSITIVE, "确定") { p0, _ ->
                                    scanViewModel.updateToDb().apply {
                                        when (this) {
                                            EnumSaveFlag.FLAG_ADD -> {
                                                show("保存成功")
                                            }
                                            EnumSaveFlag.FLAG_ADD_FAILURE -> {
                                                show("保存失败")
                                            }
                                            EnumSaveFlag.FLAG_UPDATE -> {
                                                show("更新成功")
                                            }
                                            EnumSaveFlag.FLAG_UPDATE_FAILURE -> {
                                                show("更新失败")
                                            }
                                            else -> {
                                                show("操作无效")
                                            }
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

        binding.tvRecordClear.setOnClickListener {
            clearAllText()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var selectedImage: ByteArray?
        // 使用ActivityResultLauncher注册图库返回的结果
        galleryResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val imageUri = result.data?.data
                    val inputStream = requireActivity().contentResolver.openInputStream(imageUri!!)
                    selectedImage = inputStream?.readBytes()
                    // 对选择的图片进行base64加密
                    //val base64String = Base64.getEncoder().encodeToString(selectedImage)
                    //println("Base64加密结果: $base64String")
                    onPickResult(Base64Util.encode(selectedImage))
                }
            }
    }

    private fun pickPicture() {
        // 使用ActivityResultLauncher注册图库返回的结果
        // 创建一个启动图库的Intent
        val galleryIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        galleryResultLauncher.launch(galleryIntent)
    }

    private fun getToPerson(): String? {
        val shp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            requireContext().applicationContext
        )
        return shp.getString("toPerson", "报销人员姓名")
    }

    private fun clearAllText() {
        if (scanFlag) {
            edtList.forEach {
                it.setText("")
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
        edtList.forEach {
            it.isEnabled = enable
        }
    }

    private fun show(info: String) {
        Toast.makeText(requireContext(), info, Toast.LENGTH_SHORT).show()
    }

    private fun analysisResult(result: String): Boolean {
        val gson = Gson()
        try {
            val fpBean = gson.fromJson(result, FPBean::class.java)
            val resultBean = RealBean()
            scanViewModel.setCurrentRealBean(
                resultBean.apply {
                    fpToPerson = getToPerson()
                    fpToCompany = fpBean.words_result.purchaserName
                    fpCode = fpBean.words_result.invoiceCode.toString()
                    fpNumber = fpBean.words_result.invoiceNum.toString()
                    fpDate = BaseUtil.stringToLong(fpBean.words_result.invoiceDate)
                    fpThing = fpBean.words_result.commodityName[0].word.toString()
                    fpFromCompanyCode = fpBean.words_result.sellerRegisterNum.toString()
                    fpFromCompanyName = fpBean.words_result.sellerName.toString()
                    fpMoney = fpBean.words_result.commodityAmount[0].word.toString()
                    fpTax = fpBean.words_result.commodityTax[0].word.toString()
                    fpAll = fpBean.words_result.amountInFiguers.toString()
                })
            setEditAble(true)
            for (i in 0 until edtList.size) {
                edtList[i].setText(resultBean.toStringList()[i])
            }
            scanFlag = true
            return true
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            return false
        }
    }

    private fun saveEdit() {
        val hasEditResult = arrayListOf<String>()
        edtList.forEach {
            hasEditResult.add(it.text.toString())
        }
        scanViewModel.setCurrentRealBean(
            RealBean().apply {
                fpToPerson = hasEditResult[0]
                fpToCompany = hasEditResult[1]
                fpCode = hasEditResult[2]
                fpNumber = hasEditResult[3]
                fpDate = BaseUtil.stringToLong(hasEditResult[4])
                fpThing = hasEditResult[5]
                fpFromCompanyCode = hasEditResult[6]
                fpFromCompanyName = hasEditResult[7]
                fpMoney = hasEditResult[8]
                fpTax = hasEditResult[9]
                fpAll = hasEditResult[10]
            }
        )
    }

    private fun onPickResult(fpPicString: String) {
        // 识别成功回调，增值税发票
        run {
            loadingDialog.show()
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                HttpUtil.multipleInvoice(fpPicString)?.apply {
                    withContext(Dispatchers.Main) {
                        if (isEmpty()) {
                            scanFlag = false
                        } else {
                            try {
                                val resFlag = analysisResult(this@apply)
                                if (resFlag) {
                                    run {
                                        show("扫描成功！")
                                    }
                                } else {
                                    run {
                                        show("扫描失败！")
                                    }
                                }
                            } catch (e: IndexOutOfBoundsException) {
                                run {
                                    show("扫描得到的结果有误，请重试！")
                                }
                            }
                        }
                    }
                }
            }
        }
        requireActivity().runOnUiThread {
            loadingDialog.dismiss()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        OCR.getInstance(requireContext()).release()
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