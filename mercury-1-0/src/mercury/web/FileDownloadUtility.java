package mercury.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * File download utility for Relay-Download 
 * 
 */
public class FileDownloadUtility {

	/** Buffer Size for download stream */
	private static final int $bufferSize = 1024 * 8; 

	private static final String $charset = "UTF-8";

	private FileDownloadUtility() {
		// do nothing;
	}

	public static void download(HttpServletRequest request, HttpServletResponse response, File file, boolean resumable)
			throws ServletException, IOException {

		// To-De : Remove below blocks. Header Debug
		{
			Enumeration<String> names = request.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				String msg = String.format("[Req-Header] %s:%s", name, request.getHeader(name));
				System.out.println(msg);
			}
			names = request.getParameterNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				String msg = String.format("[Req-Params] %s:%s", name, request.getParameter(name));
				System.out.println(msg);
			}
		}

		if (file == null || !file.exists() || file.length() < 0 || file.isDirectory()) {
			throw new IOException("File is not exist or size is 0 or directory");
		}

		InputStream is = null;

		String mimetype = request.getSession().getServletContext().getMimeType(file.getName());
		
		try {
			is = new FileInputStream(file);
			download(request, response, is, file, mimetype, resumable);
		} finally {
			try {
				is.close();
			} catch (Exception ex) {
			}
		}
	}

	private static void download(HttpServletRequest request, HttpServletResponse response, InputStream is,
			File file, String mimetype, boolean resumable)
			throws ServletException, IOException {

		long startPositionBytes = 0;

		String mime = mimetype;

		if (mimetype == null || mimetype.length() == 0) {
			mime = "application/octet-stream;";
		}

		byte[] buffer = new byte[$bufferSize];

		response.setContentType(mime + "; charset=" + $charset);

		String userAgent = request.getHeader("User-Agent");

		// "attachment;" occurs 'download window' in IE 5.5 and below. 
		if (userAgent != null && userAgent.indexOf("MSIE 5.5") > -1) { 
			response.setHeader("Content-Disposition", "filename=" + URLEncoder.encode(file.getName(), "UTF-8") + ";");
		} else if (userAgent != null && userAgent.indexOf("MSIE") > -1) { 
			response.setHeader("Content-Disposition",
					"attachment; filename=" + java.net.URLEncoder.encode(file.getName(), "UTF-8") + ";");
		} else { // Mozilla or Opera
			response.setHeader("Content-Disposition",
					"attachment; filename=" + new String(file.getName().getBytes($charset), "latin1") + ";");
		}

		if (file.length() > 0) {
			response.setHeader("Accept-Ranges", "bytes");
			response.setHeader("Content-Length", "" + file.length());
		}

		// by rfc2616 https://tools.ietf.org/html/rfc2616
		// "Content-Length"
		// Accept-Ranges: bytes
		// Range : byte-range-spec = first-byte-pos "-" [last-byte-pos]
		// HTTP/1.1 206 Partial content
		// Date: Wed, 15 Nov 1995 06:25:24 GMT
		// Last-Modified: Wed, 15 Nov 1995 04:58:08 GMT
		// Content-Range: bytes 21010-47021/47022
		// Content-Length: 26012
		// Content-Type: image/gif

		// FireFox
		// [Req-Header] host:127.0.0.1:8080
		// [Req-Header] user-agent:Mozilla/5.0 (Windows NT 6.1; WOW64; rv:52.0)
		// Gecko/20100101 Firefox/52.0
		// [Req-Header]
		// accept:text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
		// [Req-Header] accept-language:ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3
		// [Req-Header] accept-encoding:gzip, deflate
		// [Req-Header] referer:http://127.0.0.1:8080/mercury/
		// [Req-Header] cookie:JSESSIONID=37F47431917CF4F582612F009ABF380B
		// [Req-Header] connection:keep-alive
		// [Req-Header] range:bytes=132884634-
		// [Req-Header] if-unmodified-since:Fri, 07 Apr 2017 23:35:38 KST
		// [Req-Params] resumable:true
		// [Res-Header] Content-Disposition:attachment; filename=test_Movie.avi;
		// [Res-Header] Accept-Ranges:bytes
		// [Res-Header] Date:Sat, 22 Apr 2017 16:38:34 KST
		// [Res-Header] Last-Modified:Fri, 07 Apr 2017 23:35:38 KST
		
		// Chrome 57 
		// [Req-Header] host:127.0.0.1:8080
		// [Req-Header] connection:keep-alive
		// [Req-Header] range:bytes=129041634-
		// [Req-Header] if-range:Fri, 07 Apr 2017 23:35:38 KST
		// [Req-Header] referer:http://127.0.0.1:8080/mercury/
		// [Req-Header] user-agent:Mozilla/5.0 (Windows NT 6.1; Win64; x64)
		// AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133
		// Safari/537.36
		// [Req-Header] accept-encoding:gzip, deflate, sdch, br
		// [Req-Header] accept-language:ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4
		// [Req-Header] cookie:JSESSIONID=357B5316A2B7733555814A0FF17BE794

		if (resumable) {
			
			//Fire Fox
			String reqFileDate = request.getHeader("if-unmodified-since");
			//Chrome
			if (reqFileDate == null) {
				reqFileDate = request.getHeader("if-range");
			}
			
			DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
			String fileLastModified = df.format(file.lastModified());

			response.setHeader("Date", df.format(System.currentTimeMillis()));
			response.setHeader("Last-Modified", fileLastModified);
			
			String range = request.getHeader("range");
			
			if (reqFileDate != null && reqFileDate.equals(fileLastModified) && range != null
					&& range.indexOf("bytes=") >= 0 && range.indexOf('-') > 0) {
				range = range.substring(range.indexOf("bytes=") + 6, range.lastIndexOf('-'));
				try {
					startPositionBytes = Long.parseLong(range);
				} catch (Exception e) {
					// e.printStackTrace();
					System.err.println(e.getMessage());
				}
			}
			if (startPositionBytes > 0) {
				response.setHeader("Content-Length", Long.toString(file.length() - startPositionBytes));
				response.setHeader("Content-Range",
						String.format("bytes %d-%d/%d", startPositionBytes, file.length() - 1, file.length()));
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
			} else {
				response.setHeader("Content-Length", "" + file.length());
			}
		}

		// To-Do : Remove blocks on real use. 
		// Debug Response Header
		for (String name : response.getHeaderNames()) {
			System.out.println(String.format("[Res-Header] %s:%s", name, response.getHeader(name)));
		}

		BufferedInputStream fileInputStream = null;
		BufferedOutputStream outStream = null;

		try {
			fileInputStream = new BufferedInputStream(is);
			outStream = new BufferedOutputStream(response.getOutputStream());

			fileInputStream.skip(startPositionBytes);

			int read = 0;
			long cnt = 0;
			
			while ((read = fileInputStream.read(buffer)) != -1) {
				outStream.write(buffer, 0, read);
				
				//To-Do : Remove below blocks, just for debugging
				{
					Thread.sleep(50);
					if (cnt % 1000 == 0) {
						System.out.println("");
					}
					if (cnt % 10 == 0) {
						System.out.print('#');
					}
					cnt++;
				}
			}
			System.out.println("");
			
		} catch (IOException ex) {
			// ex.printStackTrace();
			System.err.println(ex.getMessage());
		} catch (InterruptedException e) {
			// e.printStackTrace();
			System.err.println(e.getMessage());
		} finally {
			try {
				outStream.close();
			} catch (Exception ex1) {
			}

			try {
				fileInputStream.close();
			} catch (Exception ex2) {

			}
		}
	}
}