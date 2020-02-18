import groovy.json.JsonSlurper
import hudson.AbortException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.RuleChain
import util.*

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

class NexusUploadTest extends BasePiperTest {
    private ExpectedException exception = ExpectedException.none()

    private JenkinsCredentialsRule credentialsRule = new JenkinsCredentialsRule(this)
    private JenkinsReadJsonRule readJsonRule = new JenkinsReadJsonRule(this)
    private JenkinsShellCallRule shellCallRule = new JenkinsShellCallRule(this)
    private JenkinsStepRule stepRule = new JenkinsStepRule(this)
    private JenkinsWriteFileRule writeFileRule = new JenkinsWriteFileRule(this)
    private JenkinsFileExistsRule fileExistsRule = new JenkinsFileExistsRule(this, [])

    private List withEnvArgs = []

    @Rule
    public RuleChain rules = Rules
        .getCommonRules(this)
        .around(exception)
        .around(new JenkinsReadYamlRule(this))
        .around(credentialsRule)
        .around(readJsonRule)
        .around(shellCallRule)
        .around(stepRule)
        .around(writeFileRule)
        .around(fileExistsRule)

    @Before
    void init() {
        helper.registerAllowedMethod('fileExists', [Map], {
            return true
        })
        helper.registerAllowedMethod("readJSON", [Map], { m ->
            if (m.file == 'nexusUpload_reports.json')
                return [[target: "1234.pdf", mandatory: true]]
            if (m.file == 'nexusUpload_links.json')
                return []
            if (m.text != null)
                return new JsonSlurper().parseText(m.text)
        })
        helper.registerAllowedMethod("withEnv", [List.class, Closure.class], {arguments, closure ->
            arguments.each {arg ->
                withEnvArgs.add(arg.toString())
            }
            return closure()
        })
        credentialsRule.withCredentials('idOfCxCredential', "admin", "admin123")
        shellCallRule.setReturnValue('./piper getConfig --contextConfig --stepMetadata \'metadata/nexusUpload.yaml\'',
            '{"nexusCredentialsId": "idOfCxCredential", "verbose": false, ' +
            ' "url": "localhost:8081", "repository": "maven-releases", "version": "1.0", ' +
            ' "groupId": "org", "artifacts": ' +
            '    [{ "artifactId": "blob", ' +
            '       "classifier": "blob-1.0", ' +
            '       "type": "pom", ' +
            '       "file": "pom.xml"}] ' +
            '}'
        )
    }

    @Test
    void testDeployPom() {
        stepRule.step.nexusUpload(
            juStabUtils: utils,
            jenkinsUtilsStub: jenkinsUtils,
            testParam: "This is test content",
            script: nullScript,
        )
        // asserts
        assertThat(writeFileRule.files['metadata/nexusUpload.yaml'], containsString('name: nexusUpload'))
        assertThat(withEnvArgs[0], allOf(startsWith('PIPER_parametersJSON'), containsString('"testParam":"This is test content"')))
        assertThat(shellCallRule.shell[1], is('./piper nexusUpload'))
    }
}