#!/usr/bin/env python3
'''
Utility script to run Randoop
'''

import re
from os import listdir
from os.path import realpath, join
from subprocess import Popen

from tools.java import PATH_SEPARATOR

DEFAULT_RANDOOP_HOME = '.'# join('C:/', 'tools', 'randoop-4.3.0')
DEFAULT_RANDOOP_JAR = 'randoop-all-4.3.0.jar'
RANDOOP_MAIN_CLASS = 'randoop.main.Main'

REGRESSION_TEST_DRIVER_CLASS = 'RegressionTestDriver'
REGEX_REGRESSION_TEST = re.compile(r'^RegressionTest\d+\.java$')

def list_test_files(dir):
    for filename in listdir(dir):
        if REGEX_REGRESSION_TEST.match(filename) is not None:
            yield filename

def strip_assertions(test_files):
    if type(test_files) is not str:
        for test_file in test_files:
            strip_assertions(test_file)
    else:
        test_suite = ''
        with open(test_files, mode='r') as fp:
            test_suite = fp.read()
        with open(test_files, mode='w') as fp:
            for line in test_suite.splitlines(keepends=True):
                if not line.strip().startswith('org.junit.Assert'):
                    fp.write(line)

def gen_testcases(clazz, classpath, seed, budget=600, max_tests=100000000, workdir=None, assertions=False, literals=None, randoop_home=DEFAULT_RANDOOP_HOME, randoop_jar=DEFAULT_RANDOOP_JAR):
    if classpath is None:
        classpath = ''
    elif type(classpath) is not str:
        classpath = PATH_SEPARATOR.join(map(realpath, classpath))
    args = [
        'gentests', f'--testclass={clazz}',
        f'--randomseed={seed}', f'--time-limit={budget}', f'--output-limit={max_tests}',
        '--usethreads=true', '--call-timeout=3000',
        #'--deterministic',
        '--no-error-revealing-tests=true',
        '--check-compilable=true',
        '--forbid-null=true',
        '--literals-level=ALL',
        '--junit-reflection-allowed=false', # This generates a main function for the test classes, so JUnit is not needed
    ]
    if literals:
        args.append(f'--literals-file={literals}')
    return run_randoop_process(workdir, classpath, randoop_home, randoop_jar, *args)

import sys
RANDOOP_SHELL = sys.platform == 'win32'

def run_randoop_process(workdir, classpath, randoop_home, randoop_jar, *args):
    if classpath is None:
        classpath = []
    elif type(classpath) is str:
        classpath = classpath.split(PATH_SEPARATOR)
    classpath = PATH_SEPARATOR.join([*map(realpath, classpath), realpath(join(randoop_home, randoop_jar))])
    return Popen(shell=RANDOOP_SHELL, cwd=workdir, args=['java', '-cp', classpath, RANDOOP_MAIN_CLASS, *map(str, args)]).wait()

def main():
    import sys
    import os
    randoop_home = DEFAULT_RANDOOP_HOME
    randoop_jar = DEFAULT_RANDOOP_JAR
    if 'RANDOOP_HOME' in os.environ:
        randoop_home = os.environ['RANDOOP_HOME']
    if 'RANDOOP_JAR' in os.environ:
        randoop_jar = os.environ['RANDOOP_JAR']
    classpath = sys.argv[1]
    randoop_args = sys.argv[2:]
    if randoop_args[0] == 'strip':
        strip_assertions(test_files=classpath)
    elif randoop_args[0] == 'class':
        gen_testcases(clazz=randoop_args[1], classpath=classpath, seed=1, budget=60, workdir=None, randoop_home=randoop_home, randoop_jar=randoop_jar)
    else:
        run_randoop_process(None, classpath, randoop_home, randoop_jar, *randoop_args)
