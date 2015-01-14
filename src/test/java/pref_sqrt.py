from java.lang import *

now = float(str(System.currentTimeMillis()))
result = 0.0
for i in range(1, 1000000):
	result += Math.sqrt(i)
	
then = float(str(System.currentTimeMillis()))
print("after " + str(then - now) + " ms " + str(result))