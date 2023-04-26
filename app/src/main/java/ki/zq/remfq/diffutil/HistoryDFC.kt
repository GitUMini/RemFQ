package ki.zq.remfq.diffutil

import androidx.recyclerview.widget.DiffUtil
import ki.zq.remfq.bean.RealBean

class HistoryDFC : DiffUtil.ItemCallback<RealBean>() {

    override fun areItemsTheSame(oldItem: RealBean, newItem: RealBean): Boolean {
        return oldItem.fpNumber == newItem.fpNumber
    }

    override fun areContentsTheSame(oldItem: RealBean, newItem: RealBean): Boolean {
        return (oldItem.fpToPerson == newItem.fpToPerson &&
                oldItem.fpToCompany == newItem.fpToCompany &&
                oldItem.fpCode == newItem.fpCode &&
                oldItem.fpDate == newItem.fpDate &&
                oldItem.fpThing == newItem.fpThing &&
                oldItem.fpFromCompanyCode == newItem.fpFromCompanyCode &&
                oldItem.fpFromCompanyName == newItem.fpFromCompanyName &&
                oldItem.fpMoney == newItem.fpMoney &&
                oldItem.fpTax == newItem.fpTax &&
                oldItem.fpAll == newItem.fpAll)
    }
}