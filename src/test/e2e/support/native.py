"""Independent storage oracle. Requests use storage protocols, never application APIs."""
import base64
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import stat
import sys
from urllib.parse import quote, unquote, urlparse
import xml.etree.ElementTree as ET

import boto3
from botocore.config import Config
import paramiko
import requests
import smbclient


class Storage:
    def __init__(self, drive):
        self.drive = drive
        self.directories = set()
        if drive == 's3':
            self.s3 = boto3.client('s3', endpoint_url='http://s3:9000',
                aws_access_key_id='minioadmin', aws_secret_access_key='minioadmin',
                region_name='us-east-1', config=Config(connect_timeout=5, read_timeout=30,
                retries={'max_attempts': 1}, s3={'addressing_style': 'path'}))
        elif drive == 'sftp':
            transport = paramiko.Transport(('sftp', 2222))
            transport.connect(username='sftpuser', password='sftppass')
            self.sftp = paramiko.SFTPClient.from_transport(transport)
        elif drive == 'samba':
            smbclient.register_session('samba', username='sambauser', password='sambapass', connection_timeout=5)
        elif drive == 'webdav':
            self.http = requests.Session()
            self.http.auth = ('webdav', 'webdav')

    def local(self, path):
        return '/data/e2e-local/' + path

    def unc(self, path):
        return '\\\\samba\\testshare\\' + path.replace('/', '\\')

    def url(self, path):
        return 'http://webdav/' + quote(path, safe='/')

    def mkdir(self, path):
        if path in self.directories:
            return
        if self.drive == 'tmp':
            Path(self.local(path)).mkdir(parents=True, exist_ok=True)
            Path(self.local(path)).chmod(0o777)
        elif self.drive == 's3':
            self.s3.put_object(Bucket='esup-files', Key=path.rstrip('/')+'/', Body=b'')
        elif self.drive == 'samba':
            smbclient.makedirs(self.unc(path), exist_ok=True)
        elif self.drive == 'sftp':
            try:
                self.sftp.mkdir(path)
            except OSError:
                if not stat.S_ISDIR(self.sftp.stat(path).st_mode):
                    raise
        else:
            response = self.http.request('MKCOL', self.url(path), timeout=10)
            if response.status_code not in (201, 405):
                response.raise_for_status()

        self.directories.add(path)

    def parents(self, path):
        if self.drive == 's3':
            return
        parts = path.strip('/').split('/')
        for index in range(1, len(parts)+1):
            self.mkdir('/'.join(parts[:index]))

    def write(self, path, data):
        self.parents(str(PurePosixPath(path).parent))
        if self.drive == 'tmp':
            Path(self.local(path)).write_bytes(data)
            Path(self.local(path)).chmod(0o666)
        elif self.drive == 's3':
            self.s3.put_object(Bucket='esup-files', Key=path, Body=data)
        elif self.drive == 'samba':
            with smbclient.open_file(self.unc(path), mode='wb') as output:
                output.write(data)
        elif self.drive == 'sftp':
            with self.sftp.file(path, 'wb') as output:
                output.write(data)
        else:
            self.http.put(self.url(path), data=data, timeout=30).raise_for_status()

    def read(self, path):
        if self.drive == 'tmp':
            return Path(self.local(path)).read_bytes()
        if self.drive == 's3':
            return self.s3.get_object(Bucket='esup-files', Key=path)['Body'].read()
        if self.drive == 'samba':
            with smbclient.open_file(self.unc(path), mode='rb') as source:
                return source.read()
        if self.drive == 'sftp':
            with self.sftp.file(path, 'rb') as source:
                return source.read()
        response = self.http.get(self.url(path), timeout=30)
        response.raise_for_status()
        return response.content

    def entries(self, path):
        if self.drive == 'tmp':
            return [(p.name, p.is_dir()) for p in Path(self.local(path)).iterdir()]
        if self.drive == 's3':
            prefix = path.rstrip('/') + '/'
            result = {}
            for page in self.s3.get_paginator('list_objects_v2').paginate(Bucket='esup-files', Prefix=prefix):
                for entry in page.get('Contents', []):
                    tail = entry['Key'][len(prefix):]
                    if tail:
                        result[tail.split('/')[0]] = '/' in tail
            return list(result.items())
        if self.drive == 'samba':
            return [(e.name, e.is_dir()) for e in smbclient.scandir(self.unc(path))]
        if self.drive == 'sftp':
            return [(e.filename, stat.S_ISDIR(e.st_mode)) for e in self.sftp.listdir_attr(path)]
        response = self.http.request('PROPFIND', self.url(path.rstrip('/')+'/'), headers={'Depth': '1'}, timeout=10)
        response.raise_for_status()
        result = []
        for element in ET.fromstring(response.content).findall('{DAV:}response'):
            href = unquote(urlparse(element.findtext('{DAV:}href')).path).rstrip('/')
            if href == '/'+path.rstrip('/'):
                continue
            result.append((href.rsplit('/', 1)[-1], element.find('.//{DAV:}collection') is not None))
        return result

    def snapshot(self, path):
        result = {}
        if self.drive == 's3':
            # Preserve file/prefix collisions and empty directory markers without merging names.
            prefix = path.rstrip('/') + '/'
            for page in self.s3.get_paginator('list_objects_v2').paginate(Bucket='esup-files', Prefix=prefix):
                for entry in page.get('Contents', []):
                    name = entry['Key'][len(prefix):]
                    if not name:
                        continue
                    parts = name.rstrip('/').split('/')
                    for index in range(1, len(parts)):
                        result['/'.join(parts[:index])+'/'] = {'type': 'directory'}
                    if name.endswith('/'):
                        result[name] = {'type': 'directory'}
                    else:
                        data = self.read(entry['Key'])
                        result[name] = {'type': 'file', 'size': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
            return result
        for name, directory in self.entries(path):
            child = path.rstrip('/')+'/'+name
            if directory:
                result[name+'/'] = {'type': 'directory'}
                for key, value in self.snapshot(child).items():
                    result[name+'/'+key] = value
            else:
                data = self.read(child)
                result[name] = {'type': 'file', 'size': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
        return result

    def remove(self, path):
        if self.drive == 's3':
            keys = [path]
            for page in self.s3.get_paginator('list_objects_v2').paginate(Bucket='esup-files', Prefix=path.rstrip('/')+'/'):
                keys.extend(entry['Key'] for entry in page.get('Contents', []))
            for key in keys:
                self.s3.delete_object(Bucket='esup-files', Key=key)
        elif self.drive == 'webdav':
            response = self.http.delete(self.url(path), timeout=30)
            if response.status_code != 404:
                response.raise_for_status()
        else:
            for name, directory in self.entries(path):
                child = path+'/'+name
                if directory:
                    self.remove(child)
                elif self.drive == 'tmp':
                    Path(self.local(child)).unlink()
                elif self.drive == 'samba':
                    smbclient.remove(self.unc(child))
                else:
                    self.sftp.remove(child)
            if self.drive == 'tmp':
                Path(self.local(path)).rmdir()
            elif self.drive == 'samba':
                smbclient.rmdir(self.unc(path))
            else:
                self.sftp.rmdir(path)


def execute(request):
    drive, path = request['drive'], request['path']
    # All mutations and reads are confined to the current campaign namespace.
    root = 'e2e/' + os.environ['E2E_RUN']
    if path != root and not path.startswith(root+'/'):
        raise ValueError('Path outside campaign namespace')
    if '\\' in path or any(part in ('..', '.', '') for part in path.split('/')):
        raise ValueError('Unsafe native path')
    storage = Storage(drive)
    action = request['action']
    if action == 'prepare':
        storage.parents(path)
        for name, value in request['entries'].items():
            relative = PurePosixPath(name)
            if '\\' in name or relative.is_absolute() or '..' in relative.parts:
                raise ValueError('Unsafe fixture name')
            if value is None:
                if drive == 's3':
                    # Nonempty S3 folders are inferred from keys; only empty folders need markers.
                    prefix = name.rstrip('/')+'/'
                    if not any(other != name and other.startswith(prefix) for other in request['entries']):
                        storage.mkdir(path+'/'+name.rstrip('/'))
                else:
                    storage.parents(path+'/'+name.rstrip('/'))
            else:
                storage.write(path+'/'+name, base64.b64decode(value))
        if drive == 's3':
            # Keep the test directory after deleting its last child, without rewriting every ancestor.
            storage.mkdir(path)
        return storage.snapshot(path)
    if action == 'snapshot':
        return storage.snapshot(path)
    if action == 'protect':
        if drive == 'tmp':
            Path(storage.local(path)).chmod(0o555)
        elif drive == 'sftp':
            storage.sftp.chmod(path, 0o555)
        else:
            raise ValueError('POSIX write protection is available only for local and SFTP fixtures')
        return True
    if action == 'remove':
        storage.remove(path)
        return True
    raise ValueError('Unknown native action')


if __name__ == '__main__':
    print(json.dumps(execute(json.load(sys.stdin)), ensure_ascii=False))
