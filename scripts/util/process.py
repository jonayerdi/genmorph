import psutil

def kill_proc_tree(pid, including_parent=True):    
    parent = psutil.Process(pid)
    children = parent.children(recursive=True)
    if including_parent:
        try:
            parent.kill()
        except: pass
    for child in children:
        try:
            child.kill()
        except: pass
