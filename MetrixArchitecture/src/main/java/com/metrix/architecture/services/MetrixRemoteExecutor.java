package com.metrix.architecture.services;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.Log;

import com.metrix.architecture.BuildConfig;
import com.metrix.architecture.services.RemoteMessagesHandler.HandlerException;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * HttpClient wrapper class to execute web service methods GET/POST/PUT
 * 
 * @author elin
 * 
 */
public class MetrixRemoteExecutor {
	private static final String POST_BODY_INPUTS_END = "~~PBIEND~~";
	private static final String ATTACHMENT_END_SIGN = "ATTACHMENT_END_SIGN";

	private final int timeout;
	private final String userAgent;

	public MetrixRemoteExecutor(Context context) {
		this(context, 0);
	}

	public MetrixRemoteExecutor(Context context, int timeout) {
		this.timeout = timeout;
		userAgent = buildUserAgent(context);
	}

	/**
	 * Build and return a user-agent string that can identify this application
	 * to remote servers. Contains the package name and version code.
	 */
	private static String buildUserAgent(Context context) {
		try {
			final PackageManager manager = context.getPackageManager();
			final PackageInfo info = manager.getPackageInfo(
					context.getPackageName(), 0);

			// Some APIs require "(gzip)" in the user-agent string.
			return info.packageName + "/" + info.versionName + " ("
					+ info.versionCode + ") (gzip)";
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	/**
	 * Execute a GET request, passing a valid response through
	 */
	public String executeGet(String url) throws JSONException, HandlerException {
		try {
			HttpURLConnection connection = openConnection(url);
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				return getResponseFromConnection(connection);
			} else {
				throw new HandlerException(AndroidResourceHelper.getMessage("UnexpServerResponseFor2Args",
						connection.getResponseMessage(), connection.getRequestMethod() + " " + connection.getURL()));
			}
		} catch (SocketTimeoutException timeoutException) {
			if (BuildConfig.DEBUG) {
				Log.d(MetrixRemoteExecutor.class.getSimpleName(), "*** TIMEOUT ***", timeoutException);
			}
			throw new HandlerException(AndroidResourceHelper.getMessage("ProbReadingRemoteResFor1Args", "GET " + url), timeoutException);
		} catch (IOException e) {
			throw new HandlerException(AndroidResourceHelper.getMessage("ProbReadingRemoteResFor1Args", "GET " + url), e);
		}
	}

	private HttpURLConnection openConnection(String url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setInstanceFollowRedirects(true);
		connection.setAllowUserInteraction(true);
		connection.setRequestProperty("User-Agent", userAgent);
		connection.setRequestProperty("Accept-Encoding", "gzip");
		connection.setRequestProperty("Accept", "*/*");
		connection.setRequestProperty("Connection", "keep-alive");
		if (timeout > 0) {
			connection.setConnectTimeout(timeout * 1000);
		}
		return connection;
	}

	private String getResponseFromConnection(HttpURLConnection connection) throws IOException {
		Charset contentCharset = Charset.forName(StandardCharsets.UTF_8.name());
		String contentType = connection.getContentType();
		if (!TextUtils.isEmpty(contentType)) {
			Pattern c = Pattern.compile(".*charset=([a-zA-Z0-9-_]+)");
			Matcher m = c.matcher(contentType);
			if (m.find()) {
				try {
					contentCharset = Charset.forName(m.group(1));
				} catch (IllegalCharsetNameException ignore) {
					LogManager.getInstance().error(ignore);
				}
			}
		}

		// Get either content stream or error stream depending on status. It is up to calling function to care about status, not here.
		InputStream is = (connection.getResponseCode() < HttpURLConnection.HTTP_MULT_CHOICE ? connection.getInputStream() : connection.getErrorStream());
		// If we received compressed data then wrap the stream to uncompress
		if ("gzip".equals(connection.getContentEncoding())) {
			is = new GZIPInputStream(is);
		}

		String line;
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, contentCharset));
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		is.close();

