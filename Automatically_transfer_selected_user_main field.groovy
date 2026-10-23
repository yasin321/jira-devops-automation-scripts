import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

// نام دقیق کاستوم فیلد خود را اینجا قرار دهید (یا از ID آن مثل customfield_10500 استفاده کنید)
final String CUSTOM_FIELD_ID = "customfield_10500" 

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def targetField = customFieldManager.getCustomFieldObject(CUSTOM_FIELD_ID)

if (targetField) {
    def selectedUser = issue.getCustomFieldValue(targetField) as ApplicationUser
    if (selectedUser) {
        // تغییر Assignee اصلی تسک به کاربر انتخاب شده از رول Head of Sales
        issue.setAssignee(selectedUser)
    }
}
