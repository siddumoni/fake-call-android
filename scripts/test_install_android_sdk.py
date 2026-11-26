import unittest

from install_android_sdk import emulator_image_candidates


class EmulatorImageCandidateTests(unittest.TestCase):
    def test_prefers_highest_android_17_api_with_standard_google_apis(self):
        available = {
            "system-images;android-37.1;google_apis_ps16k;x86_64",
            "system-images;android-37.1;google_apis;x86_64",
            "system-images;android-37.0;google_apis;x86_64",
            "system-images;android-36;google_apis;x86_64",
        }

        self.assertEqual(
            [
                ("system-images;android-37.1;google_apis;x86_64", "37"),
                ("system-images;android-37.1;google_apis_ps16k;x86_64", "37"),
                ("system-images;android-37.0;google_apis;x86_64", "37"),
                ("system-images;android-36;google_apis;x86_64", "36"),
            ],
            emulator_image_candidates(available),
        )

    def test_falls_back_to_api_36_when_no_android_17_image_is_available(self):
        available = {"system-images;android-36;google_apis;x86_64"}

        self.assertEqual(
            [("system-images;android-36;google_apis;x86_64", "36")],
            emulator_image_candidates(available),
        )

    def test_returns_no_candidates_when_no_compatible_image_exists(self):
        self.assertEqual([], emulator_image_candidates(set()))


if __name__ == "__main__":
    unittest.main()
