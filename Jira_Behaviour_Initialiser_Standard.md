# Resolving ScriptRunner Syntax & Type Checking Errors in Jira Behaviours

به عنوان یک معمار راهکار Atlassian، این یکی از خطاهای بسیار کلاسیک و البته فریبنده در توسعه ScriptRunner برای Jira Data Center است. در این مستند، مشکل را کالبدشکافی کرده و یک کد کاملاً Production-Ready و ایمن برای محیط Enterprise (Jira 10) ارائه می‌دهیم.

## ۱. تحلیل مسئله
کدی که نوشته می‌شود از نظر منطق (Logic) کاملاً درست است، اما در ادیتور ScriptRunner (که از Monaco Editor استفاده می‌کند) دچار خطای Syntax و Type Checking می‌شود.

**ریشه مشکل چیست؟**
دو ریشه اصلی برای این خطا وجود دارد:
1. **کاراکترهای نامرئی (Non-breaking spaces):** کاراکترهای فاصله‌ی نامرئی با فرمت `\u00A0` یا NBSP که معمولاً هنگام کپی کردن کد از چت‌بات‌ها یا ویرایشگرهای متنی وارد می‌شوند و کامپایلر Groovy آن‌ها را به عنوان خطای Syntax می‌شناسد.
2. **فقدان Type Casting در ادیتور:** در ScriptRunner، متد `getFieldById()` به صورت پیش‌فرض یک آبجکت جنریک برمی‌گرداند. ادیتور متدهای `.setReadOnly()` را برای این آبجکت ناشناس غیرمعتبر می‌داند و زیر آن‌ها خط قرمز می‌کشد.
3. **مشکل با متون RTL و ایموجی‌ها:** ترکیب ایموجی با متن فارسی در محیط ادیتور آنلاین گاهی باعث به هم ریختگی Parsing خطوط می‌شود.

## ۲. راه‌حل‌های ممکن
* **راهکار ۱ (نادیده گرفتن خطا - Ignore):** ScriptRunner اجازه می‌دهد کدهایی که خطای Type Checking دارند را Save کنید. **(Bad Practice)** این کار به شدت ناامن است و خطاهای پنهان در Runtime می‌توانند باعث افت پرفورمنس شوند.
* **راهکار ۲ (اصلاح تایپ‌ها و پاکسازی کد - Static Type Casting):** معرفی دقیق کلاس `FormField` به کامپایلر و حذف کاراکترهای مخرب. کد کاملاً Type-Safe می‌شود، ادیتور قابلیت Auto-complete می‌دهد و هیچ خطایی در لاگ سرور ثبت نمی‌شود.

## ۳. بهترین پیشنهاد معماری
به عنوان یک Technical Lead، **راهکار ۲** تنها گزینه قابل قبول برای یک محیط Enterprise است. استفاده از کلاس صریح `com.onresolve.jira.groovy.user.FormField` باعث می‌شود ادیتور دقیقاً بداند با چه چیزی سروکار دارد.

## ۴. مراحل پیاده‌سازی (Implementation Steps)

### مرحله اول: انتخاب جایگاه درست کد
این کد را **نباید** روی فیلد Assignee (به عنوان Field Server-side script) قرار دهید، زیرا در آن صورت فقط وقتی فیلد تغییر کند اجرا می‌شود. این کد باید در بخش **Initialiser** قرار بگیرد تا از همان ثانیه اول لود شدن صفحه اجرا شود.

### مرحله دوم: جایگذاری کد استاندارد (در Initialiser)
```groovy
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.permission.GlobalPermissionKey
import com.atlassian.jira.permission.ProjectPermissions
import com.onresolve.jira.groovy.user.FormField // اضافه شدن برای رفع خطای Type Checking

// ۱. گرفتن کانتکست کاربر فعلی
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def globalPermissionManager = ComponentAccessor.getGlobalPermissionManager()
def permissionManager = ComponentAccessor.getPermissionManager()
def project = issueContext.projectObject

// بررسی null بودن پروژه (جلوگیری از NullPointerException در صفحات خاص)
boolean isProjectAdmin = false
if (project) {
    isProjectAdmin = permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, currentUser)
}

// ۲. بررسی سطح دسترسی: آیا کاربر Jira Admin (Global) یا Project Admin است؟
boolean isSystemAdmin = globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, currentUser)
boolean isAdmin = isSystemAdmin || isProjectAdmin

// ۳. کستینگ صریح به FormField برای جلوگیری از خطای ادیتور ScriptRunner
FormField assigneeField = getFieldById("assignee")
def action = getAction()

// ۴. پیاده‌سازی منطق بیزینسی
if (isAdmin) {
    // ادمین‌ها همیشه و در همه صفحات دسترسی دارند
    assigneeField.setReadOnly(false)
    assigneeField.clearHelpText()
    
} else if (action != null) {
    // کاربر ادمین نیست، اما روی صفحه Transition است -> دسترسی ویرایش باز است
    assigneeField.setReadOnly(false)
    assigneeField.clearHelpText()
    
} else {
    // کاربر در تلاش برای Inline Edit یا استفاده از صفحه Edit Issue است -> بسته می‌شود
    assigneeField.setReadOnly(true)
    assigneeField.setHelpText("ویرایش مستقیم فیلد غیرفعال است. لطفا از طریق دکمه های گردش کار اقدام کنید.")
}
