
number = 123456789
Arr = [int(i) for i in str(number)]
#Arr = [1,2,3,4,5,6,7,8,7,6,5,4,3,2,1]
max_len = 0
palindrome = -1

def power(base, exponent):
	if (exponent == 0):
		return 1;
	if (exponent == 1):
		return base;
	return base * power(base, exponent-1);


def slice_array(start_index, end_index):
	palindrome = 0
	i = start_index
	power_of_ten = end_index - start_index
	while (i <= end_index):
		palindrome += Arr[i] * power(10, power_of_ten)
		power_of_ten -= 1
		i += 1
	return palindrome

def expand_from_center(index_l, index_r, n):
	global max_len, palindrome
	while ((index_l >= 0) and (index_r < n) and 
		(Arr[index_l] == Arr[index_r])):
		index_l -= 1
		index_r += 1
	index_l += 1
	index_r -= 1
	len = index_r - index_l + 1
	if (len > 0 and len > max_len):
		max_len = len
		palindrome = slice_array(index_l, index_r)
	return None

def find_longest_palindrome(len):
	for i in range(len):
		expand_from_center(i, i, len);

		expand_from_center(i, i+1, len);
	# base case
	expand_from_center(len-1, len-1, len);

#print power(10, 2)
#print slice_array(14, 14)
#expand_from_center(6, 6,len(Arr))

find_longest_palindrome(len(Arr))
print max_len, palindrome
