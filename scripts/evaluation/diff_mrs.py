from os import listdir
from os.path import join

def compare(f1, f2):
    with open(f1, mode='r') as fd:
        e1 = fd.read()
    with open(f2, mode='r') as fd:
        e2 = fd.read()
    return e1.strip() == e2.strip()

def diff_mrs(mrs1, mrs2):
    for experiment in listdir(mrs1):
        for sut in listdir(join(mrs1, experiment)):
            for mr in listdir(join(mrs1, experiment, sut)):
                if mr.endswith('.jir.txt'):
                    if not (compare(join(mrs1, experiment, sut, mr), join(mrs2, experiment, sut, mr)) and compare(join(mrs1, experiment, sut, mr[:-len('.jir.txt')] + '.jor.txt'), join(mrs2, experiment, sut, mr[:-len('.jir.txt')] + '.jor.txt'))):
                        mrname = mr[:-len('.jir.txt')].split('@')[1]
                        print(f'{experiment}/{sut}/{mrname}')

def main():
    import sys
    if len(sys.argv) != 3:
        print('./diff_mrs.py <MRS1> <MRS2>')
        exit(1)
    diff_mrs(sys.argv[1], sys.argv[2])
