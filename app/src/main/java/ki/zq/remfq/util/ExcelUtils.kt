package ki.zq.remfq.util

import jxl.Workbook
import jxl.WorkbookSettings
import jxl.format.Alignment
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.Colour
import jxl.write.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

object ExcelUtils {
    private var arial14font: WritableFont? = null
    private var arial14format: WritableCellFormat? = null
    private var arial10font: WritableFont? = null
    private var arial10format: WritableCellFormat? = null
    private var arial12font: WritableFont? = null
    private var arial12format: WritableCellFormat? = null
    private const val UTF8_ENCODING = "UTF-8"
    //const val GBK_ENCODING = "GBK"

    /**
     * 单元格的格式设置 字体大小 颜色 对齐方式、背景颜色等...
     */
    @Suppress("INACCESSIBLE_TYPE")
    private fun format() {
        try {
            arial14font = WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD)
            arial14font!!.colour = Colour.LIGHT_BLUE
            arial14format = WritableCellFormat(arial14font)
            arial14format!!.alignment = Alignment.CENTRE
            arial14format!!.setBorder(Border.ALL, BorderLineStyle.THIN)
            arial14format!!.setBackground(Colour.VERY_LIGHT_YELLOW)
            arial10font = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD)
            arial10format = WritableCellFormat(arial10font)
            arial10format!!.alignment = Alignment.CENTRE
            arial10format!!.setBorder(Border.ALL, BorderLineStyle.THIN)
            arial10format!!.setBackground(Colour.GRAY_25)
            arial12font = WritableFont(WritableFont.ARIAL, 10)
            arial12format = WritableCellFormat(arial12font)
            arial10format!!.alignment = Alignment.CENTRE //对齐格式
            arial12format!!.setBorder(Border.ALL, BorderLineStyle.THIN) //设置边框
        } catch (e: WriteException) {
            e.printStackTrace()
        }
    }

    /**
     * 初始化Excel
     * @param fileName
     * @param colName
     */
    fun initExcel(fileName: String, colName: Array<String>) {
        format()
        var workbook: WritableWorkbook? = null
        try {
            val file = File(fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            workbook = Workbook.createWorkbook(file)
            val sheet = workbook.createSheet("发票登记电子台账", 0)
            //创建标题栏
            sheet.addCell(Label(0, 0, fileName, arial14format) as WritableCell)
            for (col in colName.indices) {
                sheet.addCell(Label(col, 0, colName[col], arial10format))
            }
            sheet.setRowView(0, 340) //设置行高
            workbook.write()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (workbook != null) {
                try {
                    workbook.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun <T> writeObjListToExcel(objList: List<T>?, fileName: String): Boolean {
        var flag = false
        if (objList != null && objList.isNotEmpty()) {
            var writeBook: WritableWorkbook? = null
            var input: InputStream? = null
            try {
                val setEncode = WorkbookSettings()
                setEncode.encoding = UTF8_ENCODING
                input = FileInputStream(File(fileName))
                val workbook = Workbook.getWorkbook(input)
                writeBook = Workbook.createWorkbook(File(fileName), workbook)
                val sheet = writeBook.getSheet(0)

//              sheet.mergeCells(0,1,0,objList.size()); //合并单元格
//              sheet.mergeCells()
                for (j in objList.indices) {
                    val list = objList[j] as ArrayList<*>
                    for (i in list.indices) {
                        sheet.addCell(Label(i, j + 1, list[i].toString(), arial12format))
                        if (list[i].toString().length <= 5) {
                            sheet.setColumnView(i, list[i].toString().length + 8) //设置列宽
                        } else {
                            sheet.setColumnView(i, list[i].toString().length + 5) //设置列宽
                        }
                    }
                    sheet.setRowView(j + 1, 350) //设置行高
                }
                writeBook.write()
                flag = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (writeBook != null) {
                    try {
                        writeBook.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (input != null) {
                    try {
                        input.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return flag
    }
}