# AndroidCamStream-Client

A prototype Android client aplication that streams frames, from the front camera, to a server at a certain resolution (i.e. 160x120, 320x240, 640x480, etc) and frequency (i.e. 1hz, 2hz, 5hz, etc).


## Quick Overview

### Motivation
This application was developed as a prototype client Android application (AndroidCamStream-Client), to allow to experiment with the use-case of client/server image stream processing, where the user's mobile device is the source of the frames and the remote server (AndroidCamStream-Server) is where the images are received for further processing.

### User Interaction
The user is first prompt to login into a server (LoginActivity) and then prompt to start streaming the front camera live feed (MainActivity). While streaming the user can change the frequency in real-time (Seekbar) but not the resolution. To change the resolution the user must stop streaming first, change the resolution (Spinner) and then start streaming again. 

### CameraX API
This app makes extensive use of the Android CameraX API to connect with the device camera. Namely, the following use-cases are fully implemented:

- Preview use-case
Renders the live feed of the camera on the screen. You can grab the image, on the Bitmap format, from the live-feed at the same resolution as rendered on the screen.
- ImageAnalysis use-case
Delivers you the raw image frames directly on the YUV format at the target resolution defined on the use-case.

Both are sources for streaming the frames with different subtilities. The current implementation makes use of the Bitmap taken from the life-feed preview, scaled to the target resolution set by the user, for streaming.

## Android Compatibility
This application should be compatible with most Android smartphones and tablets, both in portrait and landscape orientation. Additionaly, an Android TV launcher is also included on the manifest, so this application can run as it is on Android TV's. During development, this app was tested on a Sony Xperia Z5 Compact, Samsung Galaxy S8 and Android TV Emulator on a Macbook Pro 16".

- SDK requirements:
  - minSdkVersion 23
  - targetSdkVersion 29
  - compileSdkVersion 29
  
### User permissions 
Although, this application is meant for experiment under controled use-case experiments at a prototype level, it actually implements the proper prompts for requesting the user for consent with dangerous permissions. This is needed in order to give the user the control to allow/deny the app from accessing the camera. However, keep in mind that without the camera permission this app becomes useless. 


## Architecture

// TODO

## How to run
Since this is a client/server use-case you need to have an instance of the server application AndroidCamStream-Server running, so you can then connect the AndroidCamStream-Client applicaiton to it. Please visit the repo of the AndroidCamStream-Server for more information on the server-side.

### Running the client
- Clone the project source-code to your local machine.
- Open with the latest version of Android Studio. (For your reference, the original code was written with Android Studio 4.0.1.)
- Update/sync your gradle. 
- Connect a real Android device through USB with Developer permission active or launch a Androide Emulator Device.
  - Android 6.0 (API Level 23) or greater. 
- Build/Run the project.
  - Make sure you have the API Level 29 Build tools installed on your Android Studio.

### Activities
This application is composed of 2 activities: 
- LoginActivity
  - This is the launch activity. It handles the user login with the server.
  - All fields are required, but do not user real data on you username/password. Remember, this is just for testing/experimenting.
   - If the server is launched with authentication enabled, only users/passwords recognised by the server are accepted.
   - Otherwise, any username/password will work as the server will accept any credential.
   - The server ip and server port must be the real ip address and port the server machine is listening to.
   - Please notice that even if the server is running at localhost, the localhost of the emulator is a different address, therefore you must connect the emulator to the real localhost ip.
   - Press login to authenticate with the server
    - If authentication succeeds the app will continue to the second activity
- MainActivity
  - This is the activity that connects to the camera and streams the frames to the server.
  - The Live Feed has a 4:3 320dp x 240dp resolution.
  - Select the resolution you wanna stream to the server.
  - Select the frequency you wanna stream to the server.
  - Press START STREAMING

### Logcat Debug filters
You can check the debug logs outputed by the application, using the following 3 filters with the Log Tag:
- LoginActivityDebug
  - Outputs debug info from the methods from the LoginActivity.
  - Might be interesting to understand if you are successfuly opening the websocket to the server.
- MainActivityDebug
  - Outputs debug info from the methods from the MainActivity.
  - Might be interesting to double check the resolution is being streamed.
- ServerClientDebug
  - Outputs debug info from the callbacks between Client/Server socket communication.
  

## License
The sourcecode of this project is public under the MIT license. Feel free to fork it and change it at your will to customized it to your own projects.

> The MIT License is short and to the point. It lets people do almost anything they want with your project, like making and distributing closed source versions.
