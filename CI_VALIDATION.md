# Java 8 CI validation

The workflow runs on draft PRs too. Pushes to `security/**` also trigger it, so the
first run does not require installing a workflow on master or marking a PR ready.
It uses Temurin 8 and the repository's Maven 3.6.3 wrapper. No deployment, secrets,
write token, native OCR installation or live translation requests are used.

## Evidence and interpretation

- `clean verify` compiles and packages the service and executes JUnit 4 tests.
  All four current test suites must run, with at least nine total tests and no
  skipped tests. Failures are not ignored; later evidence steps still run.
- `dependency-tree.txt` includes verbose Maven mediation; `version-conflicts.txt`
  lists omitted conflicting versions. These are review evidence, not automatically
  failing convergence rules: imposing convergence on this legacy BOM would mix a
  dependency upgrade into the CI change.
- The resolved graph and executable jar library names must exclude Fastjson,
  Commons FileUpload and Springfox Swagger UI. This is not a shaded-class scan.
- `osv-results.json` records OSV matches for resolved Maven versions, including
  test dependencies. Existing findings produce a warning rather than suppressing
  them or claiming a secure build. OSV request errors fail the step. OSV matching
  does not determine severity, reachability or actual deployed exposure, and does
  not cover native/system libraries. Review advisory records and fixes separately.
- Logs, Surefire reports, dependency and jar inventories are retained as Actions
  artifacts for 14 days, even on failure. A green CI result is build/regression
  evidence, not approval to deploy the obsolete Boot 2.0 stack.

## Still required before merge

On an isolated machine matching the intended native environment, record actual
JDK, Tesseract/Leptonica, Tess4J and language-data versions. Test trusted PNG and
JPEG samples and compare expected text (including the RGB PNG conversion).
Exercise OCR failure and confirm generated `ocr-*.png` files are deleted on both
success and failure. Check every enabled language and configured tessdata path.
The unit tests validate image decoding and bounds but do not invoke native OCR.

Translation fixture tests validate JSON response shape only. Separately exercise
Nashorn token generation and the real upstream endpoint with harmless text,
timeouts/proxy settings, failure responses and concurrent requests. The unofficial
Google endpoint may fail independently of a successful build. If local Excel
access is deliberately enabled, test controlled fixture read/write as well.

Review remaining advisories in Spring/Tomcat/Jackson, POI/XML/ZIP, H2, drivers,
HttpClient and native dependencies against `SECURITY_REVIEW.md`. Do not silently
upgrade the framework or relax these tests to get a green run.
