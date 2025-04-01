# https://gist.github.com/vwxyzjn/c054bae6dfa6f80e6c663df70347e238
import site;
import os
import glob
for f in glob.glob(os.path.join(site.getsitepackages()[0], "jep/libjep.*")):
    print(f)