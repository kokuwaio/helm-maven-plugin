// verify that registry push was tried

def appFile = new File(basedir, '/target/helm/repo/app-1.0.0.tgz')
def logs = new File(basedir, 'build.log').text
assert logs.contains("push ${appFile} oci://127.0.0.1")
