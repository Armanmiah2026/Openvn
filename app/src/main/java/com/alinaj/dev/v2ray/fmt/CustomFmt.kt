package com.alinaj.dev.v2ray.fmt

import com.alinaj.dev.v2ray.dto.EConfigType
import com.alinaj.dev.v2ray.dto.ProfileItem
import com.alinaj.dev.v2ray.dto.V2rayConfig
import com.alinaj.dev.v2ray.util.JsonUtil

object CustomFmt : FmtBase() {
    fun parse(str: String): ProfileItem? {
        val config = ProfileItem.create(EConfigType.CUSTOM)

        val fullConfig = JsonUtil.fromJson(str, V2rayConfig::class.java)
        val outbound = fullConfig.getProxyOutbound()

        config.remarks = fullConfig?.remarks ?: System.currentTimeMillis().toString()
        config.server = outbound?.getServerAddress()
        config.serverPort = outbound?.getServerPort().toString()

        return config
    }
}