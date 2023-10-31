from os import listdir, makedirs, remove
from os.path import join, split, isdir, isfile, splitdrive
from random import Random
from shutil import copyfile, move

def listdir_flat(root):
    '''
    List all files under `root`, exploring subdirectories recursively.

    Yields paths relative to `root`.
    '''
    to_explore = [root]
    while to_explore:
        current = to_explore.pop()
        if isdir(current):
            for child in listdir(current):
                to_explore.append(join(current, child))
        yield current

def isnonemptydir(dir):
    if isdir(dir):
        for child in listdir(dir):
            childpath = join(dir, child)
            if not isdir(childpath) or isnonemptydir(childpath):
                return True
    return False

def combine_directories(target_dir, other_dirs, overwrite=False):
    '''
    Combine files from `other_dirs` into `target_dir`.
    
    `target_dir` itself may contain some files.
    '''
    for dir in other_dirs:
        for child in listdir(dir):
            src = join(dir, child)
            if isfile(src):
                dst = join(target_dir, child)
                assert overwrite or not isfile(dst), f'Destination {dst} already exists'
                copyfile(src=src, dst=dst)

def find_file(dir, condition=lambda file, filename: True):
    for filename in listdir(dir):
        file = join(dir, filename)
        if condition(file, filename):
            return file
    return None

def moveall(src, dst):
    assert isdir(src)
    makedirs(dst, exist_ok=True)
    assert isdir(dst)
    for child in listdir(src):
        move(src=join(src, child), dst=dst)

def sample_dir(dir, samples, rng=Random(x=0), condition=lambda _: True):
    files = list(filter(condition, listdir(dir)))
    remove_count = len(files) - samples
    if remove_count > 0:
        for f in rng.sample(population=files, k=remove_count):
            remove(join(dir, f))

def split_path(path, state=None):
    if state is None:
        state = []
    if not path:
        return state[::-1]
    head, tail = split(path)
    if tail:
        state.append(tail)
    elif head:
        drive, rest = splitdrive(head)
        if drive:
            state.append(drive)
        else:
            state.append(rest)
        head = ''
    return split_path(path=head, state=state)

def read_lines(filepath, linemap=lambda l: l.strip(), linefilter=lambda l: l) -> list:
    with open(filepath, mode='r') as fp:
        return list(filter(linefilter, map(linemap, iter(fp))))
