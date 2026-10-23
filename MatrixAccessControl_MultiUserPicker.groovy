import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

// Server-Side Behaviour Script for Custom Multi-User Picker Field ("مجریان تسک")
// Validates multiple assignees against Matrix Access Control before Post-Function execution

def executorsField = getFieldByName("مجریان تسک")
def rawValue = executorsField.getValue()

if (!rawValue) {
    executorsField.clearError()
    return
}

// Safe casting to support both Single and Multi-User selection states
List<String> usernames = []
if (rawValue instanceof List) {
    usernames = rawValue.collect { it.toString() }
} else {
    usernames.add(rawValue.toString())
}

def userManager = ComponentAccessor.getUserManager()
def userPropertyManager = ComponentAccessor.getUserPropertyManager()
def groupManager = ComponentAccessor.getGroupManager()

def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
if (groupManager.isUserInGroup(currentUser, "jira-administrators")) {
    executorsField.clearError()
    return
}

def getUserRoles = { ApplicationUser u ->
    if (!u) return []
    def propertySet = userPropertyManager.getPropertySet(u)
    if (!propertySet) return []
    def propString = propertySet.getString("jira.meta.SalesLevel") ?: propertySet.getString("SalesLevel")
    if (!propString) return []
    return propString.split(",").collect { it.trim() }
}

def currentUserRoles = getUserRoles(currentUser)
if (!currentUserRoles) {
    executorsField.setError("شما به عنوان کاربر فعلی، سطح فروش (SalesLevel) ندارید و مجاز به ایجاد تسک نیستید.")
    return
}

List<String> invalidUsers = []
List<String> missingRoleUsers = []

// Loop validation across all selected users
usernames.each { String uname ->
    def targetUser = userManager.getUserByName(uname) ?: userManager.getUserByKey(uname)
    
    if (targetUser) {
        def targetUserRoles = getUserRoles(targetUser)

        if (!targetUserRoles) {
            missingRoleUsers.add(targetUser.getDisplayName())
        } else {
            boolean isAllowed = false
            
            if (currentUserRoles.contains("HQ")) {
                isAllowed = targetUserRoles.intersect(["HQ", "Region1", "Region2"]).size() > 0
            } else if (currentUserRoles.contains("Region1")) {
                isAllowed = targetUserRoles.contains("Region1")
            } else if (currentUserRoles.contains("Region2")) {
                isAllowed = targetUserRoles.contains("Region2")
            }

            if (!isAllowed) {
                invalidUsers.add(targetUser.getDisplayName())
            }
        }
    }
}

// Display categorized validation errors
if (missingRoleUsers) {
    executorsField.setError("نقش سازمانی (SalesLevel) برای این افراد ثبت نشده است: ${missingRoleUsers.join('، ')}")
} else if (invalidUsers) {
    executorsField.setError("طبق پالیسی امنیتی، شما مجاز به ارجاع کار به این افراد نیستید: ${invalidUsers.join('، ')}")
} else {
    executorsField.clearError()
}
