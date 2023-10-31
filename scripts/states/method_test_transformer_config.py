#!/usr/bin/env python3
'''
Utility script to run MethodTestTransformerConfig (generate method test transformations config file from constants list).

./method_test_transformer_config.py <INPUT_CONSTANTS_FILE> <OUTPUT_TRANSFORMATIONS_FILE>
'''

from os import listdir
from os.path import isfile, join
from subprocess import Popen

from config import *

from tools.java import PATH_SEPARATOR

MAIN_CLASS = 'ch.usi.methodtest.MethodTestTransformerConfig'

def merge_literals_lists(literals_files):
    literals = {}
    for literals_file in literals_files:
        with open(literals_file, mode='r') as fd:
            for line in fd:
                line = line.strip()
                if line:
                    value, count = line.split(',')
                    count = int(count)
                    literals.setdefault(value, 0)
                    literals[value] = literals[value] + count
    return literals

def merge_variables_lists(variables_files):
    variables = {}
    for variables_file in variables_files:
        with open(variables_file, mode='r') as fd:
            for line in fd:
                line = line.strip()
                if line:
                    variable, value, count = line.split(',')
                    count = int(count)
                    variables.setdefault(variable, (value, 0))
                    if variables[variable][0] != value:
                        variables[variable] = (None, None)
                    else:
                        variables[variable] = (value, variables[variable][1] + count)
    values_counts = {}
    for k in filter(lambda k: variables[k][0] is not None, variables):
        value, count = variables[k][0], variables[k][1]
        values_counts.setdefault(value, 0)
        values_counts[value] = values_counts[value] + count
    return values_counts

def merge_values_lists(*values_lists):
    values_counts = {}
    for values_list in values_lists:
        for value in values_list:
            count = values_list[value]
            values_counts.setdefault(value, 0)
            values_counts[value] = values_counts[value] + count
    return values_counts

def write_values_list(filepath, values_list):
    with open(filepath, mode='wb') as fd:
        for line in values_list:
            fd.write(f'{line}\n'.encode(encoding='utf-8'))

def merge_values_lists_dir(dir, outfile):
    all_files = list(filter(lambda f: isfile(f), map(lambda f: join(dir, f), listdir(dir))))
    literals_files = filter(lambda f: f.endswith('.literals.txt'), all_files)
    variables_files = filter(lambda f: f.endswith('.variables.txt'), all_files)
    values_counts = merge_values_lists(merge_literals_lists(literals_files), merge_variables_lists(variables_files))
    write_values_list(
        filepath=outfile,
        values_list=(f'{value},{count}' for value, count in map(lambda k: (k, values_counts[k]), values_counts))
    )

def read_values_list_file(values_list_file):
    with open(values_list_file, mode='r') as fd:
        return { 
            value: int(count) 
            for value, count in map(lambda l: l.strip().split(','), filter(lambda l: l, fd)) 
        }

def method_test_transformer_config(args, classpath=None, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS):
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
    if len(sys.argv) != 3:
        print(f'Expected 2 arguments, got {len(sys.argv)}')
        exit(1)
    method_test_transformer_config(args=sys.argv[1:])
