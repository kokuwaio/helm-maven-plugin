package io.kokuwa.maven.helm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.helm.pojo.ValueOverride;

@DisplayName("helm:upgrade")
public class UpgradeMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void upgrade(UpgradeMojo mojo) {
		mojo.setSkipUpgrade(false);
		mojo.setReleaseName("foo");
		assertHelm(mojo, "upgrade foo src/test/resources/simple --install");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(UpgradeMojo mojo) {
		assertHelm(mojo.setSkipUpgrade(false).setSkip(true));
		assertHelm(mojo.setSkipUpgrade(true).setSkip(false));
		assertHelm(mojo.setSkipUpgrade(true).setSkip(true));
	}

	@DisplayName("with flag dry-run")
	@Test
	void dryRun(UpgradeMojo mojo) {
		mojo.setSkipUpgrade(false);
		mojo.setReleaseName("foo");
		mojo.setUpgradeDryRun(true);
		assertHelm(mojo, "upgrade foo src/test/resources/simple --install --dry-run");
	}

	@DisplayName("with flag force")
	@Test
	void force(UpgradeMojo mojo) {
		mojo.setSkipUpgrade(false);
		mojo.setReleaseName("foo");
		mojo.setUpgradeForce(true);
		assertHelm(mojo, "upgrade foo src/test/resources/simple --install --force");
	}

	@DisplayName("with flag atomic")
	@Test
	void atomic(UpgradeMojo mojo) {
		mojo.setSkipUpgrade(false);
		mojo.setReleaseName("foo");
		mojo.setUpgradeWithInstall(false);
		mojo.setUpgradeAtomic(true);
		assertHelm(mojo, "upgrade foo src/test/resources/simple --atomic");
	}

	@DisplayName("with flags atomic and timeout")
	@Test
	void atomicAndTimeout(UpgradeMojo mojo) {
		mojo.setSkipUpgrade(false);
		mojo.setReleaseName("foo");
		mojo.setUpgradeWithInstall(false);
		mojo.setUpgradeAtomic(true);
		mojo.setUpgradeTimeout(30);
		assertHelm(mojo, "upgrade foo src/test/resources/simple --atomic --timeout 30s");
	}

	@DisplayName("without flag install")
	@Test
	void install(UpgradeMojo mojo) {
		mojo.setSkipUpgrade(false);
		mojo.setReleaseName("foo");
		mojo.setUpgradeWithInstall(false);
		assertHelm(mojo, "upgrade foo src/test/resources/simple");
	}

	@DisplayName("with values overrides")
	@Test
	void valuesFile(UpgradeMojo mojo) {
		mojo.setSkipUpgrade(false);
		mojo.setReleaseName("foo");
		mojo.setValues(new ValueOverride().setYamlFile("bar.yaml"));
		assertHelm(mojo, "upgrade foo src/test/resources/simple --values bar.yaml --install");
	}
}
