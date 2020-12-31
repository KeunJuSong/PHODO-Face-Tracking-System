# Import libraries
import RPi.GPIO as GPIO
import time

# Set GPIO numbering mode
GPIO.setmode(GPIO.BOARD)

# Set pin 22 as an output, and set servo1 as pin 22 as PWM
GPIO.setup(22,GPIO.OUT)
servo1 = GPIO.PWM(22,100) # Note 22 is pin, 50 = 50Hz pulse

#start PWM running, but with value of 0 (pulse off)
servo1.start(0)
print ("Waiting for 2 seconds")
time.sleep(2)

#Let's move the servo!
print ("Rotating 180 degrees in 10 steps")

# Define variable duty
duty = 7

servo1.ChangeDutyCycle(51)
time.sleep(2)

servo1.ChangeDutyCycle(30)
time.sleep(2)

servo1.ChangeDutyCycle(70)
time.sleep(2)
