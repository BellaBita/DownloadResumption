<%@ page language="java" contentType="text/html; charset=EUC-KR"
	pageEncoding="EUC-KR"%>
<%
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
    if (request.getProtocol().equals("HTTP/1.1"))
        response.setHeader("Cache-Control", "no-cache");
%>
<!DOCTYPE html>
<html>
<head>
<meta charshet="utf-8" />
<title>Http 1.1 Resume Download Test</title>
<link href="css/style.css" rel="stylesheet" />
</head>
<body>
	<h1>Welcome to Mercury 1.0</h1>
	<p>
		Session Data :
		<%=session.getAttribute("NEW_SESSION")%></p>
	<br />
	<ul>
		<li><a href="./download.jsp">donwload.jsp</a></li>
		<li><a href="./download.jsp?resumable=true">./download.jsp?resumable=true</a></li>
		<li><a href="./download.jsp?resumable=true&sessionCheck=true">./download.jsp?resumable=true&sessionCheck=true</a></li>
		<li><a href="./newSessionData.jsp">New Session Data </a></li>
		<li><a href="./clearSessionData.jsp">Clean Session Data</a></li>
	</ul>
</body>
</html>