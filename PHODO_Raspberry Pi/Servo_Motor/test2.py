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
servo1.ChangeDutyCycle(14)
time.sleep(2)

#turn back to 0 degrees
print ("Turning back to 0 degrees")
servo1.ChangeDutyCycle(4)
time.sleep(0.5)
servo1.ChangeDutyCycle(14)

#Clean things up at the end
servo1.stop()
GPIO.cleanup()
print ("Goodbye")

