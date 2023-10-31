#!/usr/bin/env python3
'''
List methods in a class file.
'''

from subprocess import Popen, PIPE

from config import *

MAIN_CLASS = 'ch.usi.gassert.filechange.ListMethods'

def list_methods_java_file(source_file, container=list):
    returncode, stdout = list_methods(args=[source_file], stdout=PIPE)
    assert returncode == 0, 'Error executing list_methods'
    return container(filter(lambda l: l, map(lambda l: l.strip(), stdout.splitlines(keepends=False))))

def list_methods(args, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = ['java', '-cp', gassert_jar, main_class, *map(str, args)]
    if stdout == PIPE:
        popen = Popen(args=args, stdout=PIPE, stderr=PIPE)
        stdout, stderr = popen.communicate()
        return popen.returncode, stdout.decode(encoding='utf-8')
    else:
        return Popen(args=args, stdout=stdout).wait()

def main():
    import sys
    list_methods(args=sys.argv[1:])
