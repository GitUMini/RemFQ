package ki.zq.remfq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.baidu.ocr.sdk.OCR
import com.baidu.ocr.sdk.OnResultListener
import com.baidu.ocr.sdk.exception.OCRError
import com.baidu.ocr.sdk.model.AccessToken
import com.baidu.ocr.ui.camera.CameraActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import ki.zq.remfq.bean.FPBean
import ki.zq.remfq.bean.RealBean
import ki.zq.remfq.databinding.FragmentScanBinding
import ki.zq.remfq.model.ScanViewModel
import ki.zq.remfq.ocr.FileUtil
import ki.zq.remfq.ocr.RecognizeService
import ki.zq.remfq.util.BaseUtil
import ki.zq.remfq.util.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var scanViewModel: ScanViewModel
    private lateinit var edtList: MutableList<EditText>
    private lateinit var loadingDialog: LoadingDialog

    private val requestCodeVatInvoice = 131
    private var hasGotToken = false
    private val alertDialog: AlertDialog.Builder? = null

    private var scanFlag = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater)
        scanViewModel = ViewModelProvider(requireActivity()).get(ScanViewModel::class.java)
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
        // 增值税发票识别
        binding.tvRecordScan.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (!checkTokenStatus()) {
                    return
                }
                val intent = Intent(requireActivity(), CameraActivity::class.java)
                intent.putExtra(
                    CameraActivity.KEY_OUTPUT_FILE_PATH,
                    FileUtil.getSaveFile(requireContext()).absolutePath
                )
                intent.putExtra(
                    CameraActivity.KEY_CONTENT_TYPE,
                    CameraActivity.CONTENT_TYPE_GENERAL
                )
                startActivityForResult(intent, requestCodeVatInvoice)
            }
        })

        binding.tvRecordSave.setOnClickListener {
            val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            val flag = requireActivity().checkSelfPermission(permission)
            if (flag == PackageManager.PERMISSION_DENIED) {
                requestPermissions(arrayOf(permission), 0)
            }
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
                                    requireActivity().lifecycleScope.launch {
                                        withContext(Dispatchers.IO) {
                                            val isExist = scanViewModel.isExist(realBean.fpNumber!!)
                                            if (isExist!!) {
                                                val count = scanViewModel.updateBeansToDb()
                                                if (count != 0)
                                                    withContext(Dispatchers.Main) {
                                                        show("该发票已被扫描过，已更新！")
                                                    }
                                                else
                                                    withContext(Dispatchers.Main) {
                                                        show("发票信息更新失败，请重试！")
                                                    }

                                            } else {
                                                scanViewModel.addBeanToDb()
                                                withContext(Dispatchers.Main) {
                                                    show("该发票未被扫描过，已保存！")
                                                }
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
        initAccessToken()
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

    private fun checkTokenStatus(): Boolean {
        if (!hasGotToken) {
            Toast.makeText(requireContext(), "Token还未成功获取", Toast.LENGTH_LONG).show()
        }
        return hasGotToken
    }

    //以license文件方式初始化
    private fun initAccessToken() {
        OCR.getInstance(requireContext()).initAccessToken(object : OnResultListener<AccessToken> {
            override fun onResult(accessToken: AccessToken) {
                val token = accessToken.accessToken
                token.toString()
                hasGotToken = true
            }

            override fun onError(error: OCRError) {
                error.printStackTrace()
                requireActivity().runOnUiThread {
                    alertDialog?.setTitle("获取Token失败")
                        ?.setMessage(error.message.toString())
                        ?.setPositiveButton("确定", null)
                        ?.show()
                }
            }
        }, requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 识别成功回调，增值税发票
        if (requestCode == requestCodeVatInvoice && resultCode == Activity.RESULT_OK) {
            run {
                loadingDialog.show()
            }
            val path = FileUtil.getSaveFile(requireContext()).absolutePath
            RecognizeService.recVatInvoice(requireContext(), path) { result ->
                run {
                    if (result.isEmpty()) {
                        scanFlag = false
                    } else {
                        try {
                            val resFlag = analysisResult(result)
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
                requireActivity().runOnUiThread {
                    loadingDialog.dismiss()
                }
            }
        } else {
            run {
                show("取消扫描！")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initAccessToken()
        } else {
            Toast.makeText(requireContext(), "没有该权限则无法进行票据识别！", Toast.LENGTH_LONG).show()
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