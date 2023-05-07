package ki.zq.remfp.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import ki.zq.remfp.R
import ki.zq.remfp.bean.RealBean
import ki.zq.remfp.model.HistoryViewModel
import ki.zq.remfp.util.BaseUtil

object CPHistory {
    @Composable
    fun CpHistoryItem(historyViewModel: HistoryViewModel) {
        val dataList by historyViewModel.allBeansLiveData.observeAsState(emptyList())
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            Text(
                text = "导出位置：/storage/emulated/0/Android/data/ki.zq.remfp/files/",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (dataList.isEmpty()) HistoryEmptyItem()
            else {
                LazyColumn {
                    repeat(dataList.size) {
                        item {
                            HistoryNotEmptyItem(
                                dataList[it],
                                onDelete = { historyViewModel.deleteBean(realBeanOuter) })
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HistoryNotEmptyItem(realBean: RealBean, onDelete: (RealBean) -> Unit) {
        val showOwnDialog = remember { mutableStateOf(false) }
        val showChildDetailDialog = remember { mutableStateOf(false) }
        val showChildDeleteDialog = remember { mutableStateOf(false) }
        val data = remember { mutableStateOf("") }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp, bottom = 3.dp)
                .clickable {
                    realBeanOuter = realBean
                    showOwnDialog.value = true
                },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(3.dp),
            border = BorderStroke(0.dp, Color.White),
        ) {
            if (showOwnDialog.value) {
                Alert(
                    showOwnDialog = showOwnDialog.value,
                    onChoiceClicked = {
                        showChildDetailDialog.value = it == 0
                        showChildDeleteDialog.value = it == 1
                    },
                    onDismiss = { showOwnDialog.value = false }
                )
            }
             if (showChildDeleteDialog.value) {
                AlertDialog(
                    onDismissRequest = { showChildDeleteDialog.value = false },
                    title = { Text("删除发票？") },
                    confirmButton = {
                        TextButton(onClick = {
                            onDelete(realBeanOuter)
                            showChildDeleteDialog.value = false
                        }) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showChildDeleteDialog.value = false }) {
                            Text("取消")
                        }
                    }
                )
            }
            Column {
                Text(
                    text = realBean.fpDate!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(id = R.color.seed))
                        .padding(3.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Text(
                    text = "发票号码: " + realBean.fpNumber,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 3.dp, end = 6.dp, bottom = 3.dp)
                )
                Text(
                    text = "开票企业: " + realBean.fpFromCompanyName,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp)
                )
                Text(
                    text = "开票金额: " + realBean.fpAll,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 3.dp, end = 6.dp, bottom = 3.dp)
                )
            }
        }
    }

    @Composable
    fun HistoryEmptyItem() {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "当前无数据",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
        }
    }

    @Composable
    fun Alert(
        showOwnDialog: Boolean,
        onChoiceClicked: (index: Int) -> Unit,
        onDismiss: () -> Unit
    ) {
        if (showOwnDialog) {
            AlertDialog(
                modifier = Modifier,
                properties = DialogProperties(dismissOnClickOutside = false),
                title = {
                    Text("请选择操作")
                },
                text = {
                    Column {
                        Text(
                            text = "详细信息",
                            Modifier
                                .padding(top = 12.dp, bottom = 12.dp)
                                .fillMaxWidth()
                                .clickable {
                                    onChoiceClicked(0)
                                    onDismiss()
                                }
                        )
                        Text(
                            text = "删除发票",
                            Modifier
                                .padding(top = 12.dp, bottom = 12.dp)
                                .fillMaxWidth()
                                .clickable {
                                    onChoiceClicked(1)
                                    onDismiss()
                                }
                        )
                    }
                },
                onDismissRequest = onDismiss,
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                },
                confirmButton = {}
            )
        }
    }

    private lateinit var realBeanOuter: RealBean

    private fun getBeanDetail(): String {
        val detailBuilder: StringBuilder = StringBuilder()
        realBeanOuter.apply {
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
}