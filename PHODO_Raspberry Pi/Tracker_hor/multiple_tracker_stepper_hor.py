import serial
import threading
import time
import RPi.GPIO as GPIO
from pathlib import Path

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

b_device = Path("/dev/rfcomm0")

# Coordinates x, y
crd_x = [0, 0, 0, 0, 0]
crd_y = [0, 0, 0, 0, 0]
cnt = 0
fin_x = 0
fin_y = 0
tmp = [0, 0]

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
			except serial.serialutil.SerialException as e:
				print(e)
				print("Bluetooth connection lost")
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
			if(cnt == 2):
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
			if(cnt == 4):
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
			if(cnt == 5):
				cnt = 0

		tmpcnt = float(tmp[1])
		fin_x = (crd_x[0] + crd_x[1] + crd_x[2] + crd_x[3] + crd_x[4])/tmpcnt
		fin_y = (crd_y[0] + crd_y[1] + crd_y[2] + crd_y[3] + crd_y[4])/tmpcnt

def runStepper():
	while True:
		global fin_x
		#global fin_y
		global ControlPin
		global seq
		global exception_flag

		if(exception_flag == 1):
			exception_flag = 0
			break

		# Run Stepper Motor to Right
		if(fin_x > 1000):
			for halfstep in range(4):
				for pin in range(4):
					GPIO.output(ControlPin[pin], seq[halfstep][pin])
				time.sleep(0.0028)
		# Run Stepper Motor to Left
		elif(fin_x < 500):
			for halfstep in range(4):
				for pin in range(4):
					GPIO.output(ControlPin[3-pin], seq[halfstep][pin])
				time.sleep(0.0028)

# Set getCoord to Thread
def getCoord_thread():
	thread=threading.Thread(target=getCoord)
	thread.daemon=True
	thread.start()

# Main
while True:

	if (b_device.exists() == True):
		getCoord_thread()
		runStepper()
	else:
		print("Bluetooth not connected")
		time.sleep(5)
			
