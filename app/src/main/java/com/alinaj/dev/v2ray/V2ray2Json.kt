package com.alinaj.dev.v2ray

import android.content.Context
import com.alinaj.dev.v2ray.util.AngConfigManager
import com.alinaj.dev.v2ray.util.MmkvManager

object V2ray2Json {

    @JvmStatic
    fun convert(context: Context, v2ray: String) : String {

        if (v2ray.isEmpty()) {
            return ""
        }
        if (!(v2ray.startsWith("vmess://") || v2ray.startsWith("vless://") || v2ray.startsWith("trojan://") ||
            v2ray.startsWith("ss://") || v2ray.startsWith("socks://") || v2ray.startsWith("wireguard://") || v2ray.startsWith("hysteria2://"))
        ) {
            return v2ray
        }

        try {
            MmkvManager.removeServer(MmkvManager.getSelectServer().toString())
            AngConfigManager.importBatchConfig(v2ray, "", false)
            return AngConfigManager.shareFullContent2Clipboard(context, MmkvManager.getSelectServer().toString())
        }catch (e: Exception){}
        return ""
    }
}
