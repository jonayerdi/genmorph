import sys
import glob
import os
from os.path import join, split, splitdrive, realpath
import time
import shutil
from datetime import datetime
import subprocess
from threading import Timer
import csv
import argparse

OASIS_JAR = join('libs', 'oasis', 'build', 'libs', 'OASIs-1.0-SNAPSHOT-all.jar')
OASIS_MAIN_CLASS = 'ch.usi.oasis.OASIs'

if sys.platform == 'win32':
    PATH_SEPARATOR = ';'
elif sys.platform.startswith('linux'):
    PATH_SEPARATOR = ':'
else:
    raise Exception('Unknown platform')

def split_path(path, state=None):
    if state is None:
        state = []
    if not path:
        return state[::-1]
    head, tail = split(path)
    if tail:
        state.append(tail)
    elif head:
        drive, rest = splitdrive(head)
        if drive:
            state.append(drive)
        else:
            state.append(rest)
        head = ''
    return split_path(path=head, state=state)

def get_java_url_path(root):
    '''
    This function generates paths that should work for ClassLoader URLs
    '''
    parts = split_path(realpath(root))
    path = '/'.join(parts)
    if parts[0] == '/':
        # Avoid double leading '/'s on unix paths: //home/...
        path = path[1:]
    return path

def get_file_prefix_list(mr_dir):
    prefix_list = set()
    listfiles = glob.glob(mr_dir + "*")
    listfiles.sort()
    for outer_dir in listfiles:
        for inner_dir in glob.glob(outer_dir + os.sep + "*"):
            for file in glob.glob(inner_dir + os.sep + "*"):
                if (not 'oasis_test' in file):
                    prefix = file[0:file.rindex('.txt') - 4]
                    prefix_list.add(prefix)

    prefix_list = list(prefix_list)
    prefix_list.sort()
    return prefix_list


def main():
    parser = argparse.ArgumentParser(description='OASIS Runs')
    parser.add_argument('--mr_dir', '-md', default='experiment_results/mrsNoFP/', help='The directory containing generated metamorphic relationships')
    parser.add_argument('--oasis_jar_location', '-ojl', default=OASIS_JAR, help='The jar of OASIs')
    parser.add_argument('--subject_root', '-sr', default=join('experiment_results/oasis/subject_root'), help='Root directory of the subject project')  #
    args = parser.parse_args()

    mr_dir = args.mr_dir
    jar_location = args.oasis_jar_location
    subject_root = get_java_url_path(args.subject_root)

    srcFileLocation = os.path.join(subject_root, "src", "main", "java")
    binFileLocation = os.path.join(subject_root, "src", "main", "java")
    execLogFile = "exec_log.txt"
    overallBudget = 600
    timeout = 200
    fpBudget = 150

    fnBudget = "0"
    mode = "FP"
    debug = "false"

    prefix_list = get_file_prefix_list(mr_dir)
    print("number of MRs:", len(prefix_list))

    for prefix in prefix_list:
        guava_loc = os.path.join(subject_root, "src", "main", "java", "GuavaClass.java")
        lang_loc = os.path.join(subject_root, "src", "main", "java", "LangClass.java")

        os.remove(guava_loc)
        os.remove(lang_loc)

        shutil.copy(os.path.join("experiment_results", "oasis", "classes_clean_copy", "GuavaClass.java"), guava_loc);
        shutil.copy(os.path.join("experiment_results", "oasis", "classes_clean_copy", "LangClass.java"), lang_loc);

        dir = os.path.split(prefix)[0]
        short_name = os.path.split(os.path.split(prefix)[0])[1]
        short_name_split = short_name.split("?")
        print(short_name_split)

        methodName = short_name_split[1]
        className = short_name_split[0]
        testFileLocation = os.path.join(dir, "oasis_test", os.path.split(prefix)[1])
        csvFileLocation = os.path.join(testFileLocation, "exec_history.csv")

        print("testFileLocation", testFileLocation)
#        if os.path.exists(testFileLocation):
#            shutil.rmtree(testFileLocation)
#        continue
        if not os.path.exists(testFileLocation):
            os.makedirs(testFileLocation)

            if short_name_split[2] != "0":
                print("non-zero:", short_name_split[2])

            inputRel = prefix + ".jir.txt"
            outputRel = prefix + ".jor.txt"

            budget = overallBudget
            with open(csvFileLocation, 'w') as f:
                # create the csv writer
                writer = csv.writer(f)
                #while budget > 30:
                while budget == overallBudget:
                    start_time = datetime.now()

                    if budget < fpBudget:
                        fpBudget = int(budget)
                        print("budget swap:", fpBudget)

                    classSource = className.replace(".", "/") + ".java"
                    OASIsSourceFiles = subject_root + "/oasis_src/"
                    sourceFile = subject_root + "/src/main/java/" + classSource
                    OASIsSourceFile = OASIsSourceFiles + classSource
                    shutil.rmtree(OASIsSourceFiles, ignore_errors=True)
                    os.makedirs(split(OASIsSourceFile)[0])
                    shutil.copyfile(sourceFile, OASIsSourceFile)

                    oasis_cp = [OASIsSourceFiles, subject_root + "/target/classes/", jar_location]
                    oasis_cp = PATH_SEPARATOR.join(map(realpath, oasis_cp))

                    cmd = [
                        'java', '-cp', oasis_cp, OASIS_MAIN_CLASS, subject_root, className, methodName,
                        inputRel, outputRel, testFileLocation, str(int(fpBudget)), debug,
                    ]

                    kill = lambda process: process.kill()
                    ping = subprocess.Popen(
                        cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                    my_timer = Timer(timeout, kill, [ping])
                    try:
                        my_timer.start()
                        stdout, stderr = ping.communicate()
                    finally:
                        my_timer.cancel()

                    end_time = datetime.now()

                    difference = (end_time - start_time)
                    total_seconds = difference.total_seconds()

                    ts = time.time()

                    test_suite_loc = os.path.join(testFileLocation, className.replace(".", os.sep) + "_ESTest.java")
                    if os.path.exists(test_suite_loc):
                        os.rename(test_suite_loc, os.path.join(testFileLocation, className.replace(".", os.sep) + str(ts) + "_ESTest.java"))

                    evo_out_loc = os.path.join(testFileLocation, "evo_out.txt")
                    if os.path.exists(evo_out_loc):
                        shutil.move(evo_out_loc, os.path.join(testFileLocation, "evo_out" + str(ts) + ".txt"))

                    evo_err_loc = os.path.join(testFileLocation, "evo_err.txt")
                    if os.path.exists(evo_err_loc):
                        shutil.move(evo_err_loc, os.path.join(testFileLocation, "evo_err" + str(ts) + ".txt"))

                    if os.path.exists(execLogFile):
                        shutil.move(execLogFile, os.path.join(testFileLocation, "exec_log" + str(ts) + ".txt"))
                    else:
                        print("no exec log file")

                    budget = budget - total_seconds
                    print(start_time, end_time, total_seconds, budget)
                    writer.writerow([ts, start_time, end_time, total_seconds, budget])

                    #read test_suite file
                    fileLoc = os.path.join(testFileLocation, className.replace(".", os.sep) + str(ts) + "_ESTest.java")
                    test_data = ""
                    if os.path.exists(fileLoc):
                        with open(fileLoc, "r") as f:
                            test_data = f.read().replace("\n", "")

                        if 'public void test0()' in test_data:
                            print("we already have FP")
                            break

if __name__ == '__main__':
    main()
