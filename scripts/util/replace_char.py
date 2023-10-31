from os import listdir
from os.path import isdir, join
from shutil import move

def replace_char(root, char, new_char):
    new_name = root.replace(char, new_char)
    if new_name != root:
        move(src=root, dst=new_name)
    if isdir(new_name):
        for child in listdir(new_name):
            replace_char(root=join(new_name, child), char=char, new_char=new_char)

def main():
    import sys
    if len(sys.argv) != 4:
        print('./replace_char.py <ROOT> <CHAR> <NEW_CHAR>')
        exit(-1)
    root = sys.argv[1]
    char = sys.argv[2]
    new_char = sys.argv[3]
    replace_char(root=root, char=char, new_char=new_char)
