// verify that the helm directory not exist
assert !new File(basedir, 'src/main/helm/charts').exists()
assert !new File(basedir, 'src/main/helm/Chart.lock').exists()

def logs = new File(basedir, 'build.log').text

// verify that goal was execute
assert logs.contains("Cleanups chart ${basedir}")
