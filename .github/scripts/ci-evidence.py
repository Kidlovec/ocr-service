"""CI evidence; stdlib only. No native OCR or external translation calls."""
import json
import os
from pathlib import Path
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

REPORTS = Path('ci-reports')
REPORTS.mkdir(exist_ok=True)


def summary(text):
    print(text)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as handle:
            handle.write(text + '\n')


def dependencies():
    # dependency:list: group:artifact:type:[classifier:]version:scope
    text = (REPORTS / 'dependency-list.txt').read_text()
    result = set()
    for line in text.splitlines():
        fields = line.strip().split()[0].split(':') if line.strip() else []
        if len(fields) >= 5 and fields[-1] in ('compile', 'runtime', 'test', 'provided', 'system'):
            result.add((fields[0], fields[1], fields[-2]))
    if not result:
        raise RuntimeError('No resolved dependencies found')
    return sorted(result)


def tests():
    expected = {'DemoApplicationTests': 1, 'LocalExcelAccessTest': 1,
                'GoogleApiTest': 3, 'UploadImageTest': 4}
    found = {}
    total = 0
    for report in Path('target/surefire-reports').glob('TEST-*.xml'):
        suite = ET.parse(report).getroot()
        name = suite.attrib['name'].split('.')[-1]
        counts = {key: int(suite.get(key, '0')) for key in ('tests', 'failures', 'errors', 'skipped')}
        if counts['errors'] or counts['failures'] or counts['skipped']:
            raise RuntimeError('{}: {}'.format(name, counts))
        found[name] = counts['tests']
        total += counts['tests']
    for name, minimum in expected.items():
        if found.get(name, 0) < minimum:
            raise RuntimeError('Missing expected tests: ' + name)
    summary('JUnit: {} tests passed; all four expected suites ran. Native OCR and live translation are not tested.'.format(total))


def inventory():
    resolved = dependencies()
    forbidden = {('com.alibaba', 'fastjson'), ('commons-fileupload', 'commons-fileupload'),
                 ('io.springfox', 'springfox-swagger-ui')}
    bad = [':'.join(item) for item in resolved if item[:2] in forbidden]
    jars = list(Path('target').glob('*.jar'))
    if len(jars) != 1:
        raise RuntimeError('Expected one executable jar, found {}'.format(jars))
    with zipfile.ZipFile(jars[0]) as archive:
        libs = sorted(name for name in archive.namelist() if name.startswith('BOOT-INF/lib/') and name.endswith('.jar'))
        if not libs:
            raise RuntimeError('No BOOT-INF/lib dependencies found')
        (REPORTS / 'packaged-libraries.txt').write_text('\n'.join(libs) + '\n')
        bad += [name for name in libs if re.search(r'/(fastjson|commons-fileupload|springfox-swagger-ui)-', name)]
    if bad:
        raise RuntimeError('Removed dependencies remain: ' + ', '.join(bad))
    summary('Removed Fastjson/FileUpload/Swagger UI coordinates absent from resolved graph and packaged library names. Shaded copies are not ruled out. Review version-conflicts.txt; Maven mediation is not proof of runtime incompatibility.')


def osv():
    resolved = dependencies()
    queries = [{'package': {'ecosystem': 'Maven', 'name': g + ':' + a}, 'version': v}
               for g, a, v in resolved]
    request = urllib.request.Request('https://api.osv.dev/v1/querybatch',
                                     data=json.dumps({'queries': queries}).encode(),
                                     headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=90) as response:
        results = json.load(response)['results']
    evidence = [{'coordinate': ':'.join(dep), 'advisories': hit.get('vulns', [])}
                for dep, hit in zip(resolved, results)]
    (REPORTS / 'osv-results.json').write_text(json.dumps(evidence, indent=2) + '\n')
    affected = [item for item in evidence if item['advisories']]
    lines = ['OSV advisory inventory (not a severity or reachability assessment):']
    for item in affected:
        lines.append('- {}: {}'.format(item['coordinate'], ', '.join(v['id'] for v in item['advisories'])))
    lines.append('Affected coordinates: {} / {}. Known legacy stack advisories are reported, not suppressed or automatically fixed. A green build is not security approval.'.format(len(affected), len(resolved)))
    summary('\n'.join(lines))
    if affected:
        print('::warning::Remaining Maven advisories: review ci-reports/osv-results.json before merge')


if __name__ == '__main__':
    {'tests': tests, 'dependencies': inventory, 'osv': osv}[sys.argv[1]]()
