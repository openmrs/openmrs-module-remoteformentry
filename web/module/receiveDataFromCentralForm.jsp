<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Remote Form Entry Recieve Data" otherwise="/login.htm" redirect="/module/remoteformentry/receiveDataFromCentral.form" />

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<h2>
	<spring:message code="remoteformentry.receiveDataFromCentral.title" />
</h2>

<b class="boxHeader"><spring:message code="remoteformentry.receiveDataFromCentral.header" />:</b>
<div class="box">
	<form method="post" action="" enctype="multipart/form-data">
		<spring:message code="remoteformentry.receiveDataFromCentral.help" />
		<br/><br/>
		<input type="file" name="returnedData" size="30" />
		<input type="submit" name="action" value='<spring:message code="remoteformentry.upload" />' />
	</form>
</div>

<br/>
<spring:message code="remoteformentry.receiveDataFromCentral.files"/> ${receiveFilesDirectory}:
<table cellspacing="0" cellpadding="3">
	<tr>
		<th><spring:message code="remoteformentry.filename"/></th>
		<th><spring:message code="remoteformentry.dateModified" /></th>
	</tr>
<c:forEach var="fileEntry" items="${receivedFiles}" varStatus="rowStatus">
	<tr class="<c:choose><c:when test="${rowStatus.index % 2 == 0}">evenRow</c:when><c:otherwise>oddRow</c:otherwise></c:choose>">
		<td>${fileEntry.key}</td>
		<td><openmrs:formatDate date="${fileEntry.value}" type="long"/></td>
	</tr>
</c:forEach>
</table>

<c:if test="${fn:length(receivedFiles) == 0}" >
	<spring:message code="remoteformentry.noFilesFound" />
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp"%>
