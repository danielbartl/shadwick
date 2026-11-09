#!/usr/bin/env python3
"""Check release coordinates and the actual Maven payload using only Python's standard library."""
import argparse
import hashlib
from io import BytesIO
from pathlib import Path
import re
import struct
import sys
import xml.etree.ElementTree as ET
from zipfile import ZipFile, BadZipFile

NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
ROOT = Path(__file__).resolve().parents[1]


def pom(path):
    return ET.parse(path).getroot()


def value(element, path):
    return element.findtext(path, namespaces=NS)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def coordinates(root):
    parent = pom(root / 'pom.xml')
    version = value(parent, 'm:version')
    require(value(parent, 'm:groupId') == 'dev.jbaby', 'Unexpected release groupId')
    require(value(parent, 'm:artifactId') == 'shadwick-parent', 'Unexpected parent artifactId')
    for module, artifact in [('shadwick-core', 'shadwick'), ('shadwick-examples', 'shadwick-examples')]:
        child = pom(root / module / 'pom.xml')
        require(value(child, 'm:parent/m:version') == version, f'{module} parent version differs')
        require(value(child, 'm:artifactId') == artifact, f'Unexpected {module} artifactId')
        require(value(child, 'm:parent/m:groupId') == 'dev.jbaby', f'Unexpected {module} groupId')
    return parent, version


