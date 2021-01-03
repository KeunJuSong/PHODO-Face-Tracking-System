# **PHODO MCU(Micro Controller Unit)**

## **Overview**
* PHODO MCU : Raspberry Pi Zero W
* Control Stepping Motor and Servo Motor for tracking action
* Link between PHODO application with Bluetooth
* See [phodo_final.py](PHODO-Face-Tracking-System/PHODO_Raspberry Pi/PHODO_Tracking algorithm/phodo_final.py) is setting code for PHODO system.
* See **PHODO_Tracking algorithm** folder. It is setting code for PHODO system.

## **Function of PHODO MCU**
* **Bluetooth**
  * **PySerial** 
    * Open source library for implementing Bluetooth communication as serial port communication
    * It encapsulates access to a serial port.
    * Using this library, it allows Bluetooth communication with only a few short lines of code.
  
  * **blueZ**
    * pySeiral requires another package to make Bluetooth communication behave as if it were connected to a serial port.
    * We use the RFCOMM protocol in the blueZ package.
  
  * **Bluetooth Data parsing**
   <img src="https://user-images.githubusercontent.com/48046183/103474346-52172f00-4de6-11eb-9625-b8c94ea73dce.png" width="60%">

* **Control Motor**
  * **Servo Motor**
    * CN0023 Servo Motor - Vertical Control Motor
  
  * **Stepping Motor**
    * 28BYJ-48 Stepping Motor - Horizontal Control Motor
  
  * **pigpiod**
    * pigpiod is a package library to prevent Jittering phenomenon in Servo Motor.
    * It is consisted of C langauge, so the vibration of the servo motor can be eliminated by dividing the hardware control and operation processing at the low level to prevent interference from occurring.

## **Circuit diagram**
<img src="https://user-images.githubusercontent.com/48046183/103474524-33199c80-4de8-11eb-9564-911c5b03ea68.png" width="70%">

## **Refernece**
* **PySerial** : https://pyserial.readthedocs.io/en/latest/pyserial.html
* **BlueZ** : http://www.bluez.org/
* **pigpiod** : http://abyz.me.uk/rpi/pigpio/index.html