		return sb.toString();
	}

	public String executePost(String url, String contentType, String postMessage) throws HandlerException {
		try {
			HttpURLConnection connection = openConnection(url);
			connection.setDoOutput(true);
			connection.setRequestProperty("User-Agent", "Metrix Mobile");
			if (TextUtils.isEmpty(contentType)) {
				connection.setRequestProperty(
						"Accept",
						"text/html,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
				connection.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
			} else {
				connection.setRequestProperty("Content-Type", contentType);
				connection.setRequestProperty("Accept", contentType);
			}
			try (OutputStream os = connection.getOutputStream()) {
				byte[] msg = postMessage.getBytes(StandardCharsets.UTF_8);
				os.write(msg, 0, msg.length);
			}
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				return getResponseFromConnection(connection);
			} else {
				throw new HandlerException(AndroidResourceHelper.getMessage("UnexpServerResponseFor2Args",
						connection.getResponseMessage(), connection.getRequestMethod() + " " + connection.getURL()));
			}
		} catch (SocketTimeoutException timeoutException) {
			if (BuildConfig.DEBUG) {
				Log.d(MetrixRemoteExecutor.class.getSimpleName(), "*** TIMEOUT ***", timeoutException);
			}
			throw new HandlerException(AndroidResourceHelper.getMessage("ProbReadingRemoteResFor1Args", "GET " + url), timeoutException);
		} catch (IOException e) {
			throw new HandlerException(AndroidResourceHelper.getMessage("ProbReadingRemoteResFor1Args", "POST " + url), e);
		}
	}
	
	/**
	 * This method can be used to perform an HTTP Post with binary data based on the file
	 * identified by the file Uri to a REST service method identified by the serverUrl.
	 * @param serverUrl The url of the REST method to post to.
	 * @param message The message associated to the binary data.
	 * @param fileUri The address of the file containing the data to be sent in a binary fashion.
	 * @return The server response code.
	 * @since 5.6
	 */
	public int executePostBinary(String serverUrl, String message, String fileUri, String postBodyInputs){
		String serverResponseMessage = "";
		int serverResponseCode = 0;
		String upLoadServerUri = serverUrl;
		String fileName = fileUri;
	
		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;
	
		File sourceFile = new File(fileUri);
		if (!sourceFile.isFile()) {
			LogManager.getInstance().error("Metrix Mobile", "Source File Does not exist");
			return -2; // return code for file missing
		}

		try {
			// open a URL connection to the Servlet
			FileInputStream fileInputStream = new FileInputStream(sourceFile);
			URL url = new URL(upLoadServerUri);
			conn = (HttpURLConnection) url.openConnection(); // Open a HTTP connection to the URL
			conn.setChunkedStreamingMode(1024);
			conn.setRequestMethod("POST");

			conn.setDoInput(true); // Allow Inputs
			conn.setDoOutput(true); // Allow Outputs
			conn.setUseCaches(false); // Don't use a Cached Copy
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("ENCTYPE", "multipart/form-data");

			dos = new DataOutputStream(conn.getOutputStream());

			// text
			dos.writeBytes(twoHyphens + boundary + lineEnd);
			String base64Message = MetrixStringHelper.encodeBase64String(message);
			dos.writeBytes("Content-Type: text/plain;charset=UTF-8;boundary="+boundary+ ";");
			dos.writeBytes("postBody=\"" + postBodyInputs + "\"" + POST_BODY_INPUTS_END + ";");
			dos.writeBytes("message=\"" + base64Message + "\"" + ATTACHMENT_END_SIGN + lineEnd);
			dos.writeBytes(lineEnd);	
			
			dos.writeBytes(twoHyphens + boundary + lineEnd);
			//dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""+ fileName + "\"" + lineEnd);
			dos.writeBytes("Content-Type: multipart/form-data;boundary=" + boundary+ ";name=\"uploaded_file\";filename=\""+ MetrixStringHelper.encodeBase64String(fileName) + "\"" + lineEnd);
			dos.writeBytes(lineEnd);
	
			bytesAvailable = fileInputStream.available(); // create a buffer of  maximum size
			 
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];
	
			// read file and write it into form...
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
	
			while (bytesRead > 0) {
				dos.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}	
			// send multipart form data necesssary after file data...
			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
	
			// Responses from the server (code and message)
			serverResponseCode = conn.getResponseCode();
			serverResponseMessage = conn.getResponseMessage();

			StringBuilder response = new StringBuilder();
			try(BufferedReader br = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), "utf-8"))) {

				String responseLine = null;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
			}

			LogManager.getInstance().info("Metrix Mobile: Upload file to server HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);
			// close streams
			LogManager.getInstance().info("Metrix Mobile: Upload file to server "+ fileName + " File is written");
			// If mobile service returns false, we will treat it as authentication error 401
			if(response.toString().contains(("false")))
				serverResponseCode = 401;

			fileInputStream.close();
			dos.flush();
			dos.close();
		  } catch (InterruptedIOException ex){
			  // This code is to be used for resend
			  serverResponseCode = -1;
		  } catch (Exception ex) {
			  LogManager.getInstance().error(ex);
		  }

		return serverResponseCode;  // like 200 (Ok)
	}
}
