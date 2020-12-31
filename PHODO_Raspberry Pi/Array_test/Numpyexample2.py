# Python program to dmeonstrate 
# Structured array 

import numpy as np

a = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

a[0] = np.array([(100, 200, 1)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])
a[1] = np.array([(200, 400, 2)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])
a[2] = np.array([(300, 600, 3)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])
a[3] = np.array([(400, 800, 4)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])
a[4] = np.array([(500, 1000, 5)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])
a[5] = np.array([(600, 1200, 6)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])
a[6] = np.array([(700, 1400, 7)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])
a[7] = np.array([(800, 1600, 8)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])
a[8] = np.array([(900, 1800, 9)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])
a[9] = np.array([(1000, 2000, 10)], dtype=[('crd_x', (np.int32)), ('crd_y', np.int32), ('id', np.int32)])

for i in range(9):
    if a[i]['id'] == 5:
        b = a[i]['crd_x'] + 100
        print(b)
