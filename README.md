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

## **Development Environment**
* Android Studio IDE (Java)
* Python in Raspberry Pi Terminal

## **Reference**
* Face Recognition : https://github.com/pillarpond/face-recognizer-android
* CameraView Library : https://github.com/natario1/CameraView
* Real-Time Face Detection on Android with ML Kit and CameraView : https://heartbeat.fritz.ai/building-a-real-time-face-detector-in-android-with-ml-kit-f930eb7b36d9
