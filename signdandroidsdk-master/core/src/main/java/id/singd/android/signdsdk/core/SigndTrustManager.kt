package id.singd.android.signdsdk.core

import android.content.Context
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class SigndTrustManager {
    companion object {

        @Throws(GeneralSecurityException::class)
        fun getInstance(ctx: Context) : X509TrustManager{
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificates = certificateFactory.generateCertificates(ctx.resources.openRawResource(R.raw.cert))
            if (certificates.isEmpty()) {
                throw IllegalArgumentException("expected non-empty set of trusted certificates")
            }

            // Put the certificates a key store.
            val password = "password".toCharArray() // Any password will work.
            val keyStore = newEmptyKeyStore(password)
            var index = 0
            for (certificate in certificates) {
                val certificateAlias = Integer.toString(index++)
                keyStore.setCertificateEntry(certificateAlias, certificate)
            }

            // Use it to build an X509 trust manager.
            val keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, password)
            val trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            val trustManagers = trustManagerFactory.getTrustManagers()
            if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) {
                throw IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers))
            }
            return trustManagers[0] as X509TrustManager
        }

        @Throws(GeneralSecurityException::class)
        private fun newEmptyKeyStore(password: CharArray): KeyStore {
            try {
                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                val stream: InputStream? = null // By convention, 'null' creates an empty key store.
                keyStore.load(stream, password)
                return keyStore
            } catch (e: IOException) {
                throw AssertionError(e)
            }

        }
    }
}