#!/usr/bin/env python3
"""Partition the remaining browser classes; ShowcaseBrowserTest has its own all-skin jobs."""
import argparse
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def browser_classes(root):
    sources = root / 'shadwick-examples/src/test/java'
    tagged = [path for path in sources.rglob('*.java')
              if re.search(r'@Tag\s*\(\s*"browser"\s*\)', path.read_text())]
    unexpected = [str(path) for path in tagged if not path.name.endswith('BrowserTest.java')]
    if unexpected:
        raise ValueError(f'Browser classes must follow the BrowserTest naming convention: {unexpected}')
    return sorted(path.stem for path in tagged if path.stem != 'ShowcaseBrowserTest')


def select_shard(classes, index, count):
    if count < 1 or not 0 <= index < count:
        raise ValueError('Shard index must be within the shard count')
    selected = classes[index::count]
    if not selected:
        raise ValueError('Empty browser shard; refusing to run an implicit full suite')
    return ','.join(selected)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--index', type=int, required=True)
    parser.add_argument('--count', type=int, default=4)
    args = parser.parse_args()
    print(select_shard(browser_classes(ROOT), args.index, args.count))
