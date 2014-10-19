# Check if an array of integers is a palindrome.

number = 101010101
A = [int(i) for i in str(number)]
#A = [1,2,3,4,5,6,7,8,7,6,5,4,3,2,1]
i_A = iter(A)

def get_random():
	return next(i_A)

def check_if_palindrome(length):
	index = 0;
	is_palindrome = True;

	while (is_palindrome and index < length / 2):
	    if (A[index] != A[length-index-1]):
	    	is_palindrome = False
	    	break
	    index += 1
	return is_palindrome

length = len(A)
for i in range(length):
	temp = get_random()
	A[i] = temp

is_palindrome = check_if_palindrome(length)

print "Is", str(A), "a palindrome?"
print is_palindrome
