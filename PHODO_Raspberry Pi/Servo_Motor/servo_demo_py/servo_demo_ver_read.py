import pigpio
from time import sleep

pi = pigpio.pi()

while True:
    print(pi.get_servo_pulsewidth(17))
    sleep(2)
