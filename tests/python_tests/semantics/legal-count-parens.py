def count_paren(n):
	s = 0;
	if (n == 0 or n == 1):
		return 1;
	else:
		for i in range(n):
			s += count_paren(i) * count_paren(n-i-1)
	return s;

print count_paren(6)