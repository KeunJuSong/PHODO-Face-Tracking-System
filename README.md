<img src="https://user-images.githubusercontent.com/48046183/103460806-afff3480-4d5c-11eb-88e3-e6636b4c3b1e.PNG" width="15%">

# **PHODO : Face Tracking System**

## **Developers & Oragnization**
### **Devlopers**
* Keun Ju Song
* Yechan Yun
* HyeKyung Kim
* SoHee Park
* JungYoong Min
* Su Min Kim (academic adviser)
### **Oragnization**
* Korea Polytechnic University LINC+
* 20th Industrial Technology Exhibition, Korea Polytechnic University (http://expo.kpu.ac.kr/)

## **Overview**
PHODO is face tracking system that consisted of the main body and mobile app.

PHODO's face tracking technology is based on Machine Learning.

From this system, we can anticipate applications in Entertainment and Security field with facial similarity.

See more information about PHODO : **https://yyc9920.github.io/phodo.github.io/index.html**

## **System Architecture**

### **System Block Diagram**
<img src="https://user-images.githubusercontent.com/48046183/103452023-6982e900-4d0e-11eb-9d64-eec16f13facf.png" width="70%">

### **H/W**
* Raspberry Pi Zero W (MCU)
* Stepping Motors (2EA)
* Servo Motor
* LiPo battery
* Rechargeable 5V Lipo USB Boost - Adafruit
* Ultrasonic Sensor - HC-SR04
* Tripod 
* Camera Rail

### **Application**
#### **PHODO** 
* Supporting basic camera function
* Tracking Faces 
* Supporting Buletooth that link the main body 

### **3D Modeling**
* Design the external apperance by JungYoong Min

<p float="left">
<img src="https://user-images.githubusercontent.com/48046183/103457424-9bae3e00-4d42-11eb-89a6-bca5c761262f.png" width="40%">
<img src="https://user-images.githubusercontent.com/48046183/103457426-9e109800-4d42-11eb-99b2-22fd8b5983a1.png" width="40%">
</p>


## **Function of the System**
* **Multiple Face Tracking**: When there are more than one face on the screen, tracking the coordinates of the faces by averaging operations.
* **User's Face Customizing**: PHODO can recognize the face individually, so it can tracking just one person that user want.
* **Handsfree Capture**: PHODO have voice recognition to support the handsfree capture. Setting voice language is Korean.
* **3-Div Optimal Composition**: PHODO can take the photo automatically with optimal composition by controling face coordinates. Face position is established Left-Center-Right.

## **Result**
* **PHODO main body**
<img src="https://user-images.githubusercontent.com/48046183/103452046-a64ee000-4d0e-11eb-8903-77714bf11989.jpg" width="50%">

* **PHODO Application**
 <p float="left">
 <img src="https://user-images.githubusercontent.com/48046183/103452071-e4e49a80-4d0e-11eb-8d2f-90ec86fb5758.jpg" width="25%">
 <img src="https://user-images.githubusercontent.com/48046183/103452076-ee6e0280-4d0e-11eb-8693-f192eb55da96.jpg" width="25%">
 <img src="https://user-images.githubusercontent.com/48046183/103452077-ef9f2f80-4d0e-11eb-8961-c9dfc41aa7c1.jpg" width="25%">
 </p>

* **PHODO Face Tracking action**
<img src="https://user-images.githubusercontent.com/48046183/103651923-a583ab80-4fa5-11eb-9cc7-a64b90c0c8b9.gif" width="70%">

## **Development Environment**
* Android Studio IDE (Java, C++, Kotlin)
* Raspberry Pi Terminal (Python)

## **Reference**
* Face Recognition : https://github.com/pillarpond/face-recognizer-android
* CameraView Library : https://github.com/natario1/CameraView
* Real-Time Face Detection on Android with ML Kit and CameraView : https://heartbeat.fritz.ai/building-a-real-time-face-detector-in-android-with-ml-kit-f930eb7b36d9
