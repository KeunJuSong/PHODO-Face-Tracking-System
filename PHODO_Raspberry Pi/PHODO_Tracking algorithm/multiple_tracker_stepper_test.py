import serial
import threading
import time
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

def getFaceInfo():
    while(1):
        global exception_flag
        global faceInfo
        global crd
        global fin_x
        global fin_y
        global tmp
        global tmp2
        global faceId
        if(b_device.exists() == True):
            # Read Coordinates String via Bluetooth Communication
            try:
                ser = serial.Serial('/dev/rfcomm0')
                coord = ser.readline()
                coord = coord.decode('utf-8')
                coord = coord[0:-2]
            except serial.serialutil.SerialException:
                fin_x = 500
                fin_y = 1000
                print("Bluetooth connection lost")
                exception_flag = 1
                time.sleep(0.5)
                break
            else:
                pass
            crd = coord.split('/')
            tmp = crd[1].split(',')
            tmp2 = tmp[1].split(';')
            # crd[0] => crd_x tmp[0] => crd_y, tmp2[0] => face Id, tmp2[1] => number of faces
            faceId = int(tmp2[0])%10

            faceInfo[faceId] = [int(float(crd[0])), int(float(tmp[0]))]
            # print(faceInfo)

def getCoord():
    while(True):
        global crd_x
        global crd_y
        global fin_x
        global fin_y
        global faceInfo
        global crd
        global tmp
        global tmp2
        global faceId

        if(tmp2[1] == '1'):
            crd_x[0] = int(float(crd[0]))
            crd_y[0] = int(float(tmp[0]))
            print("Face ", end='')
            print(1)
            print("x = ", end='')
            print(crd_x[0])
            print("y = ", end='')
            print(crd_y[0])

        elif(tmp2[1] == '2'):
            if(faceId == 0):
                crd_x[0] = faceInfo[0][0]
                crd_y[0] = faceInfo[0][1]
                print("Face ", end='')
                print(1)
                print("x = ", end='')
                print(crd_x[0])
                print("y = ", end='')
                print(crd_y[0])
            elif(faceId == 1):
                crd_x[1] = faceInfo[1][0]
                crd_y[1] = faceInfo[1][1]
                print("Face ", end='')
                print(2)
                print("x = ", end='')
                print(crd_x[1])
                print("y = ", end='')
                print(crd_y[1])
            elif(faceId == 2):
                crd_x[2] = faceInfo[2][0]
                crd_y[2] = faceInfo[2][1]
                print("Face ", end='')
                print(3)
                print("x = ", end='')
                print(crd_x[2])
                print("y = ", end='')
                print(crd_y[2])
            elif(faceId == 3):
                crd_x[3] = faceInfo[3][0]
                crd_y[3] = faceInfo[3][1]
                print("Face ", end='')
                print(4)
                print("x = ", end='')
                print(crd_x[3])
                print("y = ", end='')
                print(crd_y[3])
            elif(faceId == 4):
                crd_x[4] = faceInfo[4][0]
                crd_y[4] = faceInfo[4][1]
                print("Face ", end='')
                print(5)
                print("x = ", end='')
                print(crd_x[4])
                print("y = ", end='')
                print(crd_y[4])
            elif(faceId == 5):
                crd_x[5] = faceInfo[5][0]
                crd_y[5] = faceInfo[5][1]
                print("Face ", end='')
                print(6)
                print("x = ", end='')
                print(crd_x[5])
                print("y = ", end='')
                print(crd_y[5])
            elif(faceId == 6):
                crd_x[6] = faceInfo[6][0]
                crd_y[6] = faceInfo[6][1]
                print("Face ", end='')
                print(7)
                print("x = ", end='')
                print(crd_x[6])
                print("y = ", end='')
                print(crd_y[6])
            elif(faceId == 7):
                crd_x[7] = faceInfo[7][0]
                crd_y[7] = faceInfo[7][1]
                print("Face ", end='')
                print(8)
                print("x = ", end='')
                print(crd_x[7])
                print("y = ", end='')
                print(crd_y[7])
            elif(faceId == 8):
                crd_x[8] = faceInfo[8][0]
                crd_y[8] = faceInfo[8][1]
                print("Face ", end='')
                print(9)
                print("x = ", end='')
                print(crd_x[8])
                print("y = ", end='')
                print(crd_y[8])
            elif(faceId == 9):
                crd_x[9] = faceInfo[9][0]
                crd_y[9] = faceInfo[9][1]
                print("Face ", end='')
                print(10)
                print("x = ", end='')
                print(crd_x[9])
                print("y = ", end='')
                print(crd_y[9])

        elif(tmp2[1] == '3'):
            if(faceId == 0):
                crd_x[0] = faceInfo[0][0]
                crd_y[0] = faceInfo[0][1]
                print("Face ", end='')
                print(1)
                print("x = ", end='')
                print(crd_x[0])
                print("y = ", end='')
                print(crd_y[0])
            elif(faceId == 1):
                crd_x[1] = faceInfo[1][0]
                crd_y[1] = faceInfo[1][1]
                print("Face ", end='')
                print(2)
                print("x = ", end='')
                print(crd_x[1])
                print("y = ", end='')
                print(crd_y[1])
            elif(faceId == 2):
                crd_x[2] = faceInfo[2][0]
                crd_y[2] = faceInfo[2][1]
                print("Face ", end='')
                print(3)
                print("x = ", end='')
                print(crd_x[2])
                print("y = ", end='')
                print(crd_y[2])
            elif(faceId == 3):
                crd_x[3] = faceInfo[3][0]
                crd_y[3] = faceInfo[3][1]
                print("Face ", end='')
                print(4)
                print("x = ", end='')
                print(crd_x[3])
                print("y = ", end='')
                print(crd_y[3])
            elif(faceId == 4):
                crd_x[4] = faceInfo[4][0]
                crd_y[4] = faceInfo[4][1]
                print("Face ", end='')
                print(5)
                print("x = ", end='')
                print(crd_x[4])
                print("y = ", end='')
                print(crd_y[4])
            elif(faceId == 5):
                crd_x[5] = faceInfo[5][0]
                crd_y[5] = faceInfo[5][1]
                print("Face ", end='')
                print(6)
                print("x = ", end='')
                print(crd_x[5])
                print("y = ", end='')
                print(crd_y[5])
            elif(faceId == 6):
                crd_x[6] = faceInfo[6][0]
                crd_y[6] = faceInfo[6][1]
                print("Face ", end='')
                print(7)
                print("x = ", end='')
                print(crd_x[6])
                print("y = ", end='')
                print(crd_y[6])
            elif(faceId == 7):
                crd_x[7] = faceInfo[7][0]
                crd_y[7] = faceInfo[7][1]
                print("Face ", end='')
                print(8)
                print("x = ", end='')
                print(crd_x[7])
                print("y = ", end='')
                print(crd_y[7])
            elif(faceId == 8):
                crd_x[8] = faceInfo[8][0]
                crd_y[8] = faceInfo[8][1]
                print("Face ", end='')
                print(9)
                print("x = ", end='')
                print(crd_x[8])
                print("y = ", end='')
                print(crd_y[8])
            elif(faceId == 9):
                crd_x[9] = faceInfo[9][0]
                crd_y[9] = faceInfo[9][1]
                print("Face ", end='')
                print(10)
                print("x = ", end='')
                print(crd_x[9])
                print("y = ", end='')
                print(crd_y[9])

        elif(tmp2[1] == '4'):
            if(faceId == 0):
                crd_x[0] = faceInfo[0][0]
                crd_y[0] = faceInfo[0][1]
                print("Face ", end='')
                print(1)
                print("x = ", end='')
                print(crd_x[0])
                print("y = ", end='')
                print(crd_y[0])
            elif(faceId == 1):
                crd_x[1] = faceInfo[1][0]
                crd_y[1] = faceInfo[1][1]
                print("Face ", end='')
                print(2)
                print("x = ", end='')
                print(crd_x[1])
                print("y = ", end='')
                print(crd_y[1])
            elif(faceId == 2):
                crd_x[2] = faceInfo[2][0]
                crd_y[2] = faceInfo[2][1]
                print("Face ", end='')
                print(3)
                print("x = ", end='')
                print(crd_x[2])
                print("y = ", end='')
                print(crd_y[2])
            elif(faceId == 3):
                crd_x[3] = faceInfo[3][0]
                crd_y[3] = faceInfo[3][1]
                print("Face ", end='')
                print(4)
                print("x = ", end='')
                print(crd_x[3])
                print("y = ", end='')
                print(crd_y[3])
            elif(faceId == 4):
                crd_x[4] = faceInfo[4][0]
                crd_y[4] = faceInfo[4][1]
                print("Face ", end='')
                print(5)
                print("x = ", end='')
                print(crd_x[4])
                print("y = ", end='')
                print(crd_y[4])
            elif(faceId == 5):
                crd_x[5] = faceInfo[5][0]
                crd_y[5] = faceInfo[5][1]
                print("Face ", end='')
                print(6)
                print("x = ", end='')
                print(crd_x[5])
                print("y = ", end='')
                print(crd_y[5])
            elif(faceId == 6):
                crd_x[6] = faceInfo[6][0]
                crd_y[6] = faceInfo[6][1]
                print("Face ", end='')
                print(7)
                print("x = ", end='')
                print(crd_x[6])
                print("y = ", end='')
                print(crd_y[6])
            elif(faceId == 7):
                crd_x[7] = faceInfo[7][0]
                crd_y[7] = faceInfo[7][1]
                print("Face ", end='')
                print(8)
                print("x = ", end='')
                print(crd_x[7])
                print("y = ", end='')
                print(crd_y[7])
            elif(faceId == 8):
                crd_x[8] = faceInfo[8][0]
                crd_y[8] = faceInfo[8][1]
                print("Face ", end='')
                print(9)
                print("x = ", end='')
                print(crd_x[8])
                print("y = ", end='')
                print(crd_y[8])
            elif(faceId == 9):
                crd_x[9] = faceInfo[9][0]
                crd_y[9] = faceInfo[9][1]
                print("Face ", end='')
                print(10)
                print("x = ", end='')
                print(crd_x[9])
                print("y = ", end='')
                print(crd_y[9])

        elif(tmp2[1] == '5'):
            if(faceId == 0):
                crd_x[0] = faceInfo[0][0]
                crd_y[0] = faceInfo[0][1]
                print("Face ", end='')
                print(1)
                print("x = ", end='')
                print(crd_x[0])
                print("y = ", end='')
                print(crd_y[0])
            elif(faceId == 1):
                crd_x[1] = faceInfo[1][0]
                crd_y[1] = faceInfo[1][1]
                print("Face ", end='')
                print(2)
                print("x = ", end='')
                print(crd_x[1])
                print("y = ", end='')
                print(crd_y[1])
            elif(faceId == 2):
                crd_x[2] = faceInfo[2][0]
                crd_y[2] = faceInfo[2][1]
                print("Face ", end='')
                print(3)
                print("x = ", end='')
                print(crd_x[2])
                print("y = ", end='')
                print(crd_y[2])
            elif(faceId == 3):
                crd_x[3] = faceInfo[3][0]
                crd_y[3] = faceInfo[3][1]
                print("Face ", end='')
                print(4)
                print("x = ", end='')
                print(crd_x[3])
                print("y = ", end='')
                print(crd_y[3])
            elif(faceId == 4):
                crd_x[4] = faceInfo[4][0]
                crd_y[4] = faceInfo[4][1]
                print("Face ", end='')
                print(5)
                print("x = ", end='')
                print(crd_x[4])
                print("y = ", end='')
                print(crd_y[4])
            elif(faceId == 5):
                crd_x[5] = faceInfo[5][0]
                crd_y[5] = faceInfo[5][1]
                print("Face ", end='')
                print(6)
                print("x = ", end='')
                print(crd_x[5])
                print("y = ", end='')
                print(crd_y[5])
            elif(faceId == 6):
                crd_x[6] = faceInfo[6][0]
                crd_y[6] = faceInfo[6][1]
                print("Face ", end='')
                print(7)
                print("x = ", end='')
                print(crd_x[6])
                print("y = ", end='')
                print(crd_y[6])
            elif(faceId == 7):
                crd_x[7] = faceInfo[7][0]
                crd_y[7] = faceInfo[7][1]
                print("Face ", end='')
                print(8)
                print("x = ", end='')
                print(crd_x[7])
                print("y = ", end='')
                print(crd_y[7])
            elif(faceId == 8):
                crd_x[8] = faceInfo[8][0]
                crd_y[8] = faceInfo[8][1]
                print("Face ", end='')
                print(9)
                print("x = ", end='')
                print(crd_x[8])
                print("y = ", end='')
                print(crd_y[8])
            elif(faceId == 9):
                crd_x[9] = faceInfo[9][0]
                crd_y[9] = faceInfo[9][1]
                print("Face ", end='')
                print(10)
                print("x = ", end='')
                print(crd_x[9])
                print("y = ", end='')
                print(crd_y[9])

        tmpcnt = int(float(tmp2[1]))
        if(tmpcnt == 0):
            tmpcnt = 1
        fin_x = (crd_x[0] + crd_x[1] + crd_x[2] + crd_x[3] + crd_x[4] + crd_x[5] + crd_x[6] + crd_x[7] + crd_x[8] + crd_x[9])/tmpcnt
        fin_y = (crd_y[0] + crd_y[1] + crd_y[2] + crd_y[3] + crd_y[4] + crd_y[5] + crd_y[6] + crd_y[7] + crd_y[8] + crd_y[9])/tmpcnt
        print("fin_x = ", end='')
        print(fin_x)
        print("fin_y = ", end='')
        print(fin_y)

        time.sleep(0.05)
        crd_x=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        crd_y=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

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

# Set defined functions to Thread
def getFaceInfo_thread():
    thread=threading.Thread(target=getFaceInfo)
    thread.daemon=True
    thread.start()

def getCoord_thread():
    thread=threading.Thread(target=getCoord)
    thread.daemon=True
    thread.start()

def runStepperY_thread():
    thread=threading.Thread(target=runStepperY)
    thread.daemon=True
    thread.start()

# Main
while True:
    if (b_device.exists() == True):
        getFaceInfo_thread()
        getCoord_thread()
        runStepperY_thread()
        runStepperX()
    else:
        print("Bluetooth not connected")
        time.sleep(5)
