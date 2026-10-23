# Jira UI Customization: Dynamic "Assign to me" Restriction via Web Fragments

به عنوان یک معمار سیستم، توجه به دقیق‌ترین جزئیات در توسعه ScriptRunner برای Jira Data Center الزامی است. این مستند به بررسی و حل یکی از چالش‌های رایج در پنهان‌سازی المان‌های UI (دکمه Assign to me) با استفاده از ماژول `Hide UI element` می‌پردازد.

## ۱. تحلیل مسئله
در پنهان‌سازی آیتم‌های رابط کاربری از طریق ScriptRunner، دو چالش اساسی وجود دارد:

1. **ریشه پیدا نکردن آیتم (Module Key):** در نسخه‌های جدیدتر Jira Data Center، آتلاسیان Module Key کامل پکیج را الزامی کرده است و نام کوتاه را قبول نمی‌کند. شناسه صحیح دکمه اساین به خود این مورد است: `com.atlassian.jira.plugin.system.issueoperations:assign-to-me`
2. **نکته انحرافی در منطق Condition:** در زیر ادیتور ScriptRunner نوشته شده است: *Note that the item will be displayed if the code returns a truthy object*. این یعنی با وجود اینکه نام ماژول "Hide UI element" است، کدی که در Condition می‌نویسیم در واقع یک Web Condition استاندارد جیرا است:
   * اگر کد `true` برگرداند ⬅️ دکمه نمایش داده می‌شود (Show).
   * اگر کد `false` برگرداند ⬅️ دکمه پنهان می‌شود (Hide).

## ۲. راه‌حل و پیشنهاد معماری
معماری صحیح این است که لاجیک کد را بر اساس استانداردهای Web Item بازنویسی کنیم. ما به جیرا می‌گوییم: "این دکمه را در همه جا پنهان کن (`return false`)، مــــگر اینکه هر ۳ شرط بیزینسی (وضعیت خاص، نقش خاص، خالی بودن فیلد) برقرار باشد (آن‌وقت `return true`)."

## ۳. مراحل پیاده‌سازی (Implementation Steps)

* **در فیلد Hide what:** دقیقاً این گزینه را انتخاب کنید:
  `com.atlassian.jira.plugin.system.issueoperations:assign-to-me`
* **در باکس Condition:** کد Groovy زیر که Production-Ready و اصلاح شده است را قرار دهید:

```groovy
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRoleManager

// اگر تسک باز نیست یا کاربر لاگین نکرده، قطعا دکمه باید مخفی باشد (return false)
if (issue == null || user == null) {
    return false 
}

def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def financeRole = projectRoleManager.getProjectRole("finance expert")

// اگر نقش اصلا در سیستم وجود ندارد، دکمه مخفی شود تا جلوی ارور را بگیرد
if (financeRole == null) {
    log.error("Role 'finance expert' does not exist in Jira!")
    return false 
}

// 1. بررسی نقش کاربر
boolean isFinanceExpert = projectRoleManager.isUserInProjectRole(user, financeRole, issue.getProjectObject())

// 2. بررسی وضعیت تسک
boolean isCorrectStatus = issue.getStatus().getName() == "اقدام توسط واحد اعتبارسنجی"

// 3. بررسی خالی بودن فیلد Assignee
boolean isUnassigned = (issue.getAssigneeId() == null)

// منطق نهایی بر اساس استانداردهای Web Condition:
// فقط در صورتی که "وضعیت درست است" و "کاربر فایننس است" و "تسک اساین نشده"، دکمه را نشان بده (true)
// در غیر این صورت، برای تمام افراد دیگر و در تمام وضعیت‌های دیگر، دکمه را مخفی کن (false)
if (isCorrectStatus && isFinanceExpert && isUnassigned) {
    return true
} else {
    return false
}
