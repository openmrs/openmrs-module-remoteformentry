<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Remote Form Entry Import" otherwise="/login.htm" redirect="/module/remoteformentry/import.form" />

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<h2>
	<spring:message code="remoteformentry.import.title" />
</h2>

<b class="boxHeader"><spring:message code="remoteformentry.import.header" />:</b>
<div class="box">
	<form method="post" action="" enctype="multipart/form-data">
		<spring:message code="remoteformentry.import.help" />:<br/>
		<input type="file" name="queueImport" size="30" />
		<input type="submit" name="action" value='<spring:message code="remoteformentry.import" />' />
	</form>
</div>

<br/>
<spring:message code="remoteformentry.import.files"/> ${importDirectory}:
<table cellspacing="0" cellpadding="3">
	<tr>
		<th><spring:message code="remoteformentry.filename"/></th>
		<th><spring:message code="remoteformentry.dateModified" /></th>
	</tr>
<c:forEach var="fileEntry" items="${importedFiles}" varStatus="rowStatus">
	<tr class="<c:choose><c:when test="${rowStatus.index % 2 == 0}">evenRow</c:when><c:otherwise>oddRow</c:otherwise></c:choose>">
		<td>${fileEntry.key}</td>
		<td><openmrs:formatDate date="${fileEntry.value}" type="long"/></td>
	</tr>
</c:forEach>
</table>

<c:if test="${fn:length(importedFiles) == 0}" >
	<spring:message code="remoteformentry.noFilesFound" />
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp"%>
