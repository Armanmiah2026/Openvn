package com.alinaj.dev.v2ray.util

import com.alinaj.dev.v2ray.AppConfig
import com.alinaj.dev.v2ray.util.Utils.parseInt
import kotlin.Int

object SettingsManager {

    fun getSocksPort(): Int {
        return parseInt(MmkvManager.decodeSettingsString(AppConfig.PREF_SOCKS_PORT), AppConfig.PORT_SOCKS.toInt())
    }

    fun getHttpPort(): Int {
        return getSocksPort() + (if (Utils.isXray()) 0 else 1)
    }
}
