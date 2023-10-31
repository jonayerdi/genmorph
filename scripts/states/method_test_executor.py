#!/usr/bin/env python3
'''
Utility script to run MethodTestExecutor.

./method_test_executor.py <TEST_INPUTS_DIR_OR_FILE> <CLASSPATH>
'''

from os import cpu_count, listdir
from os.path import isfile, splitext, join
from subprocess import Popen

from config import *

from tools.java import PATH_SEPARATOR, ADD_OPENS
from util.popen_executor import PopenExecutor

MAIN_CLASS = 'ch.usi.methodtest.MethodTestExecutor'
IS_METHOD_INPUTS_FILE = lambda f: f.endswith('.methodinputs')

def method_test_executor_dir_processes(dir, classpath, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, workers=None, add_opens=None):
    '''
    Run MethodTestExecutor for each test input in `dir` in separate parallel processes
    '''
    if workers is None:
        workers = max(cpu_count() - 1, 1)
    if add_opens is None:
        add_opens = ADD_OPENS
    if type(classpath) == str:
        classpath = [classpath]
    if not classpath:
        classpath = []
    classpath = [*classpath, gassert_jar] # FIXME: This means that classpath can override classes used by GAssert!
    classpath = PATH_SEPARATOR.join(map(realpath, classpath))
    popens = PopenExecutor(workers=workers)
    for child in listdir(dir):
        test_file = join(dir, child)
        if isfile(test_file) and IS_METHOD_INPUTS_FILE(child):
            popens.wait_for_worker()
            args = [
                'java', *add_opens, '-cp', classpath, main_class, test_file
            ]
            popens.submit(Popen(args=args))
    popens.wait_for_all()
    return 0 # I'm sure everything worked fine :)

def method_test_executor(args, classpath, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, add_opens=None):
    if add_opens is None:
        add_opens = ADD_OPENS
    if type(classpath) == str:
        classpath = [classpath]
    if not classpath:
        classpath = []
    classpath = [gassert_jar, *classpath]
    classpath = PATH_SEPARATOR.join(map(realpath, classpath))
    args = [
        'java', *add_opens, '-cp', classpath, main_class, *map(str, args)
    ]
    return Popen(args=args).wait()

def main():
    import sys
    inputs_files = sys.argv[1]
    classpath = sys.argv[2].split(PATH_SEPARATOR)
    method_test_executor(args=[inputs_files], classpath=classpath)
