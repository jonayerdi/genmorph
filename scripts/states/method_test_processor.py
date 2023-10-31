#!/usr/bin/env python3
'''
Utility script to run MethodTestProcessor (delete duplicate and corrupt .methodinputs files).

./method_test_processor.py <TEST_INPUTS_DIR> <CLASSPATH>
'''

from subprocess import Popen

from config import *

from tools.java import PATH_SEPARATOR, ADD_OPENS

MAIN_CLASS = 'ch.usi.methodtest.MethodTestsProcessor'

def method_test_processor(args, classpath, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, add_opens=None):
    if add_opens is None:
        add_opens = ADD_OPENS
    if type(classpath) == str:
        classpath = [classpath]
    if not classpath:
        classpath = []
    classpath = [*classpath, gassert_jar] # FIXME: This means that classpath can override classes used by GAssert!
    classpath = PATH_SEPARATOR.join(map(realpath, classpath))
    args = [
        'java', *add_opens, '-cp', classpath, main_class, *map(str, args)
    ]
    return Popen(args=args).wait()

def main():
    import sys
    inputs_dir = sys.argv[1]
    classpath = sys.argv[2].split(PATH_SEPARATOR)
    method_test_processor(args=[inputs_dir], classpath=classpath)
