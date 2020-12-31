import serial
import threading
import time
import RPi.GPIO as GPIO
from pathlib import Path

# Set GPIO numbering mode
GPIO.setmode(GPIO.BOARD)

# Set pin 22 as an output, and set servo1 as pin 22 as PWM
# You can connect servos to I/O pins including 7, 11, 12, 13, 15, 16, 18 & 22
GPIO.setup(22,GPIO.OUT)
servo1 = GPIO.PWM(22,100) # Note 22 is pin, 100 = 100Hz pulse

servo1.start(0)
time.sleep(1)

b_device = Path("/dev/rfcomm0")

# Coordinates x, y
crd_x = [0, 0, 0, 0, 0]
crd_y = [0, 0, 0, 0, 0]
cnt = 0
fin_x = 0
fin_y = 0
tmp = [0, 0]
duty = 14

exception_flag = 0

def getCoord():
    while True:
        global b_device
        global crd_x
        global crd_y
        global cnt
        global fin_x
        global fin_y
        global exception_flag
        if(b_device.exists() == True):
            # Read Coordinates String via Bluetooth Communication
            try:
                ser = serial.Serial('/dev/rfcomm0')
                coord = ser.readline()
                coord = coord.decode('utf-8')
                coord = coord[0:-2]
            except serial.serialutil.SerialException:
                print("Bluetooth connection lost - getCoord")
                exception_flag = 1
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
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])

        elif(tmp[1] == '2'):
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
            if(cnt == 2):
                cnt = 0

        elif(tmp[1] == '3'):
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
                crd_x[1] = tmp_x
                crd_y[1] = tmp_y
                print("Face ", end='')
                print(cnt+1)
                print("x = ", end='')
                print(crd_x[cnt])
                print("y = ", end='')
                print(crd_y[cnt])

            cnt += 1
            if(cnt == 3):
                cnt = 0

        elif(tmp[1] == '4'):
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
            if(cnt == 4):
                cnt = 0

        elif(tmp[1] == '5'):
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
            if(cnt == 5):
                cnt = 0

        tmpcnt = float(tmp[1])
        # Calculate the Average of Coordinates
        fin_x = (crd_x[0] + crd_x[1] + crd_x[2] + crd_x[3] + crd_x[4])/tmpcnt
        fin_y = (crd_y[0] + crd_y[1] + crd_y[2] + crd_y[3] + crd_y[4])/tmpcnt

def runServo():
    while True:
        global servo1
        global fin_x
        #global fin_y
        global exception_flag
        global duty

        manx_rspeed = (fin_x - 600)/600
        manx_lspeed = (fin_x - 400)/600

        if(exception_flag == 1):
            print("test")
            exception_flag = 0
            servo1.ChangeDutyCycle(14)
            time.sleep(0.5)
            break

        # Run Servo Motor to Right
        if(fin_x > 600):
            servo1.ChangeDutyCycle(duty)
            time.sleep(0.1)
            duty = 14 + manx_rspeed
            # Run Servo Motor to Left
        elif(fin_x < 400):
            servo1.ChangeDutyCycle(duty)
            time.sleep(0.1)
            duty = 14 + manx_lspeed
        else:
            duty = 14
            servo1.ChangeDutyCycle(duty)
            time.sleep(0.1)

# Set getCoord to Thread
def getCoord_thread():
    thread=threading.Thread(target=getCoord)
    thread.daemon=True
    thread.start()

# Main
while True:
    if (b_device.exists() == True):
        getCoord_thread()
        runServo()
    else:
        print("Bluetooth not connected")
        print("Sleep 5 Seconds")
        time.sleep(5)

