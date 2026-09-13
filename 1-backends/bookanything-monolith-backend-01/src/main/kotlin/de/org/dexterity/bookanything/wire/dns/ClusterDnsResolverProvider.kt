package de.org.dexterity.bookanything.wire.dns

import java.net.InetAddress
import java.net.spi.InetAddressResolver
import java.net.spi.InetAddressResolverProvider
import java.util.Hashtable
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream
import javax.naming.directory.InitialDirContext

/**
 * Custom InetAddressResolverProvider for Java 18+ (JEP 418).
 * Automatically resolves all internal Kubernetes cluster domains (*.cluster.local)
 * against MicroK8s CoreDNS (10.152.183.10) without requiring changes to /etc/hosts or root privileges.
 */
class ClusterDnsResolverProvider : InetAddressResolverProvider() {

    override fun name(): String = "ClusterDnsResolverProvider"

    override fun get(configuration: Configuration): InetAddressResolver {
        val builtin = configuration.builtinResolver()
        return object : InetAddressResolver {
            private val cache = ConcurrentHashMap<String, ByteArray>()
            private val coreDnsUrl = "dns://10.152.183.10"

            override fun lookupByName(
                host: String,
                lookupPolicy: InetAddressResolver.LookupPolicy
            ): Stream<InetAddress> {
                if (host.endsWith(".cluster.local", ignoreCase = true)) {
                    val cached = cache[host]
                    if (cached != null) {
                        return Stream.of(InetAddress.getByAddress(host, cached))
                    }
                    try {
                        val env = Hashtable<String, String>()
                        env["java.naming.factory.initial"] = "com.sun.jndi.dns.DnsContextFactory"
                        env["java.naming.provider.url"] = coreDnsUrl
                        env["com.sun.jndi.dns.timeout.initial"] = "2000"
                        env["com.sun.jndi.dns.timeout.retries"] = "1"
                        val ctx = InitialDirContext(env)
                        val attrs = ctx.getAttributes(host, arrayOf("A"))
                        val ipStr = attrs.get("A")?.get()?.toString()
                        ctx.close()
                        if (ipStr != null) {
                            val addr = InetAddress.getByName(ipStr).address
                            cache[host] = addr
                            return Stream.of(InetAddress.getByAddress(host, addr))
                        }
                    } catch (_: Exception) {
                        // fallback to builtin if resolution fails
                    }
                }
                return builtin.lookupByName(host, lookupPolicy)
            }

            override fun lookupByAddress(addr: ByteArray): String {
                return builtin.lookupByAddress(addr)
            }
        }
    }
}
