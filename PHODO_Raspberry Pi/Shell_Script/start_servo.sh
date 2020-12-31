#!/bin/bash

bluetoothctl discoverable on
while :
do
	sudo rfcomm watch hci0 dp12cks34 &
	# Waiting for bluetooth device to be connected in background
	# dp12cks34 => chage this to your RPi password

	python3 /home/pi/Capstone_design/Tracker_hor/multiple_tracker_servo_hor.py
	# Track face with servo motor
done
