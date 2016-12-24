# GPS-Accelerometer Android App

An Android app that periodically collects data from the GPS and accelerometer sensors and stores it on a local buffer. If internet access is enabled by the user, it uploads the stored data to the server once the local buffer reaches a threshold.

- The *java* and *layout* folders contain all the files used in the Android app.
- The *web_server* folder contains the PHP file that lets the Android app post a file to the server by the HTTP POST method.
- The *screenshots* folder contains the screenshots of the app. The sampling rate of the GPS and accelerometer sensors are controlled by the UI. The buffer storage threshold after which the stored data should be sent to the server, and the access of wireless networks is defined in the UI, too.
