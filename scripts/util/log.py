from datetime import datetime

def timestamp():
    return str(datetime.now()).split('.')[0]

def log_nothing(text, **kwargs):
    pass

def log_stdout(text, **kwargs):
    return print(f'[{timestamp()}] {text}', **kwargs)
