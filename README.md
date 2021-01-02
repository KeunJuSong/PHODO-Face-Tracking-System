# **PHODO-Face-Tracking-System**

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
* 20th Industrial Technology Exhibition, Korea Polytechnic University.

## **OverView**
PHODO is face tracking system that consisted of the main body and mobile app.

PHODO's face tracking technology is based on Machine Learning.

From this system, we can anticipate applications in Entertainment and Security field with facial similarity.

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

## **Function of the System**
* **Multiple Face Tracking**: When there are more than one face on the screen, tracking the coordinates of the faces by averaging operations.
* **User's Face Customizing**: PHODO can recognize the face individually, so it can tracking just one person that user want.
* **Handsfree Capture**: PHODO have voice recognition to support the handsfree capture. Setting voice language is Korean.
* **3-Div Optimal Composition**: PHODO can take the photo automatically with optimal composition by controling face coordinates. Face position is established Left-Center-Right.

## **Result**
* PHODO main body
<img src="https://user-images.githubusercontent.com/48046183/103452046-a64ee000-4d0e-11eb-8903-77714bf11989.jpg" width="50%">

* PHODO Application
  * Main Activity
  <img src="https://user-images.githubusercontent.com/48046183/103452071-e4e49a80-4d0e-11eb-8d2f-90ec86fb5758.jpg" width="25%">
  
  * Face Recognition
  <img src="https://user-images.githubusercontent.com/48046183/103452076-ee6e0280-4d0e-11eb-8693-f192eb55da96.jpg" width="25%">
  
  * Face Detection
  <img src="https://user-images.githubusercontent.com/48046183/103452077-ef9f2f80-4d0e-11eb-8961-c9dfc41aa7c1.jpg" width="25%">

## **Development Environment**
* Android Studio IDE (Java)
* Python in Raspberry Pi Terminal

## **Reference**
* Face Recognition : https://github.com/pillarpond/face-recognizer-android
* CameraView Library : https://github.com/natario1/CameraView
* Real-Time Face Detection on Android with ML Kit and CameraView : https://heartbeat.fritz.ai/building-a-real-time-face-detector-in-android-with-ml-kit-f930eb7b36d9
