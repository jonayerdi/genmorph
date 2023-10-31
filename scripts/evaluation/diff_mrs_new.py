from os import listdir
from os.path import join, isfile

def diff_mrs_new(mrs1, mrs2):
    for experiment in listdir(mrs2):
        for sut in listdir(join(mrs2, experiment)):
            for mr in listdir(join(mrs2, experiment, sut)):
                if mr.endswith('.jir.txt'):
                    if not isfile(join(mrs1, experiment, sut, mr)):
                        mrname = mr[:-len('.jir.txt')].split('@')[1]
                        print(f'{experiment}/{sut}/{mrname}')

def main():
    import sys
    if len(sys.argv) != 3:
        print('./diff_mrs_new.py <MRS1> <MRS2>')
        exit(1)
    diff_mrs_new(sys.argv[1], sys.argv[2])
