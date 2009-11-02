<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Remote Form Entry Edit Properties" otherwise="/login.htm" redirect="/module/remoteformentry/setupForms.form" />

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<h2>
	<spring:message code="remoteformentry.setupForms.title" />
</h2>

<div class="boxHeader">
	<b><spring:message code="remoteformentry.setupForms.boxHeaderTitle" /></b>
</div>

<form method="post" class="box">
	<c:choose>
		<c:when test="${fn:length(forms) < 1}">
			<spring:message code="remoteformentry.setupForms.noForms"/>
		</c:when>
		<c:otherwise>
			<table cellpadding="2" cellspacing="0" id="formTable" width="98%">
				<tr>
					<th> </th>
					<th> <spring:message code="general.name" /> </th>
					<th> <spring:message code="Form.version" /> </th>
					<th> <spring:message code="Form.published" /> </th>
				</tr>
				<c:forEach var="form" items="${forms}" varStatus="status">
					<tr class="<c:choose><c:when test="${status.index % 2 == 0}">evenRow</c:when><c:otherwise>oddRow</c:otherwise></c:choose>">
						<td><input type="checkbox" name="formId" value="${form.formId}" /></td>
						<td><a href="formEdit.form?formId=${form.formId}">${form.name}</a></td>
						<td>${form.version}</td>
						<td><c:if test="${form.published == true}"><spring:message code="general.yes"/></c:if></td>
					</tr>
				</c:forEach>
			</table>
			<br/>
			<input type="submit" value="<spring:message code="remoteformentry.setupForms.setFormSchemas"/>"/>
		</c:otherwise>
	</c:choose>
</form>

<%@ include file="/WEB-INF/template/footer.jsp"%>
