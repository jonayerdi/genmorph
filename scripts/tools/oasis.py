from os.path import join, realpath
from subprocess import Popen

from tools.java import PATH_SEPARATOR

OASIS_JAR = join('libs', 'oasis', 'build', 'libs', 'OASIs-1.0-SNAPSHOT-all.jar')
OASIS_MAIN_CLASS = 'ch.usi.oasis.OASIs'

def run_oasis_process(
        subject_root, class_name, method_name, 
        irs_file, ors_file, test_file_dir,
        shell=False, cwd=None,
):
    cp = [
        subject_root + '/oasis_src/',
        subject_root + '/target/classes/',
        OASIS_JAR,
    ]
    command = [
        f'java',
        f'-cp', PATH_SEPARATOR.join(map(realpath, cp)),
        OASIS_MAIN_CLASS,
        subject_root, class_name, method_name,
        irs_file, ors_file, test_file_dir,
    ]
    return Popen(shell=shell, cwd=cwd, args=command).wait()

def main():
    import sys
    run_oasis_process(*sys.argv[1:])
