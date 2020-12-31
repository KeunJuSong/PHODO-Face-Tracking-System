import serial
import threading
import time
import re
import pigpio
import RPi.GPIO as GPIO
import sys
import signal
from pathlib import Path

#GPIO 핀
TRIG = 16 # 트리거
ECHO = 18 # 에코

servo_y = pigpio.pi() # gpio 25

b_device = Path("/dev/rfcomm0")

fin_x = 0
fin_y = 0
crd = 0
tmp = [0, 0]
exception_flag = 0
bSerData = 'E'
bdataInfo = [0, 'S']
duty_y = 1470 # Perpendicular duty cycle
servo_y.set_servo_pulsewidth(25, 1470)
time.sleep(2)
servo_y.set_servo_pulsewidth(25, 0)

ControlPin = [7, 11, 13, 15]
ControlPin_Rail = [29, 31, 33, 35]

seq = [ [1, 0, 0, 0],
        [1, 1, 0, 0],
	[0, 1, 0, 0],
	[0, 1, 1, 0],
	[0, 0, 1, 0],
	[0, 0, 1, 1],
	[0, 0, 0, 1],
	[1, 0, 0, 1] ]

#거리 타임 아웃 용
MAX_DISTANCE_CM = 300
MAX_DURATION_TIMEOUT = (MAX_DISTANCE_CM * 2 * 29.1) #17460 # 17460us = 300cm

distance = 0

# 키보드 CTRL + C 누르면 종료 되게 처리
def signal_handler(signal, frame):
    print('You pressed Ctrl+C!')
    GPIO.cleanup()
    sys.exit(0)
signal.signal(signal.SIGINT, signal_handler)

# cm 환산 함수
# 아두이노 UltraDistSensor 코드에서 가져옴
def distanceInCm(duration):
    # 물체에 도착후 돌아오는 시간 계산
    # 시간 = cm / 음속 * 왕복
    # t   = 0.01/340 * 2= 0.000058824초 (58.824us)

    # 인식까지의 시간
    # t = 0.01/340 = 0.000029412초 (29.412us)

    # duration은 왕복 시간이니 인식까지의 시간에서 2로 나눔
    return (duration/2)/29.1


# 거리 표시
def print_distance(distance):
    if distance == 0:
        distanceMsg = 'Distance : out of range                   \r'
    else:
        distanceMsg = 'Distance : ' + str(distance) + 'cm' + '        \r'
    sys.stdout.write(distanceMsg)
    sys.stdout.flush()

def readSerialLine():
    while(1):
        global exception_flag
        global servo_y
        global fin_x
        global fin_y
        global duty_y
        global tmp
        global crd
        global bSerData
        global bdataInfo
        if(b_device.exists() == True):
            # Read Coordinates String via Bluetooth Communication
            try:
                servo_y = pigpio.pi()
                ser = serial.Serial('/dev/rfcomm0')
                bSerData = ser.readline()
                bSerData = bSerData.decode('utf-8')
                bSerData = bSerData[0:-2]
            except serial.serialutil.SerialException:
                fin_x = 500
                fin_y = 350
                servo_y.stop()
                print('\033[31m' + "Bluetooth connection lost" + '\033[0m')
                exception_flag = 1
                time.sleep(0.5)
                break
        else:
            fin_x = 500
            servo_y.stop()

        if(bSerData == 'E'):
            servo_y.set_servo_pulsewidth(25, 1470)
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
        print('\033[33m' + "Mode : ", end='')
        print(tmp[0])
        print("Distance Mode : ", end='')
        print(bdataInfo[1] + '\033[0m')
        print('\033[96m' + "x : ", end='')
        print(crd[0])
        print("y : ", end='')
        print(crd[1] + '\033[0m')

