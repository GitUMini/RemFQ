package ki.zq.remfq.diffutil

import androidx.recyclerview.widget.DiffUtil
import ki.zq.remfq.bean.RealBean

class HistoryDiffUtil(
    private val oldVideoList: MutableList<RealBean>,
    private val newVideoList: MutableList<RealBean>
) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldVideoList.size
    }

    override fun getNewListSize(): Int {
        return newVideoList.size
    }

    // 判断Item是否已经存在
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldVideoList[oldItemPosition].fpNumber === newVideoList[newItemPosition].fpNumber
    }

    // 如果Item已经存在则会调用此方法，判断Item的内容是否一致
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return (oldVideoList[oldItemPosition].fpToPerson == newVideoList[newItemPosition].fpToPerson &&
                oldVideoList[oldItemPosition].fpToCompany == newVideoList[newItemPosition].fpToCompany &&
                oldVideoList[oldItemPosition].fpCode == newVideoList[newItemPosition].fpCode &&
                oldVideoList[oldItemPosition].fpDate == newVideoList[newItemPosition].fpDate &&
                oldVideoList[oldItemPosition].fpThing == newVideoList[newItemPosition].fpThing &&
                oldVideoList[oldItemPosition].fpFromCompanyCode == newVideoList[newItemPosition].fpFromCompanyCode &&
                oldVideoList[oldItemPosition].fpFromCompanyName == newVideoList[newItemPosition].fpFromCompanyName &&
                oldVideoList[oldItemPosition].fpMoney == newVideoList[newItemPosition].fpMoney &&
                oldVideoList[oldItemPosition].fpTax == newVideoList[newItemPosition].fpTax &&
                oldVideoList[oldItemPosition].fpAll == newVideoList[newItemPosition].fpAll)
    }
}