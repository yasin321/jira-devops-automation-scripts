# Triple-Layer Restriction: "Assign to me" by Workflow Status

This architecture enforces strict access control on the "Assign to me" action, restricting it to a specific workflow status. It uses a Defense-in-Depth approach across three Jira layers to prevent API and UI bypasses.

## 1. Backend Validation (ScriptRunner Behaviours)
**Target:** Server-Side Script on the `Assignee` field.
**Mechanism:** Blocks form submission and API calls if the issue is not in the allowed target status.

```groovy
def assigneeField = getFieldById("assignee")
def currentUser = com.atlassian.jira.component.ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def issue = underlyingIssue

if (!issue) return

def targetStatusId = "STATUS_ID" // Replace with actual status ID
def newAssignee = assigneeField.getValue()

if (issue.getStatus().getId() != targetStatusId) {
    if (newAssignee == currentUser.getUsername() || newAssignee == currentUser.getKey()) {
        assigneeField.setError("شما فقط در وضعیت مجاز به استفاده از Assign to me هستید.")
    } else {
        assigneeField.clearError()
    }
} else {
    assigneeField.clearError()
}

2. View Issue UI Sanitization (ScriptRunner Web Fragment)
Location: atl.jira.view.issue.right.context
Mechanism: Dynamically evaluates the issue context and hides the button in the standard issue view when outside the allowed status.

Condition Script:

issue.getProjectId().toString() == "PROJECT_ID" && 
issue.getIssueTypeId() == "ISSUE_TYPE_ID" && 
issue.getStatus().getId() != "STATUS_ID"

Provider Script:

writer.write("""
<style>
    #assign-to-me, #assign-to-me-trigger, .assign-to-me-link { display: none !important; }
</style>
""")



3. Agile Board DOM Observer (Announcement Banner)
Target: Jira Announcement Banner (HTML/JS)
Mechanism: Uses MutationObserver to monitor React/SPA DOM changes on Agile Boards (RapidBoard.jspa) and dynamically hides the assignment button based on REST API status checks, preventing client-side bypasses.


<script>
AJS.$(document).ready(function() {
    function enforceAccountAssignHidden() {
        var urlParams = new URLSearchParams(window.location.search);
        var selectedIssue = urlParams.get('selectedIssue');
        
        if (selectedIssue && window.location.href.indexOf("RapidBoard.jspa") > -1) {
            AJS.$.get(AJS.contextPath() + '/rest/api/2/issue/' + selectedIssue + '?fields=issuetype,project,status', function(data) {
                if (data && data.fields) {
                    var issueTypeId = data.fields.issuetype.id;
                    var projectId = data.fields.project.id;
                    var statusId = data.fields.status.id;
                    
                    var targetProjectId = "PROJECT_ID";
                    var targetIssueTypeId = "ISSUE_TYPE_ID";
                    var allowedStatusId = "STATUS_ID";
                    
                    if (projectId === targetProjectId && issueTypeId === targetIssueTypeId && statusId !== allowedStatusId) {
                        if (AJS.$('#account-hide-assign-css').length === 0) {
                            AJS.$('head').append('<style id="account-hide-assign-css">.assign-to-me-link, a#assign-to-me.issueaction-assign-to-me, a#assign-to-me, .ghx-detail-assign-to-me { display: none !important; pointer-events: none !important; opacity: 0 !important; width: 0 !important; height: 0 !important; overflow: hidden !important; }</style>');
                        }
                    } else {
                        AJS.$('#account-hide-assign-css').remove();
                    }
                }
            });
        } else {
            AJS.$('#account-hide-assign-css').remove();
        }
    }

    var accountTargetNode = document.body;
    var accountConfig = { childList: true, subtree: true };

    var accountObserver = new MutationObserver(function(mutationsList, observer) {
        for (var mutation of mutationsList) {
            if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                for (var node of mutation.addedNodes) {
                    if (node.nodeType === 1) { 
                        if (node.id === 'ghx-detail-view' || node.classList.contains('ghx-detail-issue')) {
                            enforceAccountAssignHidden();
                            break; 
                        }
                    }
                }
            }
        }
    });

    accountObserver.observe(accountTargetNode, accountConfig);

    AJS.$(document).on('click', '.ghx-issue', function() {
        setTimeout(enforceAccountAssignHidden, 300);
    });

    setTimeout(enforceAccountAssignHidden, 800);
});
</script>