def runStepper_x():
    while True:
        global tmp
        global halfstep
        global pin
        global ControlPin
        global ControlPin_Rail
        global seq
        global fin_x
        global distance

        if(distance > 28.4):
            distance = 0
        elif(distance < 4):
            distance = 0

        GPIO.setmode(GPIO.BOARD)

        for pin in ControlPin:
            GPIO.setup(pin, GPIO.OUT)
            GPIO.output(pin,0)
        for pin in ControlPin_Rail:
            GPIO.setup(pin, GPIO.OUT)
            GPIO.output(pin,0)

        if(bSerData == 'E'):
            pass
        else:
            if(tmp[0] == 'N' or tmp[0] == 'C'):
                manx_rspeed = (fin_x - 550)/225000
                manx_lspeed = (fin_x - 450)/225000
                # Run Stepper Motor to Right
                if(fin_x > 550):
                    for halfstep in range(8):
                        for pin in range(4):
                            GPIO.output(ControlPin[3-pin], seq[halfstep][pin])
                        time.sleep(0.0033-manx_rspeed)
                # Run Stepper Motor to Left
                elif(fin_x < 450):
                    for halfstep in range(8):
                        for pin in range(4):
                            GPIO.output(ControlPin[pin], seq[halfstep][pin])
                        time.sleep(0.0032-manx_lspeed)
                else:
                    pass
            elif(tmp[0] == 'L'):
                manx_rspeed = (fin_x - 384)/308000
                manx_lspeed = (fin_x - 283)/141500
                # Run Stepper Motor to Right
                if(fin_x > 384):
                    for halfstep in range(8):
                        for pin in range(4):
                            GPIO.output(ControlPin[3-pin], seq[halfstep][pin])
                        time.sleep(0.0033-manx_rspeed)
                # Run Stepper Motor to Left
                elif(fin_x < 283):
                    for halfstep in range(8):
                        for pin in range(4):
                            GPIO.output(ControlPin[pin], seq[halfstep][pin])
                        time.sleep(0.0032-manx_lspeed)
                else:
                    pass
            elif(tmp[0] == 'R'):
                manx_rspeed = (fin_x - 717)/141500
                manx_lspeed = (fin_x - 616)/308000
                # Run Servo Motor to Right
                if(fin_x > 717):
                    for halfstep in range(8):
                        for pin in range(4):
                            GPIO.output(ControlPin[3-pin], seq[halfstep][pin])
                        time.sleep(0.0033-manx_rspeed)
                # Run Servo Motor to Left
                elif(fin_x < 616):
                    for halfstep in range(8):
                        for pin in range(4):
                            GPIO.output(ControlPin[pin], seq[halfstep][pin])
                        time.sleep(0.0032-manx_lspeed)
                else:
                    pass
                
            elif(tmp[0] == 'RT'):
                manx_rspeed = (fin_x - 550)/225000
                manx_lspeed = (fin_x - 450)/225000
                # Run Stepper Motor to Right
                if(fin_x > 550):
                    if(distance > 4.4):
                        for halfstep in range(8):
                            for pin in range(4):
                                GPIO.output(ControlPin_Rail[pin], seq[halfstep][pin])
                            time.sleep(0.0030-manx_rspeed)
                    elif(distance == 0):
                        pass

                # Run Stepper Motor to Left
                elif(fin_x < 450):
                    if(distance < 28.0):
                        for halfstep in range(8):
                            for pin in range(4):
                                GPIO.output(ControlPin_Rail[3-pin], seq[halfstep][pin])
                            time.sleep(0.0031-manx_lspeed)
                    elif(distance == 0):
                        pass
                else:
                    pass

def runServo_y():
    while True:
        global servo_y
        #global fin_x
        global fin_y
        global exception_flag
        global duty_y
        global bSerData
        global bdataInfo

        if(duty_y > 1770):
            duty_y = 1770
        if(duty_y < 1170):
            duty_y = 1170

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
                if(duty_y > 1770):
                    duty_y = 1770
                if(duty_y < 1170):
                    duty_y = 1170
                servo_y.set_servo_pulsewidth(25, duty_y)
                time.sleep(0.02)
            # Run Servo Motor to Left
            elif(fin_y < 200):
                duty_y = duty_y - many_dspeed
                if(duty_y > 1770):
                    duty_y = 1770
                if(duty_y < 1170):
                    duty_y = 1170
                servo_y.set_servo_pulsewidth(25, duty_y)
                time.sleep(0.02)
            else:
                time.sleep(0.1)
        elif(bdataInfo[1] == 'M1'):
            many_uspeed = (fin_y - 400)/20
            many_dspeed = (fin_y - 300)/20
            # Run Servo Motor to Right
            if(fin_y > 400):
                duty_y = duty_y - many_uspeed
                if(duty_y > 1770):
                    duty_y = 1770
                if(duty_y < 1170):
                    duty_y = 1170
                servo_y.set_servo_pulsewidth(25, duty_y)
                time.sleep(0.02)
            # Run Servo Motor to Left
            elif(fin_y < 300):
                duty_y = duty_y - many_dspeed
                if(duty_y > 1770):
                    duty_y = 1770
                if(duty_y < 1170):
                    duty_y = 1170
                servo_y.set_servo_pulsewidth(25, duty_y)
                time.sleep(0.02)
            else:
                time.sleep(0.1)
        elif(bdataInfo[1] == 'M2'):
            many_uspeed = (fin_y - 500)/20
            many_dspeed = (fin_y - 400)/20
            # Run Servo Motor to Right
            if(fin_y > 500):
                duty_y = duty_y - many_uspeed
                if(duty_y > 1770):
                    duty_y = 1770
                if(duty_y < 1170):
                    duty_y = 1170
                servo_y.set_servo_pulsewidth(25, duty_y)
                time.sleep(0.02)
            # Run Servo Motor to Left
            elif(fin_y < 400):
                duty_y = duty_y - many_dspeed
                if(duty_y > 1770):
                    duty_y = 1770
                if(duty_y < 1170):
                    duty_y = 1170
                servo_y.set_servo_pulsewidth(25, duty_y)
                time.sleep(0.02)
            else:
                time.sleep(0.1)
        elif(bdataInfo[1] == 'M3'):
            many_uspeed = (fin_y - 580)/20
            many_dspeed = (fin_y - 500)/20
            # Run Servo Motor to Right
            if(fin_y > 580):
                duty_y = duty_y - many_uspeed
                if(duty_y > 1770):
                    duty_y = 1770
                if(duty_y < 1170):
                    duty_y = 1170
                servo_y.set_servo_pulsewidth(25, duty_y)
                time.sleep(0.02)
            # Run Servo Motor to Left
            elif(fin_y < 500):
                duty_y = duty_y - many_dspeed
                if(duty_y > 1770):
                    duty_y = 1770
                if(duty_y < 1170):
                    duty_y = 1170
                servo_y.set_servo_pulsewidth(25, duty_y)
                time.sleep(0.02)
            else:
                time.sleep(0.1)

