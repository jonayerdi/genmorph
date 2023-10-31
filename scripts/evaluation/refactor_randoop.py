#!/usr/bin/env python3
'''
Refactor multiple Evosuite testsuites into different classnames
'''

from os import listdir, makedirs
from os.path import join ,split, splitext, isdir, isfile

JAVA_IDENTIFIER_CHARS = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_'
def is_broken_identifier(before, after):
    return before[-1] in JAVA_IDENTIFIER_CHARS or after[0] in JAVA_IDENTIFIER_CHARS

def rename_identifier(code, old, new):
    result = ''
    parts = code.split(old)
    for i in range(len(parts)):
        result += parts[i]
        if i + 1 == len(parts):
            break
        if is_broken_identifier(before=parts[i], after=parts[i+1]):
            result += old
        else:
            result += new
    return result

def rename_identifiers(code, rename):
    result = code
    for old in rename:
        result = rename_identifier(code=result, old=old, new=rename[old])
    return result

def refactor_code(infile, outfile, clazz_renames):
    with open(infile, mode='r') as infp, open(outfile, mode='wb') as outfp:
        for line in infp:
            # Rename class
            line = f'{rename_identifiers(code=line, rename=clazz_renames)}'
            # Write line
            outfp.write(line.encode('utf-8'))

def find_test_classes(root):
    search = [root]
    while search:
        current = search.pop()
        for child in listdir(current):
            c = join(current, child)
            if isdir(c):
                search.append(c)
            elif isfile(c):
                p, f = split(c)
                if f.startswith('RegressionTest') and f.endswith('.java') and not f.endswith('Driver.java'):
                    yield c

def refactor_randoop(in_dir, out_dir):
    makedirs(out_dir, exist_ok=True)
    for test_source in find_test_classes(in_dir):
        path, source = split(test_source)
        clazz, _ = splitext(source)
        i = 0
        new_clazz = None
        new_source = None
        while new_clazz is None or isfile(new_source):
            i += 1
            new_clazz = f'{clazz}_{i}'
            new_source = join(out_dir, f'{new_clazz}.java')
        refactor_code(infile=test_source, outfile=new_source, clazz_renames={clazz: new_clazz})

def main():
    import sys
    import re
    if len(sys.argv) != 3:
        print('./refactor_randoop.py <IN_DIR> <OUT_DIR>')
        exit(1)
    in_dir = sys.argv[1]
    out_dir = sys.argv[2]
    refactor_randoop(in_dir=in_dir, out_dir=out_dir)
