<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Remote Form Entry Edit Properties" otherwise="/login.htm" redirect="/module/remoteformentry/properties.form" />

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<h2>
	<spring:message code="remoteformentry.properties.title" />
</h2>

<form method="post" action="">
	<b class="boxHeader"><spring:message code="remoteformentry.properties" />:</b>
	<div class="box">
		<table cellspacing="0">
			<tr>
				<td colspan="2">
					<c:choose>
						<c:when test="${isCentral and isRemote}">
							<span class="error"><spring:message code="remoteformentry.properties.isCentralAndRemote" /></span><br/>
							<span class="error"><spring:message code="remoteformentry.properties.isCentralAndRemoteFix" /></span>
						</c:when>
						<c:when test="${isCentral}">
							<spring:message code="remoteformentry.properties.isCentral" />
						</c:when>
						<c:when test="${isRemote}">
							<spring:message code="remoteformentry.properties.isRemote" />
							<c:if test="${formentryProcessorEnabled}">
								<br/><br/>
								<span class="error"><spring:message code="remoteformentry.properties.processorIsRunning" /></span>
							</c:if>
						</c:when>
					</c:choose>
					
					<br/><br/>
				</td>
			</tr>
			<c:if test="${isCentral}">
				<tr>
					<td colspan="2"><spring:message code="remoteformentry.properties.initialEncounters.help" /></td>
				</tr>
				<tr>
					<td>
						<c:forEach var="enc" items="${encounterTypes}">
							<input type="checkbox" name="encounterTypeId" value="${enc.key.encounterTypeId}"
								id="${enc.key.encounterTypeId}"
								<c:if test="${enc.value}">checked</c:if>>
							<label for="${enc.key.encounterTypeId}">
								${enc.key.name}
							</label> <br/>
						</c:forEach>
						<br/><br/>
					</td>
				</tr>
			</c:if>
			<c:if test="${isRemote}">
				<tr>
					<td colspan="2">
						<spring:message code="remoteformentry.properties.location.help"/><br/>
						<spring:message code="remoteformentry.properties.location"/>:<br/>
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
						<br/><br/>
					</td>
				</tr>
			</c:if>
			
		</table>
		<br/>
		
		<input type="submit" value='<spring:message code="general.submit"/>'/>
	</div>
</form>

<%@ include file="/WEB-INF/template/footer.jsp"%>
