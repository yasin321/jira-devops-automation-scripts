import java.lang.String
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

// Server-Side Behaviour Script for System Field: Assignee
// Enforces Matrix Access Control (RBAC) via Jira User Properties (SalesLevel)

def assigneeField = getFieldById("assignee")
def userManager = ComponentAccessor.getUserManager()
def groupManager = ComponentAccessor.getGroupManager()
def userPropertyManager = ComponentAccessor.getUserPropertyManager()

def selectedUsername = assigneeField.getValue() as String
if (!selectedUsername || selectedUsername == "-1") {
    assigneeField.clearError()
    return
}

def targetUser = userManager.getUserByName(selectedUsername) ?: userManager.getUserByKey(selectedUsername)
if (!targetUser) {
    assigneeField.setError("خطای سیستمی: کاربر انتخاب شده در دایرکتوری جیرا یافت نشد.")
    return
}

def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
if (groupManager.isUserInGroup(currentUser, "jira-administrators")) {
    assigneeField.clearError()
    return
}

def getUserRoles = { ApplicationUser u ->
    if (!u) return []
    def propertySet = userPropertyManager.getPropertySet(u)
    if (!propertySet) return []
    // Handle both UI-created (jira.meta.*) and API-created user properties
    def propString = propertySet.getString("jira.meta.SalesLevel") ?: propertySet.getString("SalesLevel")
    if (!propString) return []
    return propString.split(",").collect { it.trim() }
}

def currentUserRoles = getUserRoles(currentUser)
def targetUserRoles = getUserRoles(targetUser)

if (!currentUserRoles) {
    assigneeField.setError("شما به عنوان کاربر فعلی، سطح فروش (SalesLevel) ندارید و مجاز به ارجاع تیکت نیستید.")
    return
}

if (!targetUserRoles) {
    assigneeField.setError("نقش سازمانی (SalesLevel) برای این شخص در تنظیمات جیرا ثبت نشده است. ارجاع مسدود است.")
    return
}

boolean isAllowed = false
if (currentUserRoles.contains("HQ")) {
    isAllowed = targetUserRoles.intersect(["HQ", "Region1", "Region2"]).size() > 0
} else if (currentUserRoles.contains("Region1")) {
    isAllowed = targetUserRoles.contains("Region1")
} else if (currentUserRoles.contains("Region2")) {
    isAllowed = targetUserRoles.contains("Region2")
}

if (!isAllowed) {
    assigneeField.setError("طبق پالیسی امنیتی فروش، شما مجاز به ارجاع تیکت به این شخص (تیم متفاوت) نیستید.")
} else {
    assigneeField.clearError()
}
