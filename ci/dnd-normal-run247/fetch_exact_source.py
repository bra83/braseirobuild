#!/usr/bin/env python3
"""Braseiro D&D NORMAL: fetch the reconciled 606547 source with 10/10 SHA proof.

No RUN245 patches or fallback sources are allowed. This routine is additive and
is not the game's runtime. It validates before extraction and exits nonzero on
any missing or incorrect part. Supports offline regression tests via --local-parts.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import pathlib
import shutil
import sys
import zipfile

CHUNK = 1024 * 1024
EXPECTED_SHA = '2774b450ecf5be179976199b22bab8584f76939dcf305d82a84171a199282e06'
EXPECTED_VERSION_CODE = '606547'
EXPECTED_DB = '61'
EXPECTED_BACKUP = '21'
EXPECTED_APPLICATION_ID = 'com.braseiro.dnd5e'
EXPECTED_ZIP_ENTRIES = 1437


def sha256_path(path: pathlib.Path) -> str:
    h = hashlib.sha256()
    with path.open('rb') as fp:
        for blob in iter(lambda: fp.read(CHUNK), b''):
            h.update(blob)
    return h.hexdigest()


def fail(message: str) -> None:
    raise RuntimeError(message)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--manifest', required=True, type=pathlib.Path)
    ap.add_argument('--scratch', required=True, type=pathlib.Path)
    ap.add_argument('--project', required=True, type=pathlib.Path)
    ap.add_argument('--local-parts', type=pathlib.Path)
    args = ap.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding='utf-8'))
    if manifest['source_sha256'] != EXPECTED_SHA:
        fail('SOURCE_MANIFEST_SHA_MISMATCH')
    if manifest.get('identity', {}) != {'applicationId': EXPECTED_APPLICATION_ID,
                                         'versionCode': 606547, 'db_schema': 61,
                                         'backup_format': 21}:
        fail('SOURCE_MANIFEST_IDENTITY_MISMATCH')
    parts_dir = args.scratch / 'parts'
    parts_dir.mkdir(parents=True, exist_ok=True)
    archive = args.scratch / manifest['source_filename']
    if args.project.exists() and any(args.project.iterdir()):
        fail('PROJECT_TARGET_NONEMPTY_REFUSING_OVERWRITE')
    args.project.mkdir(parents=True, exist_ok=True)
    with archive.open('wb') as out:
        for i, part in enumerate(manifest['parts']):
            dst = parts_dir / ('part%03d' % i)
            if args.local_parts is not None:
                original = args.local_parts / part['name']
                if not original.is_file():
                    fail(f'LOCAL_PART_{i:03d}_MISSING')
                shutil.copyfile(original, dst)
            else:
                try:
                    import gdown  # only needed for the BRELGA cloud fetch
                    result = gdown.download(id=part['drive_id'], output=str(dst), quiet=True, fuzzy=False)
                except Exception as exc:
                    fail(f'DRIVE_PART_{i:03d}_DOWNLOAD_EXCEPTION:{exc.__class__.__name__}:{exc}')
                if result is None or not dst.is_file():
                    fail(f'DRIVE_PART_{i:03d}_DOWNLOAD_FAILED:{part["drive_id"]}')
            if dst.stat().st_size != part['size']:
                fail(f'PART_{i:03d}_SIZE_MISMATCH')
            if sha256_path(dst) != part['sha256']:
                fail(f'PART_{i:03d}_SHA256_MISMATCH')
            with dst.open('rb') as source:
                shutil.copyfileobj(source, out, CHUNK)
            print(f'PART_{i:03d}=VERIFIED sha256={part["sha256"]}', flush=True)
    if archive.stat().st_size != manifest['source_size']:
        fail('SOURCE_ARCHIVE_SIZE_MISMATCH')
    if sha256_path(archive) != EXPECTED_SHA:
        fail('SOURCE_ARCHIVE_SHA256_MISMATCH')
    with zipfile.ZipFile(archive) as zf:
        names = zf.namelist()
        if len(names) != EXPECTED_ZIP_ENTRIES or len(names) != len(set(names)):
            fail('SOURCE_ARCHIVE_ENTRY_COUNT_OR_DUPLICATE')
        if zf.testzip() is not None:
            fail('SOURCE_ARCHIVE_CRC_MISMATCH')
        for member in names:
            path = pathlib.PurePosixPath(member)
            if path.is_absolute() or '..' in path.parts or '\\' in member:
                fail('ZIP_UNSAFE_PATH_' + member)
        zf.extractall(args.project)
    props = {}
    for line in (args.project / 'version.properties').read_text(encoding='utf-8').splitlines():
        if '=' in line and not line.strip().startswith('#'):
            key, value = line.split('=', 1)
            props[key.strip()] = value.strip()
    for field, expected in [('VERSION_CODE',EXPECTED_VERSION_CODE),
                            ('DB_SCHEMA',EXPECTED_DB),('BACKUP_FORMAT',EXPECTED_BACKUP)]:
        if props.get(field) != expected:
            fail(f'IDENTITY_{field}_MISMATCH:actual={props.get(field)}')
    if not props.get('VERSION_NAME','').startswith('6.6.14-r247'):
        fail('IDENTITY_VERSION_NAME_MISMATCH')
    app_config = (args.project / 'app' / 'build.gradle.kts').read_text(encoding='utf-8')
    if EXPECTED_APPLICATION_ID not in app_config:
        fail('IDENTITY_APPLICATION_ID_MISMATCH')
    if not (args.project / 'gradlew.bat').is_file():
        fail('GRADLE_ROOT_MISSING')
    db = (args.project / 'app' / 'src' / 'main' / 'java' / 'com' / 'braseiro' / 'dnd5e' / 'AppDb.java').read_text(encoding='utf-8')
    if 'DB_VERSION = 61' not in db:
        fail('ACTUAL_APPDB_VERSION_MISMATCH')
    report = args.scratch / 'SOURCE_VERIFY.txt'
    report.write_text(
        'PROJECT=Braseiro D&D NORMAL\n'
        'SOURCE_ZIP_SHA256='+EXPECTED_SHA+'\n'
        'SOURCE_BYTES='+str(archive.stat().st_size)+'\n'
        'ZIP_ENTRIES='+str(len(names))+'\n'
        'PART_COUNT='+str(len(manifest['parts']))+'\n'
        'APP_ID='+EXPECTED_APPLICATION_ID+'\n'
        'VERSION_NAME='+props['VERSION_NAME']+'\n'
        'VERSION_CODE='+EXPECTED_VERSION_CODE+'\n'
        'DB_SCHEMA='+EXPECTED_DB+'\n'
        'BACKUP_FORMAT='+EXPECTED_BACKUP+'\n'
        'ARCHIVE_STATUS=SHA_CRC_IDENTITY_PASS\n', encoding='utf-8')
    print(report.read_text(encoding='utf-8'), end='', flush=True)
    return 0

if __name__ == '__main__':
    try:
        sys.exit(main())
    except Exception as exc:
        print(f'SOURCE_FETCH_VALIDATION_FAILED={exc}', file=sys.stderr, flush=True)
        sys.exit(2)
