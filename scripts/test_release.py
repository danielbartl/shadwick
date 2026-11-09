"""Regression tests for release gates that must fail before staging a bad payload."""
import hashlib
from io import BytesIO
from unittest.mock import patch
import importlib.util
from pathlib import Path
import shutil
import tempfile
import unittest
import xml.etree.ElementTree as ET
from zipfile import ZipFile

SPEC = importlib.util.spec_from_file_location('check_release', Path(__file__).with_name('check-release.py'))
release = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release)


class ReleaseChecksTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        for name in ['pom.xml', 'shadwick-core/pom.xml', 'shadwick-examples/pom.xml']:
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(release.ROOT / name, path)
        (self.root / 'docs/releases').mkdir(parents=True)
        (self.root / 'docs/releases/0.1.0.md').write_text('# Release notes\n')
        (self.root / 'CHANGELOG.md').write_text('# Changelog\n\n## 0.1.0\n')
        self.set_version('0.1.0')
        self.update('pom.xml', 'm:properties/m:wicket.version', '11.0.0')
        self.update('pom.xml', 'm:properties/m:maven.compiler.release', '21')

    def update(self, name, path, text):
        filename = self.root / name
        tree = ET.parse(filename)
        tree.getroot().find(path, release.NS).text = text
        tree.write(filename, encoding='unicode')

    def set_version(self, version):
        self.update('pom.xml', 'm:version', version)
        self.update('pom.xml', 'm:scm/m:tag', f'v{version}')
        for name in ['shadwick-core/pom.xml', 'shadwick-examples/pom.xml']:
            self.update(name, 'm:parent/m:version', version)

    def test_complete_matching_metadata_is_accepted(self):
        self.assertEqual(release.metadata(self.root, 'v0.1.0'), '0.1.0')

    def test_snapshot_cannot_be_staged_as_a_release(self):
        self.set_version('0.1.0-SNAPSHOT')
        with self.assertRaisesRegex(ValueError, 'without SNAPSHOT'):
            release.metadata(self.root, 'v0.1.0-SNAPSHOT')

    def test_tag_must_match_the_artifact_version(self):
        with self.assertRaisesRegex(ValueError, 'Tag must match'):
            release.metadata(self.root, 'v0.2.0')

    def test_wicket_10_cannot_be_staged_as_the_initial_release(self):
        self.update('pom.xml', 'm:properties/m:wicket.version', '10.11.0')
        with self.assertRaisesRegex(ValueError, 'stable Wicket 11'):
            release.metadata(self.root, 'v0.1.0')

    def test_wicket_11_requires_the_java_upgrade(self):
        self.update('pom.xml', 'm:properties/m:maven.compiler.release', '17')
        with self.assertRaisesRegex(ValueError, 'Java 21'):
            release.metadata(self.root, 'v0.1.0')

    def test_wicket_11_requires_the_servlet_upgrade(self):
        self.update('pom.xml', 'm:properties/m:jakarta-servlet-api.version', '6.0.0')
        with self.assertRaisesRegex(ValueError, 'Servlet 6.1'):
            release.metadata(self.root, 'v0.1.0')

    def test_child_must_resolve_the_released_parent(self):
        self.update('shadwick-core/pom.xml', 'm:parent/m:version', '0.0.9')
        with self.assertRaisesRegex(ValueError, 'parent version differs'):
            release.metadata(self.root, 'v0.1.0')

    def test_required_parent_metadata_cannot_be_omitted(self):
        self.update('pom.xml', 'm:licenses/m:license/m:url', '')
        with self.assertRaisesRegex(ValueError, 'Missing Central POM metadata'):
            release.metadata(self.root, 'v0.1.0')

    def test_archive_identity_must_match_even_when_the_filename_looks_correct(self):
        binary = BytesIO()
        with ZipFile(binary, 'w') as jar:
            jar.writestr('META-INF/MANIFEST.MF', 'Automatic-Module-Name: dev.jbaby.shadwick\n')
            jar.writestr('META-INF/LICENSE', 'License')
            jar.writestr('META-INF/NOTICE', 'Notice')
            jar.writestr('META-INF/maven/dev.jbaby/shadwick/pom.properties',
                         'groupId=dev.jbaby\nartifactId=shadwick\nversion=0.1.0-SNAPSHOT\n')
        with self.assertRaisesRegex(ValueError, 'Embedded Maven identity does not match'):
            release.inspect_archives(binary.getvalue(), b'', b'', java_release=21, version='0.1.0')

    def test_collect_replaces_older_snapshot_artifacts_and_checksums(self):
        output = self.root / 'target/release'
        output.mkdir(parents=True)
        for name in ['shadwick-0.1.0-SNAPSHOT.jar', 'shadwick-0.1.0-SNAPSHOT-sources.jar',
                     'shadwick-0.1.0-SNAPSHOT-javadoc.jar', 'shadwick-0.1.0-SNAPSHOT.pom',
                     'shadwick-parent-0.1.0-SNAPSHOT.pom', 'SHA256SUMS']:
            (output / name).write_bytes(b'old payload')
        jars = {f'shadwick-0.1.0{suffix}.jar': b'candidate' for suffix in ['', '-sources', '-javadoc']}
        with patch.object(release, 'artifacts', return_value=('0.1.0', jars)):
            release.collect(self.root)
        expected = {*jars, 'shadwick-parent-0.1.0.pom', 'shadwick-0.1.0.pom', 'SHA256SUMS'}
        self.assertEqual({p.name for p in output.iterdir()}, expected)
        checksums = (output / 'SHA256SUMS').read_text().splitlines()
        self.assertEqual(len(checksums), 5)
        for line in checksums:
            digest, name = line.split('  ')
            self.assertEqual(hashlib.sha256((output / name).read_bytes()).hexdigest(), digest)

    def test_collect_refuses_unrelated_files_without_deleting_them(self):
        output = self.root / 'target/release'
        output.mkdir(parents=True)
        unknown = output / 'review-notes.txt'
        unknown.write_text('keep this')
        with patch.object(release, 'artifacts', return_value=('0.1.0', {})):
            with self.assertRaisesRegex(ValueError, 'Unexpected file in release export'):
                release.collect(self.root)
        self.assertEqual(unknown.read_text(), 'keep this')

    def test_examples_or_unexpected_files_cannot_enter_the_bundle(self):
        filename = self.root / 'bundle.zip'
        with ZipFile(filename, 'w') as archive:
            archive.writestr('dev/jbaby/shadwick-examples/0.1.0/shadwick-examples-0.1.0.jar', b'example')
        with self.assertRaisesRegex(ValueError, 'Unexpected bundle entries'):
            release.bundle(self.root, filename)

    def test_every_artifact_requires_a_detached_signature(self):
        filename = self.root / 'bundle.zip'
        with ZipFile(filename, 'w') as archive:
            archive.writestr('dev/jbaby/shadwick/0.1.0/shadwick-0.1.0.jar', b'payload')
        with self.assertRaisesRegex(ValueError, 'Artifact/signature missing'):
            release.bundle(self.root, filename)

    def test_bundle_checksum_must_match_the_actual_bytes(self):
        filename = self.root / 'bundle.zip'
        names = ['dev/jbaby/shadwick-parent/0.1.0/shadwick-parent-0.1.0.pom'] + [
            'dev/jbaby/shadwick/0.1.0/shadwick-0.1.0' + suffix
            for suffix in ['.pom', '.jar', '-sources.jar', '-javadoc.jar']]
        with ZipFile(filename, 'w') as archive:
            for name in names:
                data = b'payload'
                archive.writestr(name, data)
                archive.writestr(name + '.asc', '-----BEGIN PGP SIGNATURE-----\n')
                for suffix in ['md5', 'sha1', 'sha256', 'sha512']:
                    archive.writestr(name + '.' + suffix, 'wrong' if suffix == 'sha256' else hashlib.new(suffix, data).hexdigest())
        with self.assertRaisesRegex(ValueError, 'Invalid checksum'):
            release.bundle(self.root, filename)


if __name__ == '__main__':
    unittest.main()
