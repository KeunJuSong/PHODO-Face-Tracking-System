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

# Coordinates x, y
crd_x = [0, 0, 0, 0, 0]
crd_y = [0, 0, 0, 0, 0]
cnt = 0
fin_x = 0
fin_y = 0
crd = [0, 0]
tmp = [0, 0]
tmp_x = 0
tmp_y = 0

exception_flag = 0

def getCoord():
    while True:
        global b_device
        global exception_flag
        global crd
        global tmp
        global fin_x
        global fin_y
        global tmp_x
        global tmp_y
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
            # tmp[0] => crd_y, tmp[1] => number of faces
        else:
            continue

        tmp_x = float(crd[0])
        tmp_y = float(tmp[0])
        time.sleep(0.1)

def opCoord():
    while True:
        global crd_x
        global crd_y
        global cnt
        global fin_x
        global fin_y
        global tmp_x
        global tmp_y

        # end='' => in python, when you use 'print', it automatically change line.
        # end='' makes to use 'print' without changing line.

        if(tmp[1] == '1'):
            crd_x[4] = 0
            crd_y[4] = 0
            crd_x[3] = 0
            crd_y[3] = 0
            crd_x[2] = 0
            crd_y[2] = 0
            crd_x[1] = 0
            crd_y[1] = 0
            crd_x[0] = tmp_x
            crd_y[0] = tmp_y
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[0])
            print("y = ", end='')
            print(crd_y[0])

        if(tmp[1] == '2'):
            crd_x[4] = 0
            crd_y[4] = 0
            crd_x[3] = 0
            crd_y[3] = 0
            crd_x[2] = 0
            crd_y[2] = 0
            if(cnt == 0):
                crd_x[0] = tmp_x
                crd_y[0] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 1):
                crd_x[1] = tmp_x
                crd_y[1] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            cnt += 1
            if(cnt > 1):
                cnt = 0

        if(tmp[1] == '3'):
            crd_x[4] = 0
            crd_y[4] = 0
            crd_x[3] = 0
            crd_y[3] = 0
            if(cnt == 0):
                crd_x[0] = tmp_x
                crd_y[0] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 1):
                crd_x[1] = tmp_x
                crd_y[1] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 2):
                crd_x[2] = tmp_x
                crd_y[2] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            cnt += 1
            if(cnt > 2):
                cnt = 0

        if(tmp[1] == '4'):
            crd_x[4] = 0
            crd_y[4] = 0
            if(cnt == 0):
                crd_x[0] = tmp_x
                crd_y[0] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 1):
                crd_x[1] = tmp_x
                crd_y[1] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 2):
                crd_x[2] = tmp_x
                crd_y[2] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 3):
                crd_x[3] = tmp_x
                crd_y[3] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            cnt += 1
            if(cnt > 3):
                cnt = 0

        if(tmp[1] == '5'):
            if(cnt == 0):
                crd_x[0] = tmp_x
                crd_y[0] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 1):
                crd_x[1] = tmp_x
                crd_y[1] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 2):
                crd_x[2] = tmp_x
                crd_y[2] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 3):
                crd_x[3] = tmp_x
                crd_y[3] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            elif(cnt == 4):
                crd_x[4] = tmp_x
                crd_y[4] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])
            cnt += 1
            if(cnt > 4):
                cnt = 0

        tmpcnt = float(tmp[1])
        if(tmpcnt == 0):
            tmpcnt = 1
        fin_x = (crd_x[0] + crd_x[1] + crd_x[2] + crd_x[3] + crd_x[4])/tmpcnt
        fin_y = (crd_y[0] + crd_y[1] + crd_y[2] + crd_y[3] + crd_y[4])/tmpcnt
        print(fin_x)
        print(fin_y)
        time.sleep(0.1)

def runStepperX():
    while True:
        global fin_x
        global fin_y
        #global fin_y
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
        #global fin_y
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

# Set getCoord to Thread
def getCoord_thread():
    thread=threading.Thread(target=getCoord)
    thread.daemon=True
    thread.start()

def opCoord_thread():
    thread=threading.Thread(target=opCoord)
    thread.daemon=True
    thread.start()

def runStepperY_thread():
    thread=threading.Thread(target=runStepperY)
    thread.daemon=True
    thread.start()

# Main
while True:
    if (b_device.exists() == True):
        getCoord_thread()
        opCoord_thread()
        runStepperY_thread()
        runStepperX()
    else:
        print("Bluetooth not connected")
        time.sleep(5)
