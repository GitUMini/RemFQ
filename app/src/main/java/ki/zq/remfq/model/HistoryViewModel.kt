package ki.zq.remfq.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ki.zq.remfq.bean.RealBean
import ki.zq.remfq.db.RealBeanDB

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val realBeanDB = RealBeanDB.getDatabase(application.applicationContext)

    private val ocrDao by lazy {
        realBeanDB?.ocrDao()
    }

    val allBeans = ocrDao?.getAllBeans()
    fun deleteBean(vararg realBean: RealBean) = ocrDao?.deleteBeans(*realBean)
}