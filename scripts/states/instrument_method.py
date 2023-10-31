#!/usr/bin/env python3
'''
Instrument the given Java file and method

./instrument_method.py backup ./demo/demo/MyClass.java MyClass sin 0 ch.usi.gassert.serialization.InputsSerializingMethodVisitor
'''

from subprocess import PIPE, Popen

from config import *

MAIN_CLASS = 'ch.usi.gassert.filechange.AddInstrumentationMethod'
SERIALIZE_INPUTS_VISITOR = 'ch.usi.gassert.serialization.InputsSerializingMethodVisitor'
SERIALIZE_STATES_VISITOR = 'ch.usi.gassert.serialization.StateSerializingMethodVisitor'
ONLINE_SERIALIZE_STATES_VISITOR = 'ch.usi.gassert.serialization.OnlineStateSerializingMethodVisitor'

def format_java_source(source_file, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS):
    return instrument_method(args=['format', source_file, '0', '0', '0'], gassert_jar=gassert_jar, main_class=main_class)

def find_instrumented_lines(source_file=None, source=None):
    if source is None:
        with open(source_file, mode='r') as fd:
            source = fd.read()
    instrumented = False
    for i, line in enumerate(source.split('\n'), start=1):
        if '/* BEGIN GAssert instrumented method */' in line:
            instrumented = True
        if '/* END GAssert instrumented method */' in line:
            instrumented = False
        if instrumented:
            yield i

def find_method_lines(source_file, method_name, method_index, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS):
    retval, stdout = instrument_method(
        args=['comments', source_file, '0', method_name, method_index], 
        gassert_jar=gassert_jar, main_class=main_class, stdout=PIPE
    )
    assert retval == 0, f'Error instrumenting file "{source_file}", method={method_name}:{method_index}'
    for i in find_instrumented_lines(source=stdout):
        yield i

def instrument_method(args, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = ['java', '-cp', gassert_jar, main_class, *map(str, args)]
    if stdout == PIPE:
        popen = Popen(args=args, stdout=PIPE, stderr=PIPE)
        stdout, stderr = popen.communicate()
        return popen.returncode, stdout.decode(encoding='utf-8')
    else:
        return Popen(args=args, stdout=stdout).wait()

def main():
    import sys
    #format_java_source(sys.argv[1])
    #print(list(find_method_lines(*sys.argv[1:])))
    instrument_method(args=sys.argv[1:])
