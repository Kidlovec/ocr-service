# ocr-service security review — 2026-10-30

Baseline: master `51cce9e19fa59af6f00293992694b5268e0759b0` (last commit 2020-03-27). Review is static source analysis, not proof of deployment exposure or a penetration test. No online service was contacted or changed. No PR was merged.

## Decision

The README explicitly calls this a toy. There is no deployment configuration or CI in the checked-out tree, and no evidence here establishes whether it is running elsewhere. If unused and no downstream consumers rely on it, retaining the source as a read-only archive is the lowest-maintenance option. First confirm no VM/container/process, scheduled job, published image, downstream caller or copied deployment still runs it. Archiving GitHub does not stop or patch any such deployment, remove vulnerable artifacts, or erase existing alerts. Do not merely suppress alerts while an instance remains active.

If still useful, this patch is local-demo containment, not production certification. A supported-framework migration is required before Internet exposure.

## Reachability and severity

| Area | Evidence in baseline | Assessment |
| --- | --- | --- |
| Upload path traversal / overwrite | `OcrController.getText` writes `UPLOAD_PATH + file.getOriginalFilename()` before validating content; `convert` separately opens the raw original filename with FileOutputStream | Concrete source-level vulnerability if upload endpoints are reachable and target files are writable. Traversal can escape the upload directory; non-PNG branch can overwrite a chosen path in the process working directory or an absolute path. OCR failure occurs after the write. No exploitation performed. |
| Server-side Excel file access | GET `/translate-local/?path=...` passes arbitrary path to EasyExcel.read and then EasyExcel.write; no application authentication | Concrete unrestricted server-path operation, limited to accessible/parseable Excel and filesystem permissions. Not a claim of arbitrary-byte file read or generic RCE. Disable by default. |
| Resource exhaustion | 200 MB/file, 600 MB/request; getBytes; unbounded batch count, decoded pixels and native OCR work; no rate limit | Reachable application-level memory/disk/CPU risk if exposed. Temporary files were not deleted. Patch adds bounds and cleanup; container multipart parsing occurs before controller checks, so these checks do not fix Tomcat parser vulnerabilities or concurrency exhaustion. |
| Commons FileUpload 1.3.3 | Declared directly, but no `org.apache.commons.fileupload` calls, `CommonsMultipartResolver`, or custom multipart resolver found anywhere in source | The standalone Commons parser route is not demonstrated. Boot's standard Servlet multipart resolver delegates to embedded Tomcat, so this is not a safe-upload conclusion. Tomcat has its own forked parser and independent CVEs. Remove unused direct dependency, then verify resolved tree. |
| Fastjson 1.2.58 | Only application use is `JSONArray.parse(result)` in GoogleApi; result comes from fixed HTTPS Google Translate URLs through HttpClient. No Fastjson MVC converter, RequestBody JSON parser, AutoType enabling or application Java-serialization entry found | Parser is genuinely invoked, so not declaration-only. Ordinary clients control translation text, not the complete upstream JSON response; no direct attacker-JSON-to-Fastjson path demonstrated. Compromised/untrusted upstream responses or changed deployment code would change that assessment. No demonstrated RCE. Replace with existing Jackson tree API without polymorphic typing. |
| Swagger UI 2.9.2 | Runtime UI dependency and unconditional `@EnableSwagger2`; README points to `/swagger-ui.html` | Browser-side CSS injection / XSS advisory, not demonstrated server-side Java RCE. Requires reachable UI and relevant malicious document/browser interaction path. Static metadata is not proof it is exploitable or safe. Remove vulnerable UI asset dependency; gate Swagger JSON config off by default. |
| Authentication / CORS | No Spring Security dependency/config in tree; controller has `@CrossOrigin` | Source provides no access control. Actual reverse proxy/network policy is unknown. Cross-origin annotation does not itself prove exploitation. Default loopback binding reduces direct network exposure but does not authenticate callers or stop forwarding. |

## Dependabot PR compatibility

- PR #1 only changes fastjson 1.2.58 to 1.2.83. This keeps the package/API used by the current JSONArray code and is plausibly low source-compatibility impact on Java 8, but has no current build/smoke-test evidence. It is not sufficient security remediation in 2026: the maintainer now lists CVE-2026-16723 affecting 1.2.68–1.2.83 under Boot executable-jar conditions. The live maintainer wiki describes 1.2.84 and SafeMode/noneautotype alternatives; older advisory snapshots disagree on patched availability. This patch avoids relying on those differences by removing the application dependency and parse call. Do not call 1.2.83 a safe final target.
- PR #2 only changes Commons FileUpload 1.3.3 to 1.5. There are no application API calls to break, but it remains affected by CVE-2025-48976. Also the 1.5 file-count limit introduced for CVE-2023-24998 must be explicitly enabled; a version bump alone is not a configured limit. Removing this unused declaration is preferable. If a future caller needs Commons FileUpload, use the then-current patched compatible 1.x release and configure limits; switching to 2.x is a separate migration.
- Both PRs remain open and untouched. Mergeable means no Git merge conflict, not runtime compatibility. This patch supersedes their intended changes if validated and accepted; do not close them before that decision.

## Additional stack constraints and residual risks

