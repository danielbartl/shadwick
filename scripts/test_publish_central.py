"""Publication must reject wrong or unvalidated deployments before sending a publish request."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import Mock

SPEC = importlib.util.spec_from_file_location('publish_central', Path(__file__).with_name('publish-central.py'))
central = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(central)
DEPLOYMENT = '12345678-1234-1234-1234-123456789abc'


def status(state='VALIDATED', version='0.1.0', deployment=DEPLOYMENT):
    return {'deploymentId': deployment, 'deploymentState': state,
            'purls': [f'pkg:maven/dev.jbaby/{artifact}@{version}' for artifact in ['shadwick-parent', 'shadwick']]}


class PublicationChecksTest(unittest.TestCase):
    def test_wrong_deployment_or_version_never_triggers_publication(self):
        for response in [status(deployment='different'), status(version='0.2.0'), {**status(), 'purls': []}]:
            with self.subTest(response=response):
                request = Mock(return_value=response)
                with self.assertRaises(ValueError):
                    central.publish_deployment(request, DEPLOYMENT, '0.1.0', publish=True)
                self.assertEqual(request.call_count, 1)

    def test_unvalidated_or_failed_deployment_never_triggers_publication(self):
        for state in ['PENDING', 'VALIDATING', 'FAILED', None]:
            with self.subTest(state=state):
                request = Mock(return_value=status(state))
                with self.assertRaises(ValueError):
                    central.publish_deployment(request, DEPLOYMENT, '0.1.0', publish=True)
                self.assertEqual(request.call_count, 1)

    def test_status_check_does_not_publish(self):
        request = Mock(return_value=status())
        self.assertEqual(central.publish_deployment(request, DEPLOYMENT, '0.1.0')['deploymentState'], 'VALIDATED')
        self.assertEqual(request.call_count, 1)

    def test_validated_deployment_is_published_once_then_polled(self):
        request = Mock(side_effect=[status(), None, status('PUBLISHING'), status('PUBLISHED')])
        pause = Mock()
        result = central.publish_deployment(request, DEPLOYMENT, '0.1.0', publish=True, pause=pause)
        self.assertEqual(result['deploymentState'], 'PUBLISHED')
        self.assertEqual([call.args[0] for call in request.call_args_list].count(f'/deployment/{DEPLOYMENT}'), 1)
        pause.assert_called_once_with(5)

    def test_retry_does_not_republish_an_in_progress_or_published_deployment(self):
        for state in ['PUBLISHING', 'PUBLISHED']:
            with self.subTest(state=state):
                request = Mock(side_effect=[status(state), status('PUBLISHED')])
                central.publish_deployment(request, DEPLOYMENT, '0.1.0', publish=True)
                self.assertTrue(all(call.args[0].startswith('/status?') for call in request.call_args_list))

    def test_poll_timeout_does_not_repeat_publication(self):
        request = Mock(return_value=status('PUBLISHING'))
        with self.assertRaisesRegex(ValueError, 'still pending'):
            central.publish_deployment(request, DEPLOYMENT, '0.1.0', publish=True, pause=Mock(), attempts=2)
        self.assertEqual(request.call_count, 3)


if __name__ == '__main__':
    unittest.main()
