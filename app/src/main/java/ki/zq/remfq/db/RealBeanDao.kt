package ki.zq.remfq.db

import androidx.lifecycle.LiveData
import androidx.room.*
import ki.zq.remfq.bean.RealBean
import kotlinx.coroutines.flow.Flow

@Dao
interface RealBeanDao {
    @Insert
    fun insertBeans(vararg realBeans: RealBean?)

    @Update
    fun updateBeans(vararg realBeans: RealBean?): Int

    @Delete
    fun deleteBeans(vararg realBeans: RealBean?): Int

    @Suppress("unused")
    @Query("DELETE FROM INVOICE")
    fun deleteAllBeans()

    @Suppress("unused")
    @Query("SELECT * FROM INVOICE WHERE fpNumber LIKE :number")
    fun getBean(number: String): RealBean?

    @Query("SELECT * FROM INVOICE ORDER BY ID DESC")
    fun getAllBeans(): LiveData<MutableList<RealBean>>

    @Suppress("unused")
    @Query("SELECT * FROM INVOICE WHERE fpNumber LIKE :number")
    fun isExist(number: String): Boolean

    @Suppress("unused")
    @Query("SELECT * FROM INVOICE WHERE (fpDate>=:dateStart AND fpDate<=:dateEnd) OR (fpDate>=:dateEnd AND fpDate<=:dateStart)")
    fun getBetween(dateStart: Long, dateEnd: Long): MutableList<RealBean>
}