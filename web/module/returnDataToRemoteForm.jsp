<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Remote Form Entry Return To Remote" otherwise="/login.htm" redirect="/module/remoteformentry/returnDataToRemote.form" />

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<h2>
	<spring:message code="remoteformentry.returnToRemote.title" />
</h2>

<form method="post" action="${pageContext.request.contextPath}/moduleServlet/remoteformentry/returnToRemoteDownload">
	<b class="boxHeader"><spring:message code="remoteformentry.returnToRemote.boxHeader" />:</b>
	<div class="box">
		<spring:message code="remoteformentry.returnToRemote.help" /><br/>
		
		<br/>
		
		<spring:message code="remoteformentry.returnToRemote.location" />:
		<select name="locationId">
			<c:forEach var="remoteLocationMap" items="${remoteLocations}">
				<option value="${remoteLocationMap.key.locationId}"
					<c:if test="${locationId == remoteLocationMap.key.locationId}">selected</c:if>
				>
					<c:forEach var="location" items="${remoteLocationMap.value}">
						${location.name}
					</c:forEach>
				</option>
			</c:forEach>
		</select>
		
		<c:if test="${staleData}">
			<br/>
			<span class="error">
				<spring:message code="remoteformentry.returnToRemote.staleData" /> : ${lastModified}
			</span>
		</c:if>
		
		<br/>
		<input type="submit" value='<spring:message code="general.download" />' />
	</div>
</form>

<br/>
<spring:message code="remoteformentry.returnToRemote.files"/> ${returnDirectory}:
<table cellspacing="0" cellpadding="3">
	<tr>
		<th><spring:message code="remoteformentry.filename"/></th>
		<th><spring:message code="remoteformentry.dateModified" /></th>
	</tr>
<c:forEach var="fileEntry" items="${returnedFiles}" varStatus="rowStatus">
	<tr class="<c:choose><c:when test="${rowStatus.index % 2 == 0}">evenRow</c:when><c:otherwise>oddRow</c:otherwise></c:choose>">
		<td>${fileEntry.key}</td>
		<td><openmrs:formatDate date="${fileEntry.value}" type="long"/></td>
	</tr>
</c:forEach>
</table>

<c:if test="${fn:length(returnedFiles) == 0}" >
	<spring:message code="remoteformentry.noFilesFound" />
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp"%>
