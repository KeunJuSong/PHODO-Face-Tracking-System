import serial
import threading
import time
import re
import RPi.GPIO as GPIO
from pathlib import Path

GPIO.setmode(GPIO.BOARD)
# Set Stepper motor pins to 7, 11, 13, 15 (Depending on Board Number)
ControlPin = [7, 11, 13, 15]
# Set Stepper motor pins to 31, 33, 35, 37 (Depending on Board Number)
ControlPin2 = [31, 33, 35, 37]

for pin in ControlPin:
    GPIO.setup(pin, GPIO.OUT)
    GPIO.output(pin,0)
for pin in ControlPin2:
    GPIO.setup(pin, GPIO.OUT)
    GPIO.output(pin,0)
# Set pins End.

# 4 Sequences
seq = [ [1, 0, 0, 0],
		[0, 1, 0, 0],
		[0, 0, 1, 0],
		[0, 0, 0, 1] ]

b_device = Path("/dev/rfcomm0")

crd_x = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
crd_y = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
faceInfo = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
cnt = 0
fin_x = 0
fin_y = 0
crd = [0, 0]
tmp = [0, 0]
tmp2 = [0, 0]
exception_flag = 0
faceId = 0
coord = 0
faceNum = 0

def readSerialLine():
    while(1):
        global exception_flag
        global fin_x
        global fin_y
        global coord
        global faceNum
        if(b_device.exists() == True):
            # Read Coordinates String via Bluetooth Communication
            try:
                ser = serial.Serial('/dev/rfcomm0')
                coord = ser.readline()
                coord = coord.decode('utf-8')
                coord = coord[0:-2]
                faceNum = coord.split(':')
                # faceNum[0] => crd infos, faceNum[1] => number of faces
            except serial.serialutil.SerialException:
                fin_x = 500
                fin_y = 1000
                print("Bluetooth connection lost")
                exception_flag = 1
                time.sleep(0.5)
                break
        else:
            continue

        fn = int(faceNum[1])
        # crd#[0] => crd_x, crd#[1] => crd_y
        if(fn == 1):
            fi1 = faceNum[0][0:-1]
            crd1 = fi1.split('/')
            print("faces = 1")
            print(crd1)
            fin_x = float(crd1[0])
            fin_y = float(crd1[1])
        elif(fn == 2):
            fi2 = re.split('!', faceNum[0][0:-1])
            crd1 = fi2[0].split('/')
            crd2 = fi2[1].split('/')
            print("faces = 2")
            print(crd1)
            print(crd2)
            fin_x = (float(crd1[0]) + float(crd2[0]))/2
            fin_y = (float(crd1[1]) + float(crd2[1]))/2
        elif(fn == 3):
            fi3 = re.split('!|"', faceNum[0][0:-1])
            crd1 = fi3[0].split('/')
            crd2 = fi3[1].split('/')
            crd3 = fi3[2].split('/')
            print("faces = 3")
            print(crd1)
            print(crd2)
            print(crd3)
            fin_x = (float(crd1[0]) + float(crd2[0]) + float(crd3[0]))/3
            fin_y = (float(crd1[1]) + float(crd2[1]) + float(crd3[1]))/3
        elif(fn == 4):
            fi4 = re.split('!|"|#', faceNum[0][0:-1])
            crd1 = fi4[0].split('/')
            crd2 = fi4[1].split('/')
            crd3 = fi4[2].split('/')
            crd4 = fi4[3].split('/')
            print("faces = 4")
            print(crd1)
            print(crd2)
            print(crd3)
            print(crd4)
            fin_x = (float(crd1[0]) + float(crd2[0]) + float(crd3[0]) + float(crd4[0]))/4
            fin_y = (float(crd1[1]) + float(crd2[1]) + float(crd3[1]) + float(crd4[1]))/4
        elif(fn == 5):
            fi5 = re.split('!|"|#|$', faceNum[0][0:-1])
            crd1 = fi5[0].split('/')
            crd2 = fi5[1].split('/')
            crd3 = fi5[2].split('/')
            crd4 = fi5[3].split('/')
            crd5 = fi5[4].split('/')
            print("faces = 5")
            print(crd1)
            print(crd2)
            print(crd3)
            print(crd4)
            print(crd5)
            fin_x = (float(crd1[0]) + float(crd2[0]) + float(crd3[0]) + float(crd4[0]) + float(crd5[0]))/5
            fin_y = (float(crd1[1]) + float(crd2[1]) + float(crd3[1]) + float(crd4[1]) + float(crd5[1]))/5
        else:
            fin_x = 500
            fin_y = 1000
        time.sleep(0.01)
        print("fin_x = ", end='')
        print(fin_x, end='')
        print(", fin_y = ", end='')
        print(fin_y)

def runStepperX():
    while True:
        global fin_x
        global fin_y
        global ControlPin2
        global seq
        global exception_flag

        manX_rSpeed = (fin_x - 600)/50000
        manX_lSpeed = (400 - fin_x)/50000

        if(exception_flag == 1):
            exception_flag = 0
            fin_x = 500
            fin_y = 1000
            time.sleep(0.5)
            break

        # Run Stepper Motor to Right
        if(fin_x > 600):
            for halfstep in range(4):
                for pin in range(4):
                    GPIO.output(ControlPin2[pin], seq[halfstep][pin])
                time.sleep(0.0105 - manX_rSpeed)
        # Run Stepper Motor to Left
        elif(fin_x < 400):
            for halfstep in range(4):
                for pin in range(4):
                    GPIO.output(ControlPin2[3-pin], seq[halfstep][pin])
                time.sleep(0.0105 - manX_lSpeed)

def runStepperY():
    while True:
        global fin_y
        global ControlPin
        global seq
        global exception_flag

        manY_uSpeed = (fin_y - 1100)/100000
        manY_dSpeed = (900 - fin_y)/100000

        # Run Stepper Motor Upward
        if(fin_y > 1100):
            for halfstep in range(4):
                for pin in range(4):
                    GPIO.output(ControlPin[pin], seq[halfstep][pin])
                time.sleep(0.0115 - manY_uSpeed)
        # Run Stepper Motor to Downward
        elif(fin_y < 900):
            for halfstep in range(4):
                for pin in range(4):
                    GPIO.output(ControlPin[3-pin], seq[halfstep][pin])
                time.sleep(0.0100 - manY_dSpeed)

def readSerialLine_thread():
    thread = threading.Thread(target = readSerialLine)
    thread.daemon = True
    thread.start()

def runStepperY_thread():
    thread = threading.Thread(target = runStepperY)
    thread.daemon = True
    thread.start()

while(1):
    if(b_device.exists() == True):
        readSerialLine_thread()
        runStepperY_thread()
        runStepperX()
    else:
        print("Bluetooth not connected")
        time.sleep(5)
