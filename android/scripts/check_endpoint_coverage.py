"""Compare Express registrations with Retrofit declarations, including generated routers."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[2]
server = (root / 'Backend/server.js').read_text(encoding='utf-8')
api = (root / 'android/app/src/main/java/com/gastrack/app/data/api/GasTrackApi.kt').read_text(encoding='utf-8')
declared = [(m, '/' + p.strip('/')) for m, p in re.findall(r'@(GET|POST|PUT|DELETE|PATCH)\("([^"]+)"\)', api)]

def canonical(path):
    return re.sub(r'\{[^}]+\}|:[A-Za-z]+|\$\{[^}]+\}', '{}', path.rstrip('/') or '/')

mounts = re.findall(r'app\.use\("(/[^"]+)",\s*([A-Za-z]+)\)', server)
imports = dict(re.findall(r'const\s+(\w+)\s*=\s*require\("\./routes/([^"]+)"\)', server))
mounts.append(('/user-settings', 'userSettingsRoutes'))
imports['userSettingsRoutes'] = 'userSettingsRoutes'
expected = {('GET', '/health')}
for prefix, variable in mounts:
    if variable not in imports:
        continue
    source = (root / 'Backend/routes' / (imports[variable] + '.js')).read_text(encoding='utf-8')
    for method, suffix in re.findall(r'router\.(get|post|put|patch|delete)\(\s*[\'"`]([^\'"`]+)[\'"`]', source):
        expected.add((method.upper(), prefix + (suffix.rstrip('/') if suffix != '/' else '')))
    if prefix in ('/brands', '/categories'):
        expected.update((method, prefix + path) for method, path in [('GET',''),('POST',''),('PUT','/{id}'),('DELETE','/{id}')])
    if prefix == '/suppliers':
        expected.update([('POST','/suppliers/types'),('PUT','/suppliers/types/{typeId}')])
    if prefix == '/user-settings':
        expected.update([('POST','/user-settings/{kind}'),('PUT','/user-settings/{kind}/{id}'),('DELETE','/user-settings/{kind}/{id}')])

actual = {(method, canonical(path)) for method, path in declared}
missing = sorted((method, path) for method, path in expected if (method, canonical(path)) not in actual)
for method, path in missing:
    print('MISSING', method, path)
print(f'{len(expected)} backend route patterns; {len(declared)} Retrofit declarations; {len(missing)} missing patterns.')
raise SystemExit(bool(missing))
