package ki.zq.remfq.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ki.zq.remfq.util.BaseUtil

@Entity(tableName = "INVOICE")
class RealBean {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null

    @ColumnInfo
    var fpToPerson: String? = null

    @ColumnInfo
    var fpToCompany: String? = null

    @ColumnInfo
    var fpCode: String? = null

    @ColumnInfo
    var fpNumber: String? = null //发票号码唯一

    @ColumnInfo
    var fpDate: Long? = null

    @ColumnInfo
    var fpThing: String? = null

    @ColumnInfo
    var fpFromCompanyCode: String? = null

    @ColumnInfo
    var fpFromCompanyName: String? = null

    @ColumnInfo
    var fpMoney: String? = null

    @ColumnInfo
    var fpTax: String? = null

    @ColumnInfo
    var fpAll: String? = null

    override fun toString(): String {
        return "{" +
                "\"id\":\"" + id + '\"' +
                ", \"fpToPerson\":\"" + fpToPerson + '\"' +
                ", \"fpToCompany\":\"" + fpToCompany + '\"' +
                ", \"fpCode\":\"" + fpCode + '\"' +
                ", \"fpNumber\":\"" + fpNumber + '\"' +
                ", \"fpDate\":\"" + fpDate + '\"' +
                ", \"fpThing\":\"" + fpThing + '\"' +
                ", \"fpFromCompanyCode\":\"" + fpFromCompanyCode + '\"' +
                ", \"fpFromCompanyName\":\"" + fpFromCompanyName + '\"' +
                ", \"fpMoney\":\"" + fpMoney + '\"' +
                ", \"fpTax\":\"" + fpTax + '\"' +
                ", \"fpAll\":\"" + fpAll + '\"' +
                '}'
    }

    fun toStringList(): MutableList<String?> {
        return arrayListOf(
            fpToPerson,
            fpToCompany,
            fpCode,
            fpNumber,
            BaseUtil.longToString(fpDate!!),
            fpThing,
            fpFromCompanyCode,
            fpFromCompanyName,
            fpMoney,
            fpTax,
            fpAll
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RealBean) return false
        return fpNumber == other.fpNumber
    }

    override fun hashCode(): Int {
        return fpNumber.hashCode()
    }
}