- Boot 2.0.0 manages Spring Framework 5.0.4 and embedded Tomcat 8.5.28 (BOM baseline, not a resolved dependency inventory). Those are obsolete. Tomcat's multipart parser is affected by the part-count DoS described in CVE-2023-24998 (fixed in 8.5.85); the external commons-fileupload jar change does not replace Tomcat's internal parser. Exact packaged versions and all subsequent advisories must be checked from a successful dependency tree/SBOM.
- Boot 2.x OSS support ended with 2.7.18 in November 2023. Moving to 2.7.18 may be a migration bridge, not a supported free-security endpoint. Avoid overriding Spring/Tomcat/Jackson versions piecemeal without compatibility tests.
- Java 8 is a source/target declaration, not proof of an unpatched JDK installation. Identify actual vendor, update number and support channel. Legacy build should use a maintained vendor's patched JDK 8. Boot 2.0 documents Java 8/9, not JDK 17; old Lombok/ASM/reflection may fail on 17.
- GoogleApi uses `ScriptEngineManager.getEngineByName("javascript")` and evaluates bundled gettk.js. Nashorn was removed in JDK 15. A Java 17 migration must replace or explicitly provide the engine; changing java.version alone will break translation. GoogleApi shares mutable Browser URL and ScriptEngine state across requests; concurrency correctness needs separate work. The unofficial Google Translate endpoint may no longer work and must be smoke-tested.
- Boot 3 requires Java 17 and Jakarta namespace changes. Migrate via a tested 2.7 bridge, replace Springfox with a compatible maintained OpenAPI solution, review javax validation/persistence imports and the third-party graceful-shutdown integration, and replace the old JS engine use. Select a supported Boot release at implementation time.
- Spring4Shell CVE-2022-22965 is not automatically exploitable merely because Spring is old: the published exploit requires JDK 9+ and traditional Tomcat WAR deployment. The declared Java 8 / default executable-jar baseline does not establish those conditions; real deployment remains unknown.
- Tess4J 3.2.1, native Tesseract/Leptonica, EasyExcel 2.1.6 and its POI/XML/ZIP dependencies, H2/MySQL drivers, HttpClient and old Jackson remain further dependency-review targets. This review does not assert they are clean. H2 console exposure is not enabled in the checked-in properties. No log4j-core usage is established; do not infer Log4Shell from a generic logging bridge.
- Image bounds do not sandbox native image/OCR parsers. Run with least privilege, read-only application filesystem, constrained writable temp space, bounded concurrency, timeouts and current native libraries if retaining the service.

## Patch behavior changes

1. Remove direct Commons FileUpload, Fastjson and Swagger UI declarations; exclude the first two from EasyExcel's dependency path. Full-tree verification still required.
2. Parse Google translation arrays using existing Jackson `JsonNode` API, rejecting unexpected shapes; never enable default typing.
3. Decode only PNG/JPEG, check dimensions before allocating the decoded image, enforce file/batch/request bounds, use generated temporary filenames, and delete on OCR success/failure.
4. Bind to loopback by default; deny local Excel path operations with HTTP 403 unless explicitly enabled for trusted local use. Swagger JSON config defaults off and UI is absent.
5. Make the pre-existing JUnit 4 context test discoverable and add parser, image validation and default Excel denial regressions. No framework upgrade mixed into containment patch.

## Validation evidence and release gates

- `./mvnw -B test` on baseline and `./mvnw -B verify` on patched source both stop before Maven starts: `UnknownHostException: repo.maven.apache.org` while obtaining Maven 3.6.3. This is not a compilation/test failure and not a test pass. Host has OpenJDK 17.0.20; no Maven binary or JDK 8 installed.
- `git diff --check` passed. POM is well-formed XML. Source review performed. No resolved dependency tree, SBOM scan, runnable package or native OCR/translation smoke test was possible locally.
- Before merging: run `./mvnw -B clean verify` with patched JDK 8 and Maven Central access; inspect test counts (context plus regressions), dependency tree and packaged jar for all transitive Fastjson/FileUpload/UI copies. Rescan all resolved dependencies. Run PNG/JPEG OCR success/failure/cleanup tests with compatible native libraries and languages, check disabled endpoints, and exercise translation against a controlled fixture followed by a real upstream smoke test. Compare output with baseline on trusted sample files.
- Before any deployment: resolve obsolete-framework/container risks, authenticate and rate-limit requests, inventory actual environment/clients, validate new size/bind defaults, and obtain owner's explicit approval. This draft patch alone is not deployable production hardening.

## Primary references

- https://commons.apache.org/proper/commons-fileupload/security.html
- https://tomcat.apache.org/security-8.html
- https://github.com/advisories/GHSA-c427-hjc3-wrfw
- https://github.com/alibaba/fastjson2/wiki/Security-Advisory:-Remote-Code-Execution-in-fastjson-1.2.68%E2%80%931.2.83
- https://github.com/advisories/GHSA-crf3-v9rr-v7hj
- https://docs.spring.io/spring-boot/docs/2.0.0.RELEASE/reference/htmlsingle/
- https://spring.io/blog/2023/11/23/spring-boot-2-7-18-available-now/
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide
- https://spring.io/security/cve-2022-22965/
- https://openjdk.org/jeps/372
