# Installing Okay React Native Module 


####  Create a folder called **custom_modules** in your React Native project root folder:

```sh
$ mkdir ~/project_dir/custom_modules
```

#####  Copy the downloaded React Native module from this repository to custom_modules folder

```sh
$ cp ~/Downloads/RNOkaySDK ~/project_dir/custom_modules
```
#####  Add RNOKaySDK as a local dependency to your package.json file:

```sh
"react-native-okay-sdk": "file:custom_modules/RNOkaySDK"
```

##### Install node_modules:

Run the following command from your project root folder

```sh
$ yarn install
```

##### Link library with react-native:

Run the following command from your project root folder

```sh
$ react-native link react-native-okay-sdk
```
<br>

## Android
### Configure Android project:

Locate your ***project_dir/android/app/build.gradle*** file in your project workspace, then set your minSDKVersion in the *build.gradle* to API 16.

```groovy
buildscript {
    ext {
        buildToolsVersion = "28.0.3"
        minSdkVersion = 16
        compileSdkVersion = 28
        targetSdkVersion = 28
        supportLibVersion = "28.0.0"
    }
    .....
    dependencies {
      classpath("com.android.tools.build:gradle:3.4.1") // update gradle to 3.4.1
      ...
    }
    .....
}
```

Add Okay maven repository to your Android ***project_dir/android/build.gradle***  file

```groovy
allprojects {
    repositories {
        mavenLocal()
        google()
        jcenter()
        maven {
            // All of React Native (JS, Obj-C sources, Android binaries) is installed from npm
            url "$rootDir/../node_modules/react-native/android"

        }
        // Begin: Add This
        maven {
            url 'https://dl.bintray.com/okaythis/maven'
        }
        // End:
    }
}
```

#### Add permissions to AndroidManifest.xml**:
Locate your *project_dir/android/src/main/AndroidManifest.xml* file, then add these Android permissions to the file.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

##### Add **databinding** and **multidex** for android:

Add the following to your Android *project_dir/android/app/build.gradle* file

```groovy
android {
    compileSdkVersion rootProject.ext.compileSdkVersion

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    // Begin Add DataBinding
    dataBinding {
        enabled = true
    }
    // End
    defaultConfig {
       ...
       multiDexEnabled true // Add this line
    }
    ...
}
```
<br>


## **iOS**
### **Configure iOS project:**

#### Download **PSA.framework** and **PSACommon.framework**:
You can get the zipped version of the SDK here: https://github.com/Okaythis/PSACommonIOS. Unpack content of **PSA.zip** and **PSACommon.zip** to the RN's project_dir/ios

If you are using pods please see this guide.

####  Setup XCode project structure

- Right-click on **Frameworks** folder in Project Structure
- Click Add files to "PROJECT_NAME"...
- Select **PSA.framework** and **PSACommon.framework** from your computer.

#### Open **PROJECT_NAME** target

- Open **Build Phases** tab
- Remove **PSA.framework** and **PSACommon.framework** from **Link Binaries with Libraries**
- Open **General** tab
- Drag and drop **PSA.framework** and **PSACommon.framework** into **Embedded Binaries**

##### Enable Push Notifications

Please visit this link to enable Push Notification for iOS devices when using React Native: https://facebook.github.io/react-native/docs/pushnotificationios

##### Install react-native Firebase:

Please see this document for further instruction on how to install Firebase for React Native https://rnfirebase.io/.

## **API Usage**
- init(okayServerAdress: string): Promise<string> (Android Only)
- permissionRequest(): Promise<[]>
- updateDeviceToken(token) (iOS only)
- isEnrolled(): boolean
- enrollProcedure(SpaEnrollData): Promise
- linkTenant(linkingCode: number, SpaStorage): Promise
- unlinkTenant(tenantId: number, SpaStorage): Promise
- isReadyForAuthorization(): boolean
- authorization(SpaAuthorizationData): Promise

### **SDK Initialization(Android Only):**

We will need to call the init(endpoint) on the SDK to properly initialize Okay server address on Android. For example we pass in 'https://demostand.okaythis.com/' as our server endpoint.

```javascript
CompontentDidMount() {
  RNOkaySdk.init("https://demostand.okaythis.com/").then(response =>
     ...
  );
}
```

For Okay to correctly, you are required to prompt the user to grant the following permissions return by  *permissionRequest()* method.

```javascript
// fetches an array of required permissions  
RNOkaySdk.permissionRequest().then(response => console.log(response)); 
```

### **Update Okay with Token received from PushNotificationsIOS (iOS ONLY)**
We will need to update Okay with push notification token generated for iOS devices.

```javascript
// We can update iOS PNS token in this lifecycle method here
CompontentDidMount() {
  PushNotificationIOS.addEventListener('register', token => {
    RNOkaySdk.updateDeviceToken(token);
    });
}
```

### **How to enrollment a user**
If the permission above have been granted we can now proceed to enrolling the user. Okay SDK provides the *enrollProcedure(SpaEnrollData)* method which takes a Json with "SpaEnrollData" as key. 

