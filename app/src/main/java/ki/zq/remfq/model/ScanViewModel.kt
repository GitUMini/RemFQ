package ki.zq.remfq.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ki.zq.remfq.bean.RealBean
import ki.zq.remfq.db.RealBeanDB
import ki.zq.remfq.enums.EnumSaveFlag
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

    private fun innerUpdateToDb() = ocrDao?.updateBeans(currentBeanLiveData.value)
    fun updateToDb(): EnumSaveFlag {
        var flag = EnumSaveFlag.FLAG_INITIAL
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                isExist(currentBeanLiveData.value?.fpNumber!!)?.also { existFlag ->
                    if (existFlag) {
                        innerUpdateToDb()?.also { updateFlag ->
                            flag = withContext(Dispatchers.Main) {
                                if (updateFlag != 0)
                                    EnumSaveFlag.FLAG_UPDATE
                                else
                                    EnumSaveFlag.FLAG_UPDATE_FAILURE
                            }
                        }
                    } else {
                        addBeanToDb()
                        flag = EnumSaveFlag.FLAG_ADD
                    }
                }
            }
        }
        return flag
    }

    private fun isExist(number: String) = ocrDao?.isExist(number)
    private fun addBeanToDb() = ocrDao?.insertBeans(currentBeanLiveData.value)
}