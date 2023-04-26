package ki.zq.remfq.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ki.zq.remfq.bean.RealBean
import ki.zq.remfq.db.RealBeanDB

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentBeanMutableLiveData: MutableLiveData<RealBean> by lazy {
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
    val currentBeanLiveData: LiveData<RealBean> get() = _currentBeanMutableLiveData
    fun setCurrentRealBean(realBean: RealBean) {
        _currentBeanMutableLiveData.value = realBean
    }

    private val realBeanDB = RealBeanDB.getDatabase(application.applicationContext)
    private val ocrDao by lazy { realBeanDB?.ocrDao() }
    fun updateBeansToDb() = ocrDao?.updateBeans(currentBeanLiveData.value)
    fun isExist(number: String) = ocrDao?.isExist(number)
    fun addBeanToDb() = ocrDao?.insertBeans(currentBeanLiveData.value)
}