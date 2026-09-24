package com.alinaj.dev.v2ray.extension

import java.net.URI
/**
 * Some extensions
 */

val URI.idnHost: String
    get() = (host!!).replace("[", "").replace("]", "")

fun String.removeWhiteSpace(): String = replace("\\s+".toRegex(), "")

fun CharSequence?.isNotNullEmpty(): Boolean = (this != null && this.isNotEmpty())