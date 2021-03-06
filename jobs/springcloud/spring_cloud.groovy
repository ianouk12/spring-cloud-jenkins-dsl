package springcloud

import io.springframework.cloud.ci.*
import io.springframework.cloud.compatibility.BootCompatibilityBuildMaker
import io.springframework.cloud.compatibility.ClusterCompatibilityBuildMaker
import io.springframework.cloud.compatibility.CompatibilityBuildMaker
import io.springframework.cloud.compatibility.ConsulCompatibilityBuildMaker
import io.springframework.cloud.e2e.CloudFoundryBreweryTestExecutor
import io.springframework.cloud.e2e.CloudFoundryEndToEndBuildMaker
import io.springframework.cloud.e2e.EndToEndBuildMaker
import io.springframework.cloud.e2e.SleuthEndToEndBuildMaker
import io.springframework.cloud.f2f.AppDeployingBuildMaker
import javaposse.jobdsl.dsl.DslFactory

import static io.springframework.cloud.common.AllCloudJobs.*
import static io.springframework.cloud.compatibility.CompatibilityBuildMaker.COMPATIBILITY_BUILD_DEFAULT_SUFFIX

DslFactory dsl = this

println "Projects with tests $ALL_JOBS_WITH_TESTS"
println "Projects without tests $JOBS_WITHOUT_TESTS"

// COMPATIBILITY BUILDS
(ALL_DEFAULT_JOBS).each { String projectName->
	new CompatibilityBuildMaker(dsl).build(projectName, everyThreeHours())
}
JOBS_WITHOUT_TESTS.each {
	new CompatibilityBuildMaker(dsl).buildWithoutTests(it, everyThreeHours())
}
new CompatibilityBuildMaker(dsl, COMPATIBILITY_BUILD_DEFAULT_SUFFIX, 'spring-cloud-samples')
		.build('tests', everyThreeHours())
new ConsulCompatibilityBuildMaker(dsl).build(everyThreeHours())
new ClusterCompatibilityBuildMaker(dsl).build(everyThreeHours())
new BootCompatibilityBuildMaker(dsl).build()

// BENCHMARK BUILDS
new BenchmarksBuildMaker(dsl).buildSleuth()

// CI BUILDS
new DocsAppBuildMaker(dsl).buildDocs(everyThreeHours())
new SpringCloudDeployBuildMaker(dsl).with { SpringCloudDeployBuildMaker maker ->
	(ALL_DEFAULT_JOBS).each {
		maker.deploy(it)
	}
	JOBS_WITHOUT_TESTS.each {
		maker.deployWithoutTests(it)
	}
}
new ConsulSpringCloudDeployBuildMaker(dsl).deploy()
new ClusterSpringCloudDeployBuildMaker(dsl).deploy()
// CI BUILDS FOR INCUBATOR
new VaultSpringCloudDeployBuildMaker(dsl).deploy()
// CI BUILD FOR SPRING CLOUD CONTRACTS (Codearte repo)
new SpringCloudContractDeployBuildMaker(dsl).with {
	deployMaven('spring-cloud-contract-verifier-maven-plugin', 'accurest-maven-plugin')
	deployGradle('spring-cloud-contract-verifier', 'accurest')
}


// E2E BUILDS
['spring-cloud-netflix', 'spring-cloud-zookeeper', 'spring-cloud-consul'].eachWithIndex { String projectName, int index ->
	def maker = new EndToEndBuildMaker(dsl)
	maker.build(projectName, maker.everySixHoursStartingFrom(index + 1))
}
new SleuthEndToEndBuildMaker(dsl).with {
	buildSleuth(everySixHoursStartingFrom(4))
	buildSleuthStream(everySixHoursStartingFrom(5))
	buildSleuthStreamKafka(everySixHoursStartingFrom(6))
}

// E2E on CF
new CloudFoundryEndToEndBuildMaker(dsl).with {
	buildBreweryForDocs()
	buildSleuthDocApps()
	buildSpringCloudStream()
}
new CloudFoundryBreweryTestExecutor(dsl).buildBreweryForDocsTests()

// CUSTOM E2E
// Josh's CI APP
new EndToEndBuildMaker(dsl, 'joshlong').with {
	buildWithoutTests('bootiful-microservices',
			'scripts/scenario_brixton_tester.sh',
			everySaturday(),
			'scripts/kill_all.sh')
}
new EndToEndBuildMaker(dsl, 'hecklerm').with {
	buildWithoutTests('mark-hecklers-services',
			'DemoCIProjectSuite',
			'exerciseEndpoints.sh',
			everySaturday(),
			'scripts/kill_all.sh')
}

/*// SONAR BUILDS - disabled until sonar is set
['spring-cloud-bus', 'spring-cloud-commons', 'spring-cloud-sleuth', 'spring-cloud-netflix', 'spring-cloud-zookeeper'].each {
	new SonarBuildMaker(dsl).buildSonar(it)
}
new ConsulSonarBuildMaker(dsl).buildSonar()
new ClusterSonarBuildMaker(dsl).buildSonar()*/

// F2F
new AppDeployingBuildMaker(dsl).with {
	build('marcingrzejszczak', 'atom-feed')
	build('dsyer', 'github-analytics')
}

// ========== FUNCTIONS ==========

String everyThreeHours() {
	return "H H/3 * * *"
}
