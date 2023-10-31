#!/usr/bin/env python3
'''
Utility script to run MethodTestTransformer (generate follow-up test cases from .methodinputs files).

./method_test_transformer.py <TEST_INPUTS_DIR_OR_FILE> <STATE_FILE> <OUTPUT_DIR> <CLASSPATH> [TRANSFORMATIONS_FILE]
'''

from subprocess import Popen

from config import *

from tools.java import PATH_SEPARATOR

MAIN_CLASS = 'ch.usi.methodtest.MethodTestTransformer'

def method_test_transformer(args, classpath, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS):
    if type(classpath) == str:
        classpath = [classpath]
    if not classpath:
        classpath = []
    classpath = [gassert_jar, *classpath]
    classpath = PATH_SEPARATOR.join(map(realpath, classpath))
    args = ['java', '-cp', classpath, main_class, *map(str, args)]
    return Popen(args=args).wait()

def main():
    import sys
    inputs_files = sys.argv[1]
    state_file = sys.argv[2]
    output_dir = sys.argv[3]
    classpath = sys.argv[4].split(PATH_SEPARATOR)
    args = [inputs_files, state_file, output_dir]
    if len(sys.argv) > 5:
        args.append(sys.argv[5])
    method_test_transformer(args=args, classpath=classpath)
