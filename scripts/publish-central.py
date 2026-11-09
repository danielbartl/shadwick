#!/usr/bin/env python3
"""Publish an already validated Central deployment, checking its identity before any write."""
import argparse
import base64
import json
import os
import re
import time
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE_URL = 'https://central.sonatype.com/api/v1/publisher'


def validate_status(status, deployment, version):
    if status.get('deploymentId') != deployment:
        raise ValueError('Central returned a different deployment ID')
    state = status.get('deploymentState')
    if state not in {'VALIDATED', 'PUBLISHING', 'PUBLISHED'}:
        raise ValueError(f'Deployment is not ready for publication: {state}')
    expected = {f'pkg:maven/dev.jbaby/{artifact}@{version}'
                for artifact in ['shadwick-parent', 'shadwick']}
    actual = {purl.split('?', 1)[0] for purl in status.get('purls', [])}
    if actual != expected:
        # Central can omit package metadata after accepting publication. These states never trigger a write.
        if not actual and state in {'PUBLISHING', 'PUBLISHED'}:
            return state
        raise ValueError(f'Deployment coordinates differ from the release ({state}): {sorted(actual)}')
    return state


def publish_deployment(request, deployment, version, publish=False, pause=time.sleep, attempts=120):
    status = request(f'/status?id={deployment}')
    state = validate_status(status, deployment, version)
    if not publish or state == 'PUBLISHED':
        return status
    if state == 'VALIDATED':
        request(f'/deployment/{deployment}')
    for _ in range(attempts):
        status = request(f'/status?id={deployment}')
        state = validate_status(status, deployment, version)
        if state == 'PUBLISHED':
            return status
        pause(5)
    raise ValueError('Central publication is still pending; inspect this deployment before retrying')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--deployment', required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--publish', action='store_true', help='Publish after checking the validated deployment')
    args = parser.parse_args()
    if not re.fullmatch(r'[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}', args.deployment):
        parser.error('Deployment must be a Central deployment UUID')
    if not re.fullmatch(r'\d+\.\d+\.\d+', args.version):
        parser.error('Version must be X.Y.Z')
    credentials = f"{os.environ['CENTRAL_USERNAME']}:{os.environ['CENTRAL_PASSWORD']}"
    token = base64.b64encode(credentials.encode()).decode()

    def request(path):
        req = Request(BASE_URL + path, data=b'', method='POST',
                      headers={'Authorization': 'Bearer ' + token})
        with urlopen(req, timeout=60) as response:
            body = response.read()
            return json.loads(body) if body else None

    try:
        result = publish_deployment(request, args.deployment, args.version, args.publish)
    except HTTPError as error:
        raise SystemExit(f'Central request failed with HTTP {error.code}; credentials were not logged') from None
    except ValueError as error:
        raise SystemExit(str(error)) from None
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
