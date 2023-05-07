package ki.zq.remfp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ki.zq.remfp.bean.RealBean
import ki.zq.remfp.db.RealBeanDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val realBeanDB = RealBeanDB.getDatabase(application.applicationContext)
    private val ocrDao = realBeanDB.ocrDao()

    private var _deleteLiveData: MutableLiveData<Int> = MutableLiveData<Int>().also {
        it.value = 0
    }
    val deleteLiveData: LiveData<Int> = _deleteLiveData

    val allBeansLiveData = ocrDao.getAllBeans()

    fun deleteBean(realBean: RealBean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val deleteFlag = ocrDao.deleteBeans(realBean)
                withContext(Dispatchers.Main) {
                    _deleteLiveData.value = deleteFlag
                }
            }
        }
    }
}