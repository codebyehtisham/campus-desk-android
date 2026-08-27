package com.derived.campusdesk.networking.client

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * System DNS first, then Cloudflare DoH, then last-known Railway A records.
 * Emulators and some carrier/Private DNS setups fail `*.up.railway.app` even when
 * the same host resolves in a desktop browser.
 */
object ResilientDns : Dns {
    private val system: Dns = Dns.SYSTEM

    private val bootstrapDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> = when (hostname.lowercase()) {
            "cloudflare-dns.com", "1.1.1.1" -> listOf(InetAddress.getByName("1.1.1.1"))
            "1.0.0.1" -> listOf(InetAddress.getByName("1.0.0.1"))
            "dns.google", "8.8.8.8" -> listOf(InetAddress.getByName("8.8.8.8"))
            else -> system.lookup(hostname)
        }
    }

    /** Bootstrap DoH without depending on system DNS for the resolver itself. */
    private val doh: Dns by lazy {
        val bootstrap = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .dns(bootstrapDns)
            .build()
        DnsOverHttps.Builder()
            .client(bootstrap)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
            )
            .build()
    }

    /** Last-resort IPs (refreshed when known); used only if system + DoH both fail. */
    private val staticHosts: Map<String, List<String>> = mapOf(
        "adequate-success-production-39da.up.railway.app" to listOf("69.46.46.23"),
        "campusdesk-production-9ab3.up.railway.app" to listOf("69.46.46.24"),
    )

    override fun lookup(hostname: String): List<InetAddress> {
        val host = hostname.trim().lowercase()
        try {
            return system.lookup(host)
        } catch (systemFailure: UnknownHostException) {
            try {
                return doh.lookup(host)
            } catch (_: Exception) {
                val fallback = staticHosts[host]
                    ?.mapNotNull { runCatching { InetAddress.getByName(it) }.getOrNull() }
                    .orEmpty()
                if (fallback.isNotEmpty()) return fallback
                throw systemFailure
            }
        }
    }
}
