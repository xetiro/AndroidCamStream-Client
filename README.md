# AndroidCamStream-Client

A prototype Android client aplication that streams frames, from the front camera, to a server at a certain resolution (i.e. 160x120, 320x240, 640x480, etc) and frequency (i.e. 1hz, 2hz, 5hz, etc).

## Quick Overview

### Motivation
This application was developed as a prototype client Android application (AndroidCamStream-Client), to allow to experiment with the use-case of client-server image processing, where the users mobile device is the source of the frames and the remote server (AndroidCamStream-Server) is where the image processing can be done.

### User Interaction
The user is first prompt to login into a server (LoginActivity) and then prompt to start streaming the front camera live feed (MainActivity). While streaming the user can change the frequency in real-time (Seekbar) but not the resolution. To change the resolution the user must stop streaming first, change the resolution (Spinner) and then start streaming again. 

## Android Compatibility
This application should be compatible with most smartphones and tablets, both in portrait and landscape orientation. Additional an Android TV launcher is also included on the manifest, so this application can run as it is on Android TV's. During development, this app was tested on a Sony Xperia Z5 Compact, Samsung Galaxy S8 and Android TV Emulator on a Macbook Pro 16".

- SDK requirements:
  - minSdkVersion 23
  - targetSdkVersion 29
  - compileSdkVersion 29
  
### User permissions 
Although, this application is meant for experiment under controled use-cases at a prototype level, it actually implements the proper prompts for dangerous permissions, in order to give the user the control to allow/deny the app from accessing the camera. However, keep in mind that without the camera permission this app becomes useless. 

## Architecture

// TODO

## How to run
Just clone the project and open with the latest version of Android Studio. Update/sync your gradle. Build/Run the project. Make sure you have compileSdkVersion of the SDK Tools installed, and you shouldn't major problems. For your reference, the original code was written with Android Studio 4.0.1.

## License
The sourcecode of this project is public under the MIT license. Feel free to fork it and change it at your will to customized it to your own projects.

> The MIT License is short and to the point. It lets people do almost anything they want with your project, like making and distributing closed source versions.
