import serial
import threading
import time
import RPi.GPIO as GPIO

GPIO.setmode(GPIO.BOARD)

# Set Stepper motor pins to 7, 11, 13, 15 (Depending on Board Number)
ControlPin = [7, 11, 13, 15]

for pin in ControlPin:
	GPIO.setup(pin, GPIO.OUT)
	GPIO.output(pin,0)
# Set pins End.

# 4 Sequences
seq = [ [1, 0, 0, 0],
		[0, 1, 0, 0],
		[0, 0, 1, 0],
		[0, 0, 0, 1] ]

ser = serial.Serial('/dev/rfcomm0')
ser.isOpen()

# Coordinates x, y
crd_x = 0
crd_y = 0

def getCoord():
	while True:
		global crd_x
		global crd_y
		global ser
		# Read Coordinates String via Bluetooth Communication
		coord = ser.readline()
		# Pull Out Coordinates From String to float Data
		coord = coord.decode('utf-8')
		coord = coord[0:-4]
		crd = coord.split('/')
		crd_x = float(crd[0])
		crd_y = float(crd[1])
		print("x = ", end='')
		print(crd_x)
		print("y = ", end='')
		print(crd_y)

def runStepper():
	while True:
		global crd_x
		global crd_y
		global ControlPin
		global seq
		# Run Stepper Motor to Right
		if(crd_x > 1000):
			for halfstep in range(4):
				for pin in range(4):
					GPIO.output(ControlPin[pin], seq[halfstep][pin])
				time.sleep(0.0028)
			print("Turnning Right")
		# Run Stepper Motor to Left
		elif(crd_x < 500):
			for halfstep in range(4):
				for pin in range(4):
					GPIO.output(ControlPin[3-pin], seq[halfstep][pin])
				time.sleep(0.0028)
			print("Turnning Left")

# Set getCoord to Thread
def getCoord_thread():
	thread=threading.Thread(target=getCoord)
	thread.daemon=True
	thread.start()

# Main
getCoord_thread()
runStepper()
