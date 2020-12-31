# Import libraries
import RPi.GPIO as GPIO
import time

# Set GPIO numbering mode
GPIO.setmode(GPIO.BOARD)

# Set pin 22 as an output, and set servo1 as pin 22 as PWM
GPIO.setup(11,GPIO.OUT)
servo1 = GPIO.PWM(11,50) # Note 22 is pin, 50 = 50Hz pulse

#start PWM running, but with value of 0 (pulse off)
servo1.start(0)
print ("Waiting for 2 seconds")
servo1.ChangeDutyCycle(10)
time.sleep(2)

print ("Waiting for 2 seconds")
servo1.ChangeDutyCycle(12)
time.sleep(2)

#turn back to 0 degrees
print ("Turning back to 0 degrees")
servo1.ChangeDutyCycle(8)
time.sleep(2)

print ("Waiting for 2 seconds")
servo1.ChangeDutyCycle(9.8)
time.sleep(2)

#Clean things up at the end
servo1.stop()
GPIO.cleanup()
print ("Goodbye")

