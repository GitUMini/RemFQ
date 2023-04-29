package ki.zq.remfq.util

import android.content.Context.MODE_PRIVATE
import android.util.Log
import ki.zq.remfq.BaseApplication
import ki.zq.remfq.bean.RealBean
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

object BaseUtil {
    fun longToString(time: Long): String = run {
        val format = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
        val date = Date(time)
        format.format(date)
    }

    fun lts2(time: Long): Int = run {
        val format = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
        val date = Date(time)
        format.format(date).toInt()
    }

    fun stringToLong(time: String): Long = run {
        val format = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
        val date = format.parse(time)
        date?.time ?: 0
    }

    fun log(logStr: String) {
        Log.d("MyTag", logStr)
    }

    fun saveToken(token: String) {
        BaseApplication.instance.getSharedPreferences("OCR_TOKEN", MODE_PRIVATE).apply {
            edit().apply {
                putString("token", token)
                apply()
            }
            saveTokenDate()
        }
    }

    fun getToken(): String? {
        BaseApplication.instance.getSharedPreferences("OCR_TOKEN", MODE_PRIVATE).apply {
            return getString("token", "")
        }
    }

    private fun saveTokenDate() {
        BaseApplication.instance.getSharedPreferences("OCR_TOKEN", MODE_PRIVATE).apply {
            edit().apply {
                val now = LocalDate.now() // 获取当前日期
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // 定义日期格式
                val formatted = now.format(formatter) // 格式化日期
                putString("tokenDate", formatted)
                apply()
            }
        }
    }

    private fun getTokenDate(): String? {
        BaseApplication.instance.getSharedPreferences("OCR_TOKEN", MODE_PRIVATE).apply {
            return getString("tokenDate", "")
        }
    }

    fun checkFPData(realBean: RealBean): String {
        val fpCodeFlag = if (realBean.fpCode!!.length == 12) "" else "发票代码"
        val fpNumberFlag = if (realBean.fpNumber!!.length == 8) "" else "发票号码"
        val fpMoneyFlag =
            if ((realBean.fpAll!!.toDouble() == realBean.fpMoney!!.toDouble() + realBean.fpTax!!.toDouble())) "" else "金额、税额、合计"
        return "$fpCodeFlag$fpNumberFlag$fpMoneyFlag"
    }

    fun isOutDate(): Boolean {
        return if (getTokenDate().isNullOrBlank()) false
        else{
            val now = LocalDate.now() // 获取当前日期
            val target = LocalDate.parse(getTokenDate()) // 目标日期
            val days = ChronoUnit.DAYS.between(now, target) // 计算两个日期之间的天数差值
            println("相隔${days}天")
            days > 30
        }
    }
}