def ultrasonic():
    # 파이썬 GPIO 모드
    GPIO.setmode(GPIO.BOARD)

    # 핀 설정
    GPIO.setup(TRIG, GPIO.OUT) # 트리거 출력
    GPIO.setup(ECHO, GPIO.IN)  # 에코 입력

    print('To Exit, Press the CTRL+C Keys')

    # HC-SR04 시작 전 잠시 대기
    GPIO.output(TRIG, False)
    print('Waiting For Sensor To Ready')
    time.sleep(1) # 1초

    pulse_start = 0
    pulse_end = 0

    #시작
    print('Start!!')
    while True:
        global distance
        #171206 중간에 통신 안되는 문제 개선용      
        fail = False
        time.sleep(0.1)
        # 트리거를 10us 동안 High 했다가 Low로 함.
        # sleep 0.00001 = 10us
        GPIO.output(TRIG, True)
        time.sleep(0.00001)
        GPIO.output(TRIG, False)

        # ECHO로 신호가 들어 올때까지 대기
        timeout = time.time()
        while GPIO.input(ECHO) == 0:
            #들어왔으면 시작 시간을 변수에 저장
            pulse_start = time.time()
            if ((pulse_start - timeout)*1000000) >= MAX_DURATION_TIMEOUT:
                #171206 중간에 통신 안되는 문제 개선용        
                #continue
                fail = True
                break

        #171206 중간에 통신 안되는 문제 개선용        
        if fail:
            continue
        #ECHO로 인식 종료 시점까지 대기
        timeout = time.time()
        while GPIO.input(ECHO) == 1:
            #종료 시간 변수에 저장
            pulse_end = time.time()
            if ((pulse_end - pulse_start)*1000000) >= MAX_DURATION_TIMEOUT:
                print_distance(0) 
                #171206 중간에 통신 안되는 문제 개선용        
                #continue
                fail = True
                break

        #171206 중간에 통신 안되는 문제 개선용        
        if fail:
            continue

        #인식 시작부터 종료까지의 차가 바로 거리 인식 시간
        pulse_duration = (pulse_end - pulse_start) * 1000000

        # 시간을 cm로 환산
        distance = distanceInCm(pulse_duration)
        #print(pulse_duration)
        #print('')
        # 자리수 반올림
        distance = round(distance, 2)

        #표시
        print_distance(distance)

    GPIO.cleanup()

def readSerialLine_thread():
    thread = threading.Thread(target = readSerialLine)
    thread.daemon = True
    thread.start()

def runStepper_x_thread():
    thread = threading.Thread(target = runStepper_x)
    thread.daemon = True
    thread.start()

def ultrasonic_thread():
    thread = threading.Thread(target = ultrasonic)
    thread.daemon = True
    thread.start()

while(1):
    if(b_device.exists() == True):
        readSerialLine_thread()
        ultrasonic_thread()
        runStepper_x_thread()
        runServo_y()
    else:
        GPIO.cleanup()
        print("Bluetooth not connected")
        time.sleep(5)
