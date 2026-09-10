# Playwright UI Testing Framework
## Developed by Ali H. Hussein

A data-driven, keyword-based UI test automation framework built with **Java**, **Microsoft Playwright**, and **TestNG**. Test cases are written in plain English inside Excel sheets — no coding required for testers.

---

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
- [Configuration](#configuration)
- [Excel Test Case Format](#excel-test-case-format)
- [Supported Test Step Keywords](#supported-test-step-keywords)
- [How to Write Test Case Steps](#how-to-write-test-case-steps)
- [How It Works](#how-it-works)
- [Running Tests](#running-tests)
- [Output & Reports](#output--reports)
- [Validation Engine](#validation-engine)
- [Performance & Timing Report](#performance--timing-report)
- [Module Reference](#module-reference)

---

## Overview

This framework allows QA engineers to define test scenarios in a structured Excel file using plain-English step descriptions. The framework parses these steps, maps them to UI action handlers, executes them via Playwright, captures screenshots at each step, and generates a Word document report per test case.

Key capabilities:
- **Data-driven execution** — all test cases live in an Excel file
- **Keyword-driven steps** — plain English commands like `"Go to Business CRM/Individuals"` or `"Press Save"`
- **Database-backed navigation** — menu paths are resolved dynamically through a SQL Server table
- **Auto screenshots** — a screenshot is taken after every step
- **Word report generation** — each test case produces a `.docx` report with steps and screenshots
- **DB validation** — optional SQL-based assertions after test execution
- **Multi-browser support** — Chromium, Firefox, WebKit

---

## Technology Stack

| Component       | Library / Version              |
|-----------------|-------------------------------|
| Browser Automation | Microsoft Playwright 1.41.0 |
| Test Runner     | TestNG 7.8.0                  |
| Build Tool      | Maven                         |
| Language        | Java 11                       |
| Excel Handling  | Apache POI 5.2.3 (OOXML)     |
| Database        | Microsoft SQL Server (JDBC 12.4.1) |
| Word Reports    | Apache POI XWPFDocument       |
| Logging         | java.util.logging + SLF4J     |

---

## Project Structure

```
playwright-uitesting/
├── pom.xml                              # Maven dependencies & build config
├── testng.xml                           # TestNG suite definition
├── src/
│   ├── main/
│   │   ├── java/uitesting/
│   │   │   ├── core/
│   │   │   │   ├── BrowserFactory.java         # Playwright browser init/teardown
│   │   │   │   ├── LoadingPageHandler.java      # Page load & network idle waits
│   │   │   │   └── TestStepProcessor.java       # Keyword router — dispatches steps to handlers
│   │   │   ├── handlers/
│   │   │   │   ├── NavigationHandler.java       # "Go to X/Y/Z" — DB-driven menu navigation
│   │   │   │   ├── ButtonHandler.java           # "Press [button]"
│   │   │   │   ├── InsertHandler.java           # "Insert [value] in [field]"
│   │   │   │   ├── DropdownHandler.java         # "Select [option] from [dropdown]"
│   │   │   │   ├── EnableToggleButtonHandler.java # "Enable/Disable [toggle]"
│   │   │   │   ├── GroupHandler.java            # "In Group [name] ..."
│   │   │   │   ├── EditFilterHandler.java       # "In [field] Edit Filter ..."
│   │   │   │   ├── AdvanceFilterValueHandler.java # "In Advanced Filter set [field] to [value]"
│   │   │   │   ├── DragAndDropHandler.java      # "Drag [element] to [target]"
│   │   │   │   ├── UploadHandler.java           # "Upload [file] to [field]"
│   │   │   │   ├── ExportHandler.java           # "Export [format]"
│   │   │   │   ├── DrillDownHandler.java        # "Drill [down/up] on [field]"
│   │   │   │   ├── UnderRowHandler.java         # "Under [row] Press/Insert/Select ..."
│   │   │   │   ├── BPHandler.java               # "Business Process [action]"
│   │   │   │   ├── View360Handler.java          # "View 360 [entity]"
│   │   │   │   ├── SectionHandler.java          # "In section [name] Press [button]"
│   │   │   │   ├── WaitWorkOrderHandler.java    # "Wait Work Order [status]"
│   │   │   │   ├── DatabaseQueriesAction.java   # "Run Database Query"
│   │   │   │   └── MiscHandlers.java            # Miscellaneous / placeholder handlers
│   │   │   ├── utils/
│   │   │   │   ├── ConfigManager.java           # Singleton config reader (application.properties)
│   │   │   │   ├── ExcelReader.java             # Reads test cases from .xlsx
│   │   │   │   ├── DatabaseHelper.java          # SQL Server query execution & result writing
│   │   │   │   ├── ScreenshotHelper.java        # Captures and saves screenshots per step
│   │   │   │   ├── WordReportGenerator.java     # Generates .docx test reports with screenshots
│   │   │   │   └── PerformanceReportGenerator.java  # Generates .docx performance & timing report for the full suite
│   │   │   └── validation/
│   │   │       └── ValidationEngine.java        # Validates test results via DB or file checks
│   │   └── resources/
│   │       └── application.properties           # All runtime configuration
│   └── test/
│       └── java/uitesting/
│           └── TestRunner.java                  # TestNG entry point (runAllTestCases / runSingleTestCase)
```

---

## Prerequisites

- Java 11+
- Maven 3.6+
- Microsoft SQL Server (for navigation table and optional validation)
- The application under test accessible at a known URL

---

## Setup & Installation

**1. Clone the repository**
```bash
git clone <repo-url>
cd playwright-uitesting
```

**2. Install Playwright browsers**
```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

**3. Configure the framework** — edit `src/main/resources/application.properties` (see below).

**4. Prepare your test Excel file** at the path specified in `test.excel.file`.

**5. Create the screenshots and report output folders** (e.g., `C:/Framework/Screenshots`, `C:/Framework/WordReports`).

---

## Configuration

All settings are managed in `src/main/resources/application.properties`:

```properties
# ── Browser ──────────────────────────────────────────
browser.type=chromium          # chromium | firefox | webkit
browser.headless=false         # true to run silently
browser.slow_mo=0              # ms delay between actions (useful for debugging)
browser.timeout=30000          # default element wait timeout (ms)

# ── Application ──────────────────────────────────────
app.base.url=http://192.168.110.137:9072

# ── Database (SQL Server) ─────────────────────────────
db.server=192.168.110.135
db.port=1433
db.name=ToneV2testing
db.username=sa
db.password=yourpassword
db.encrypt=false
db.trust.server.certificate=true

# ── Test Files ────────────────────────────────────────
test.excel.file=C:/Framework/TestCases.xlsx
test.manual.file=C:/Framework/Test.docx
test.validation.file=C:/Framework/ValidationResults.xlsx
test.screenshots.dir=C:/Framework/Screenshots

# ── Login Credentials ─────────────────────────────────
login.email=admin@example.com
login.password=yourpassword

# ── Navigation ────────────────────────────────────────
navigation.db.table=SideMenu   # DB table used to resolve menu paths
```

---

## Excel Test Case Format

The framework reads the **first sheet** of the Excel file. Row 1 is treated as a header and skipped.

| Column | Index | Field              | Description |
|--------|-------|--------------------|-------------|
| A      | 0     | TC ID              | Unique test case identifier (e.g., `TC001`) |
| E      | 4     | Title              | Human-readable test case name |
| F      | 5     | Description        | Optional description |
| G      | 6     | Test Steps         | Newline-separated list of step keywords |
| P      | 15    | Validation Flag    | `Implementation Validation`, `File Exported`, or `Search Validation` |
| Q      | 16    | Connection String  | Optional override JDBC URL for validation |
| R      | 17    | Validation Query   | SQL query or filename pattern for validation |
| S      | 18    | Executed Query     | SQL to pre-run before a "Run Database Query" step |

**Example Test Steps cell (column G):**
```
1-Login to CRM.
2-Go to Business CRM/Individuals.
3-Insert into Customer No.**12000017095.
4-Press on Search.
5-View 360.
6-Logout from CRM.
```

---

## Supported Test Step Keywords

The `TestStepProcessor` routes each step by matching keywords in the step string:

| Keyword Pattern | Handler | Example Step |
|----------------|---------|--------------|
| `Login` | Built-in | `Login` |
| `Logout` / `Log out` | Built-in | `Logout` |
| `Go to` | `NavigationHandler` | `Go to CRM/BusinessCRM` |
| `Press` | `ButtonHandler` | `Press Save` |
| `Insert` | `InsertHandler` | `Insert John in First Name` |
| `Select` | `DropdownHandler` | `Select Active from Status` |
| `Enable` / `Disable` | `EnableToggleButtonHandler` | `Enable Auto Renewal` |
| `In ... Edit Filter` | `EditFilterHandler` | `In Name Edit Filter contains Test` |
| `In Advanced Filter set` | `AdvanceFilterValueHandler` | `In Advanced Filter set Date from 01/01/2024` |
| `In Group` | `GroupHandler` | `In Group Services Press Add` |
| `Drag` | `DragAndDropHandler` | `Drag Row 1 to Row 3` |
| `Upload` | `UploadHandler` | `Upload invoice.pdf to Attachment` |
| `Export` | `ExportHandler` | `Export Excel` |
| `Drill` | `DrillDownHandler` | `Drill Down on Amount` |
| `Under` | `UnderRowHandler` | `Under Row 2 Press Delete` |
| `Business Process` / `Bussiness Process` | `BPHandler` | `Business Process Activate` |
| `View 360` | `View360Handler` | `View 360 Customer` |
| `section ... Press` | `SectionHandler` | `In section Details Press Edit` |
| `Wait Work Order` | `WaitWorkOrderHandler` | `Wait Work Order Completed` |
| `Run Database Query` | `DatabaseQueriesAction` | `Run Database Query` |

---

## How to Write Test Case Steps

Each test case step is a single plain-English line written in column G of the Excel sheet. Steps are separated by newlines. The framework reads each line, matches it against a keyword, and executes the corresponding UI action.

> **Golden rule:** Every step must end with a period `.` unless stated otherwise. The separator between a field label and its value is always `**` (double asterisk).

Below is the complete reference for every supported step type, with real examples.

---

### Login & Logout

Logs in using the credentials defined in `application.properties`. No parameters needed.

```
Login
Logout
```

Both `Logout` and `Log out` are accepted.

---

### Go to — Navigation

Navigates to a page using the side menu. The path segments must match the menu labels stored in the `SideMenu` database table, separated by `/`.

```
Go to Business CRM/Individuals.
Go to Billing/Invoices.
```

> The path lookup is **case-insensitive**.

---

### Press on — Button Click

Clicks any visible button, link, or action element by its label text.

**Simple button:**
```
Press on Search.
Press on Submit.
Press on Cancel.
Press on Edit.
```

**Button inside a grid's action menu:**
```
In the grid, Press on Approve.
In the grid, Press on Reject.
```

**Button associated with a specific field (labeled button):**
```
Press on Search Button for Customer Name.
Press on Clear Button for Phone Number.
```

**Show the filter bar:**
```
Press on Show Filter.
```

---

### Insert into — Text Field Input

Types a value into a labelled input field. The separator between the field name and the value is `**`.

**Standard text field:**
```
Insert into Customer No.**12000017095.
Insert into First Name.**John.
Insert into Description.**This is a test description.
```

**Date/time field:**
```
Insert Datetime Start Date.**01/01/2024.
Insert Datetime End Date.**31/12/2024 23:59.
```

**Multiple values into a tag/chip input (each value gets added with the + button):**
```
Insert & Add into Tags.**Tag1**Tag2**Tag3.
```

**Cell inside a grid (row and column are 1-based):**
```
Insert into grid row:1 & Column 2 with value 500.
Insert into grid row:3 & Column 4 with value Test Value.
```

---

### Select the — Dropdown Selection

Selects a value from a dropdown. The separator between the dropdown label and the value to select is `**`.

**Simple dropdown (opens list and picks by text):**
```
Select the Status**Active.
Select the Account Type**Individual.
Select the Currency**USD.
```

**Searchable dropdown (types to filter, then picks):**
```
Search & Select for Country**Lebanon.
Search & Select for Customer**Acme Corp.
```

**Searchable dropdown inside an Advanced Filter row (row is 1-based):**
```
Search & Select In Advanced Filter row 1 for Status**Active.
```

**Searchable dropdown inside a grid cell (row and column are 1-based):**
```
Search & Select into grid row:2 & Column 3 with value Lebanon.
```

---

### Enable / Disable — Toggle Switch

Flips a toggle switch on or off by its label. Use `**` to flip multiple toggles in one step.

**Single toggle:**
```
Enable the Auto Renewal.
Disable the Auto Renewal.
```

**Multiple toggles at once:**
```
Enable the Auto Renewal**Send Notifications.
```

**Toggle a specific product by name in the Products grid:**
```
Enable the Premium Package.
```

**Toggle inside a grid by row and column (1-based):**
```
Enable the in grid row:2 & Column:3.
Disable the in grid row:1 & Column:5.
```

---

### In Advanced Filter set — Advanced Filter Input

Sets a value inside an open Advanced Filter modal. Used after the Advanced Filter modal is already open.

**Text or date input:**
```
In Advanced Filter set to 01/01/2024.
In Advanced Filter set to Active.
```

**Dropdown input inside the modal (use `**` to separate choices):**
```
In Advanced Filter set to Lebanon**Beirut.
```

---

### In [Section], Edit Filter — Edit Filter Button

Clicks the "Edit Filter" button inside a named section on the page.

```
In Customer Info, press on Edit Filter.
In Orders, press on Edit Filter.
```

---

### In Group — Group Row Actions

Acts on a numbered group row. Groups are the repeated sections rendered by `ng-repeat='group in ctrl.groups'`.

**Press a button inside a specific group:**
```
1-In Group1, Press on Add.
2-In Group2, Press on Delete.
```

**Search & select a dropdown inside a group row:**
```
In Group 1 Search & Select row 1 for Service**Fiber.
In Group 2 Search & Select row 2 for Product**Premium Plan.
```

---

### Drag & Drop

Drags an element identified by its title to the configured target location.

```
Drag & Drop Column Header Name.
Drag & Drop Priority Rule.
```

> The drop target is a hardcoded XPath in `DragAndDropHandler.java`. Update it if your target container differs.

---

### Upload file — File Upload

Uploads a file from a local path to a file input on the page.

```
Upload file C:/Files/invoice.pdf.
Upload file C:/Attachments/contract.docx.
```

---

### Export — Grid Export

Clicks the grid cog icon and triggers the export action. No additional parameters are needed.

```
Export.
```

---

### Drill down — Expandable Row

Expands a collapsible/drill-down row in a grid by matching text in that row.

```
Drill down January.
Drill down 2024.
Drill down Services.
```

---

## How It Works

```
TestRunner (TestNG)
    │
    ├── Reads all TestCaseRows from Excel via ExcelReader
    │
    ├── For each TestCaseRow:
    │     ├── Launches browser via BrowserFactory
    │     ├── Creates a TestStepProcessor
    │     │
    │     └── For each step in the test case:
    │           ├── TestStepProcessor.process(step)  →  routes to correct Handler
    │           ├── LoadingPageHandler.waitForPageLoad() + waitForNetworkIdle()
    │           ├── ScreenshotHelper.capture(step)
    │           └── Stores step title + screenshot path
    │
    ├── WordReportGenerator generates a .docx per test case
    ├── PerformanceReportGenerator generates a suite-level .docx performance & timing report
    ├── ValidationEngine.validate() (optional, currently commented out)
    └── Results written back to Excel via ExcelReader.writeResults()
```

**Navigation** uses a SQL Server table (`SideMenu` by default) to resolve menu paths. The `NavigationHandler` queries the DB with the path segments from the step (e.g., `CRM/Accounts`) and clicks the matching menu items, handling any intermediate sub-menus automatically.

**Page Load Waiting** is handled by `LoadingPageHandler`, which detects both standard pages (network idle + DOM loaded) and CRM Billing pages (waits for Angular loading indicators and task editor overlays to disappear).

---

## Running Tests

**Run all test cases:**
```bash
mvn test
```

**Run a specific test method via TestNG:**
```bash
mvn test -Dtest=TestRunner#runAllTestCases
```

**Run a single test case** — edit `TestRunner.java` and set the target TC ID:
```java
String targetTcId = "TC002";  // change as needed
```
Then run:
```bash
mvn test -Dtest=TestRunner#runSingleTestCase
```

**Run with a different browser:**
```properties
# in application.properties
browser.type=firefox
```

**Run headlessly (CI/CD):**
```properties
browser.headless=true
```

---

## Output & Reports

After a test run, the following outputs are produced:

| Output | Location | Description |
|--------|----------|-------------|
| Screenshots | `test.screenshots.dir` | One PNG per step, named after the step text |
| Word Reports | `C:/Framework/WordReports/` | One `.docx` per test case with step headings and embedded screenshots |
| Performance Report | `C:/Framework/WordReports/` | One `.docx` for the full suite run with timing breakdowns (see below) |
| Validation Excel | `test.validation.file` | TC IDs with Pass/Fail results (when validation is enabled) |

---

## Performance & Timing Report

At the end of every suite run, `PerformanceReportGenerator` produces a single Word document (`PerformanceReport_<suffix>_<timestamp>.docx`) saved alongside the regular Word reports. It gives a full picture of how long the suite — and each individual step — took to execute.

**Report structure:**

- **Cover / Summary** — run date, total test cases, pass/fail counts, and total suite duration displayed as stat cards.
- **Summary Table** — one row per test case showing TC ID, title, start time, end time, total duration, and overall status (colour-coded green/red).
- **Per test case section** — detailed block for each TC containing:
  - Status, start time, end time, duration, and step count.
  - A "Slowest Step" callout highlighting the single most time-consuming step.
  - A step-by-step timing table with colour coding:
    - 🟥 Red row — step failed (threw an exception).
    - 🟨 Amber row — step exceeded the slow-step threshold (default: **5 seconds**).
    - 🟦 Light blue row — alternating row for readability.
    - ⬜ White row — normal passing step.

**Data model — collecting timings in `TestRunner`:**

```java
// Record each step
long start = System.currentTimeMillis();
try {
    processor.process(step);
    stepTimings.add(new PerformanceReportGenerator.StepTiming(step, System.currentTimeMillis() - start, true));
} catch (Exception e) {
    stepTimings.add(new PerformanceReportGenerator.StepTiming(step, System.currentTimeMillis() - start, false));
}

// After all TCs are done, generate the report
PerformanceReportGenerator perfGen = new PerformanceReportGenerator("C:/Framework/WordReports");
perfGen.generateReport(suiteStartTime, suiteEndTime, allTcTimings, "MySuite");
```

**Slow-step threshold** is defined as `SLOW_STEP_THRESHOLD_MS = 5000` (5 s) inside `PerformanceReportGenerator`. Adjust this constant to fit your application's expected response times.

---


## Module Reference

### `BrowserFactory`
Initialises the Playwright instance, launches the configured browser (Chromium / Firefox / WebKit), creates a browser context at 1920×1080 resolution, and exposes the `Page`. Also provides static helpers `isModalOpen()`, `getModalCount()`, and `isCRMBillingPage()`.

### `LoadingPageHandler`
Waits for the UI to stabilise after each action. For standard pages it waits for DOM content loaded and network idle. For CRM Billing pages it additionally waits for Angular `ng-show` loading overlays and task editor panels to disappear before proceeding.

### `TestStepProcessor`
The central keyword router. Receives a plain-English step string, matches it against keyword patterns (see table above), and delegates to the appropriate handler. All handlers are instantiated here and share the same `Page` instance.

### `ConfigManager`
Singleton that reads `application.properties` from the classpath. Provides typed getters for all configuration values (browser settings, URLs, DB credentials, file paths, login credentials).

### `ExcelReader`
Reads the test case Excel file using Apache POI. Maps fixed column indices to `TestCaseRow` fields. Also provides `writeResults()` to write Pass/Fail results back to a separate Excel file.

### `DatabaseHelper`
Manages SQL Server connections via JDBC. Provides `executeQuery()` for SELECT statements, `clearValidationTable()` to reset results before a suite run, and `insertValidationResult()` to record TC outcomes.

### `ScreenshotHelper`
Takes a full-page PNG screenshot after each step. Screenshots are named based on the sanitised step text and stored in the configured directory.

### `WordReportGenerator`
Generates a `.docx` Word document for each test case. The document contains a heading with the TC ID and title, followed by each step label as a sub-heading with its corresponding screenshot embedded below it.

### `PerformanceReportGenerator`
Generates a suite-level `.docx` performance and timing report after all test cases have run. The report includes a summary cover page with pass/fail statistics and total duration, a summary table across all test cases, and a dedicated per-TC section with a step-by-step timing table. Steps that exceeded the slow-step threshold (default 5 s) are highlighted in amber; failed steps are highlighted in red. The file is saved as `PerformanceReport_<suffix>_<timestamp>.docx` in the configured output directory. Internally uses two inner data classes — `StepTiming` (step name, duration, pass/fail) and `TestCaseTiming` (TC metadata + list of `StepTiming`) — which are populated by `TestRunner` during execution.

### `ValidationEngine`
Compares actual application state against expected values post-execution. Supports DB record count checks, downloaded file existence checks, and UI-vs-DB value comparison.

---
