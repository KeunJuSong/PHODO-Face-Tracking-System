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

    crd = coord.split('/')
    # crd[0] => crd_x

    tmp = crd[1].split(',')
    # tmp[0] => crd_y, tmp[1] => number of faces
    # end='' => in python, when you use 'print', it automatically change line.
    # end='' makes to use 'print' without changing line.

    if(tmp[1] == '1'):
        crd_x[0] = crd[0]
        crd_y[0] = tmp[0]
        print("Face ", end='')
        print(cnt+1)
        print("x = ", end='')
        print(crd_x[cnt])
        print("y = ", end='')
        print(crd_y[cnt])

    if(tmp[1] == '2'):
        if(cnt == 0):
            crd_x[0] = crd[0]
            crd_y[0] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 1):
            crd_x[1] = crd[0]
            crd_y[1] = tmp[0]
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
        if(cnt == 0):
            crd_x[0] = crd[0]
            crd_y[0] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 1):
            crd_x[1] = crd[0]
            crd_y[1] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 2):
            crd_x[1] = crd[0]
            crd_y[1] = tmp[0]
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
        if(cnt == 0):
            crd_x[0] = crd[0]
            crd_y[0] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 1):
            crd_x[1] = crd[0]
            crd_y[1] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 2):
            crd_x[2] = crd[0]
            crd_y[2] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 3):
            crd_x[3] = crd[0]
            crd_y[3] = tmp[0]
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
            crd_x[0] = crd[0]
            crd_y[0] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 1):
            crd_x[1] = crd[0]
            crd_y[1] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 2):
            crd_x[2] = crd[0]
            crd_y[2] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 3):
            crd_x[3] = crd[0]
            crd_y[3] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        elif(cnt == 4):
            crd_x[4] = crd[0]
            crd_y[4] = tmp[0]
            print("Face ", end='')
            print(cnt+1)
            print("x = ", end='')
            print(crd_x[cnt])
            print("y = ", end='')
            print(crd_y[cnt])
        cnt += 1
        if(cnt == 5):
            cnt = 0

    time.sleep(0.1)
