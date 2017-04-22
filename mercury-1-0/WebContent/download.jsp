<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="EUC-KR"%>
<%@ page import="java.io.*" %>
<%@ page import="mercury.web.*" %>
<%
	String p_resumable = request.getParameter("resumable");
	boolean resumable = false;
	if (p_resumable != null && p_resumable.trim().equals("true")) {
		resumable = true;
	}

	String p_sessionCheck = request.getParameter("sessionCheck");
	boolean sessionCehck = false;
	if (p_sessionCheck != null && p_sessionCheck.trim().equals("true")) {
		sessionCehck = true;
	}

	if (sessionCehck && session.getAttribute("NEW_SESSION") == null) {
        java.util.Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String msg = String.format("[Req-Header] %s:%s", name, request.getHeader(name));
            System.out.println(msg);
        }
		response.sendRedirect("./index.jsp");
	} else {

		//File file = new File("D:\\99_Downloads\\Torrento\\test_Movie.avi");
		File file = new File("D:\\00_Documents\\00_Documents.zip");
		try {
			FileDownloadUtility.download(request, response, file, resumable);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
%>