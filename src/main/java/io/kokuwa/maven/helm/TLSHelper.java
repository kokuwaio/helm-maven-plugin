package io.kokuwa.maven.helm;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Helper for insecure TLS.
 */
public class TLSHelper {

	public static void insecure(HttpsURLConnection connection) throws MojoExecutionException {

		TrustManager trustManager = new X509TrustManager() {

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {}
		};

		try {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, new TrustManager[] { trustManager }, null);
			connection.setSSLSocketFactory(context.getSocketFactory());
			connection.setHostnameVerifier((hostname, session) -> true);
		} catch (GeneralSecurityException e) {
			throw new MojoExecutionException("Failed to setup insecure tls context.", e);
		}
	}
}
