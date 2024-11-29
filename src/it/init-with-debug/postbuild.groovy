import org.yaml.snakeyaml.Yaml

// verify that the repo is created with user and password
def repoConfig = new Yaml().load(new FileReader(new File(basedir, 'target/helm/repositories.yaml')))
def repo = repoConfig.repositories.find { it.name == 'kokuwaio-with-credentials' }

assert repo != null
assert  repo.username == 'test'
assert  repo.password == 'ChangeMe'

// verify that the password is stripped
def logs = new File(basedir, 'build.log').text
assert logs.contains('--username test --password ********')
