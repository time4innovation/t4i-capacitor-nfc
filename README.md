# t4i-capacitor-nfc

Capacitor plugin for reading NFC tags

# Android

## Requires
`@capacitor/core`

## Install
Add the following to your package.json -> dependencies
```json
"t4i-capacitor-nfc": "github:time4innovation/t4i-capacitor-nfc"
```

## Usage in JavaScript
Import the following
```javascript
import { Plugins } from "CapacitorCore";
const { Time4InnovationNfc } = Plugins;
```
You might include CapacitorCore as the following in ember-cli-build.js
```javascript
'use strict';

const EmberApp = require('ember-cli/lib/broccoli/ember-app');
const ENV = require('./config/environment.js');

module.exports = function (defaults) {
	 // CAPACITOR -> CORE
    app.import('node_modules/@capacitor/core/dist/index.cjs.js', {
        using: [{ transformation: 'cjs', as: 'CapacitorCore' }],
    });
}
```

Implement the following method and call it where you need to read an RFID-Tag.
```javasacript
nfcInit() {
    // Initialize the NFC detection
    Time4InnovationNfc.initializeNFC()
        .then((result) => {
            // Now tagInfo is part of JSObject
            console.log("NFC Tag Information:", result.tagInfo);
        })
        .catch((error) => {
            console.error("Error initializing NFC:", error);
        });
}
```

## Manifest.xml settings

```xml
<?xml version="1.0" encoding="utf-8" ?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true"
        android:networkSecurityConfig="@xml/network_security_config">
        <activity
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|locale|smallestScreenSize|screenLayout|uiMode"
            android:name=".MainActivity"
            android:label="@string/title_activity_main"
            android:theme="@style/AppTheme.NoActionBarLaunch"
            android:launchMode="singleTask"
            android:exported="true"
            android:screenOrientation="landscape">

            <!-- YOU NEED THIS: NFC intent filters for handling NFC actions START -->
            <intent-filter>
                <action android:name="android.nfc.action.NDEF_DISCOVERED" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" /><!-- Adjust this based on your tag data -->
            </intent-filter>
            <intent-filter>
                <action android:name="android.nfc.action.TECH_DISCOVERED" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.nfc.action.TAG_DISCOVERED" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
			   <!-- NFC intent filters for handling NFC actions END -->
        </activity>
    </application>

    <!-- YOU NEED THIS: Permissions START -->
    <uses-permission android:name="android.permission.NFC" />
    <uses-permission android:name="android.permission.NFC_TRANSACTION_EVENT" />
    <uses-feature android:name="android.hardware.nfc" android:required="true" />
	<!-- Permissions END -->
</manifest>
```







