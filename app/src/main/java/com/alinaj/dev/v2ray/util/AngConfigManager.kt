package com.alinaj.dev.v2ray.util

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.Log
import com.alinaj.dev.v2ray.AppConfig
import com.alinaj.dev.v2ray.AppConfig.HY2
import com.alinaj.dev.v2ray.dto.*
import com.alinaj.dev.v2ray.fmt.CustomFmt
import com.alinaj.dev.v2ray.fmt.Hysteria2Fmt
import com.alinaj.dev.v2ray.fmt.ShadowsocksFmt
import com.alinaj.dev.v2ray.fmt.SocksFmt
import com.alinaj.dev.v2ray.fmt.TrojanFmt
import com.alinaj.dev.v2ray.fmt.VlessFmt
import com.alinaj.dev.v2ray.fmt.VmessFmt
import com.alinaj.dev.v2ray.fmt.WireguardFmt
import com.alinaj.dev.v2ray.handler.V2rayConfigManager
import com.alinaj.dev.R
import java.net.URI

object AngConfigManager {
    /**
     * parse config form qrcode or...
     */
    private fun parseConfig(
        str: String?,
        subid: String,
        subItem: SubscriptionItem?,
        removedSelectedServer: ProfileItem?
    ): Int {
        try {
            if (str == null || TextUtils.isEmpty(str)) {
                return R.string.toast_none_data
            }

            val config = if (str.startsWith(EConfigType.VMESS.protocolScheme)) {
                VmessFmt.parse(str)
            } else if (str.startsWith(EConfigType.SHADOWSOCKS.protocolScheme)) {
                ShadowsocksFmt.parse(str)
            } else if (str.startsWith(EConfigType.SOCKS.protocolScheme)) {
                SocksFmt.parse(str)
            } else if (str.startsWith(EConfigType.TROJAN.protocolScheme)) {
                TrojanFmt.parse(str)
            } else if (str.startsWith(EConfigType.VLESS.protocolScheme)) {
                VlessFmt.parse(str)
            } else if (str.startsWith(EConfigType.WIREGUARD.protocolScheme)) {
                WireguardFmt.parse(str)
            } else if (str.startsWith(EConfigType.HYSTERIA2.protocolScheme) || str.startsWith(HY2)) {
                Hysteria2Fmt.parse(str)
            } else {
                null
            }

            if (config == null) {
                return R.string.toast_incorrect_protocol
            }
            //filter
            /*if (subItem?.filter != null && subItem.filter?.isNotEmpty() == true && config.remarks.isNotEmpty()) {
                val matched = Regex(pattern = subItem.filter ?: "")
                    .containsMatchIn(input = config.remarks)
                if (!matched) return -1
            }*/

            config.subscriptionId = subid
            val guid = MmkvManager.encodeServerConfig("", config)
            if (removedSelectedServer != null &&
                config.server == removedSelectedServer.server && config.serverPort == removedSelectedServer.serverPort
            ) {
                MmkvManager.setSelectServer(guid)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    fun importBatchConfig(server: String?, subid: String, append: Boolean): Int {
        var count = parseBatchConfig(Utils.decode(server!!), subid, append)
        if (count <= 0) {
            count = parseBatchConfig(server, subid, append)
        }
        if (count <= 0) {
            count = parseCustomConfigServer(server, subid)
        }
        return count
    }

    /**
     * share config
     */
    private fun shareConfig(guid: String): String {
        try {
            val config = MmkvManager.decodeServerConfig(guid) ?: return ""

            return config.configType.protocolScheme + when (config.configType) {
                EConfigType.VMESS -> VmessFmt.toUri(config)
                EConfigType.CUSTOM -> ""
                EConfigType.SHADOWSOCKS -> ShadowsocksFmt.toUri(config)
                EConfigType.SOCKS -> SocksFmt.toUri(config)
                EConfigType.HTTP -> ""
                EConfigType.VLESS -> VlessFmt.toUri(config)
                EConfigType.TROJAN -> TrojanFmt.toUri(config)
                EConfigType.WIREGUARD -> WireguardFmt.toUri(config)
                EConfigType.HYSTERIA2 -> Hysteria2Fmt.toUri(config)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * share2Clipboard
     */
    fun share2Clipboard(context: Context, guid: String): Int {
        try {
            val conf = shareConfig(guid)
            if (TextUtils.isEmpty(conf)) {
                return -1
            }

            Utils.setClipboard(context, conf)

        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    /**
     * share2Clipboard
     */
    fun shareNonCustomConfigsToClipboard(context: Context, serverList: List<String>): Int {
        try {
            val sb = StringBuilder()
            for (guid in serverList) {
                val url = shareConfig(guid)
                if (TextUtils.isEmpty(url)) {
                    continue
                }
                sb.append(url)
                sb.appendLine()
            }
            if (sb.count() > 0) {
                Utils.setClipboard(context, sb.toString())
            }
            return sb.lines().count()
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    /**
     * shareFullContent2Clipboard
     */
    fun shareFullContent2Clipboard(context: Context, guid: String?): String {
        try {
            if (guid == null) return ""
            val result = V2rayConfigManager.getV2rayConfig(context, guid)
            if (result.status) {
                val config = MmkvManager.decodeServerConfig(guid)
                if (config?.configType == EConfigType.HYSTERIA2) {
                    val socksPort = Utils.findFreePort(listOf(100 + SettingsManager.getSocksPort(), 0))
                    Hysteria2Fmt.toNativeConfig(config, socksPort)
                    //return JsonUtil.toJsonPretty(hy2Config) + "\n" + result.content
                    return result.content
                }
                return result.content
            } else {
                return ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
        return ""
    }


    fun parseBatchConfig(servers: String?, subid: String, append: Boolean): Int {
        try {
            if (servers == null) {
                return 0
            }
            val removedSelectedServer =
                if (!TextUtils.isEmpty(subid) && !append) {
                    MmkvManager.decodeServerConfig(
                        MmkvManager.getSelectServer().orEmpty()
                    )?.let {
                        if (it.subscriptionId == subid) {
                            return@let it
                        }
                        return@let null
                    }
                } else {
                    null
                }
            if (!append) {
                MmkvManager.removeServerViaSubid(subid)
            }

            val subItem = MmkvManager.decodeSubscription(subid)
            var count = 0
            servers.lines()
                .distinct()
                //.reversed()
                .forEach {
                    val resId = parseConfig(it, subid, subItem, removedSelectedServer)
                    if (resId == 0) {
                        count++
                    }
                }
            return count
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun parseCustomConfigServer(server: String?, subid: String): Int {
        if (server == null) {
            return 0
        }
        if (server.contains("inbounds")
            && server.contains("outbounds")
            && server.contains("routing")
        ) {
            try {
                val serverList: Array<Any> =
                    JsonUtil.fromJson(server, Array<Any>::class.java)

                if (serverList.isNotEmpty()) {
                    var count = 0
                    for (srv in serverList.reversed()) {
                        val config = CustomFmt.parse(JsonUtil.toJson(srv)) ?: continue
                        config.subscriptionId = subid
                        val key = MmkvManager.encodeServerConfig("", config)
                        MmkvManager.encodeServerRaw(key, JsonUtil.toJsonPretty(srv) ?: "")
                        count += 1
                    }
                    return count
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                // For compatibility
                val config = CustomFmt.parse(server) ?: return 0
                config.subscriptionId = subid
                val key = MmkvManager.encodeServerConfig("", config)
                MmkvManager.encodeServerRaw(key, server)
                return 1
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return 0
        } else if (server.startsWith("[Interface]") && server.contains("[Peer]")) {
            try {
                val config = WireguardFmt.parseWireguardConfFile(server) ?: return R.string.toast_incorrect_protocol
                val key = MmkvManager.encodeServerConfig("", config)
                MmkvManager.encodeServerRaw(key, server)
                return 1
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return 0
        } else {
            return 0
        }
    }
}