def metadata(root, tag):
    parent, version = coordinates(root)
    require(bool(re.fullmatch(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)', version)), 'Release version must be X.Y.Z without SNAPSHOT/qualifiers')
    require(tag == f'v{version}', 'Tag must match the POM version exactly')
    require(value(parent, 'm:scm/m:tag') == tag, 'SCM tag must match the release tag')
    wicket = value(parent, 'm:properties/m:wicket.version') or ''
    require(bool(re.fullmatch(r'\d+\.\d+\.\d+', wicket)) and int(wicket.split('.')[0]) >= 11,
            'Initial release requires a stable Wicket 11+ version and completed migration checks')
    require(int(value(parent, 'm:properties/m:maven.compiler.release')) >= 21,
            'Wicket 11 release requires Java 21+')
    servlet = value(parent, 'm:properties/m:jakarta-servlet-api.version') or ''
    require(bool(re.fullmatch(r'\d+\.\d+\.\d+', servlet))
            and tuple(map(int, servlet.split('.')[:2])) >= (6, 1),
            'Wicket 11 release requires Jakarta Servlet 6.1+')
    for path in ['m:name', 'm:description', 'm:url', 'm:licenses/m:license/m:name',
                 'm:licenses/m:license/m:url', 'm:developers/m:developer/m:id',
                 'm:developers/m:developer/m:name', 'm:scm/m:connection', 'm:scm/m:url']:
        require(bool(value(parent, path)), f'Missing Central POM metadata: {path}')
    require((root / f'docs/releases/{version}.md').is_file(), 'Missing versioned release notes')
    require(f'## {version}' in (root / 'CHANGELOG.md').read_text(), 'Missing changelog entry')
    return version


def inspect_archives(binary, sources, javadoc, java_release, version):
    with ZipFile(BytesIO(binary)) as jar:
        names = set(jar.namelist())
        require('Automatic-Module-Name: dev.jbaby.shadwick' in jar.read('META-INF/MANIFEST.MF').decode(), 'Automatic module name missing')
        require({'META-INF/LICENSE', 'META-INF/NOTICE'} <= names, 'Packaged license/notice missing')
        identity = 'META-INF/maven/dev.jbaby/shadwick/pom.properties'
        require(identity in names, 'Embedded Maven identity missing')
        properties = dict(line.split('=', 1) for line in jar.read(identity).decode().splitlines()
                          if '=' in line and not line.startswith('#'))
        require(all(properties.get(key) == expected for key, expected in
                    [('groupId', 'dev.jbaby'), ('artifactId', 'shadwick'), ('version', version)]),
                'Embedded Maven identity does not match the release coordinates')
        require('dev/jbaby/shadwick/components/form/SwTextField.class' in names, 'Library classes missing')
        expected_css = {f'dev/jbaby/shadwick/css/shadwick-{skin}{bundle}{minified}.css'
                        for skin in ['vega', 'lyra', 'nova', 'maia', 'mira', 'luma', 'rhea', 'sera', 'sketch']
                        for bundle in ['', '-embedded'] for minified in ['', '.min']}
        require(expected_css <= names, 'Expected all 36 CSS files (normal/minified standalone/embedded)')
        require('dev/jbaby/shadwick/js/shadwick.js' in names, 'Component runtime missing')
        require(not any('/examples/' in n or n.startswith(('BOOT-INF/', 'org/springframework/', 'org/junit/')) for n in names), 'Examples/test/runtime dependency leaked into library')
        for name in names:
            if name.endswith('.class'):
                magic, minor, major = struct.unpack('>IHH', jar.read(name)[:8])
                require(magic == 0xCAFEBABE and major <= java_release + 44, f'Bytecode newer than Java {java_release}: {name}')
    with ZipFile(BytesIO(sources)) as jar:
        require('dev/jbaby/shadwick/components/form/SwTextField.java' in jar.namelist(), 'Source archive missing library Java')
    with ZipFile(BytesIO(javadoc)) as jar:
        require('index.html' in jar.namelist(), 'Javadoc index missing')
        api = 'dev/jbaby/shadwick/components/form/SwTextField.html'
        require(api in jar.namelist() or 'dev.jbaby.shadwick/' + api in jar.namelist(), 'Javadoc API missing')


def artifacts(root):
    parent, version = coordinates(root)
    directory = root / 'shadwick-core/target'
    files = {f'shadwick-{version}{suffix}.jar': (directory / f'shadwick-{version}{suffix}.jar').read_bytes()
             for suffix in ['', '-sources', '-javadoc']}
    inspect_archives(*(files[f'shadwick-{version}{s}.jar'] for s in ['', '-sources', '-javadoc']),
                     java_release=int(value(parent, 'm:properties/m:maven.compiler.release')), version=version)
    return version, files


def collect(root):
    version, files = artifacts(root)
    files[f'shadwick-parent-{version}.pom'] = (root / 'pom.xml').read_bytes()
    files[f'shadwick-{version}.pom'] = (root / 'shadwick-core/pom.xml').read_bytes()
    output = root / 'target/release'
    output.mkdir(parents=True, exist_ok=True)
    # This generated directory is reused across candidates. Validate before removing stale managed payloads.
    stale = [path for path in output.iterdir() if path.name not in files and path.name != 'SHA256SUMS']
    for path in stale:
        require(path.is_file() and not path.is_symlink() and bool(re.fullmatch(
            r'shadwick(?:-parent)?-\d+\.\d+\.\d+(?:-SNAPSHOT)?(?:-sources|-javadoc)?\.(?:jar|pom)', path.name)),
            f'Unexpected file in release export directory: {path.name}')
    for path in stale:
        path.unlink()
    # Only the explicitly named release payload is exported; examples and signing keys stay out.
    for name, content in files.items():
        (output / name).write_bytes(content)
    (output / 'SHA256SUMS').write_text(''.join(f'{hashlib.sha256(data).hexdigest()}  {name}\n' for name, data in sorted(files.items())))
    print(f'Checked/exported {len(files)} release artifacts to {output}')


def bundle(root, filename):
    project, version = coordinates(root)
    parent = f'dev/jbaby/shadwick-parent/{version}/shadwick-parent-{version}.pom'
    prefix = f'dev/jbaby/shadwick/{version}/shadwick-{version}'
    expected = {parent, prefix + '.pom', *(prefix + suffix + '.jar' for suffix in ['', '-sources', '-javadoc'])}
    with ZipFile(filename) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)), 'Duplicate entries in Central bundle')
        actual = {n for n in names if not n.endswith('/')}
        allowed = {name + suffix for name in expected for suffix in ['', '.asc', '.md5', '.sha1', '.sha256', '.sha512', '.asc.md5', '.asc.sha1', '.asc.sha256', '.asc.sha512']}
        require(actual <= allowed, f'Unexpected bundle entries: {sorted(actual - allowed)}')
        for name in expected:
            require(name in actual and name + '.asc' in actual, f'Artifact/signature missing: {name}')
            require(archive.read(name + '.asc').startswith(b'-----BEGIN PGP SIGNATURE-----'), f'Not an armored signature: {name}')
            for ext, algorithm in [('md5', hashlib.md5), ('sha1', hashlib.sha1), ('sha256', hashlib.sha256), ('sha512', hashlib.sha512)]:
                check = name + '.' + ext
                require(check in actual, f'Checksum missing: {check}')
                require(archive.read(check).decode().strip() == algorithm(archive.read(name)).hexdigest(), f'Invalid checksum: {check}')
        inspect_archives(*(archive.read(prefix + suffix + '.jar') for suffix in ['', '-sources', '-javadoc']),
                         java_release=int(value(project, 'm:properties/m:maven.compiler.release')), version=version)
    print(f'Checked signed Central bundle: {filename}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['metadata', 'artifacts', 'collect', 'bundle'])
    parser.add_argument('--root', type=Path, default=ROOT)
    parser.add_argument('--tag')
    parser.add_argument('--bundle', type=Path)
    args = parser.parse_args()
    try:
        if args.mode == 'metadata':
            require(args.tag is not None, '--tag is required')
            print(f'Checked release metadata: {metadata(args.root, args.tag)}')
        elif args.mode == 'artifacts':
            version, _ = artifacts(args.root)
            print(f'Checked binary, sources and Javadoc: {version}')
        elif args.mode == 'collect':
            collect(args.root)
        else:
            require(args.bundle is not None, '--bundle is required')
            bundle(args.root, args.bundle)
    except (ValueError, OSError, KeyError, ET.ParseError, BadZipFile) as error:
        print(f'Release check failed: {error}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
