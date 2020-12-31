import pigpio
from time import sleep

pi = pigpio.pi()

val1 = 1200
pulse = 0

pi.set_servo_pulsewidth(17,   1470) # PWM off
sleep(1)

# GPIO #17 => Vertical Servo.
while True:
    pi.set_servo_pulsewidth(17,   0) # PWM off
    sleep(1)
    for i in range(600):
        pulse = 1170 + i
        pi.set_servo_pulsewidth(17, pulse) # PWM off
        sleep(0.02)
    pi.set_servo_pulsewidth(17,   0) # PWM off
    sleep(1)
