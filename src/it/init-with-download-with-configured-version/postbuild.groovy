// verify that configured (and outdated) version was used

def logs = new File(basedir, 'build.log').text

assert logs.contains('Downloading Helm: https://get.helm.sh/helm-v3.12.0-')
