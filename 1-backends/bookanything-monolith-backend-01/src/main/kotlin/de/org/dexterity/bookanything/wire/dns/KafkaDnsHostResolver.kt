package de.org.dexterity.bookanything.wire.dns

import org.apache.kafka.clients.HostResolver
import java.net.InetAddress
import java.util.Hashtable
import java.util.concurrent.ConcurrentHashMap
import javax.naming.directory.InitialDirContext

/**
 * Custom Kafka HostResolver.
 * Resolves Kubernetes in-cluster hostnames (*.cluster.local) directly against
 * MicroK8s CoreDNS (10.152.183.10) or static cluster IP mapping.
 */
class KafkaDnsHostResolver : HostResolver {

    private val cache = ConcurrentHashMap<String, Array<InetAddress>>()
    private val coreDnsUrl = "dns://10.152.183.10"

    override fun resolve(host: String): Array<InetAddress> {
        if (host.equals("message-broker-kafka.drr-corpshared-plat.svc.cluster.local", ignoreCase = true)) {
            return arrayOf(InetAddress.getByAddress(host, byteArrayOf(10, 152.toByte(), 183.toByte(), 188.toByte())))
        }
        if (host.endsWith(".cluster.local", ignoreCase = true)) {
            val cached = cache[host]
            if (cached != null) {
                return cached
            }
            try {
                val env = Hashtable<String, String>()
                env["java.naming.factory.initial"] = "com.sun.jndi.dns.DnsContextFactory"
                env["java.naming.provider.url"] = coreDnsUrl
                val ctx = InitialDirContext(env)
                val attrs = ctx.getAttributes(host, arrayOf("A"))
                val ipStr = attrs.get("A")?.get()?.toString()
                ctx.close()
                if (ipStr != null) {
                    val addresses = arrayOf(InetAddress.getByAddress(host, InetAddress.getByName(ipStr).address))
                    cache[host] = addresses
                    return addresses
                }
            } catch (_: Exception) {
                // fallback below
            }
        }
        return InetAddress.getAllByName(host)
    }
}
