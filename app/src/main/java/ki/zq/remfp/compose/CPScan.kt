package ki.zq.remfp.compose

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.google.gson.JsonSyntaxException
import ki.zq.remfp.R
import ki.zq.remfp.bean.RealBean
import ki.zq.remfp.model.ScanViewModel
import ki.zq.remfp.ocr.HttpUtil
import ki.zq.remfp.util.BeanUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*

object CPScan {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CPScanResult(model: ScanViewModel) {
        val dataList = model.currentBeanLiveData.value?.toStringList()
        var currentBean by remember { mutableStateOf(RealBean()) }
        var startSave by remember { mutableStateOf(false) }

        var textFields by remember { mutableStateOf(List(11) { "" }) }
        var canEditFields by remember { mutableStateOf(List(11) { false }) }
        var scanFlag by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val selected64 = remember { mutableStateOf<String?>(null) }
        val selected64s = remember { mutableStateOf<List<String?>>(emptyList()) }
        val launcherForSingleSelect =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    // 获取 ContentResolver
                    val contentResolver: ContentResolver = context.contentResolver
                    // 将 URI 转换为 Bitmap，并将其编码为 Base64 字符串
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                        selected64.value =
                            Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
                    }
                }
            }
        val launcherForMultipleSelect =
            rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                val innerSelected64s: MutableList<String> = arrayListOf()
                for (i in uris.indices) {
                    // 获取 ContentResolver
                    val contentResolver: ContentResolver = context.contentResolver
                    // 将 URI 转换为 Bitmap，并将其编码为 Base64 字符串
                    val inputStream = contentResolver.openInputStream(uris[i])
                    if (inputStream != null) {
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                        innerSelected64s.add(
                            Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
                        )
                    }
                }
                selected64s.value = innerSelected64s
            }

        val labels = arrayOf(
            "报销人", "受票单位", "发票代码", "发票号码", "开票日期", "发票名目",
            "开票企业税号", "开票企业名称", "金额", "税额", "合计"
        )

        if (!dataList.isNullOrEmpty()) {
            for (i in dataList.indices) {
                textFields.toMutableList().apply {
                    set(i, dataList[i])
                }
            }
        }
        Column {
            LaunchedEffect(key1 = selected64.value) {
                withContext(Dispatchers.IO) {
                    if (selected64.value != null && selected64.value!!.isNotEmpty())
                        HttpUtil.multipleInvoice(selected64.value!!)?.also { result ->
                            withContext(Dispatchers.Main) {
                                if (result.isEmpty()) {
                                    scanFlag = false
                                } else {
                                    val resultBean = getFinalResult(result, model, context)
                                    if (resultBean != null) {
                                        canEditFields = canEditFields.toMutableList().apply {
                                            for (i in 0 until 11)
                                                set(i, true)
                                        }
                                        textFields = textFields.toMutableList().apply {
                                            for (i in 0 until 11)
                                                set(i, resultBean.toStringList()[i])
                                        }
                                        scanFlag = true
                                        run {
                                            show(context, "扫描成功！")
                                        }
                                    } else {
                                        run {
                                            show(context, "扫描失败！")
                                        }
                                    }
                                }
                            }
                        }
                }
                selected64.value = ""
            }
            LaunchedEffect(key1 = selected64s.value) {
                withContext(Dispatchers.IO) {
                    if (selected64s.value.isNotEmpty()) {
                        for (i in selected64s.value.indices) {
                            HttpUtil.multipleInvoice(selected64s.value[i]!!)?.also { result ->
                                withContext(Dispatchers.Main) {
                                    if (result.isEmpty()) {
                                        scanFlag = false
                                    } else {
                                        val resultBean = getFinalResult(result, model, context)
                                        if (resultBean != null) {
                                            println(i)
                                            println(resultBean.toString())
                                            scanFlag = true
                                            if (scanFlag) {
                                                currentBean =
                                                    BeanUtils.listResult2RealBean(resultBean.toStringList())
                                                model.setCurrentRealBean(currentBean)
                                                startSave = true
                                            }
                                            run {
                                                show(context, "扫描成功！")
                                            }
                                        } else {
                                            run {
                                                show(context, "扫描失败！")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                selected64.value = ""
            }

            if (startSave) {
                LaunchedEffect(Unit) {
                    val isExist = withContext(Dispatchers.IO) {
                        model.getBeanFromDB()
                    }
                    if (isExist != null) {
                        show(context, "发票已经存在")
                    } else {
                        withContext(Dispatchers.IO) {
                            model.addBeanToDb()
                        }.apply {
                            if (this > 0) show(context, "发票添加成功")
                            else show(context, "发票添加失败")
                        }
                    }
                    startSave = false
                }
            }

            OperationHead(onChoiceClicked = {
                when (it) {
                    0 -> {
                        launcherForSingleSelect.launch("image/*")
                    }
                    1 -> {
                        launcherForMultipleSelect.launch("image/*")
                    }
                    2 -> {
                        if (scanFlag) {
                            currentBean = BeanUtils.listResult2RealBean(textFields)
                            model.setCurrentRealBean(currentBean)
                            startSave = true
                            if (getIsClearAfterSave(context)) {
                                textFields = textFields.toMutableList().apply {
                                    for (i in 0 until 11)
                                        set(i, "")
                                }
                                canEditFields = canEditFields.toMutableList().apply {
                                    for (i in 0 until 11)
                                        set(i, false)
                                }
                            }
                        } else
                            show(context, "请扫描后再保存！")
                    }
                    3 -> {
                        if (scanFlag) {
                            textFields = textFields.toMutableList().apply {
                                for (i in 0 until 11)
                                    set(i, "")
                            }
                            canEditFields = canEditFields.toMutableList().apply {
                                for (i in 0 until 11)
                                    set(i, false)
                            }
                            scanFlag = false
                        } else
                            show(context, "当前已无数据")
                    }
                }
            })

            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2) // 3 columns
            ) {
                for (i in 0 until 6) {
                    item {
                        OutlinedTextField(
                            value = textFields[i],
                            onValueChange = { newText ->
                                textFields = textFields.toMutableList().apply { set(i, newText) }
                            },
                            enabled = canEditFields[i],
                            singleLine = true,
                            label = { Text(labels[i]) },
                            isError = false,
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .padding(8.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = textFields[6],
                onValueChange = { newText ->
                    textFields = textFields.toMutableList().apply { set(6, newText) }
                },
                singleLine = true,
                enabled = canEditFields[6],
                label = { Text(labels[6]) },
                isError = false,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            )
            OutlinedTextField(
                value = textFields[7],
                onValueChange = { newText ->
                    textFields = textFields.toMutableList().apply { set(7, newText) }
                },
                singleLine = true,
                enabled = canEditFields[7],
                label = { Text(labels[7]) },
                isError = false,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3) // 3 columns
            ) {
                for (i in 8 until 11) {
                    item {
                        OutlinedTextField(
                            value = textFields[i],
                            enabled = canEditFields[i],
                            onValueChange = { newText ->
                                textFields = textFields.toMutableList().apply { set(i, newText) }
                            },
                            singleLine = true,
                            label = { Text(labels[i]) },
                            isError = false,
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun OperationHead(onChoiceClicked: (index: Int) -> Unit) {
        Column(
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onChoiceClicked(0) },
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_single_choose),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "单张识别", style = MaterialTheme.typography.labelSmall)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onChoiceClicked(1) },
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_muti_choose),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "多张识别", style = MaterialTheme.typography.labelSmall)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onChoiceClicked(2) }
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_save),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "保存发票", style = MaterialTheme.typography.labelSmall)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onChoiceClicked(3) }
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_clear),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "清除内容", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    private fun getFPToPerson(context: Context): String? {
        val shp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            context.applicationContext
        )
        return shp.getString("FPToPerson", "报销人员姓名")
    }
    private fun getIsClearAfterSave(context: Context): Boolean {
        val shp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            context.applicationContext
        )
        return shp.getBoolean("IsClearAfterSave", false)
    }
    private fun getFinalResult(ins: String, model: ScanViewModel, ctx: Context): RealBean? {
        var resultBean: RealBean? = null
        try {
            resultBean = BeanUtils.switchOCRBean2RealBean(ins, getFPToPerson(ctx))
            model.setCurrentRealBean(resultBean)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            run {
                show(ctx, "扫描失败！")
            }
        }
        return resultBean
    }
    private fun show(context: Context, info: String) {
        Toast.makeText(context, info, Toast.LENGTH_SHORT).show()
    }
}