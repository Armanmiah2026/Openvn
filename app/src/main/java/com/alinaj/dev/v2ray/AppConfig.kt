package com.alinaj.dev.v2ray

object AppConfig {

    const val ANG_PACKAGE = "ang.v2ray"
    const val DIR_ASSETS = "assets"

    /** Preferences mapped to MMKV storage. */
    const val PREF_SNIFFING_ENABLED = "pref_sniffing_enabled"
    const val PREF_ROUTE_ONLY_ENABLED = "pref_route_only_enabled"
    const val PREF_PER_APP_PROXY = "pref_per_app_proxy"
    const val PREF_PER_APP_PROXY_SET = "pref_per_app_proxy_set"
    const val PREF_BYPASS_APPS = "pref_bypass_apps"
    const val PREF_LOCAL_DNS_ENABLED = "pref_local_dns_enabled"
    const val PREF_FAKE_DNS_ENABLED = "pref_fake_dns_enabled"
    const val PREF_APPEND_HTTP_PROXY = "pref_append_http_proxy"
    const val PREF_LOCAL_DNS_PORT = "pref_local_dns_port"
    const val PREF_VPN_DNS = "pref_vpn_dns"
    const val PREF_VPN_BYPASS_LAN = "pref_vpn_bypass_lan"
    const val PREF_ROUTING_DOMAIN_STRATEGY = "pref_routing_domain_strategy"
    const val PREF_ROUTING_RULESET = "pref_routing_ruleset"
    const val PREF_MUX_ENABLED = "pref_mux_enabled"
    const val PREF_MUX_CONCURRENCY = "pref_mux_concurrency"
    const val PREF_MUX_XUDP_CONCURRENCY = "pref_mux_xudp_concurrency"
    const val PREF_MUX_XUDP_QUIC = "pref_mux_xudp_quic"
    const val PREF_FRAGMENT_ENABLED = "pref_fragment_enabled"
    const val PREF_FRAGMENT_PACKETS = "pref_fragment_packets"
    const val PREF_FRAGMENT_LENGTH = "pref_fragment_length"
    const val PREF_FRAGMENT_INTERVAL = "pref_fragment_interval"
    const val SUBSCRIPTION_AUTO_UPDATE = "pref_auto_update_subscription"
    const val SUBSCRIPTION_AUTO_UPDATE_INTERVAL = "pref_auto_update_interval"
    const val SUBSCRIPTION_DEFAULT_UPDATE_INTERVAL = "1440" // Default is 24 hours
    const val SUBSCRIPTION_UPDATE_TASK_NAME = "subscription_updater"
    const val PREF_SPEED_ENABLED = "pref_speed_enabled"
    const val PREF_CONFIRM_REMOVE = "pref_confirm_remove"
    const val PREF_START_SCAN_IMMEDIATE = "pref_start_scan_immediate"
    const val PREF_LANGUAGE = "pref_language"
    const val PREF_UI_MODE_NIGHT = "pref_ui_mode_night"
    const val PREF_PREFER_IPV6 = "pref_prefer_ipv6"
    const val PREF_PROXY_SHARING = "pref_proxy_sharing_enabled"
    const val PREF_ALLOW_INSECURE = "pref_allow_insecure"
    const val PREF_SOCKS_PORT = "pref_socks_port"
    const val PREF_REMOTE_DNS = "pref_remote_dns"
    const val PREF_DOMESTIC_DNS = "pref_domestic_dns"
    const val PREF_DNS_HOSTS = "pref_dns_hosts"
    const val PREF_DELAY_TEST_URL = "pref_delay_test_url"
    const val PREF_LOGLEVEL = "pref_core_loglevel"
    const val PREF_MODE = "pref_mode"
    const val PREF_IS_BOOTED = "pref_is_booted"

    const val HTTP_PROTOCOL: String = "http://"
    const val HTTPS_PROTOCOL: String = "https://"

    const val TAG_PROXY = "proxy"
    const val TAG_DIRECT = "direct"
    const val TAG_BLOCKED = "block"
    const val TAG_FRAGMENT = "fragment"

//    const val DNS_AGENT = "1.1.1.1"
//    const val DNS_DIRECT = "223.5.5.5"

    const val PROTOCOL_FREEDOM: String = "freedom"

    const val PORT_LOCAL_DNS = "10853"
    const val PORT_SOCKS = "10808"
    const val PORT_HTTP = "10809"
    const val WIREGUARD_LOCAL_ADDRESS_V4 = "172.16.0.2/32"
    const val WIREGUARD_LOCAL_ADDRESS_V6 = "2606:4700:110:8f81:d551:a0:532e:a2b3/128"
    const val WIREGUARD_LOCAL_MTU = "1420"

    const val VMESS = "vmess://"
    const val CUSTOM = ""
    const val SHADOWSOCKS = "ss://"
    const val SOCKS = "socks://"
    const val HTTP = "http://"
    const val VLESS = "vless://"
    const val TROJAN = "trojan://"
    const val WIREGUARD = "wireguard://"
    const val TUIC = "tuic://"
    const val HYSTERIA2 = "hysteria2://"
    const val HY2 = "hy2://"

    const val LOOPBACK = "127.0.0.1"
    /** Give a good name to this, IDK*/
    const val VPN = "VPN"


    /** DNS server addresses. */
    const val DNS_PROXY = "1.1.1.1"
    const val DNS_DIRECT = "223.5.5.5"
    const val DNS_VPN = "1.1.1.1"
    const val GEOSITE_PRIVATE = "geosite:private"
    const val GEOSITE_CN = "geosite:cn"
    const val GEOIP_PRIVATE = "geoip:private"
    const val GEOIP_CN = "geoip:cn"

    // Google API rule constants
    const val GOOGLEAPIS_CN_DOMAIN = "domain:googleapis.cn"
    const val GOOGLEAPIS_COM_DOMAIN = "googleapis.com"

    // Android Private DNS constants
    const val DNS_DNSPOD_DOMAIN = "dot.pub"
    const val DNS_ALIDNS_DOMAIN = "dns.alidns.com"
    const val DNS_CLOUDFLARE_DOMAIN = "one.one.one.one"
    const val DNS_GOOGLE_DOMAIN = "dns.google"
    const val DNS_QUAD9_DOMAIN = "dns.quad9.net"
    const val DNS_YANDEX_DOMAIN = "common.dot.dns.yandex.net"


    val DNS_ALIDNS_ADDRESSES = arrayListOf("223.5.5.5", "223.6.6.6", "2400:3200::1", "2400:3200:baba::1")
    val DNS_CLOUDFLARE_ADDRESSES = arrayListOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001")
    val DNS_DNSPOD_ADDRESSES = arrayListOf("1.12.12.12", "120.53.53.53")
    val DNS_GOOGLE_ADDRESSES = arrayListOf("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844")
    val DNS_QUAD9_ADDRESSES = arrayListOf("9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9")
    val DNS_YANDEX_ADDRESSES = arrayListOf("77.88.8.8", "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff")

    const val DEFAULT_PORT = 443
    const val DEFAULT_SECURITY = "auto"
    const val DEFAULT_LEVEL = 8
    const val DEFAULT_NETWORK = "tcp"
    const val TLS = "tls"
    const val REALITY = "reality"
    const val HEADER_TYPE_HTTP = "http"
}
