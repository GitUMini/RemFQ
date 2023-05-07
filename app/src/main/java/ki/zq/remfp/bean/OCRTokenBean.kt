package ki.zq.remfp.bean

import com.google.gson.annotations.SerializedName

data class OCRTokenBean(
    @SerializedName("refresh_token") val refresh_token: String,
    @SerializedName("expires_in") val expires_in: Int,
    @SerializedName("session_key") val session_session_key: String,
    @SerializedName("access_token") val access_token: String,
    @SerializedName("scope") val scope: String,
    @SerializedName("session_secret") val session_secret: String
)