"""Every browser class belongs to exactly one component shard, including newly added classes."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location('browser_shard', Path(__file__).with_name('browser-shard.py'))
shards = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(shards)


class BrowserShardTest(unittest.TestCase):
    def test_all_component_browser_classes_are_covered_once(self):
        classes = shards.browser_classes(shards.ROOT)
        selected = [shards.select_shard(classes, index, 4).split(',') for index in range(4)]
        flattened = [name for group in selected for name in group]
        self.assertEqual(sorted(flattened), classes)
        self.assertEqual(len(flattened), len(set(flattened)))
        self.assertNotIn('ShowcaseBrowserTest', flattened)
        self.assertIn('ChartFormatBrowserTest', flattened)

    def test_new_tagged_browser_class_is_discovered(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            sources = root / 'shadwick-examples/src/test/java/example'
            sources.mkdir(parents=True)
            (sources / 'FutureBrowserTest.java').write_text('@Tag("browser") class FutureBrowserTest {}')
            self.assertEqual(shards.browser_classes(root), ['FutureBrowserTest'])
            (sources / 'ForgottenTest.java').write_text('@Tag("browser") class ForgottenTest {}')
            with self.assertRaisesRegex(ValueError, 'naming convention'):
                shards.browser_classes(root)

    def test_invalid_or_empty_shard_cannot_silently_run_all_tests(self):
        for index, count in [(0, 0), (-1, 4), (4, 4), (1, 2)]:
            with self.subTest(index=index, count=count), self.assertRaises(ValueError):
                shards.select_shard(['OneBrowserTest'], index, count)


if __name__ == '__main__':
    unittest.main()
