# esup-filemanager end-to-end tests

These tests use Chromium on desktop and touch-enabled Chromium/Android and
WebKit/iPhone profiles for file access. They check the files actually written to
storage. Docker and Docker Compose v2 are the only host requirements.

## Running the tests

From the repository root:

```bash
# Essential workflows: 28 tests.
bash src/test/e2e/scripts/run.sh smoke

# Complete suite: 98 tests, including smoke.
bash src/test/e2e/scripts/run.sh full
```

The script builds and starts an isolated stack, runs the tests, then removes its
containers and volumes. It publishes no ports and does not modify development
configuration files. HTML and JUnit reports, failure traces and logs are kept in
`src/test/e2e/results/<run-id>/`.

Useful options:

```bash
# Keep videos for all tests, including successful ones.
bash src/test/e2e/scripts/run.sh smoke --video

# Keep the stack to investigate a failure; the cleanup command is printed.
bash src/test/e2e/scripts/run.sh full --keep

# Transfer S3 files through the application instead of using presigned URLs.
bash src/test/e2e/scripts/run.sh full --s3-mode=classic

# Run a single test file.
bash src/test/e2e/scripts/run.sh full -- specs/clipboard.spec.ts
```

Set `STARTUP_TIMEOUT=420` to increase the service startup timeout. Each test has
its own application session and directory. Local runs do not retry failed tests.
When `CI` is set, Playwright allows one retry and fails the run if a test is flaky.

## Watching test videos

Use `--video` with either campaign to produce a single `campaign.mp4` containing
all recordings in execution order. A title card identifies each test, its result
and its attempt number. Tests with multiple recorded pages have a clip for each
page. Recordings show browser interactions; the combined video plays them in
sequence.

```bash
bash src/test/e2e/scripts/run.sh full --video
```

Open `results/<run-id>/campaign.mp4` with a video player. The script prints its
full path when the run finishes. Assembly runs after the tests, including when
a test fails, and takes extra processing time.

Individual `.webm` recordings are available under
`results/<run-id>/test-results/<test>/` and in the HTML report's test details at
`results/<run-id>/html/index.html`. By default, recordings are kept for failed
tests. Videos are preserved when the Docker stack is cleaned up.

## Cleaning generated files

After a test run, remove all generated files with:

```bash
bash src/test/e2e/scripts/run.sh clean
```

This uses Git to remove all ignored files and directories under `src/test/e2e/`,
including results, videos, reports, logs, caches and locally installed dependencies.
Source files are preserved.

## Finding the scenarios

| File in `specs/` | Cases | Coverage |
| --- | ---: | --- |
| `auth.spec.ts` | 4 | Application permissions, anonymous access, logout and mount authentication form |
| `file-operations.spec.ts` | 7 | Creation, Unicode rename, cancellation, recursive deletion and collisions |
| `file-access.spec.ts` | 5 × 3 profiles | Filename downloads for all formats, explicit preview, keyboard/focus, folder and mixed ZIP, deselection, responsive layouts and four languages |
| `navigation.spec.ts` | 2 | Navigation, selection, sorting and language switching on local storage |
| `upload-download.spec.ts` | 13 | Upload/download contents on all five mounts; ZIP, overwrite, cancellation, size limit, drag and drop, and denied writes |
| `transfers.spec.ts` | 26 | Copy and move, checking source and destination contents |
| `clipboard.spec.ts` | 19 | Collisions, recursive destinations, missing sources/destinations, denied writes, session isolation and copy commands |
| `previews.spec.ts` | 3 | Image/audio/video previews on local storage and S3; image size limit |
| `s3.spec.ts` | 4 | Direct/classic transfer path, fallback after signing failure, pagination and multipart transfer |
| `security.spec.ts` | 4 | Unsafe path rejection, CSRF protection and inert rendering of HTML in filenames |
| `javaperf.spec.ts` | 1 | Metric display and narrow viewport layout |

The transfer matrix uses a single dataset: a selection containing a complex
filename, an empty file, a directory tree, an empty directory and binary data.
It covers copy and move within each of the five mounts (10 cases), then local
storage to each other mount and back (16 cases). ZIP contents are checked in a
dedicated scenario.

## Docker configuration

The two environments can be read independently:

- `src/etc/docker-compose.yml`: development services with local ports.
- `src/etc/docker-compose.e2e.yml`: complete test stack without published ports.

The E2E setup builds two images: the Java application
(`src/etc/docker/e2e/Dockerfile.app`) and the tests (`src/test/e2e/Dockerfile`).
The test container prepares data, waits for services and runs Playwright.
Direct/classic S3 mode is selected at startup using the same application image.
Data volumes are isolated per run.

## Organization and scope

- `specs/`: scenarios to read and modify.
- `pages/`: UI commands shared by the scenarios.
- `fixtures/`: test data and isolation.
- `support/native.py`: storage preparation and independent reads, comparing
  file sizes and SHA-256 hashes without going through the application.
- `scripts/run.sh`: startup, reports and cleanup; `wait.py` waits for dependencies
  and `test.sh` runs Playwright inside its container.

CAS/LDAP and the S3, SMB, WebDAV and SFTP servers are real dependencies. Startup
checks confirm their availability, and CAS login provides an application session.
The scenarios cover application workflows, error handling and data preservation.
File access scenarios run on desktop Chromium, a Pixel 7 profile in Chromium and
an iPhone 13 profile in WebKit. Layout checks cover widths from 320 to 1440 pixels
and all four interface languages. These are browser emulations; physical iOS and
Android devices remain useful for checking browser download prompts and file locations.
A narrow viewport check also verifies the JavaPerf layout in Chromium.

## Local checks

To work on the tests without starting Docker (Node 20+ required):

```bash
cd src/test/e2e
npm ci
npm run check
npm run list
```

Reports are stored in the local results directory and can be deleted with
`run.sh clean`.
