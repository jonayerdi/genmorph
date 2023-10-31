#!/usr/bin/env python3
'''
Utility script to batch rename method test files.

./method_test_rename.py <TEST_INPUTS_DIR>
'''

from os import listdir
from os.path import exists, isfile, join, split, splitext
from shutil import move
from tempfile import TemporaryDirectory

from states.method_test_executor import IS_METHOD_INPUTS_FILE

from config import SEPARATORS

def get_system_id(filepath):
    return splitext(split(filepath)[1])[0].split(SEPARATORS[0])[0]

def update_test_id(old_filepath, new_test_id):
    path, old_filename = split(old_filepath)
    old_filename, extension = splitext(old_filename)
    system_id, _old_test_id = old_filename.split(SEPARATORS[0])
    new_filename = f'{system_id}{SEPARATORS[0]}{new_test_id}{extension}'
    return join(path, new_filename)

def make_gen_test_id(template='test{index}', start=0):
    index = start
    while True:
        yield template.format(index=index)
        index += 1

def method_test_rename(tests_dir, gen_test_ids=make_gen_test_id):
    gen_system_test_id = {}
    with TemporaryDirectory() as tmpdir:
        for child in listdir(tests_dir):
            filepath = join(tests_dir, child)
            if isfile(filepath) and IS_METHOD_INPUTS_FILE(filepath):
                system_id = get_system_id(filepath)
                gen_system_test_id.setdefault(system_id, gen_test_ids())
                test_id = next(gen_system_test_id[system_id])
                new_filepath = update_test_id(filepath, test_id)
                if filepath != new_filepath:
                    if exists(new_filepath):
                        # Move file with the same name as the new filename into tmpdir
                        move(src=new_filepath, dst=tmpdir)
                    move(src=filepath, dst=new_filepath)
        for child in listdir(tmpdir):
            # Assign test_ids and move back the tests previously moved into tmpdir
            filepath = join(tmpdir, child)
            system_id = get_system_id(filepath)
            gen_system_test_id.setdefault(system_id, gen_test_ids())
            test_id = next(gen_system_test_id[system_id])
            new_filepath = update_test_id(filepath, test_id)
            move(src=filepath, dst=new_filepath)

def main():
    import sys
    tests_dir = sys.argv[1]
    method_test_rename(tests_dir=tests_dir)
