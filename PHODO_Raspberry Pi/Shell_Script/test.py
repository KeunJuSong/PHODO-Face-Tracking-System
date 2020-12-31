import serial
import time

ser = serial.Serial('/dev/rfcomm0')
ser.isOpen()

crd_x = [0, 0, 0, 0, 0]
crd_y = [0, 0, 0, 0, 0]
cnt = 0

while(1):
	coord = ser.readline()
	coord = coord.decode('utf-8')
	coord = coord[0:-2]
	print(coord)
