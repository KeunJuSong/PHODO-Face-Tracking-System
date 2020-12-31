import serial
import time
import re

ser = serial.Serial('/dev/rfcomm0')
ser.isOpen()

cnt = 0
tmp = 0
crd = 0

while(1):
    coord = ser.readline()
    coord = coord.decode('utf-8')
    coord = coord[0:-2]
    faceNum = coord.split(':')
    # faceNum[0] => crd infos, faceNum[1] => number of faces
    fn = int(faceNum[1])
    tmp = faceNum[0].split('!')
    print("Mode : ", end='')
    print(tmp[0])
    crd = tmp[1].split('/')
    print("x : ", end='')
    print(crd[0])
    print("y : ", end='')
    print(crd[1])
    time.sleep(0.1)
