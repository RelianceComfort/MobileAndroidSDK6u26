#!/usr/bin/env bash
echo "STARTING PRE-BUILD STEPS"
echo $APPCENTER_SOURCE_DIRECTORY
echo $VERSION_NAME
echo $VERSION_CODE

sed -iE -e "s/versionName=\"[^\"]*\"/versionName=\"$VERSION_NAME\"/g" $APPCENTER_SOURCE_DIRECTORY/Mobile/src/main/AndroidManifest.xml
sed -iE -e "s/versionCode=\"[^\"]*\"/versionCode=\"$VERSION_CODE\"/g" $APPCENTER_SOURCE_DIRECTORY/Mobile/src/main/AndroidManifest.xml

echo "BUILD FSM ANDROID MOBILE"
echo "DONE PRE-BUILD"
