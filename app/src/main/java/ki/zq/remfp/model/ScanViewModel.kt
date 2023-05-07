package ki.zq.remfp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ki.zq.remfp.bean.RealBean
import ki.zq.remfp.db.RealBeanDB
import ki.zq.remfp.enums.EnumSaveFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentBeanMutableLiveData: MutableLiveData<RealBean> by lazy {
        MutableLiveData<RealBean>().apply {
            value = RealBean().apply {
                fpToPerson = ""
                fpToCompany = ""
                fpCode = ""
                fpNumber = ""
                fpDate = ""
                fpThing = ""
                fpFromCompanyCode = ""
                fpFromCompanyName = ""
                fpMoney = ""
                fpTax = ""
                fpAll = ""
            }
        }
    }

    private val _currentBeanListMutableLiveData: MutableLiveData<MutableList<RealBean>> by lazy {
        MutableLiveData<MutableList<RealBean>>().apply {
            value = arrayListOf(RealBean().apply {
                fpToPerson = ""
                fpToCompany = ""
                fpCode = ""
                fpNumber = ""
                fpDate = ""
                fpThing = ""
                fpFromCompanyCode = ""
                fpFromCompanyName = ""
                fpMoney = ""
                fpTax = ""
                fpAll = ""
            })
        }
    }
    val currentBeanLiveData: LiveData<RealBean> get() = _currentBeanMutableLiveData
    val currentBeanListLiveData: LiveData<MutableList<RealBean>> get() = _currentBeanListMutableLiveData

    private val _saveFlagMutableLiveData: MutableLiveData<EnumSaveFlag> by lazy {
        MutableLiveData<EnumSaveFlag>().apply {
            EnumSaveFlag.FLAG_INITIAL
        }
    }
    val saveFlagLiveData: LiveData<EnumSaveFlag> get() = _saveFlagMutableLiveData

    fun setCurrentRealBean(realBean: RealBean) {
        _currentBeanMutableLiveData.value = realBean
    }

    fun setCurrentRealBeanList(realBeanList: MutableList<RealBean>) {
        _currentBeanListMutableLiveData.value = realBeanList
    }

    private val realBeanDB = RealBeanDB.getDatabase(application.applicationContext)
    private val ocrDao by lazy { realBeanDB.ocrDao() }

    fun saveToDb() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existBean = getBeanFromDB()
                if (existBean != null) {
                    withContext(Dispatchers.Main) {
                        _saveFlagMutableLiveData.value = EnumSaveFlag.FLAG_IS_EXIST
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        addBeanToDb()
                        withContext(Dispatchers.Main) {
                            _saveFlagMutableLiveData.value = EnumSaveFlag.FLAG_ADD_SUCCESS
                        }
                    }
                }
            }
        }
    }

    fun getBeanFromDB() = ocrDao.getBean(currentBeanLiveData.value?.fpNumber!!)
    fun addBeanToDb() = ocrDao.insertBeans(currentBeanLiveData.value)
}