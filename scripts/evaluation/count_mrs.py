from os import listdir
from os.path import join

def count_mrs(mrs_dir):
    count = 0
    for experiment in listdir(mrs_dir):
        experiment_dir = join(mrs_dir, experiment)
        for sut in listdir(experiment_dir):
            experiment_sut_dir = join(experiment_dir, sut)
            for ir_filename in listdir(experiment_sut_dir):
                if ir_filename.endswith('.jir.txt'):
                    count += 1
    return count

def main():
    import sys
    if len(sys.argv) != 2:
        print('./count_mrs.py <MRS_PATH>')
        exit(1)
    mrs_dir = sys.argv[1]
    print(count_mrs(mrs_dir))
