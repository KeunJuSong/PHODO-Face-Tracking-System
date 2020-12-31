import serial
import threading
import time
import re
import pigpio
from pathlib import Path

servo_x = pigpio.pi() # gpio 25
servo_y = pigpio.pi() # gpio 17

b_device = Path("/dev/rfcomm0")

fin_x = 0
fin_y = 0
crd = 0
tmp = 0
exception_flag = 0
bSerData = 0
bdataInfo = [0, 'S']
duty_x = 1470 # halt duty cycle
duty_y = 2100 # Perpendicular duty cycle
servo_y.set_servo_pulsewidth(17, 2100)
time.sleep(2)

def readSerialLine():
    while(1):
        global exception_flag
        global servo_x
        global fin_x
        global fin_y
        global duty_x
        global duty_y
        global tmp
        global crd
        global bSerData
        global bdataInfo
        if(b_device.exists() == True):
            # Read Coordinates String via Bluetooth Communication
            try:
                ser = serial.Serial('/dev/rfcomm0')
                bSerData = ser.readline()
                bSerData = bSerData.decode('utf-8')
                bSerData = bSerData[0:-2]
            except serial.serialutil.SerialException:
                fin_x = 500
                fin_y = 350
                duty_x = 1470
                servo_x.set_servo_pulsewidth(25, 1470)
                print('\033[31m' + "Bluetooth connection lost" + '\033[0m')
                exception_flag = 1
                time.sleep(0.5)
                break
        else:
            fin_x = 500
            servo_x.set_servo_pulsewidth(25, 1470)

        if(bSerData == 'E'):
            servo_x.set_servo_pulsewidth(25, 1470)
            continue
        bdataInfo = bSerData.split(':')
        # bdataInfo[0] => composition mode + crd info, bdataInfo[1] => distance mode
        # TODO : split bdataInfo[1] to distance mode and rail/normal mode
        tmp = bdataInfo[0].split('!')
        # tmp[0] => composition mode, tmp[1] => crd info
        crd = tmp[1].split('/')
        # crd[0] => crd_x, crd[1] => crd_y
        fin_x = float(crd[0])
        fin_y = float(crd[1])
        print('\033[33m' + "Composition Mode : ", end='')
        print(tmp[0])
        print("Distance Mode : ", end='')
        print(bdataInfo[1] + '\033[0m')
        print('\033[96m' + "x : ", end='')
        print(crd[0])
        print("y : ", end='')
        print(crd[1] + '\033[0m')

        if(tmp[0] == 'N' or tmp[0] == 'C'):
            manx_rspeed = (fin_x - 550)/10
            manx_lspeed = (fin_x - 450)/10
            # Run Servo Motor to Right
            if(fin_x > 550):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_rspeed
                #time.sleep(0.03)
            # Run Servo Motor to Left
            elif(fin_x < 450):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_lspeed
                #time.sleep(0.03)
            else:
                servo_x.set_servo_pulsewidth(25, 1470)
        elif(tmp[0] == 'L'):
            manx_rspeed = (fin_x - 384)/10
            manx_lspeed = (fin_x - 283)/5
            # Run Servo Motor to Right
            if(fin_x > 384):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_rspeed
                #time.sleep(0.03)
            # Run Servo Motor to Left
            elif(fin_x < 283):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_lspeed
                #time.sleep(0.03)
            else:
                servo_x.set_servo_pulsewidth(25, 1470)
        elif(tmp[0] == 'R'):
            manx_rspeed = (fin_x - 717)/5
            manx_lspeed = (fin_x - 616)/10
            # Run Servo Motor to Right
            if(fin_x > 717):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_rspeed
                #time.sleep(0.03)
            # Run Servo Motor to Left
            elif(fin_x < 616):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_lspeed
                #time.sleep(0.03)
            else:
                servo_x.set_servo_pulsewidth(25, 1470)

def runServo_x():
    while True:
        global servo_x
        global fin_x
        global fin_y
        global exception_flag
        global duty_x
        global duty_y
        global tmp
        global bdataInfo

        tmp = bdataInfo[0].split('!')

        if(exception_flag == 1):
            print("test")
            exception_flag = 0
            duty_x = 1470
            servo_x.set_servo_pulsewidth(25, 1470)
            time.sleep(0.5)
            break

        if(tmp[0] == 'N' or tmp[0] == 'C'):
            manx_rspeed = (fin_x - 550)/10
            manx_lspeed = (fin_x - 450)/10
            # Run Servo Motor to Right
            if(fin_x > 550):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_rspeed
                time.sleep(0.03)
            # Run Servo Motor to Left
            elif(fin_x < 450):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_lspeed
                time.sleep(0.03)
            else:
                servo_x.set_servo_pulsewidth(25, 1470)
        elif(tmp[0] == 'L'):
            manx_rspeed = (fin_x - 384)/10
            manx_lspeed = (fin_x - 283)/5
            # Run Servo Motor to Right
            if(fin_x > 384):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_rspeed
                time.sleep(0.03)
            # Run Servo Motor to Left
            elif(fin_x < 283):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_lspeed
                time.sleep(0.03)
            else:
                servo_x.set_servo_pulsewidth(25, 1470)
        elif(tmp[0] == 'R'):
            manx_rspeed = (fin_x - 717)/5
            manx_lspeed = (fin_x - 616)/10
            # Run Servo Motor to Right
            if(fin_x > 717):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_rspeed
                time.sleep(0.03)
            # Run Servo Motor to Left
            elif(fin_x < 616):
                servo_x.set_servo_pulsewidth(25, duty_x)
                duty_x = 1470 + manx_lspeed
                time.sleep(0.03)
            else:
                servo_x.set_servo_pulsewidth(25, 1470)