```javascript
    firebase.iid().getToken()
      .then(token => {
        RNOkaySdk.enrollProcedure({
          SpaEnrollData: {
            host: "https://demostand.okaythis.com/", // Okay server address
            appPns: token,
            pubPss: pubPssBase64, 
            installationId: "9990", 
            pageTheme: { 
              // Page Theme customization, if you don't want customization: pageTheme: null.
              actionBarTitle: "YOUR_ACTION_BAR_TITLE",
              actionBarBackgroundColor: "#ffffff",
              actionBarTextColor: "#ffffff",
              buttonTextColor: "#ffffff",
            }
          }
        }).then(response => console.log(response));
      })
      .catch(error => console.log(error));
```
SpaEnrollData contains several keys that are required for a secure communication with Okay servers. 

*"appPns"*: This is your push notification token from Firebase(This allows us to send notification to Android devices). For testing purposes we ask our users to use this value **9990** as their installationId

 *"pubPss"*: This is a public key we provide to applications that use our SDK for secure communication with our server. For testing purposes we ask our users to use the value below as their *"pubPss"* key.

 ```javascript
  const pubPssBase64 = 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxgyacF1NNWTA6rzCrtK60se9fVpTPe3HiDjHB7MybJvNdJZIgZbE9k3gQ6cdEYgTOSG823hkJCVHZrcf0/AK7G8Xf/rjhWxccOEXFTg4TQwmhbwys+sY/DmGR8nytlNVbha1DV/qOGcqAkmn9SrqW76KK+EdQFpbiOzw7RRWZuizwY3BqRfQRokr0UBJrJrizbT9ZxiVqGBwUDBQrSpsj3RUuoj90py1E88ExyaHui+jbXNITaPBUFJjbas5OOnSLVz6GrBPOD+x0HozAoYuBdoztPRxpjoNIYvgJ72wZ3kOAVPAFb48UROL7sqK2P/jwhdd02p/MDBZpMl/+BG+qQIDAQAB'

 ```
 
  *"installationId"*:  is also a number that we also provide for applications that use our SDK. For testing purposes we ask our users to use this value **9990** as their installationId
 
  *"pageTheme"*: This is a JSON object that allows you to customize the colors for our enrollment and authorization screens to suit your product branding. [Click here to see all valid color properties](https://github.com/Okaythis/okay-sdk-android/wiki/PageTheme-(Android))


### **How to link a user**
The linkTenant(linkingCode, SpaStorage) method links a user of your application with an existing tenant on Okay Cloud Based solution. When you make a linking request to your Okay servers, it returns a linkingCode as part of its response (For more information on how to send a linking request please see this [documatation](https://okaythis.com/developer/documentation/v1/server#1.2)). The linking code can be passed directly to this method after a a request to Okay Service or entered by a user via a user interface. This depends on your implementation and business logic.
The **externalId** can be retrieved from the *RNOkaySdk.enrollProcedure(...).then( externalId => ...)* method, if the method was called and executed successfully.

```javascript
  firebase.iid().getToken()
    .then(token => {
      RNOkaySdk.linkTenant(
        linkingCode,
        {
          SpaStorage: {
            appPns: token,
            pubPss: pubPssBase64,
            externalId: 'YOUR_EXTERNAL_ID',
            installationId: "9990",
            enrollmentId: null
          }
        })
  })
```

### **How to unlink a user**
If a user was successfully linked to a tenant and you now wish to unlink that user from your tenant on Okay, you can use the *unlinkTenant(tenantId, SpaStorage)* method to do this as described below.

```javascript
  firebase.iid().getToken()
    .then(token => {
      RNOkaySdk.unlinkTenant(
        tenantId,
        {
          SpaStorage: {
            appPns: token,
            pubPss: pubPssBase64,
            externalId: 'YOUR_EXTERNAL_ID',
            installationId: "9990",
            enrollmentId: null
          }
      })
  })
```


### **Authorizing a User's Action**
When there is a transaction that needs to be authorized by your application, Okay sends a push notification to your mobile app with details needed to process the authorization on the client side. The body of the push notification has the following shape as shown below:

```json
{ 
  tenantId: <int>,
  sessionId: <int> 
}
```

When the push notification is received on the client side, you can retrieve the **_sessionId_** from the push notification body and in turn, pass the value to *RNOkaySdk.authorization(SpaAuthorizationData)* method directly.

```javascript
  CompontentDidMount() {
    firebase.messaging().onMessage(message => {
        startAuthorization(message.data.sessionId);
      });
  }

  startAuthorization = (sessionId) => {
    firebase.iid().getToken()
      .then(token => {
        RNOkaySdk.authorization({
          SpaAuthorizationData: {
            sessionId: sessionId, // Received from firebase messaging
            appPns: token,
            pageTheme: { // Page Theme customization, if you don't want customization: pageTheme: null
              actionBarTitle: "YOUR_ACTION_BAR_TITLE",
              actionBarBackgroundColor: "#eeffc5",
              actionBarTextColor: "#ffcc34",
              buttonTextColor: "#ccccc4",
            }
          }
        }).then(response => console.log(response));
      })
      .catch(error => console.log(error));
  }
```

### Page Theme properies for Android

- https://github.com/Okaythis/okay-sdk-android/wiki/PageTheme-(Android)
