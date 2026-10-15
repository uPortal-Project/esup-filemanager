"""Combine Playwright recordings in execution order, with a title before each clip."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import textwrap


def ffmpeg(*arguments):
    subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', *arguments], check=True)


def recordings(suites, parents=()):
    for suite in suites:
        titles = (*parents, suite['title'])
        for spec in suite.get('specs', []):
            title = ' / '.join((*titles, spec['title']))
            for test in spec['tests']:
                for result in test['results']:
                    videos = [item for item in result.get('attachments', [])
                              if item.get('contentType') == 'video/webm' and item.get('path')]
                    for index, video in enumerate(videos, 1):
                        label = f"{title}\n{result['status']} | attempt {result['retry'] + 1} | video {index}/{len(videos)}"
                        yield result['startTime'], label, Path(video['path'])
        yield from recordings(suite.get('suites', []), titles)


def main():
    output = Path(os.environ.get('E2E_OUTPUT', '/results'))
    report = json.loads((output / 'report.json').read_text())
    clips = sorted(recordings(report['suites']), key=lambda item: item[0])
    if not clips:
        raise RuntimeError('No test recordings found for the campaign video')

    with tempfile.TemporaryDirectory(prefix='efm-video-') as directory:
        temporary = Path(directory)
        playlist = []
        for index, (_, label, video) in enumerate(clips, 1):
            title = temporary / f'{index}-title.txt'
            title.write_text(f'{index}/{len(clips)}\n\n' + '\n'.join(
                textwrap.fill(line, width=65) for line in label.splitlines()))
            card = temporary / f'{index}-title.webm'
            ffmpeg('-f', 'lavfi', '-i', 'color=c=0x162333:s=800x450:r=25:d=2',
                   '-vf', f'drawtext=textfile={title}:expansion=none:fontcolor=white:fontsize=20:'
                          'x=(w-text_w)/2:y=(h-text_h)/2',
                   '-an', '-c:v', 'libvpx', '-threads', '2', str(card))
            # Generated filenames avoid quoting test names in FFmpeg's concat format.
            recording = temporary / f'{index}-recording.webm'
            recording.symlink_to(video.resolve())
            playlist.extend((f"file '{card}'", f"file '{recording}'"))

        listing = temporary / 'clips.txt'
        listing.write_text('\n'.join(playlist) + '\n')
        destination = output / 'campaign.mp4'
        ffmpeg('-f', 'concat', '-safe', '0', '-i', str(listing),
               '-vf', 'scale=800:450:force_original_aspect_ratio=decrease,'
                      'pad=800:450:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=25',
               '-an', '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '23',
               '-pix_fmt', 'yuv420p', '-threads', '2', '-movflags', '+faststart', str(destination))
    print(f'Campaign video: {destination}', flush=True)


if __name__ == '__main__':
    main()
