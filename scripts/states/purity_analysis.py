#!/usr/bin/env python3
'''
Method purity analysis for a class file.
'''

from subprocess import Popen, PIPE

from config import *
from tools.java import PATH_SEPARATOR

MAIN_CLASS = 'ch.usi.staticanalysis.PurityAnalysis'

def purity_analysis(args, cp, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = ['java', '-cp', PATH_SEPARATOR.join((gassert_jar, cp)), main_class, *map(str, args)]
    if stdout == PIPE:
        popen = Popen(args=args, stdout=PIPE, stderr=PIPE)
        stdout, stderr = popen.communicate()
        return popen.returncode, stdout.decode(encoding='utf-8')
    else:
        return Popen(args=args, stdout=stdout).wait()

def main():
    import sys
    purity_analysis(cp=sys.argv[1], args=sys.argv[2:])
