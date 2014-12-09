import numpy as np
fname = 'output_derby.txt'

with open(fname, "r") as f:
    f.seek (0, 2)           # Seek @ EOF
    fsize = f.tell()        # Get Size
    f.seek (max (fsize-1024, 0), 0) # Set pos @ last n chars
    lines = f.readlines()       # Read to end

#lines = lines[-4:]    # Get last 10 lines
unopt_times = []
opt_times = []
for line in lines:
  if line.startswith("Unoptimized:"):
    unopt_times.append(int(line.split()[1]))
  if line.startswith("Fullopt:"):
    opt_times.append(int(line.split()[1]))

improvements = [1.0 * (unopt_time - opt_time) / unopt_time * 100.0 for (unopt_time, opt_time) in zip(unopt_times, opt_times)]

print "Unoptimized: {} us".format(np.mean(unopt_times))
print "Optimized:   {} us".format(np.mean(opt_times))
print "Improvement: {}%".format(round(np.mean(improvements),4))

