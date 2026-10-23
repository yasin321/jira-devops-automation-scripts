# Jira Data Center & DevOps Automation Scripts

Custom **Groovy (ScriptRunner)**, **Bash**, and **Python** scripts designed for enterprise Jira Data Center governance, dynamic access control, REST API integrations, and Linux infrastructure maintenance.

---

## Featured Architecture: Matrix Access Control & Assignment Validation (RBAC)

### Overview
Implemented a real-time **Role-Based Matrix Access Control** mechanism in Jira Data Center using **ScriptRunner Behaviours** and **OSUser PropertySets**, avoiding custom field bloat and permission scheme complexity.

* **Files:** `MatrixAccessControl_Assignee.groovy` | `MatrixAccessControl_MultiUserPicker.groovy`
* **Key Technical Achievements:**
  * **Database Property Prefix Handling:** Handled Jira's internal `jira.meta.*` database prefix discrepancy between UI-defined and API-defined user properties (`SalesLevel`).
  * **Bypassing `setFilter` Limitations:** Replaced `setFilter` (which throws `MissingMethodException` on system fields like `Assignee`) with sub-50ms **Live UI Server-Side Validation**.
  * **Multi-Role CSV Parsing & Loop Validation:** Supported multi-region managers (`HQ,Region1`) and iterative validation on Multi-User Picker fields prior to task-cloning Post-Functions.

---

## مستند معماری: کنترل دسترسی ماتریسی در Jira Data Center

### ۱. معماری داده‌ها (Data Architecture)
برای تفکیک دسترسی‌ها بدون ایجاد Custom Fieldهای اضافی یا به هم ریختن Permission Schemeها، از **Jira User Properties** استفاده شد:
- **Property Key:** `SalesLevel`
- **Values:** `HQ` (ستاد)، `Region1` (تیم ۱)، `Region2` (تیم ۲)
- **نقش‌های چندگانه (Multi-Role):** پشتیبانی از مقادیر CSV برای مدیران چندبخشی (مثال: `HQ,Region1`).
- **هندل کردن پیشوند دیتابیس (`jira.meta.`):** پشتیبانی همزمان از مقادیر ثبت‌شده از طریق UI (`jira.meta.SalesLevel`) و API (`SalesLevel`).

### ۲. ماتریس قوانین بیزینسی (Business Logic)
- نقش **HQ**: مجاز به ارجاع به تمام سطوح (`HQ`, `Region1`, `Region2`).
- نقش **Region1 / Region2**: فقط مجاز به تعامل با هم‌تیمی‌های همان منطقه.
- گروه **jira-administrators**: مستثنی (Bypass) از قوانین محدودکننده.

### ۳. اسکریپت‌های لینوکس و زیرساخت
- **`linux_health_and_backup.sh`**: مانیتورینگ فضای دیسک و وضعیت کانتینرهای فعال Docker در سرورهای لینوکس.
