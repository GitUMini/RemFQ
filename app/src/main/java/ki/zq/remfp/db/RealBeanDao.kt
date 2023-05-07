package ki.zq.remfp.db

import androidx.lifecycle.LiveData
import androidx.room.*
import ki.zq.remfp.bean.RealBean

@Dao
interface RealBeanDao {
    @Insert
    fun insertBeans(realBeans: RealBean?):Long

    @Update
    fun updateBeans(realBeans: RealBean?): Int

    @Delete
    fun deleteBeans(realBeans: RealBean?): Int

    @Suppress("unused")
    @Query("DELETE FROM INVOICE")
    fun deleteAllBeans()

    @Query("SELECT * FROM INVOICE WHERE fpNumber = :number")
    fun getBean(number: String): RealBean?

    @Query("SELECT * FROM INVOICE ORDER BY ID DESC")
    fun getAllBeans(): LiveData<MutableList<RealBean>>

    @Suppress("unused")
    @Query("SELECT * FROM INVOICE WHERE (fpDate>=:dateStart AND fpDate<=:dateEnd) OR (fpDate>=:dateEnd AND fpDate<=:dateStart)")
    fun getBetween(dateStart: Long, dateEnd: Long): MutableList<RealBean>
}