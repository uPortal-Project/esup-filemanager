import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { Entries, Snapshot } from '../support/native';
export const hash = (data: Buffer) => createHash('sha256').update(data).digest('hex');
export const text = Buffer.from('Bonjour, monde ! Éà 東京\n', 'utf8');
export const binary = Buffer.from(Array.from({ length: 4096 }, (_, index) => index % 256));
export const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=', 'base64');
export const names = ["espace accent é 東京 ' & + # %.txt", 'empty.txt', 'binary.bin'];
// One representative selection for all copy/move scenarios.
export const transferEntries: Entries = {
    [names[0]]: text,
    'empty.txt': Buffer.alloc(0),
    'tree/empty/': null,
    'tree/nested/hello.txt': text,
    'tree/binary.bin': binary,
};
export function manifest(entries: Entries): Snapshot {
    const result: Snapshot = {};
    for (const [name, bytes] of Object.entries(entries)) {
        const parts = name.replace(/\/$/, '').split('/');
        for (let index = 1; index < parts.length; index++)
            result[parts.slice(0, index).join('/') + '/'] = { type: 'directory' };
        result[name] = bytes === null ? { type: 'directory' } : { type: 'file', size: bytes.length, sha256: hash(bytes) };
    }
    return result;
}
export function media(): Entries {
    const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'efm-media-'));
    try {
        execFileSync('ffmpeg', ['-v', 'error', '-f', 'lavfi', '-i', 'sine=frequency=440:duration=1', '-fflags', '+bitexact', '-flags:a', '+bitexact', path.join(temp, 'tone.mp3')]);
        execFileSync('ffmpeg', ['-v', 'error', '-f', 'lavfi', '-i', 'color=c=blue:s=64x64:d=1', '-c:v', 'libvpx', '-fflags', '+bitexact', '-flags:v', '+bitexact', path.join(temp, 'clip.webm')]);
        return { 'pixel.png': png, 'tone.mp3': fs.readFileSync(path.join(temp, 'tone.mp3')), 'clip.webm': fs.readFileSync(path.join(temp, 'clip.webm')) };
    }
    finally {
        fs.rmSync(temp, { recursive: true });
    }
}
