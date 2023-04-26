package ki.zq.remfq.util

import android.util.Log
import ki.zq.remfq.bean.RealBean
import java.text.SimpleDateFormat
import java.util.*

object BaseUtil {
    fun longToString(time: Long): String = run {
        Log.d("MyTag", "longToString: $time")
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

    fun checkFPData(realBean: RealBean): String {
        val fpCodeFlag = if (realBean.fpCode!!.length == 12) "" else "发票代码"
        val fpNumberFlag = if (realBean.fpNumber!!.length == 8) "" else "发票号码"
        val fpMoneyFlag =
            if ((realBean.fpAll!!.toDouble() == realBean.fpMoney!!.toDouble() + realBean.fpTax!!.toDouble())) "" else "金额、税额、合计"
        return "$fpCodeFlag$fpNumberFlag$fpMoneyFlag"
    }
}