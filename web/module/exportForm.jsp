<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Remote Form Entry Export Queue" otherwise="/login.htm" redirect="/module/remoteformentry/export.form" />

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<h2>
	<spring:message code="remoteformentry.export.title" />
</h2>

<form method="post" action="${pageContext.request.contextPath}/moduleServlet/remoteformentry/queueDownload">
	<b class="boxHeader"><spring:message code="remoteformentry.export" />:</b>
	<div class="box">
		<spring:message code="remoteformentry.export.help" />
		<br/>
		<br/>
		<c:choose>
			<c:when test="${exportList > 0}">
				${exportList} <spring:message code="remoteformentry.export.itemsAvailable"/><br/>
				<input type="submit" value='<spring:message code="general.download" />' /><br/>
				<spring:message code="remoteformentry.export.download.hint" />
			</c:when>
			<c:otherwise>
				<i>(<spring:message code="remoteformentry.export.queueEmpty"/>)</i>
			</c:otherwise>
		</c:choose>
	</div>
</form>

<br/>
<spring:message code="remoteformentry.export.files"/> ${exportDirectory}:
<table cellspacing="0" cellpadding="3">
	<tr>
		<th><spring:message code="remoteformentry.filename"/></th>
		<th><spring:message code="remoteformentry.dateModified" /></th>
	</tr>
<c:forEach var="fileEntry" items="${exportedFiles}" varStatus="rowStatus">
	<tr class="<c:choose><c:when test="${rowStatus.index % 2 == 0}">evenRow</c:when><c:otherwise>oddRow</c:otherwise></c:choose>">
		<td>${fileEntry.key}</td>
		<td><openmrs:formatDate date="${fileEntry.value}" type="long"/></td>
	</tr>
</c:forEach>
</table>

<c:if test="${fn:length(exportedFiles) == 0}" >
	<spring:message code="remoteformentry.noFilesFound" />
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp"%>
