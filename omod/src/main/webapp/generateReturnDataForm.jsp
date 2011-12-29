<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Remote Form Entry Return To Remote" otherwise="/login.htm" redirect="/module/remoteformentry/generateReturnData.form" />

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<h2>
	<spring:message code="remoteformentry.generateReturnData.title" />
</h2>

<spring:message code="remoteformentry.generateReturnData.help" />

<br/>
<br/>

<spring:message code="remoteformentry.generateReturnData.lastModified" /> : ${lastModified}

<br/>
<br/>

<a href="${pageContext.request.contextPath}/moduleServlet/remoteformentry/generateReturnData">
  <spring:message code="remoteformentry.generateReturnData.generate" />
</a>

<%@ include file="/WEB-INF/template/footer.jsp"%>
