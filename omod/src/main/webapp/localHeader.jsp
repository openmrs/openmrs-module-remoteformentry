
<jsp:directive.page import="org.openmrs.module.remoteformentry.RemoteFormEntryUtil"/><ul id="menu">
	<li class="first">
		<a href="${pageContext.request.contextPath}/admin"><spring:message code="admin.title.short"/></a>
	</li>
	
	<openmrs:hasPrivilege privilege="Remote Form Entry Edit Properties">
		<li <c:if test='<%= request.getRequestURI().contains("remoteformentry/properties") %>'>class="active"</c:if>>
			<a href="${pageContext.request.contextPath}/module/remoteformentry/properties.form">
				<spring:message code="remoteformentry.properties.manage"/>
			</a>
		</li>
	</openmrs:hasPrivilege>
	
	<c:if test="<%= RemoteFormEntryUtil.isCentralServer() %>">
		<openmrs:hasPrivilege privilege="Remote Form Entry Edit Properties">
			<li <c:if test='<%= request.getRequestURI().contains("remoteformentry/setupForms") %>'>class="active"</c:if>>
				<a href="${pageContext.request.contextPath}/module/remoteformentry/setupForms.form">
					<spring:message code="remoteformentry.setupForms.manage"/>
				</a>
			</li>
		</openmrs:hasPrivilege>
		
		<openmrs:hasPrivilege privilege="Remote Form Entry Import">
			<li <c:if test='<%= request.getRequestURI().contains("remoteformentry/import") %>'>class="active"</c:if>>
				<a href="${pageContext.request.contextPath}/module/remoteformentry/import.form">
					<spring:message code="remoteformentry.import.manage"/>
				</a>
			</li>
		</openmrs:hasPrivilege>
		
		<openmrs:hasPrivilege privilege="Remote Form Entry Return Data">
			<li <c:if test='<%= request.getRequestURI().contains("remoteformentry/returnData") %>'>class="active"</c:if>>
				<a href="${pageContext.request.contextPath}/module/remoteformentry/returnDataToRemote.form">
					<spring:message code="remoteformentry.returnToRemote.manage"/>
				</a>
			</li>
		</openmrs:hasPrivilege>
		
		<openmrs:hasPrivilege privilege="Remote Form Entry Return Data">
			<li <c:if test='<%= request.getRequestURI().contains("remoteformentry/generateReturnData") %>'>class="active"</c:if>>
				<a href="${pageContext.request.contextPath}/module/remoteformentry/generateReturnData.form">
					<spring:message code="remoteformentry.generateReturnData.manage"/>
				</a>
			</li>
		</openmrs:hasPrivilege>
		
		<openmrs:hasPrivilege privilege="Remote Form Entry Resolve Errors">
			<li <c:if test='<%= request.getRequestURI().contains("remoteformentry/resolveErrors") %>'>class="active"</c:if>>
				<a href="${pageContext.request.contextPath}/module/remoteformentry/resolveErrors.form">
					<spring:message code="remoteformentry.resolveErrors.manage"/>
				</a>
			</li>
		</openmrs:hasPrivilege>
	</c:if>
	
	<c:if test="<%= RemoteFormEntryUtil.isRemoteServer() %>">
		<openmrs:hasPrivilege privilege="Remote Form Entry Export">
			<li <c:if test='<%= request.getRequestURI().contains("remoteformentry/export") %>'>class="active"</c:if>>
				<a href="${pageContext.request.contextPath}/module/remoteformentry/export.form">
					<spring:message code="remoteformentry.export.manage"/>
				</a>
			</li>
		</openmrs:hasPrivilege>
		
		<openmrs:hasPrivilege privilege="Remote Form Entry Receive Data">
			<li <c:if test='<%= request.getRequestURI().contains("remoteformentry/receiveData") %>'>class="active"</c:if>>
				<a href="${pageContext.request.contextPath}/module/remoteformentry/receiveDataFromCentral.form">
					<spring:message code="remoteformentry.receiveDataFromCentral.manage"/>
				</a>
			</li>
		</openmrs:hasPrivilege>
	</c:if>


</ul>