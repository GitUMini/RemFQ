package ki.zq.remfq.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import ki.zq.remfq.R
import ki.zq.remfq.bean.RealBean
import ki.zq.remfq.util.BaseUtil

class HistoryAdapter : BaseQuickAdapter<RealBean, BaseViewHolder>(0) {
    private fun getColor(id: Int): Int {
        return ResourcesCompat.getColor(context.resources, id, null)
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_history, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: RealBean) {
        holder.setBackgroundColor(R.id.item_tv_time, getColor(R.color.md_theme_light_primary))
        holder.setTextColor(R.id.item_tv_time, getColor(R.color.white))

        holder.setText(R.id.item_tv_time, BaseUtil.longToString(item.fpDate!!))
        holder.setText(R.id.item_et_number, "发票号码: " + item.fpNumber)
        holder.setText(R.id.item_et_kpqy, "开票企业: " + item.fpFromCompanyName)
        holder.setText(R.id.item_et_zjj, "开票金额: " + item.fpAll)
    }
}