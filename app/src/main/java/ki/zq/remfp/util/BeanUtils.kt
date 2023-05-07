package ki.zq.remfp.util

import ki.zq.remfp.bean.OCRFPBean
import ki.zq.remfp.bean.RealBean
import ki.zq.remfp.ocr.GsonUtils

object BeanUtils {
    fun switchOCRBean2RealBean(inputString: String, toPerson: String?): RealBean {
        val ocrFPBean = GsonUtils.fromJson(inputString, OCRFPBean::class.java)
        return RealBean().apply {
            fpToPerson = toPerson
            fpToCompany = ocrFPBean.words_result.purchaserName
            fpCode =
                ocrFPBean.words_result.invoiceCode.toString()
            fpNumber =
                ocrFPBean.words_result.invoiceNum.toString()
            fpDate = ocrFPBean.words_result.invoiceDate
            fpThing =
                ocrFPBean.words_result.commodityName[0].word.toString()
            fpFromCompanyCode =
                ocrFPBean.words_result.sellerRegisterNum.toString()
            fpFromCompanyName =
                ocrFPBean.words_result.sellerName.toString()
            fpMoney =
                ocrFPBean.words_result.commodityAmount[0].word.toString()
            fpTax =
                ocrFPBean.words_result.commodityTax[0].word.toString()
            fpAll =
                ocrFPBean.words_result.amountInFiguers.toString()
        }
    }

    fun listResult2RealBean(textFields: List<String>): RealBean {
        return RealBean().apply {
            fpToPerson = textFields[0]
            fpToCompany = textFields[1]
            fpCode = textFields[2]
            fpNumber = textFields[3]
            fpDate = textFields[4]
            fpThing = textFields[5]
            fpFromCompanyCode = textFields[6]
            fpFromCompanyName = textFields[7]
            fpMoney = textFields[8]
            fpTax = textFields[9]
            fpAll = textFields[10]
        }
    }
}