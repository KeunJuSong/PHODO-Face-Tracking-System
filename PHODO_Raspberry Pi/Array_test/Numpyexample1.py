# Python program to dmeonstrate 
# Structured array 

import numpy as np

a = np.array([('Sana', 2, 21.0), ('Mansi', 7, 29.0)],
                dtype=[('name', (np.str_, 10)), ('age', np.int32), ('weight', np.float64)])

print(a)
