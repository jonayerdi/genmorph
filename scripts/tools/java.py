#!/usr/bin/env python3
'''
Utilities to do Java stuff
'''

import os
import sys
from os import listdir
from os.path import isdir, isfile, join, realpath, relpath
from subprocess import Popen
from zipfile import ZipFile

if sys.platform == 'win32':
    PATH_SEPARATOR = ';'
elif sys.platform.startswith('linux'):
    PATH_SEPARATOR = ':'
else:
    PATH_SEPARATOR = None

# Needed for some reflection stuff (Gson, XStream) in newer Java versions (16?),
# but unneeded and unsupported in Java 8
USE_ADD_OPENS = False
ADD_OPENS = [
    '--add-opens', 'java.base/java.lang=ALL-UNNAMED',
    '--add-opens', 'java.base/java.lang.reflect=ALL-UNNAMED',
    '--add-opens', 'java.base/java.util=ALL-UNNAMED',
    '--add-opens', 'java.base/java.text=ALL-UNNAMED',
    '--add-opens', 'java.base/java.io=ALL-UNNAMED',
    '--add-opens', 'java.desktop/java.awt.font=ALL-UNNAMED',
] if USE_ADD_OPENS else []

def copy_jar_class(classes, jar, path):
    if type(classes) is str:
        classes = [classes]
    with ZipFile(jar, mode='r') as zf:
        for clazz in classes:
            zf.extract(member=java_class_to_filepath(clazz=clazz, ext='.class'), path=path)

def java_class_to_simple_name(clazz):
    return clazz.split('.')[-1]

def java_class_to_filepath(clazz, ext):
    return join(*clazz.split('.')) + ext

def find_classes_in_package(root, package):
    path = join(*package.split('.'))
    dir = join(root, path)
    for file in listdir(dir):
        if isfile(join(dir, file)) and file.endswith('.java'):
            clazz_name = file[:-len('.java')]
            yield f'{package}.{clazz_name}'

def find_all_source_files(root_dir):
    '''
    Utility function to find all java source files in subdirectories of root_dir
    '''
    directories = [root_dir]
    while directories:
        directory = directories.pop()
        for child in listdir(directory):
            child_path = join(directory, child)
            if isdir(child_path):
                directories.append(child_path)
            elif isfile(child_path) and child.endswith('.java'):
                yield relpath(child_path, root_dir)

def java_proc(main_class, classpath=None, args=[], workdir=None, jvm_args=[], **kwargs):
    '''
    Utility function to run java (blocking)
    '''
    command=['java']
    if type(classpath) == str:
        classpath = [classpath]
    command.extend(jvm_args)
    if classpath:
        command.extend(['-cp', PATH_SEPARATOR.join(map(realpath, classpath))])
    command.extend([main_class, *args])
    return Popen(cwd=workdir, args=command ,**kwargs)

def java(main_class, classpath=None, args=[], workdir=None, jvm_args=[]):
    '''
    Utility function to run java (blocking)
    '''
    return java_proc(main_class=main_class, classpath=classpath, args=args, workdir=workdir, jvm_args=jvm_args).wait()

def javac(source_files, classpath=None, outdir=None, workdir=None):
    '''
    Utility function to run javac (blocking)
    '''
    command=['javac']
    if type(source_files) == str:
        source_files = [source_files]
    if type(classpath) == str:
        classpath = [classpath]
    if classpath:
        command.extend(['-cp', PATH_SEPARATOR.join(classpath)])
    if outdir:
        command.extend(['-d', outdir])
    command.extend(source_files)
    return Popen(cwd=workdir, args=command).wait()

class JavaVersion:
    '''
    Utility class to temporarily switch Java versions
    '''
    def __init__(self, javapath):
        if (javapath is not None) and (PATH_SEPARATOR is None):
            raise Exception(f'Not implemented for "{sys.platform}" platform')
        self.javapath = javapath
        self.old_javapath = None
        self.switched = False
    def switch(self):
        if self.javapath is not None and not self.switched:
            self.old_javapath = os.environ.get('JAVA_HOME', default=None)
            if self.old_javapath is not None:
                os.environ['JAVA_HOME'] = self.javapath
            os.environ['PATH'] = f"{join(self.javapath, 'bin')}{PATH_SEPARATOR}{os.environ['PATH']}"
            self.switched = True
        return self
    def reset(self):
        if self.javapath is not None and self.switched:
            if self.old_javapath is not None:
                os.environ['JAVA_HOME'] = self.old_javapath
            os.environ['PATH'] = PATH_SEPARATOR.join(os.environ['PATH'].split(PATH_SEPARATOR)[1:])
            self.old_javapath = None
            self.switched = False
    def __enter__(self): 
        return self.switch()
    def __exit__(self, type, value, traceback): 
        self.reset()
