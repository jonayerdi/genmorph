#!/usr/bin/env python3
'''
Generate main scripts for all the python files with a main function into a directory
'''

from os import listdir, makedirs
from os.path import isdir, isfile, join, relpath, split
from shutil import rmtree

RUN_SCRIPTS_MAIN = 'main'
RUN_SCRIPTS_DIR = 'run'

RUN_SCRIPT_TEMPLATE = '''#!/usr/bin/env python3

import sys
from os.path import realpath, join
sys.path.append(realpath(join(__file__, '..', '..')))

from {module} import main

if __name__ == '__main__':
    {main_fn}()
'''

def split_path(path):
    components = []
    while True:
        path, tail = split(path)
        if tail != "":
            components.append(tail)
        else :
            if path != "":
                components.append(path)
            break
    return components[::-1]

def get_module_from_path(pythonfilepath, modules_root=''):
    path = split_path(relpath(pythonfilepath, modules_root))
    return '.'.join(path)[:-len('.py')]

def write_run_script(pythonfilepath, modules_root='', main_fn=RUN_SCRIPTS_MAIN, run_scripts_dir=RUN_SCRIPTS_DIR):
    pythonfilename = split(pythonfilepath)[1]
    newpythonfilepath = join(modules_root, run_scripts_dir, pythonfilename)
    makedirs(join(modules_root, run_scripts_dir), exist_ok=True)
    module = get_module_from_path(pythonfilepath=pythonfilepath, modules_root=modules_root)
    with open(newpythonfilepath, mode='wb') as fp:
        fp.write(RUN_SCRIPT_TEMPLATE.format(module=module, main_fn=main_fn).encode())

def gen_run_scripts(modules_root='', main_fn=RUN_SCRIPTS_MAIN, run_scripts_dir=RUN_SCRIPTS_DIR):
    rmtree(join(modules_root, run_scripts_dir), ignore_errors=True)
    to_explore = [modules_root]
    while to_explore:
        path = to_explore.pop()
        if isdir(path):
            for child in listdir(path):
                to_explore.append(join(path, child))
        elif isfile(path) and path.endswith('.py'):
            with open(path, mode='r') as fp:
                if any(map(lambda l: l.startswith('def main():'), iter(fp))):
                    write_run_script(pythonfilepath=path, modules_root=modules_root, main_fn=main_fn, run_scripts_dir=run_scripts_dir)

if __name__ == '__main__':
    import sys
    if len(sys.argv) != 2:
        print('./gen_run_scripts.py <MODULES_ROOT>')
        exit(1)
    gen_run_scripts(modules_root=sys.argv[1])