def runServo_y():
    while True:
        global servo_y
        #global fin_x
        global fin_y
        global exception_flag
        global duty_y
        global bSerData
        global bdataInfo

        if(duty_y > 2495):
            duty_y = 2495
        if(duty_y < 2000):
            duty_y = 2000

        if(exception_flag == 1):
            print("test")
            exception_flag = 0
            time.sleep(0.5)
            break

        if(bSerData == 'E'):
            many_uspeed = 0
            many_dspeed = 0
            continue

        if(bdataInfo[1] == 'N'):
            many_uspeed = (fin_y - 300)/20
            many_dspeed = (fin_y - 200)/20
            # Run Servo Motor to Right
            if(fin_y > 300):
                duty_y = duty_y - many_uspeed
                if(duty_y > 2495):
                    duty_y = 2495
                if(duty_y < 2000):
                    duty_y = 2000
                servo_y.set_servo_pulsewidth(17, duty_y)
                time.sleep(0.02)
            # Run Servo Motor to Left
            elif(fin_y < 200):
                duty_y = duty_y - many_dspeed
                if(duty_y > 2495):
                    duty_y = 2495
                if(duty_y < 2000):
                    duty_y = 2000
                servo_y.set_servo_pulsewidth(17, duty_y)
                time.sleep(0.02)
            else:
                time.sleep(0.1)
        elif(bdataInfo[1] == 'M1'):
            many_uspeed = (fin_y - 400)/20
            many_dspeed = (fin_y - 300)/20
            # Run Servo Motor to Right
            if(fin_y > 400):
                duty_y = duty_y - many_uspeed
                if(duty_y > 2495):
                    duty_y = 2495
                if(duty_y < 2000):
                    duty_y = 2000
                servo_y.set_servo_pulsewidth(17, duty_y)
                time.sleep(0.02)
            # Run Servo Motor to Left
            elif(fin_y < 300):
                duty_y = duty_y - many_dspeed
                if(duty_y > 2495):
                    duty_y = 2495
                if(duty_y < 2000):
                    duty_y = 2000
                servo_y.set_servo_pulsewidth(17, duty_y)
                time.sleep(0.02)
            else:
                time.sleep(0.1)
        elif(bdataInfo[1] == 'M2'):
            many_uspeed = (fin_y - 500)/20
            many_dspeed = (fin_y - 400)/20
            # Run Servo Motor to Right
            if(fin_y > 500):
                duty_y = duty_y - many_uspeed
                if(duty_y > 2495):
                    duty_y = 2495
                if(duty_y < 2000):
                    duty_y = 2000
                servo_y.set_servo_pulsewidth(17, duty_y)
                time.sleep(0.02)
            # Run Servo Motor to Left
            elif(fin_y < 400):
                duty_y = duty_y - many_dspeed
                if(duty_y > 2495):
                    duty_y = 2495
                if(duty_y < 2000):
                    duty_y = 2000
                servo_y.set_servo_pulsewidth(17, duty_y)
                time.sleep(0.02)
            else:
                time.sleep(0.1)
        elif(bdataInfo[1] == 'M3'):
            many_uspeed = (fin_y - 580)/20
            many_dspeed = (fin_y - 500)/20
            # Run Servo Motor to Right
            if(fin_y > 580):
                duty_y = duty_y - many_uspeed
                if(duty_y > 2495):
                    duty_y = 2495
                if(duty_y < 2000):
                    duty_y = 2000
                servo_y.set_servo_pulsewidth(17, duty_y)
                time.sleep(0.02)
            # Run Servo Motor to Left
            elif(fin_y < 500):
                duty_y = duty_y - many_dspeed
                if(duty_y > 2495):
                    duty_y = 2495
                if(duty_y < 2000):
                    duty_y = 2000
                servo_y.set_servo_pulsewidth(17, duty_y)
                time.sleep(0.02)
            else:
                time.sleep(0.1)

def readSerialLine_thread():
    thread = threading.Thread(target = readSerialLine)
    thread.daemon = True
    thread.start()

def runServo_y_thread():
    thread = threading.Thread(target = runServo_y)
    thread.daemon = True
    thread.start()

while(1):
    if(b_device.exists() == True):
        readSerialLine_thread()
        runServo_y()
    else:
        print("Bluetooth not connected")
        time.sleep(5)
