import { spawn } from 'node:child_process';
import path from 'node:path';
export const drives = ['tmp', 's3', 'samba', 'webdav', 'sftp'] as const;
export type Drive = typeof drives[number];
export type Entries = Record<string, Buffer | null>;
export type Snapshot = Record<string, {
    type: string;
    size?: number;
    sha256?: string;
}>;
export function native<T>(action: string, drive: Drive, directory: string, entries?: Entries): Promise<T> {
    return new Promise((resolve, reject) => {
        const child = spawn(process.env.E2E_PYTHON || 'python', [path.join(__dirname, 'native.py')], { stdio: 'pipe' });
        const output: Buffer[] = [], errors: Buffer[] = [];
        child.stdout.on('data', chunk => output.push(chunk));
        child.stderr.on('data', chunk => errors.push(chunk));
        child.on('error', reject);
        const timeout = setTimeout(() => { child.kill('SIGKILL'); reject(new Error(`Native oracle timed out: ${drive} ${action}`)); }, 90000);
        child.on('close', code => {
            clearTimeout(timeout);
            if (code !== 0)
                reject(new Error(`${drive} ${action} ${directory}: ${Buffer.concat(errors).toString()}`));
            else {
                try {
                    resolve(JSON.parse(Buffer.concat(output).toString()));
                }
                catch (error) {
                    reject(error);
                }
            }
        });
        child.stdin.end(JSON.stringify({ action, drive, path: directory,
            entries: entries && Object.fromEntries(Object.entries(entries).map(([key, value]) => [key, value?.toString('base64') ?? null])) }));
    });
}
