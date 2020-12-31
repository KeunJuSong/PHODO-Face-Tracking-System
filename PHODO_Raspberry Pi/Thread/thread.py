import threading
from time import sleep

a = 1

def hello_1():
	while True:
		global a
		print(a)
		sleep(1)

def hello_2():
	while True:
		global a
		a+=1
		sleep(1)

def hello_1_thread():
	thread=threading.Thread(target=hello_1)
	thread.daemon=True #프로그램 종료시 프로세스도 함께 종료 (백그라운드 재생 X)
	thread.start()


hello_1_thread()
hello_2()
