"""Wait for dependencies to be usable before starting browser tests."""
import os
from pathlib import Path
import sys
import time
import requests

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'support'))
from native import execute


def wait(name, probe):
    deadline = time.monotonic() + int(os.environ.get('STARTUP_TIMEOUT', '300'))
    last = None
    while time.monotonic() < deadline:
        try:
            probe()
            print(name + ': ready', flush=True)
            return
        except Exception as error:
            last = str(error)
            time.sleep(2)
    raise RuntimeError(name + ' did not become ready: ' + str(last))


def http(url, expected):
    response = requests.get(url, timeout=10, allow_redirects=False)
    assert response.status_code in expected, f'{url}: HTTP {response.status_code}'


# Match the writable local /tmp mount without running the application as root.
Path('/data/e2e-local').chmod(0o777)
for drive in ('tmp', 's3', 'samba', 'webdav', 'sftp'):
    wait(drive, lambda drive=drive: execute({
        'drive': drive,
        'path': 'e2e/' + os.environ['E2E_RUN'] + '/probe',
        'action': 'prepare',
        'entries': {'ready.txt': 'cmVhZHk='},
    }))
wait('CAS', lambda: http('http://cas:8080/cas/login', (200,)))
wait('application', lambda: http('http://app-e2e:8080/', (302, 303)))
