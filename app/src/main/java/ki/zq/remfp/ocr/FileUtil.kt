package ki.zq.remfp.ocr

import android.content.Context
import java.io.*

/**
 * 文件读取工具类
 */
object FileUtil {
    fun getSaveFile(context: Context): File {
        return File(context.filesDir, "pic.jpg")
    }

    /**
     * 读取文件内容，作为字符串返回
     */
    @Throws(IOException::class)
    fun readFileAsString(filePath: String): String {
        val file = File(filePath)
        if (!file.exists()) {
            throw FileNotFoundException(filePath)
        }
        if (file.length() > 1024 * 1024 * 1024) {
            throw IOException("File is too large")
        }
        val sb = StringBuilder(file.length().toInt())
        // 创建字节输入流
        val fis = FileInputStream(filePath)
        // 创建一个长度为10240的Buffer
        val bay = ByteArray(10240)
        // 用于保存实际读取的字节数
        var hasRead = 0
        while (fis.read(bay).also { hasRead = it } > 0) {
            sb.append(String(bay, 0, hasRead))
        }
        fis.close()
        return sb.toString()
    }

    /**
     * 根据文件路径读取byte[] 数组
     */
    @Throws(IOException::class)
    fun readFileByBytes(filePath: String): ByteArray {
        val file = File(filePath)
        if (!file.exists()) {
            throw FileNotFoundException(filePath)
        } else {
            ByteArrayOutputStream(file.length().toInt()).use { bos ->
                val bufferedInputStream = BufferedInputStream(FileInputStream(file))
                val bufSize: Short = 1024
                val buffer = ByteArray(bufSize.toInt())
                var len1: Int
                while (-1 != bufferedInputStream.read(buffer, 0, bufSize.toInt())
                        .also { len1 = it }
                ) {
                    bos.write(buffer, 0, len1)
                }
                return bos.toByteArray()
            }
        }
    }
}