import com.squareup.okhttp.OkHttpClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ManualHttpTest {
    public static final HostnameVerifier HOSTNAME_VERIFIER = new HostnameVerifier() {
        @Override public boolean verify(String s, SSLSession sslSession) {
            System.out.println("VERIFY " + s + " " + sslSession.getPeerHost());
            return true;
        }
    };
    public static final SSLSocketFactory SSL_SOCKET_FACTORY = badSslSocketFactory();

    public static void main(String[] args) throws IOException, InterruptedException {
      test(new URL("https://api.squareup.com/_status"), false);
      //test(new URL("https://api.broadway.squareup.com/_status"), false);
//        test(new URL("http://austin.frap.net/test_path"), true);
//        test(new URL("http://austin.frap.net/test_path"), false);
//        test(new URL("http://austin.frap.net/test_path"), true);
    }

    private static void test(URL url, boolean post) throws IOException, InterruptedException {
        System.out.println(url + " " + (post ? "POST" : "GET"));
        try {
            OkHttpClient client = new OkHttpClient();
            HttpURLConnection connection = client.open(url);

            if (url.getProtocol().equals("https")) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                httpsConnection.setHostnameVerifier(HOSTNAME_VERIFIER);
                httpsConnection.setSSLSocketFactory(SSL_SOCKET_FACTORY);
            }

            connection.setConnectTimeout(5000);

            if (post) {
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                OutputStream out = connection.getOutputStream();
                out.write("hello".getBytes("UTF-8"));
                out.close();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("RESPONSE CODE: " + responseCode);

            InputStream in = responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  " + line);
            }
            reader.close();
            connection.disconnect();
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            System.out.println(" ");
        }
    }

    private static SSLSocketFactory badSslSocketFactory() {
        System.out.println("SSL CHECKS ARE DISABLED!!!");
        try {
            // Construct SSLSocketFactory that accepts any cert.
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManager permissive = new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] chain,
                        String authType) throws CertificateException {}
                @Override public void checkServerTrusted(X509Certificate[] chain,
                        String authType) throws CertificateException {}
                @Override public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            context.init(null, new TrustManager[] { permissive }, null);
            return context.getSocketFactory();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
