import com.squareup.okhttp.OkHttpClient;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.UUID;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class ManualSpdyTest {
    private final OkHttpClient client = new OkHttpClient();

    public ManualSpdyTest() {
        client.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
    }

    public static void main(String[] args) throws IOException {
        ManualSpdyTest test = new ManualSpdyTest();
        saveFile(test.fetch(new URL("https://lh6.googleusercontent.com/-wQXUUyuGHA0/UPLO9bacd_I/AAAAAAAAHAw/0ETOrI71MkI/s1744/IMG_20130113_101138.jpg")));
        printUtf8(test.fetch(new URL("https://www.google.com/")));
        printUtf8(test.fetch(new URL("https://www.google.com/")));
        printUtf8(test.fetch(new URL("https://www.google.com/")));
        printUtf8(test.fetch(new URL("https://www.google.com/")));
        printUtf8(test.fetch(new URL("https://www.google.com/")));
    }

    private static void saveFile(byte[] data) throws IOException {
        File dir = new File("/Users/jwilson/Desktop/ManualSpdyTest");
        dir.mkdirs();
        FileOutputStream out = new FileOutputStream(new File(dir, UUID.randomUUID().toString()));
        out.write(data);
        out.close();
    }

    private static void printUtf8(byte[] bytes) throws UnsupportedEncodingException {
        System.out.println(new String(bytes, "UTF-8"));
    }

    private byte[] fetch(URL url) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) client.open(url);
        connection.setReadTimeout(500);
        int responseCode = connection.getResponseCode();
        InputStream in = connection.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(responseCode);
        System.out.println(connection.getURL());
        return out.toByteArray();
    }
}
