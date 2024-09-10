package io.kokuwa.maven.helm;

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.helm.pojo.ValueOverride;

@DisplayName("helm:template")
public class TemplateMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void Template(TemplateMojo mojo) {
		mojo.setSkipTemplate(false);
		assertHelm(mojo, "template src/test/resources/simple");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(TemplateMojo mojo) {
		assertHelm(mojo.setSkipTemplate(false).setSkip(true));
		assertHelm(mojo.setSkipTemplate(true).setSkip(false));
		assertHelm(mojo.setSkipTemplate(true).setSkip(true));
	}

	@DisplayName("with flag plain-http")
	@Test
	void plainHttp(TemplateMojo mojo) {
		mojo.setSkipTemplate(false);

		mojo.setPlainHttp(false);
		mojo.setTemplatePlainHttp(null);
		assertHelm(mojo, "template src/test/resources/simple");

		mojo.setPlainHttp(true);
		mojo.setTemplatePlainHttp(null);
		assertHelm(mojo, "template src/test/resources/simple --plain-http");

		mojo.setPlainHttp(false);
		mojo.setTemplatePlainHttp(true);
		assertHelm(mojo, "template src/test/resources/simple --plain-http");

		mojo.setPlainHttp(false);
		mojo.setTemplatePlainHttp(false);
		assertHelm(mojo, "template src/test/resources/simple");
	}

	@DisplayName("with values overrides")
	@Test
	void valuesFile(TemplateMojo mojo) {
		mojo.setSkipTemplate(false);
		mojo.setValues(new ValueOverride().setYamlFile("values.yaml"));
		assertHelm(mojo, "template src/test/resources/simple --values values.yaml");
	}

	@DisplayName("with additional arguments")
	@Test
	void additionalArguments(TemplateMojo mojo) {
		mojo.setSkipTemplate(false);
		mojo.setAdditionalArguments("--foo --bar");
		assertHelm(mojo, "template src/test/resources/simple --foo --bar");
	}

	@DisplayName("with --name-template")
	@Test
	void nameTemplate(TemplateMojo mojo) {
		mojo.setSkipTemplate(false);
		mojo.setTemplateNameTemplate("my-name");
		assertHelm(mojo, "template src/test/resources/simple --name-template my-name");
	}

	@DisplayName("with --output-dir")
	@Test
	void outputDirectory(TemplateMojo mojo) {
		mojo.setSkipTemplate(false);
		mojo.setTemplateOutputDir(new File("."));
		assertHelm(mojo, "template src/test/resources/simple --output-dir .");
	}

	@DisplayName("with --generate-name")
	@Test
	void genereateName(TemplateMojo mojo) {
		mojo.setSkipTemplate(false);
		mojo.setTemplateGenerateName(true);
		assertHelm(mojo, "template src/test/resources/simple --generate-name");
	}

	@DisplayName("with dependencies")
	@Test
	void dependencies(TemplateMojo mojo) {
		mojo.setSkipTemplate(false);
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		assertHelm(mojo,
				"template src/test/resources/dependencies/b",
				"template src/test/resources/dependencies/a2",
				"template src/test/resources/dependencies/a1",
				"template src/test/resources/dependencies");
	}
}
