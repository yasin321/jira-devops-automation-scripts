# Triple-Layer Access Governance: Restricting "Assign to me" in Jira Data Center

## Architecture Overview
To enforce strict business rules preventing users from using the "Assign to me" action outside of a specific status (`در حال اقدام توسط واحد اعتبارسنجی`), standard Jira permissions are insufficient due to Agile Board DOM rendering and REST API bypasses. 

We implemented a **Defense in Depth** strategy across 3 distinct layers:

---

### Layer 1: Backend & Form Validation (ScriptRunner Behaviour)
* **Target:** Server-Side Script on System Field `Assignee`.
* **Mechanism:** Intercepts the form submission and checks the current issue status against the target workflow status. If violated, it blocks the assignment and throws a localized error message.

```groovy
def assigneeField = getFieldById("assignee")
def currentUser = com.atlassian.jira.component.ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def issue = underlyingIssue
if (!issue) return

def targetStatusId = "STATUS_ID" // Replace with actual status ID
def newAssignee = assigneeField.getValue()

if (issue.getStatus().getId() != targetStatusId) {
    if (newAssignee == currentUser.getUsername() || newAssignee == currentUser.getKey()) {
        assigneeField.setError("شما فقط در وضعیت مجاز مجاز به استفاده از Assign to me هستید.")
    } else {
        assigneeField.clearError()
    }
} else {
    assigneeField.clearError()
}
