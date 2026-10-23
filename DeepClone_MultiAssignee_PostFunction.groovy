import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.issue.index.IssueIndexingService

// Enterprise Post-Function Script: Deep Clone & Multi-Assignee Distribution
// Automatically copies all system and custom fields (Priority, Components, etc.) via cloneIssue

def issueManager = ComponentAccessor.getIssueManager()
def issueFactory = ComponentAccessor.getIssueFactory()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def labelManager = ComponentAccessor.getComponent(LabelManager.class)
def issueIndexingService = ComponentAccessor.getComponent(IssueIndexingService.class)
def loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

// Get executors custom field
def executorsField = customFieldManager.getCustomFieldObjects(issue).find { it.name == "مجریان تسک" }
if (!executorsField) return

def selectedExecutors = issue.getCustomFieldValue(executorsField) as List<ApplicationUser>
def originalAssignee = issue.getAssignee()

// 1. Consolidate target users into a Set to avoid duplicates
Set<ApplicationUser> allTargetUsers = new LinkedHashSet<>()
if (originalAssignee) allTargetUsers.add(originalAssignee)
if (selectedExecutors) allTargetUsers.addAll(selectedExecutors)

// Exclude the current task creator if required by business logic
allTargetUsers.removeAll { it.key == loggedInUser.key }
if (allTargetUsers.isEmpty()) return

// Fetch definitive issue object from database
MutableIssue mainIssue = issueManager.getIssueObject(issue.getId())
if (!mainIssue) return

// Cache original labels (as cloneIssue might skip system labels depending on Jira version)
def originalLabels = labelManager.getLabels(mainIssue.getId())?.collect { it.label }?.toSet()

List<ApplicationUser> finalUsersList = allTargetUsers.toList()
ApplicationUser firstUser = finalUsersList.first()

// 2. Update main issue for the first user and clear the executors custom field
mainIssue.setAssignee(firstUser)
mainIssue.setCustomFieldValue(executorsField, null)
issueManager.updateIssue(loggedInUser, mainIssue, EventDispatchOption.DO_NOT_DISPATCH, false)

// 3. Clone and create new tasks for remaining users dynamically
if (finalUsersList.size() > 1) {
    def remainingUsers = finalUsersList.drop(1)
    
    remainingUsers.each { ApplicationUser user ->
        // Native Deep Copy: Copies Priority, Components, and all Custom Fields automatically
        MutableIssue newTask = issueFactory.cloneIssue(mainIssue)
        
        newTask.setAssignee(user)
        
        def createdTask = issueManager.createIssueObject(loggedInUser, newTask)
        
        if (originalLabels) {
            labelManager.setLabels(loggedInUser, createdTask.getId(), originalLabels, false, false)
        }
        
        // Data Center Cluster Reindexing
        issueIndexingService.reIndex(createdTask)
    }
}
