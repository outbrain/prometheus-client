# Release Proecdure

1. ***gradle clean build*** - Build locally and make sure all tests run successfully.
2. ***gradle release -Prelease.useAutomaticVersion=true*** - Prepare the release - includes creating a commit with a non-SNAPSHOT
 version, upgrading the version to the next snapshot and pushing all these changes to the remote repo.
3. ***git checkout \<latestTag\>*** - Locally checkout the latest tag. You can see this was successfull if the file 
 *gradle.properties* contains a version that is not a SNAPSHOT version.
4. ***gradle bintrayUpload -Dbintray.user=<bintrayUser> -Dbintray.key=<bintrayApiKey>*** - Build and upload the current version
 (remember - not a SNAPSHOT version) to bintray. You can find your bintray api key in the profile page under *API Key*
5. ***git checkout master*** - Resume previous work (if you were working on the master branch).
6. Login to bintray and visit the [prometheus-client project page](https://bintray.com/outbrain/OutbrainOSS/prometheus-client)
 to publish the new version. The version will not be available for download until it is published.
