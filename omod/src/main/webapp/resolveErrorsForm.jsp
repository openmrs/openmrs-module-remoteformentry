<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Remote Form Entry Resolve Errors" otherwise="/login.htm" redirect="/module/remoteformentry/resolveErrors.form" />

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<h2>
	<spring:message code="remoteformentry.resolveErrors.title" />
</h2>

<c:set var="errorSize" value="${fn:length(resolveErrorsList)}" />

<c:choose>
	<c:when test="${errorSize < 1}">
		<br/>
		<i>(<spring:message code="remoteformentry.resolveErrors.empty"/>)</i>
		<br/>
	</c:when>
	<c:otherwise>
		
		<style type="text/css">
			#resolveErrorsTable tr td .value {
			  font-weight: bold;
			}
			#resolveErrorsTable tr.secondRow {
			  border-bottom: 1px solid black;
			}
		</style>
		
		<c:set var="size" value="10" />
		<c:set var="startIndex" value="0" />
		<c:if test="${not empty param.startIndex}">
			<c:set var="startIndex" value="${param.startIndex}" />
		</c:if>
		
		<spring:message code="remoteformentry.resolveErrors.total" arguments="${errorSize}" />
		<br/>
		<br/>
		
		<c:if test="${errorSize > 10}">
			<spring:message code="remoteformentry.resolveErrors.see"/>
			<c:if test="${startIndex != 0}">
				<c:set var="newStartIndex" value="${startIndex - 10 < 0 ? 0 : startIndex - 10}" />
				<a href="?startIndex=${newStartIndex}"><spring:message code="remoteformentry.resolveErrors.previous" arguments="${startIndex - newStartIndex}" /></a>
			</c:if>
			<c:if test="${startIndex + size < errorSize}">
				- <a href="?startIndex=${startIndex + 10}"><spring:message code="remoteformentry.resolveErrors.next" arguments="${startIndex + (2*size) > errorSize ? errorSize - (startIndex + size) : size}" /></a> 
			</c:if>
		
			<br/>
			<br/>
		</c:if>
		
		
		<b class="boxHeader"><spring:message code="remoteformentry.resolveErrors" />:</b>
		<div class="box">
			<form method="post" action="">
				<spring:message code="remoteformentry.resolveErrors.help" /> 
				<br/>
				<br/>
				<table cellpadding="3" cellspacing="0" width="100%" id="resolveErrorsTable">
					<c:forEach var="queueItem" items="${resolveErrorsList}" varStatus="queueItemStatus" begin="${startIndex}" end="${startIndex + size}">
						<tr class="<c:choose><c:when test="${queueItemStatus.index % 2 == 0}">evenRow</c:when><c:otherwise>oddRow</c:otherwise></c:choose>">
							<td>
								<!-- Info about the patient and encounter -->
								<spring:message code="Person.name" />: <span class="value">${queueItem.name}</span> <br/>
								<spring:message code="Patient.identifier" />: <span class="value">${queueItem.identifier}</span> <br/>
								<spring:message code="Person.gender" />: <span class="value">${queueItem.gender}</span> <br/>
								<c:if test="${queueItem.tribe != ''}">
									<spring:message code="Patient.tribe" />: <span class="value">${queueItem.tribe}</span> <br/>
								</c:if>
								<br/>
								<spring:message code="Encounter.location" />: <span class="value">${queueItem.location}</span> <br/>
								<spring:message code="Encounter.datetime" />: <span class="value">${queueItem.encounterDate}</span> <br/>
								<spring:message code="remoteformentry.resolveErrors.formName" />: <span class="value">${queueItem.formName} v${queueItem.formId}</span> <br/>
								<br/>
								<spring:message code="remoteformentry.resolveErrors.errorId" />: <span >${queueItem.formEntryErrorId}</span> <br/>
								<spring:message code="remoteformentry.resolveErrors.errorDateCreated" />: <span >${queueItem.dateCreated}</span> <br/>
								<spring:message code="remoteformentry.resolveErrors.error" />: <span >${queueItem.error}</span> <br/>
								<spring:message code="remoteformentry.resolveErrors.errorDetails" />: <div style="height: 40px; overflow-y: scroll; border: 1px solid #BBB;">${queueItem.errorDetails}</div> <br/>
							</td>
						</tr>
						<tr class="secondRow <c:choose><c:when test="${queueItemStatus.index % 2 == 0}">evenRow</c:when><c:otherwise>oddRow</c:otherwise></c:choose>">
							<td>
								<input type="hidden" name="formEntryErrorId" value="${queueItem.formEntryErrorId}"/>
								
								<!-- Pick a matching patient -->
								<input type="radio" name="errorItemAction-${queueItem.formEntryErrorId}" value="currentPatient" /> <spring:message code="remoteformentry.resolveErrors.matchedPatient"/>:
								<openmrs_tag:patientField formFieldName="currentPatientId-${queueItem.formEntryErrorId}" searchLabelCode="remoteformentry.resolveErrors.findPatient" initialValue="" linkUrl="" callback="setErrorAction" />
								
								<!-- Have the machinery create a new patient -->
								<input type="radio" name="errorItemAction-${queueItem.formEntryErrorId}" value="newPatient" /> 
								<spring:message code="remoteformentry.resolveErrors.noMatchedPatient"/> <br/>
								
								<!-- This is an invalid error, delete it -->
								<input type="radio" name="errorItemAction-${queueItem.formEntryErrorId}" value="deleteError" />
								<spring:message code="remoteformentry.resolveErrors.deleteError"/> <br/>
								
								<!-- I don't want to do anything to this one now -->
								<input type="radio" name="errorItemAction-${queueItem.formEntryErrorId}" value="noChange" checked="checked"/>
								<spring:message code="remoteformentry.resolveErrors.noChange"/> <br/>
								
								<br/>
							</td>
						</tr>
					</c:forEach>
						<tr>
							<td colspan="2">
								<input type="submit" name="action" value='<spring:message code="general.submit" />' />
							</td>
						</tr>
				</table>
			</form>
		</div>

	</c:otherwise>
</c:choose>

<br/>

<%@ include file="/WEB-INF/template/footer.jsp"%>
