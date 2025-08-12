# Aira AIDL Integration Demo App

This is a demo application that demonstrates how to control Aira Explorer app using Android's AIDL (Android Interface Definition Language) interface. The app shows how to connect to Aira's service and control call features such as microphone, camera, and ending calls.

| <img src="screenshots/Screenshot_1.png" width=250/> | <img src="screenshots/Screenshot_2.png" width=250 /> |
|-------------------------------------|-----------------------------------|

### Usage in Android Applications

To use Aira's AIDL interface in your Android application:

1. Copy the [`AiraAidlInterface.aidl`](app/src/main/aidl/io/aira/aira_call/AiraAidlInterface.aidl) file to your Android project's `src/main/aidl/io/aira/aira_call/` directory. The path and file name should match exactly. Otherwise, the service won't connect properly.

2. Enable AIDL in your app gradle file (gradle.kts)

    ```kts
      buildFeatures {
            aidl = true
        }
    ```
3. Add permission to your AndroidManifest.xml

    ```xml
    <uses-permission android:name="io.aira.aira_call.permission.BIND_AIDL_SERVICE" />
    ```
   
4. Build your app. The SDK tools will generate the IBinder interface file in your project's `gen/` directory.

5. Bind to the service in your Android code:

    ```kotlin
    private var airaService: AiraAidlInterface? = null
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            airaService = AiraAidlInterface.Stub.asInterface(service)
        }
    
        override fun onServiceDisconnected(name: ComponentName?) {
            airaService = null
        }
    }
    
    // Bind to the service
    val intent = Intent().apply {
        setPackage("io.aira.explorer") // You can change this to any of our Aira apps. 
        action = "io.aira.aira_call.AiraAidlInterface"
    }
    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    
    ```

6. Use the interface methods:

    ```kotlin
    // Check if in call
    val inCall = airaService?.isInCall() ?: false
    
    // Toggle microphone
    airaService?.setMicrophoneEnabled(true)
    
    // End call
    if (inCall) {
        airaService?.endCall()
    }
    ```