import serial
import time

ser = serial.Serial('/dev/rfcomm0')
ser.isOpen()

crd_x = [0, 0, 0, 0, 0]
crd_y = [0, 0, 0, 0, 0]
cnt = 0

while(1):
    coord = ser.readline()
    print(coord);
    time.sleep(0.01)
