package ki.zq.remfq.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ki.zq.remfq.bean.RealBean
import ki.zq.remfq.db.RealBeanDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val _realBeanLiveData: MutableLiveData<RealBean> by lazy {
        MutableLiveData<RealBean>().apply {
            value = RealBean().apply {
                fpToPerson = ""
                fpToCompany = ""
                fpCode = ""
                fpNumber = ""
                fpDate = System.currentTimeMillis()
                fpThing = ""
                fpFromCompanyCode = ""
                fpFromCompanyName = ""
                fpMoney = ""
                fpTax = ""
                fpAll = ""
            }
        }
    }
    private val realBeanLiveData: LiveData<RealBean> get() = _realBeanLiveData

    fun getCurrentRealBean(): LiveData<RealBean> = realBeanLiveData
    fun setCurrentRealBean(realBean: RealBean) {
        _realBeanLiveData.value = realBean
    }

    private val realBeanDB = RealBeanDB.getDatabase(application.applicationContext)

    private val ocrDao by lazy {
        realBeanDB?.ocrDao()
    }

    fun updateBeansToDb() = ocrDao?.updateBeans(getCurrentRealBean().value)

    fun getBeanFromDb(number: String) = ocrDao?.getBean(number)
    fun isExist(number: String) = ocrDao?.isExist(number)

    fun addBeanToDb() =
        viewModelScope.launch { withContext(Dispatchers.IO) { ocrDao?.insertBeans(getCurrentRealBean().value) } }

    fun getAllBeans() = ocrDao?.getAllBeans()
}