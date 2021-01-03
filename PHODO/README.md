# **PHODO Application**

## **Overview**
* PHODO Application is similar with camera application.
* PHODO Application support the multiple face tracking, handsfree capture, and 3-Div optimal composition.
* PHODO Application have Bluetooth to link between the main body.

## **Function of PHODO Application**
### **Face Tracking**
* **Face Detection** 
<img src="https://user-images.githubusercontent.com/48046183/103459987-2c424980-4d56-11eb-9fea-fcac330553eb.png" width="40%">

Face Detection technology is developed from Google ML Kit API.

Refer Google ML Kit quickstart which is in Github (See Reference)

* **Face Recognition**
<img src="https://user-images.githubusercontent.com/48046183/103460185-a0312180-4d57-11eb-82d2-748fffde287a.png" width="40%">

Face Recognition technology is developed from Tensorflow Lite and FaceNet.

Refer Tensorflow examples and face recognizer project in Github (See [Reference](#https://github.com/KeunJuSong/PHODO-Face-Tracking-System/blob/master/PHODO/README.md#reference)) 

Refer FaceNet paper to understand why we use this model in PHODO Application. (See [Reference](#https://github.com/KeunJuSong/PHODO-Face-Tracking-System/blob/master/PHODO/README.md#reference))


### **Handsfree Capture**
* **Voice Recognition**
<img src="https://user-images.githubusercontent.com/48046183/103460290-56950680-4d58-11eb-8bcd-6b01252c6664.png" width="40%">

Voice Recognition technology is developed from IBM Watson STT and Language Translator API.

Refer IBM Watson voice recognition example in Github (See Reference)

### **3-Div Optimal Composition**
<img src="https://user-images.githubusercontent.com/48046183/103460513-fa32e680-4d59-11eb-8cba-15d40bb04b06.png" width="40%">

3-Div Optimal Composition is controlled by face coordinates and specified screen range.

Specified screen range have 3 parts which are left, center, and right.

### **Bluetooth**
<img src="https://user-images.githubusercontent.com/48046183/103460574-9b21a180-4d5a-11eb-883f-7f3711568f3d.png" width="40%">

To link between MCU, especailly Raspberry Pi Zero W, we use Bluetooth SPP library to support serial communication.

Refer Bluetooth SPP library project in Github (See Reference)

## **Reference**
* Google ML Kit quickstart : https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart
* Tensorflow Lite object detection example : https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection/android
* Face Recognizer android - project : https://github.com/pillarpond/face-recognizer-android
* FaceNet paper : [FaceNet.pdf](https://github.com/KeunJuSong/PHODO-Face-Tracking-System/files/5760453/FaceNet.pdf)
* IBM Watson voice recognition : https://github.com/watson-developer-cloud/speech-android-sdk
* Bluetooth SPP library : https://github.com/akexorcist/BluetoothSPPLibrary
