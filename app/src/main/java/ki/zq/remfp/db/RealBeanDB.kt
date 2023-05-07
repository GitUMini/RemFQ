package ki.zq.remfp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ki.zq.remfp.bean.RealBean

@Database(entities = [RealBean::class], version = 2)
abstract class RealBeanDB : RoomDatabase() {
    companion object {
        @Volatile
        private var mInstance: RealBeanDB? = null

        fun getDatabase(context: Context): RealBeanDB {
            return mInstance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RealBeanDB::class.java,
                    "FaPiaoDB"
                ).build()
                mInstance = instance
                instance
            }
        }
    }

    abstract fun ocrDao(): RealBeanDao
}