package ki.zq.remfq.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ki.zq.remfq.bean.RealBean

@Database(entities = [RealBean::class], version = 1)
abstract class RealBeanDB : RoomDatabase() {
    companion object {
        private var mInstance: RealBeanDB? = null

        @Synchronized
        fun getDatabase(context: Context): RealBeanDB? {
            if (mInstance == null) {
                mInstance = Room.databaseBuilder(
                    context.applicationContext,
                    RealBeanDB::class.java, "FaPiaoDB.db"
                ).build()
            }
            return mInstance
        }
    }

    abstract fun ocrDao(): RealBeanDao
}