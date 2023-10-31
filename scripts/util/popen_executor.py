from time import sleep

class PopenExecutor:

    def __init__(self, workers, interval_secs=.05):
        assert workers > 0, f'Number of workers must be at least 1, got {workers}'
        self.popens = set()
        self.workers = workers
        self.interval_secs = interval_secs

    def submit(self, popen):
        self.popens.add(popen)

    def wait_for_any(self):
        if len(self.popens) > 0:
            while True:
                for popen in self.popens:
                    retval = popen.poll()
                    if retval is not None:
                        self.popens.remove(popen)
                        return retval
                sleep(self.interval_secs)
        return None

    def wait_for_worker(self):
        while len(self.popens) >= self.workers:
            self.wait_for_any() 

    def wait_for_all(self):
        while len(self.popens) > 0:
            self.wait_for_any()
