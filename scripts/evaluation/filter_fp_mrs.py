'''
Filter MRs that have FPs
'''

from os import makedirs, listdir
from os.path import join, isfile
from shutil import copyfile

from util.normalized_number import NormalizedNumber

def write_filtered_mrs(mrs_dir, mrs_filtered_dir, mrs_ignore):
    makedirs(mrs_filtered_dir, exist_ok=False)
    for experiment in listdir(mrs_dir):
        experiment_dir = join(mrs_dir, experiment)
        for sut in listdir(experiment_dir):
            experiment_sut_dir = join(experiment_dir, sut)
            for ir_filename in listdir(experiment_sut_dir):
                ir_filepath = join(experiment_sut_dir, ir_filename)
                if ir_filepath.endswith('.jir.txt'):
                    mr = ir_filename[:-len('.jir.txt')]
                    if mr not in mrs_ignore.get(sut, {}).get(experiment, set()):
                        or_filename = ir_filename[:-len('.jir.txt')] + '.jor.txt'
                        or_filepath = join(experiment_sut_dir, or_filename)
                        dst_dir = join(mrs_filtered_dir, experiment, sut)
                        makedirs(dst_dir, exist_ok=True)
                        copyfile(src=ir_filepath, dst=join(dst_dir, ir_filename))
                        copyfile(src=or_filepath, dst=join(dst_dir, or_filename))

def find_fp_mrs_csv(status_file):
    HEADER = ['EXPERIMENT', 'MR', 'FP', 'MS']
    with open(status_file, mode='r') as fp:
        lines = iter(fp)
        assert next(lines).strip().split(',') == HEADER, f'Invalid CSV header: {status_file}'
        for line in lines:
            data = line.strip().split(',')
            if len(data) == len(HEADER):
                fps = NormalizedNumber.parse(data[2])
                if fps.value != 0:
                    experiment = data[0]
                    mr = data[1]
                    yield experiment, mr 

def find_fp_mrs(pitest_results_dir):
    mrs_ignore = {}
    for sut in listdir(pitest_results_dir):
        mrs_ignore.setdefault(sut, {})
        status_file = join(pitest_results_dir, sut, 'mrs_status.csv')
        if isfile(status_file):
            for experiment, mr in find_fp_mrs_csv(status_file):
                mrs_ignore[sut].setdefault(experiment, set())
                mrs_ignore[sut][experiment].add(mr)
    return mrs_ignore

def find_fp_mrs_all(pitest_results_dirs):
    mrs_ignore_all = {}
    for pitest_results_dir in pitest_results_dirs:
        mrs_ignore = find_fp_mrs(pitest_results_dir)
        for sut in mrs_ignore:
            mrs_ignore_all.setdefault(sut, {})
            for experiment in mrs_ignore[sut]:
                mrs_ignore_all[sut].setdefault(experiment, set())
                mrs_ignore_all[sut][experiment] = mrs_ignore_all[sut][experiment].union(mrs_ignore[sut][experiment])
    return mrs_ignore_all

def filter_fp_mrs(mrs_dir, mrs_filtered_dir, pitest_results_dirs):
    mrs_ignore = find_fp_mrs_all(pitest_results_dirs)
    write_filtered_mrs(mrs_dir=mrs_dir, mrs_filtered_dir=mrs_filtered_dir, mrs_ignore=mrs_ignore)

def main():
    import sys
    if len(sys.argv) < 3:
        print('./filter_fp_mrs.py <MRS> <MRS_FILTERED> <PITEST_RESULTS>*')
        exit(1)
    mrs_dir = sys.argv[1]
    mrs_filtered_dir = sys.argv[2]
    pitest_results_dirs = sys.argv[3:]
    filter_fp_mrs(mrs_dir=mrs_dir, mrs_filtered_dir=mrs_filtered_dir, pitest_results_dirs=pitest_results_dirs)
