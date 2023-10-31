import sys

with open(sys.argv[1], mode='r') as fd:
    for line in fd.readlines():
        percent = float(line.strip()) * 100
        print('{0:.2f}'.format(percent) + '\\%')
